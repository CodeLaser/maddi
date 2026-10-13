#!/usr/bin/env python3
"""
The JDK hints' nullability, generated from JSpecify's annotated JDK (https://github.com/jspecify/jdk).

See maddi-aapi-archive/src/main/java/io/codelaser/maddi/aapi/archive/jdk/JDK-NULLNESS.md for what is taken, what is
left out and why, and the numbers. In short, for every public or protected member of a JDK type in a package that has a
hint file (jdk/Java*.java; AWT and Swing excluded) and that JSpecify declares:

- a @Nullable return becomes maddi @Nullable, except where the null can only come from an argument or a function
  (PASS_NAMES, PASS_SIGS): the nullability pass follows those per call;
- a @Nullable parameter becomes @Nullable; a non-null one (in @NullMarked code, not a type variable) @NotNull;
- non-null returns are not written: the pass seeds nothing from them;
- equals/hashCode/toString and Object's final, clone and finalize methods are skipped;
- an annotation already in the hints wins; a disagreement is listed (conflicts.txt) for review;
- a member the hints do not shadow gets a declaration from javap (the running JDK must be the recorded release,
  jdk/jdk-release.txt); a type they do not shadow gets a shadow class.

Then the hand corrections at the end (POST) are applied.

USAGE
    python3 jspecify-jdk-nullness.py <jspecify-jdk checkout> [--write]
        the checkout needs src/java.base, src/java.logging and src/java.net.http (a sparse, blob-less clone is
        enough); without --write it reports what it would change. Run from the maddi repository root.
        Generated against jspecify/jdk 6e433342 (2026-10-13).
"""
import sys,re,glob,os,collections,subprocess,tempfile

HERE=os.path.dirname(os.path.abspath(__file__))
H='maddi-aapi-archive/src/main/java/io/codelaser/maddi/aapi/archive/jdk'
JDK,WRITE=sys.argv[1],'--write' in sys.argv
D=tempfile.mkdtemp(prefix='jspecify-jdk-nullness-')

def run(*args):
    subprocess.run(list(args),check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)

# 1. JSpecify's declarations
dumps=[]
for m in ('java.base','java.logging','java.net.http'):
    out=f'{D}/js-{m}.tsv'; run('java','-Xmx2g',f'{HERE}/JSpecifyJdkDump.java',f'{JDK}/src/{m}/share/classes',out); dumps.append(out)
open(f'{D}/jspecify-jdk.tsv','w').write(''.join(sorted(l for f in dumps for l in open(f))))
# 2. the hint files' shadows, with source offsets
os.makedirs(f'{D}/idx')
for f in sorted(glob.glob(f'{H}/Java*.java')):
    b=os.path.basename(f)[:-5]
    if b=='JavaAwt' or b.startswith('JavaxSwing'): continue
    run('java','-Xmx1g',f'{HERE}/HintIndex.java',f,f'{D}/idx/{b}.tsv')
# 3. the JDK types of those packages, and their signatures
pk=set()
for f in glob.glob(f'{D}/idx/*.tsv'):
    for l in open(f):
        if l.startswith('OUTER'): pk.add(l.rstrip('\n').split('\t')[3])
types=set()
for l in open(f'{D}/jspecify-jdk.tsv'):
    t=l.split('\t')[0]; best=None
    for p in pk:
        if t.startswith(p+'.') and t[len(p)+1:][0].isupper() and (best is None or len(p)>len(best)): best=p
    if best: types.add((best,t))
open(f'{D}/types.txt','w').write('\n'.join(f'{p}\t{t}' for p,t in sorted(types)))
os.makedirs(f'{D}/javap')
names=sorted(p+'.'+t[len(p)+1:].replace('.','$') for p,t in types)
with open(f'{D}/javap/all.txt','w') as out:
    for i in range(0,len(names),80):
        out.write(subprocess.run(['javap','-protected']+names[i:i+80],capture_output=True,text=True).stdout)

# 4. generate

FUNCTIONAL={'Supplier','Function','BiFunction','UnaryOperator','BinaryOperator','Callable','IntFunction','LongFunction',
            'DoubleFunction','ToIntFunction','Consumer','BiConsumer','Predicate','BiPredicate','Runnable'}
passthrough=[]
# returns null only when an argument is (or a function returns) null: the pass follows the argument per call
PASS_NAMES={'orElse','orElseGet','getOrDefault','requireNonNullElse','requireNonNullElseGet','cast','compute',
            'computeIfAbsent','merge'}
PASS_SIGS={'java.lang.System.getProperty(String,String)','java.util.Properties.getProperty(String,String)',
           'java.lang.Integer.getInteger(String,Integer)','java.lang.Long.getLong(String,Long)',
           'java.util.Objects.toString(Object,String)','java.nio.charset.Charset.forName(String,Charset)',
           'java.io.ObjectInputStream.GetField.get(String,Object)'}
