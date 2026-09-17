/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2025, Bart Naudts, https://github.com/CodeLaser/maddi
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 * more details. You should have received a copy of the GNU Lesser General Public
 * License along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package io.codelaser.maddi.kotlin.k2

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import io.codelaser.maddi.cst.api.element.JavaDoc
import io.codelaser.maddi.cst.api.element.Source
import io.codelaser.maddi.cst.api.info.FieldInfo
import io.codelaser.maddi.cst.api.info.Info
import io.codelaser.maddi.cst.api.info.MethodInfo
import io.codelaser.maddi.cst.api.info.TypeInfo
import io.codelaser.maddi.cst.api.runtime.Runtime
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.kdoc.parser.KDocKnownTag
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.kdoc.psi.impl.KDocLink
import org.jetbrains.kotlin.kdoc.psi.impl.KDocSection
import org.jetbrains.kotlin.kdoc.psi.impl.KDocTag
import org.jetbrains.kotlin.psi.KtFile
import java.util.IdentityHashMap

/**
 * Where a Kotlin member's text names a project declaration, recorded on the member's own source
 * ([io.codelaser.maddi.cst.api.element.DetailedSources.Builder.putReference]). The Kotlin CST is desugared and drops
 * many references an editor must update (imports, annotations, calls inside a lambda passed to a library function,
 * ...); K2 resolves all of them, so every project reference in a member is recorded there, whether or not the CST
 * also has an element for it. "Who names X" is then the dependency graph's question: it reads these as edges.
 *
 * Shared by the [KotlinScan]s of one project, because a reference crosses source sets: detekt-core names
 * detekt-api's members. It holds
 * - **targets**: the CST [Info] each declaration's PSI became (functions, properties, constructors, classes);
 * - **hosts**: the declarations that receive the records -- a source-level function, property, constructor, class
 *   or file. A reference is recorded on its innermost host. Local functions and classes, lambdas and `object`
 *   expressions are not hosts: what is written in them is recorded on the member they are written in.
 *
 * Records are attached before a host commits, so a host must stay open until every target exists: [KotlinScan]
 * defers the commit of the hosts it converts in pass B1 to [KotlinScan.recordReferences], and attaches to the
 * constructors, classes and facades as they commit in pass B2.
 */
internal class KotlinReferenceRegistry {
    private val targetOf = IdentityHashMap<PsiElement, Info>()
    private val hostOf = IdentityHashMap<PsiElement, Info>()
    private val recorded = IdentityHashMap<Info, MutableList<Pair<Info, Source>>>()

    // a declaration's KDoc, and the links in it that name project declarations: its JavaDoc, set in attach
    private val kdocOf = IdentityHashMap<Info, KDoc>()
    private val docTags = IdentityHashMap<Info, MutableList<JavaDoc.Tag>>()

    /** The URLs of every Kotlin file converted so far, across source sets: what [KotlinReferenceWalker] calls project. */
    val projectFiles: MutableSet<String> = HashSet()

    // not a reference: the `$default` synthetic of each function or constructor that declares a default value, by the
    // declaration's PSI. A call that omits an argument calls it, from any source set, hence kept here.
    private val defaultsOf = IdentityHashMap<PsiElement, MethodInfo>()

    fun target(psi: PsiElement?, info: Info) {
        if (psi != null) targetOf[psi] = info
    }

    fun defaults(declaration: PsiElement, method: MethodInfo) {
        defaultsOf[declaration] = method
    }

    /** The `$default` synthetic of the function or constructor [declaration], or null if it declares no default. */
    fun defaultsOf(declaration: PsiElement?): MethodInfo? = declaration?.let { defaultsOf[it] }

    fun host(psi: PsiElement?, info: Info) {
        if (psi != null) hostOf[psi] = info
    }