# null only if a null was written: a class's readObject reads back what its writeObject wrote. As @Nullable, guava's
# serialization proxies seeded null into ForwardingMap.delegate() and from there into ~170 non-null positions
TRUSTED_RETURNS={'java.io.ObjectInputStream.readObject()','java.io.ObjectInputStream.readUnshared()',
                 'java.io.ObjectInput.readObject()'}
SKIP_METHODS={'equals','hashCode','toString','clone','finalize','getClass','notify','notifyAll','wait'}

def strip_generics(s):
    out='';d=0
    for c in s:
        if c=='<': d+=1
        elif c=='>': d-=1
        elif d==0: out+=c
    return out
def split_top(s):
    out=[];d=0;cur=''
    for c in s:
        if c=='<': d+=1
        elif c=='>': d-=1
        if c==',' and d==0: out.append(cur.strip()); cur=''
        else: cur+=c
    if cur.strip(): out.append(cur.strip())
    return out
def simple(t): return re.split(r'[.$]',t)[-1]
def tparams_map(tp, outer):
    m=dict(outer)
    if not tp: return m
    for p in split_top(tp.strip()[1:-1]):
        parts=p.split(' extends ',1)
        name=parts[0].strip()
        m[name]= erase(parts[1].split('&')[0].strip(), m) if len(parts)>1 else 'Object'
    return m
def erase(t,tv):
    t=t.strip(); arr=0
    if t.endswith('...'): t=t[:-3]; arr+=1
    t=strip_generics(t).strip()
    while t.endswith('[]'): t=t[:-2]; arr+=1
    base=tv.get(t, simple(t))
    return base+'[]'*arr

# --- javap
JP={}   # binary-ish fqn with dots -> (header tparams, members list)
cur=None
for l in open(f'{D}/javap/all.txt'):
    l=l.rstrip('\n')
    m=re.match(r'^(?:public |protected )?(?:abstract |final |static |sealed |non-sealed |strictfp )*(class|interface|enum|record|@interface) ([\w.$]+)(<.*?>)?(?: extends| implements| permits| \{)',l)
    if m:
        cur=m.group(2).replace('$','.'); JP[cur]=(m.group(3) or '',[]); continue
    if cur is None or not l.strip().endswith(';') or '(' not in l: continue
    s=re.sub(r' throws .*;$',';',l.strip())
    mc=re.match(r'(public|protected) ([\w.$]+)\((.*)\);$',s)
    if mc and mc.group(2).replace('$','.')==cur:
        JP[cur][1].append(dict(static=False, tp='', rt=None, name='<init>', params=split_top(mc.group(3)), ctor=True)); continue
    mm=re.match(r'(public|protected) ((?:static |final |abstract |synchronized |native |default |strictfp )*)(<.+?> )?(?:([\w.$<>\[\]?, ]+?) )?([\w$]+)\((.*)\);$',s)
    if not mm: continue
    vis,mods,tp,rt,name,ps=mm.groups()
    JP[cur][1].append(dict(static='static' in mods, tp=(tp or '').strip(), rt=rt, name=name, params=split_top(ps), ctor=rt is None))

def jp_members(t):
    if t not in JP: return {}
    ctp,ms=JP[t]; outer=tparams_map(ctp,{})
    res={}
    for m in ms:
        tv=tparams_map(m['tp'],outer)
        name='<init>' if m['ctor'] else m['name']
        key=(name, ','.join(erase(p,tv) for p in m['params']))
        res.setdefault(key,m)
    return res

# --- JSpecify
JS=collections.defaultdict(list)
for l in open(f'{D}/jspecify-jdk.tsv'):
    t,n,ps,r,pst,sc,rt=l.rstrip('\n').split('\t')
    JS[t].append((n,ps,r,pst,sc,rt))
TYPES=collections.defaultdict(list)
for l in open(f'{D}/types.txt'):
    p,t=l.rstrip('\n').split('\t'); TYPES[p].append(t)

def default(rt):
    if rt=='void': return ''
    if rt=='boolean': return 'return false; '
    if rt in ('int','long','short','byte','char','double','float'): return 'return 0; '
    return 'return null; '
ANN={'Q':'@Nullable','N':'@NotNull'}
report=collections.Counter(); conflicts=[]; nomatch=[]
for idxf in sorted(glob.glob(f'{D}/idx/*.tsv')):
    base=os.path.basename(idxf)[:-4]; hf=f'{H}/{base}.java'
    types={}; kinds={}; members={}; arity=collections.defaultdict(set); imports=[]
    for l in open(idxf):
        c=l.rstrip('\n').split('\t')
        if c[0]=='OUTER': outer_end,imports_end,pkg=int(c[1]),int(c[2]),c[3]
        elif c[0]=='IMPORT': imports.append(c[1])
        elif c[0]=='TYPE': types[c[1]]=int(c[2]); kinds[c[1]]=c[4]
        elif c[0]=='MEMBER':
            fqn,name,ps,start,mann,pstarts,panns=c[1:8]
            members[(fqn,name,ps)]=(int(start),mann,[int(x) for x in pstarts.split(',') if x],panns)
            arity[(fqn,name,len(ps.split(',')) if ps else 0)].add(ps)
    edits=[]; newdecls=collections.defaultdict(list); used=set()
    for t in TYPES.get(pkg,[]):
        jpm=jp_members(t)
        for (n,ps,r,pst,sc,rt) in JS.get(t,[]):
            if n in SKIP_METHODS: continue
            want_r = 'Q' if r=='Q' else None
            # a return null only through an argument (orElse, getOrDefault, getProperty(key, def)) or a function
            # (orElseGet, computeIfAbsent): left unhinted, the pass decides per call through its links
            if want_r and (n in PASS_NAMES or f'{t}.{n}({ps})' in PASS_SIGS or f'{t}.{n}({ps})' in TRUSTED_RETURNS):
                want_r=None; passthrough.append(f'{t}.{n}({ps})'); report['ret Q left out: through an argument']+=1
            want_p = [x if x in 'NQ' else None for x in pst]
            if not want_r and not any(want_p): continue
            key=(t,n,ps)
            if key in members:
                start,mann,pstarts,panns=members[key]
                if want_r:
                    if mann=='-': edits.append((start,ANN['Q']+' ')); used.add('Q'); report['ret Q added (existing member)']+=1
                    elif mann!='Q': conflicts.append(f'{t}.{n}({ps}) return: ours {mann}, JSpecify Q'); report['ret conflict']+=1
                for i,w in enumerate(want_p):
                    if not w or i>=len(pstarts): continue
                    have=panns[i]
                    if have=='-': edits.append((pstarts[i],ANN[w]+' ')); used.add(w); report[f'param {w} added (existing member)']+=1
                    elif have!=w: conflicts.append(f'{t}.{n}({ps}) #{i}: ours {have}, JSpecify {w}'); report['param conflict']+=1
                continue
            nparams=len(ps.split(',')) if ps else 0
            if arity.get((t,n,nparams)):
                nomatch.append(f'{t}.{n}({ps}) vs shadow {sorted(arity[(t,n,nparams)])}'); report['added beside a same-arity shadow overload']+=1
            m=jpm.get((n,ps))
            if m is None: report['skipped: not in javap (JDK 27)']+=1; continue
            prm=[]
            for i,p in enumerate(m['params']):
                p=p.replace('$','.')
                w=want_p[i] if i<len(want_p) else None
                if w: used.add(w)
                prm.append((ANN[w]+' ' if w else '')+p+f' p{i}')
            if want_r: used.add('Q')
            newdecls[t].append((m,prm,want_r))
            report['new member declarations']+=1
    # render new declarations
    text=open(hf).read()
    def depth(t): return t[len(pkg)+1:].count('.')+1
    def render_members(t,decls,ind):
        sname=t.split('.')[-1]; top=depth(t)==1
        lines=[f'{ind}// nullability from JSpecify\'s JDK (generated, jspecify-jdk-nullness.py)']
        iface=kinds.get(t)=='interface'
        for m,prm,wr in decls:
            if m['ctor'] and iface: continue
            if m['ctor']:
                lines.append(f'{ind}{sname}{"$" if top else ""}({", ".join(prm)}) {{ }}')
            else:
                rt=m['rt'].replace('$','.'); tp=(m['tp']+' ') if m['tp'] else ''
                lines.append(f'{ind}{"@Nullable " if wr else ""}{"static " if m["static"] else ("default " if iface else "")}{tp}{rt} {m["name"]}({", ".join(prm)}) {{ {default(rt)}}}')
        return lines
    # group: existing types get members appended; missing types get created (nested under their parent when it exists)
    created=collections.defaultdict(list)   # parent fqn (or pkg) -> list of rendered class blocks
    pending_parents=collections.defaultdict(list)  # a missing top-level shadow, created to hold nested ones
    def class_header(t):
        tp=JP.get(t,('',[]))[0].replace('$','.')
        return t.split('.')[-1]+('$' if depth(t)==1 else '')+tp
    for t,decls in newdecls.items():
        ind=' '*(4*depth(t)+4)
        body=render_members(t,decls,ind)
        if t in types:
            edits.append((types[t],'\n'+'\n'.join(body)+'\n'+' '*(4*depth(t))))
            report['types extended']+=1
        else:
            parent=t.rsplit('.',1)[0]
            cind=' '*(4*depth(t))
            block=[f'{cind}//{"public "}{t} (shadow created for its nullability, generated)', f'{cind}class {class_header(t)} {{']+body+[f'{cind}}}']
            if depth(t)==1:
                created[pkg].append(block)
            elif parent in types:
                created[parent].append(block)
            elif depth(parent)==1:
                pending_parents[parent].append(block)
            else:
                report['skipped: nested type two levels below a missing shadow']+=1; continue
            report['types created']+=1
    for parent,blocks in pending_parents.items():
        # the parent is created in this run too when it has members of its own; else an empty one is made here
        own=[b for b in created[pkg] if b[1].startswith(f'    class {class_header(parent)} ')]
        if own:
            own[0][-1:-1]=['']+[x for blk in blocks for x in blk]
        else:
            created[pkg].append([f'    //public {parent} (shadow created to hold nested types, generated)',
                                 f'    class {class_header(parent)} {{']+[x for blk in blocks for x in blk]+['    }'])
            report['types created']+=1
    for parent,blocks in created.items():
        pos=outer_end if parent==pkg else types[parent]
        txt='\n'+'\n\n'.join('\n'.join(b) for b in blocks)+'\n'+(' '*(4*(depth(parent)) ) if parent!=pkg else '')
        edits.append((pos,txt))
    # imports
    wild='io.codelaser.maddi.annotation.*' in imports
    add=[]
    for k,name in (('Q','Nullable'),('N','NotNull')):
        if k in used and not wild and f'io.codelaser.maddi.annotation.{name}' not in imports: add.append(f'import io.codelaser.maddi.annotation.{name};')
    if add: edits.append((imports_end,'\n'+'\n'.join(add)))
    if WRITE and edits:
        for pos,txt in sorted(edits,key=lambda e:-e[0]):
            text=text[:pos]+txt+text[pos:]
        open(hf,'w').write(text)
    report[f'files touched']+= 1 if edits else 0