    /**
     * Records every project reference of [ktFile] on its innermost host, and every KDoc with the project declarations
     * its links name on the declaration it documents.
     */
    fun KaSession.record(runtime: Runtime, ktFile: KtFile, walker: KotlinReferenceWalker,
                         javaTarget: KaSession.(KaSymbol) -> Info? = { null }) {
        with(walker) {
            walk(ktFile) { reference, symbols ->
                val host = hostFor(reference) ?: return@walk
                val identifier = sourceOf(runtime, reference.getReferencedNameElement(), "-")
                // a declaration in Java source has no PSI of ours: the Java front end's Info for it
                val targets = symbols.mapNotNull { symbol ->
                    declarationPsi(symbol)?.let { targetOf[it] } ?: javaTarget(symbol)
                }.distinct()
                targets.forEach { target -> recorded.getOrPut(host) { ArrayList() } += target to identifier }
            }
            for (kdoc in PsiTreeUtil.findChildrenOfType(ktFile, KDoc::class.java)) {
                kdoc.getOwner()?.let { documented(it) }?.let { kdocOf[it] = kdoc }
            }
            walkDocs(ktFile) { name, symbols ->
                val owner = PsiTreeUtil.getParentOfType(name, KDoc::class.java)?.getOwner()?.let { documented(it) }
                    ?: return@walkDocs
                val link = PsiTreeUtil.getParentOfType(name, KDocLink::class.java)
                val tag = PsiTreeUtil.getParentOfType(name, KDocTag::class.java)
                // the subject of a block tag (`@see x`), else a link in running text (`[x]`)
                val blockTag = tag != null && tag !is KDocSection && link != null && tag.getSubjectLink() == link
                val identifierOfTag = if (!blockTag) JavaDoc.TagIdentifier.LINK else when (tag?.knownTag) {
                    KDocKnownTag.SEE -> JavaDoc.TagIdentifier.SEE
                    KDocKnownTag.THROWS, KDocKnownTag.EXCEPTION -> JavaDoc.TagIdentifier.THROWS
                    KDocKnownTag.PARAM -> JavaDoc.TagIdentifier.PARAM
                    else -> JavaDoc.TagIdentifier.UNKNOWN_BLOCK_TAG
                }
                val content = link?.getLinkText() ?: name.text
                val source = sourceOf(runtime, link ?: name, "-")
                val nameSource = nameSourceOf(runtime, name) // the segment itself: `subConfig` of `Config.subConfig`
                symbols.mapNotNull { symbol -> declarationPsi(symbol)?.let { targetOf[it] } }.distinct().forEach { target ->
                    docTags.getOrPut(owner) { ArrayList() } +=
                        runtime.newJavaDocTag(identifierOfTag, content, target, source, nameSource, blockTag)
                }
            }
        }
    }

    /** The Info a KDoc on [declaration] belongs to: what the declaration became, else (a delegated property) its host. */
    private fun documented(declaration: PsiElement): Info? = targetOf[declaration] ?: hostOf[declaration]

    private fun hostFor(element: PsiElement): Info? {
        var p: PsiElement? = element
        while (p != null) {
            hostOf[p]?.let { return it }
            p = p.parent
        }
        return null
    }

    /** Puts the records of [host] on its source, and its KDoc; to be called while its builder is still open. */
    fun attach(runtime: Runtime, host: Info) {
        kdocOf.remove(host)?.let { kdoc ->
            // as Java's: the running text for the comment, every resolved link a tag whose sourceOfReference is the
            // name it spells. Only links to project declarations are tags: the rest resolve to nothing here.
            val javaDoc = runtime.newJavaDoc(sourceOf(runtime, kdoc, "-"), kdoc.getDefaultSection().getContent(),
                docTags.remove(host).orEmpty())
            when (host) {
                is MethodInfo -> host.builder().setJavaDoc(javaDoc)
                is FieldInfo -> host.builder().setJavaDoc(javaDoc)
                is TypeInfo -> host.builder().setJavaDoc(javaDoc)
                else -> {}
            }
        }
        val records = recorded.remove(host) ?: return
        // a file facade has no declaration of its own: its records ride on a source without a position
        val source = host.source() ?: runtime.noSource()
        val dsb = runtime.newDetailedSourcesBuilder()
        records.forEach { (target, identifier) -> dsb.putReference(target, identifier) }
        val references = dsb.build()
        val merged = source.withDetailedSources(source.detailedSources()?.merge(references) ?: references)
        when (host) {
            is MethodInfo -> host.builder().setSource(merged)
            is FieldInfo -> host.builder().setSource(merged)
            is TypeInfo -> host.builder().setSource(merged)
            else -> error("not a reference host: $host")
        }
    }
}