for k,v in sorted(report.items()): print(f'{v:6d} {k}')
open(f'{D}/conflicts.txt','w').write('\n'.join(conflicts)); open(f'{D}/nomatch.txt','w').write('\n'.join(nomatch))
open(f'{D}/passthrough.txt','w').write('\n'.join(passthrough))
print('conflicts',len(conflicts),'nomatch',len(nomatch),'passthrough',len(passthrough))

print('work directory (conflicts.txt, nomatch.txt, passthrough.txt):',D)

# 5. hand corrections: where an earlier @NotNull of ours was wrong (JSpecify's @Nullable is right), and a shadow
# whose type parameters lacked the JDK's bounds
POST=WRITE
A=H

def fix(f, pattern, repl, expect):
    if not POST: return
    p=f'{A}/{f}'; s=open(p).read(); n=len(re.findall(pattern,s))
    assert n==expect,(f,pattern,n); s=re.sub(pattern,repl,s)
    if '@Nullable' in s and 'import io.codelaser.maddi.annotation.Nullable;' not in s and 'import io.codelaser.maddi.annotation.*;' not in s:
        s=s.replace('import io.codelaser.maddi.annotation.NotNull;','import io.codelaser.maddi.annotation.NotNull;\nimport io.codelaser.maddi.annotation.Nullable;',1)
    open(p,'w').write(s)
fix('JavaIo.java', r'@NotNull CharSequence csq', '@Nullable CharSequence csq', 4)          # Writer.append prints "null"
fix('JavaIo.java', r'FilterOutputStream\$\(@NotNull OutputStream out\)', 'FilterOutputStream$(@Nullable OutputStream out)', 1)
fix('JavaUtilFunction.java', r'@NotNull Object targetRef\)', '@Nullable Object targetRef)', 1)   # Predicate.isEqual(null)
fix('JavaUtilStream.java', r'ofNullable\((/\*[^*]*\*/ )?@NotNull T t\)', r'ofNullable(\1@Nullable T t)', 1)
fix('JavaUtil.java', r'class EnumMap\$<K, V> \{', 'class EnumMap$<K extends Enum<K>, V> {', 1)
# Matcher.group() is group(0), the whole match: never null after a successful match, and it throws without one. Only
# group(int)/group(String) answer null, for an optional group that did not participate. JSpecify's @Nullable on the
# override made every 'if (m.find()) ... m.group()' a seed (diagnose, timefold 2026-10-13); MatchResult.group() is not
fix('JavaUtilRegex.java', r'@Nullable @NotModified String group\(\) \{', '@NotModified String group() {', 1)
