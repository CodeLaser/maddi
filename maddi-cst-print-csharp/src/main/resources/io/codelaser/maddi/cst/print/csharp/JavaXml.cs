// maddi: the compatibility library's JAXB and JAXP, for jaxb-xml-serialization.csmap.
//
// JAXB's runtime over XmlSerializer: a JAXBContext's types are bound by the public XmlName properties the translation
// adds to them (see JaxbXmlSerialization), and an adapter stands between a field's type and one that XML can say. The
// part of JAXP that goes with it is System.Xml's: DOM is XmlDocument (mapped directly), a parser is XmlReader, a schema
// is an XmlSchemaSet, a transformation an XslCompiledTransform. Java's API is kept, in PascalCase, where System.Xml has
// another shape: factories, sources and results, validation events.
#nullable enable
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Xml;
using System.Xml.Schema;
using System.Xml.Serialization;
using System.Xml.Xsl;

namespace Maddi.JavaCompat;

// JAXB passes null through an adapter both ways; an override may say that its values are not null
/// <summary>JAXB's XmlAdapter: <typeparamref name="V"/>, what XmlSerializer writes, for <typeparamref name="B"/>.</summary>
public abstract class JavaXmlAdapter<V, B>
{
    public abstract B? Unmarshal(V? v);

    public abstract V? Marshal(B? b);
}

/// <summary>JAXB's JAXBException.</summary>
public class JavaJaxbException : Exception
{
    public JavaJaxbException(string message) : base(message) { }

    public JavaJaxbException(string message, Exception? cause) : base(message, cause) { }

    public JavaJaxbException(Exception? cause) : base(cause?.ToString(), cause) { }

    public Exception? GetLinkedException() => InnerException;
}

/// <summary>JAXB's JAXBContext: the types it binds, each by an XmlSerializer.</summary>
public sealed class JavaJaxbContext
{
    private static readonly ConcurrentDictionary<Type, XmlSerializer> Serializers = new();

    private readonly Type[] types;

    private JavaJaxbContext(Type[] types) => this.types = types;

    /// <summary>The context of <paramref name="types"/>; as JAXB's, a type it cannot bind fails here.</summary>
    public static JavaJaxbContext NewInstance(params Type[] types)
    {
        try
        {
            foreach (var t in types) Serializer(t);
        }
        catch (InvalidOperationException e)
        {
            throw new JavaJaxbException(Describe(e), e);
        }
        return new JavaJaxbContext(types);
    }

    public JavaMarshaller CreateMarshaller() => new();

    public JavaUnmarshaller CreateUnmarshaller() => new(this);

    internal static XmlSerializer Serializer(Type type) => Serializers.GetOrAdd(type, t => new XmlSerializer(t));

    /// <summary>The bound type whose root element the reader is on.</summary>
    internal Type Root(XmlReader reader)
    {
        reader.MoveToContent();
        foreach (var t in types)
        {
            if (Serializer(t).CanDeserialize(reader)) return t;
        }
        throw new JavaJaxbException("unexpected element (uri:\"" + reader.NamespaceURI + "\", local:\""
                                    + reader.LocalName + "\"); expected the root of one of "
                                    + string.Join(", ", types.Select(t => t.Name)));
    }

    /// <summary>XmlSerializer's message, with the causes it wraps.</summary>
    internal static string Describe(Exception e)
    {
        var message = new StringBuilder(e.Message);
        for (var inner = e.InnerException; inner != null; inner = inner.InnerException) message.Append(' ').Append(inner.Message);
        return message.ToString();
    }
}

/// <summary>JAXB's Marshaller, with its properties formatted output, encoding and fragment.</summary>
public sealed class JavaMarshaller
{
    public const string JaxbFormattedOutput = "jaxb.formatted.output";
    public const string JaxbEncoding = "jaxb.encoding";
    public const string JaxbFragment = "jaxb.fragment";
    public const string JaxbSchemaLocation = "jaxb.schemaLocation";
    public const string JaxbNoNamespaceSchemaLocation = "jaxb.noNamespaceSchemaLocation";

    // no xmlns:xsi and xmlns:xsd on the root, as JAXB's
    private static readonly XmlSerializerNamespaces NoNamespaces = new(new[] { XmlQualifiedName.Empty });

    private readonly Dictionary<string, object?> properties = new();

    internal JavaMarshaller() { }

    public void SetProperty(string name, object? value) => properties[name] = value;

    public object? GetProperty(string name) => properties.TryGetValue(name, out var value) ? value : null;

    private bool Flag(string name) => GetProperty(name) is bool b ? b : GetProperty(name) is string s && bool.Parse(s);

    internal XmlWriterSettings Settings() => new()
    {
        Indent = Flag(JaxbFormattedOutput),
        Encoding = GetProperty(JaxbEncoding) is string encoding ? Encoding.GetEncoding(encoding) : new UTF8Encoding(false),
        OmitXmlDeclaration = Flag(JaxbFragment),
        CloseOutput = false,
    };

    public void Marshal(object root, JavaXmlResult result)
    {
        // JAXB's declaration says standalone="yes"
        using (var writer = result.CreateWriter(Settings(), "yes")) WriteWithSchemaLocation(root, writer);
        result.Written();
    }

    public void Marshal(object root, TextWriter writer) => Marshal(root, new JavaStreamResult(writer));

    public void Marshal(object root, Stream stream) => Marshal(root, new JavaStreamResult(stream));

    public void Marshal(object root, JavaFile file) => Marshal(root, new JavaStreamResult(file));

    /// <summary>marshal(Object, Node): the XML as the last child of <paramref name="node"/>.</summary>
    public void Marshal(object root, XmlNode node)
    {
        using (var writer = node.CreateNavigator()!.AppendChild()) WriteWithSchemaLocation(root, writer);
    }

    private const string XmlSchemaInstance = "http://www.w3.org/2001/XMLSchema-instance";

    /// <summary>
    /// The root with JAXB's xsi:schemaLocation or xsi:noNamespaceSchemaLocation, when a property gives it:
    /// XmlSerializer has no such attribute, so the XML goes through a document that gets it.
    /// </summary>
    private void WriteWithSchemaLocation(object root, XmlWriter writer)
    {
        var location = GetProperty(JaxbSchemaLocation) as string;
        var noNamespaceLocation = GetProperty(JaxbNoNamespaceSchemaLocation) as string;
        if (location == null && noNamespaceLocation == null)
        {
            Write(root, writer);
            return;
        }
        var document = new XmlDocument();
        using (var into = document.CreateNavigator()!.AppendChild()) Write(root, into);
        var element = document.DocumentElement!;
        element.SetAttribute("xmlns:xsi", XmlSchemaInstance);
        if (location != null) element.SetAttribute("schemaLocation", XmlSchemaInstance, location);
        if (noNamespaceLocation != null)
        {
            element.SetAttribute("noNamespaceSchemaLocation", XmlSchemaInstance, noNamespaceLocation);
        }
        element.WriteTo(writer);
    }

    private static void Write(object root, XmlWriter writer)
    {
        try
        {
            JavaJaxbContext.Serializer(root.GetType()).Serialize(writer, root, NoNamespaces);
        }
        catch (InvalidOperationException e)
        {
            throw new JavaJaxbException(JavaJaxbContext.Describe(e), e.InnerException ?? e);
        }
    }
}

/// <summary>JAXB's ValidationEventHandler: true to go on after the event.</summary>
public delegate bool JavaValidationEventHandler(JavaValidationEvent validationEvent);

/// <summary>JAXB's ValidationEvent: a schema's verdict on a part of the document.</summary>
public sealed class JavaValidationEvent
{
    public const int Warning = 0;
    public const int Error = 1;
    public const int FatalError = 2;

    private readonly int severity;
    private readonly string message;
    private readonly Exception? linked;
    private readonly JavaValidationEventLocator locator;

    public JavaValidationEvent(int severity, string message, JavaValidationEventLocator locator, Exception? linked = null)
    {
        this.severity = severity;
        this.message = message;
        this.locator = locator;
        this.linked = linked;
    }

    public int GetSeverity() => severity;

    public string GetMessage() => message;

    public Exception? GetLinkedException() => linked;

    public JavaValidationEventLocator GetLocator() => locator;

    public override string ToString() => "[severity=" + severity + ",message=" + message + ",locator=" + locator + "]";
}

/// <summary>JAXB's ValidationEventLocator: where the event is, the node when a document was validated.</summary>
public sealed class JavaValidationEventLocator
{
    private readonly XmlNode? node;
    private readonly int line;
    private readonly int column;

    public JavaValidationEventLocator(XmlNode? node, int line, int column)
    {
        this.node = node;
        this.line = line;
        this.column = column;
    }

    public XmlNode? GetNode() => node;

    public int GetLineNumber() => line;

    public int GetColumnNumber() => column;

    public int GetOffset() => -1;

    public object? GetObject() => null;

    public Uri? GetURL() => null;

    public override string ToString() => node != null ? "[node=" + node.Name + "]" : "[line=" + line + ",column=" + column + "]";
}

/// <summary>JAXB's ValidationEventCollector: keeps the events, and goes on.</summary>
public sealed class JavaValidationEventCollector
{
    private readonly List<JavaValidationEvent> events = new();

    public bool HandleEvent(JavaValidationEvent validationEvent)
    {
        events.Add(validationEvent);
        return true;
    }

    public JavaValidationEvent[] GetEvents() => events.ToArray();

    public bool HasEvents() => events.Count > 0;

    public void Reset() => events.Clear();

    public static implicit operator JavaValidationEventHandler(JavaValidationEventCollector collector) =>
        collector.HandleEvent;
}

/// <summary>
/// JAXB's Unmarshaller, with a schema to validate against and a handler for its events. Without a handler, the first
/// error ends the unmarshalling, as JAXB's default handler has it.
/// </summary>
public sealed class JavaUnmarshaller
{
    private readonly JavaJaxbContext context;
    private JavaSchema? schema;
    private JavaValidationEventHandler? handler;

    internal JavaUnmarshaller(JavaJaxbContext context) => this.context = context;

    public void SetSchema(JavaSchema? schema) => this.schema = schema;

    public JavaSchema? GetSchema() => schema;

    public void SetEventHandler(JavaValidationEventHandler? handler) => this.handler = handler;

    public JavaValidationEventHandler? GetEventHandler() => handler;

    /// <summary>unmarshal(Node): a document is validated as a tree, its events have their node.</summary>
    public object Unmarshal(XmlNode node)
    {
        if (schema != null)
        {
            var document = node as XmlDocument ?? node.OwnerDocument!;
            document.Schemas = schema.Set;
            // the node of an event is the exception's source
            ValidationEventHandler onEvent = (sender, e) =>
                Handle(e, (e.Exception as XmlSchemaValidationException)?.SourceObject as XmlNode ?? sender as XmlNode);
            if (node is XmlDocument) document.Validate(onEvent);
            else document.Validate(onEvent, node);
        }
        return Read(new XmlNodeReader(node), false);
    }

    public object Unmarshal(TextReader reader) => Read(XmlReader.Create(reader, JavaXmlParsing.Secure()), true);

    public object Unmarshal(Stream stream) => Read(XmlReader.Create(stream, JavaXmlParsing.Secure()), true);

    public object Unmarshal(JavaInputSource source) => Read(source.CreateReader(JavaXmlParsing.Secure()), true);

    public object Unmarshal(JavaXmlSource source) => Read(source.CreateReader(JavaXmlParsing.Secure()), true);

    public object Unmarshal(JavaFile file) => Unmarshal(new FileInfo(file.GetAbsolutePath()));

    public object Unmarshal(FileInfo file)
    {
        using var stream = file.OpenRead();
        return Unmarshal(stream);
    }

    private object Read(XmlReader input, bool validate)
    {
        var reader = input;
        if (validate && schema != null)
        {
            var settings = JavaXmlParsing.Secure();
            settings.ValidationType = ValidationType.Schema;
            settings.Schemas = schema.Set;
            settings.ValidationEventHandler += (_, e) => Handle(e, null);
            reader = XmlReader.Create(input, settings);
        }
        using (reader)
        {
            try
            {
                return JavaJaxbContext.Serializer(context.Root(reader)).Deserialize(reader)!; // JAXB's root object, never null
            }
            catch (InvalidOperationException e)
            {
                if (e.InnerException is JavaJaxbException stopped) throw stopped;
                throw new JavaJaxbException(JavaJaxbContext.Describe(e), e.InnerException ?? e);
            }
            catch (XmlException e)
            {
                throw new JavaJaxbException(e.Message, e);
            }
        }
    }

    private void Handle(ValidationEventArgs e, XmlNode? node)
    {
        var severity = e.Severity == XmlSeverityType.Warning ? JavaValidationEvent.Warning : JavaValidationEvent.Error;
        var locator = new JavaValidationEventLocator(node, e.Exception?.LineNumber ?? -1, e.Exception?.LinePosition ?? -1);
        var validationEvent = new JavaValidationEvent(severity, e.Message, locator, e.Exception);
        var goOn = handler != null ? handler(validationEvent) : severity == JavaValidationEvent.Warning;
        if (!goOn) throw new JavaJaxbException(e.Message, e.Exception);
    }
}

/// <summary>Reading XML as JAXP's secure processing has it: no DTD, no external resolution.</summary>
internal static class JavaXmlParsing
{
    internal static XmlReaderSettings Secure() => new() { DtdProcessing = DtdProcessing.Prohibit, XmlResolver = null };
}

// ---- JAXP: parsing to DOM

/// <summary>SAX's SAXException, and its subclasses below.</summary>
public class JavaSaxException : Exception
{
    public JavaSaxException() { }

    public JavaSaxException(string message) : base(message) { }

    public JavaSaxException(Exception? cause) : base(cause?.ToString(), cause) { }

    public JavaSaxException(string message, Exception? cause) : base(message, cause) { }

    public Exception? GetException() => InnerException;
}

public class JavaSaxParseException : JavaSaxException
{
    private readonly int line;
    private readonly int column;

    public JavaSaxParseException(string message, int line = -1, int column = -1, Exception? cause = null)
        : base(message, cause)
    {
        this.line = line;
        this.column = column;
    }

    public int GetLineNumber() => line;

    public int GetColumnNumber() => column;
}

public class JavaSaxNotSupportedException : JavaSaxException
{
    public JavaSaxNotSupportedException() { }

    public JavaSaxNotSupportedException(string message) : base(message) { }
}

public class JavaSaxNotRecognizedException : JavaSaxException
{
    public JavaSaxNotRecognizedException() { }

    public JavaSaxNotRecognizedException(string message) : base(message) { }
}

/// <summary>JAXP's ParserConfigurationException.</summary>
public class JavaParserConfigurationException : Exception
{
    public JavaParserConfigurationException() { }

    public JavaParserConfigurationException(string message) : base(message) { }
}

/// <summary>SAX's InputSource: a reader, a stream, or a system id (a file or a URI).</summary>
public sealed class JavaInputSource
{
    private TextReader? characterStream;
    private Stream? byteStream;
    private string? systemId;

    public JavaInputSource() { }

    public JavaInputSource(TextReader characterStream) => this.characterStream = characterStream;

    public JavaInputSource(Stream byteStream) => this.byteStream = byteStream;

    public JavaInputSource(string systemId) => this.systemId = systemId;

    public TextReader? GetCharacterStream() => characterStream;

    public void SetCharacterStream(TextReader? reader) => characterStream = reader;

    public Stream? GetByteStream() => byteStream;

    public void SetByteStream(Stream? stream) => byteStream = stream;

    public string? GetSystemId() => systemId;

    public void SetSystemId(string? id) => systemId = id;

    internal XmlReader CreateReader(XmlReaderSettings settings) =>
        characterStream != null ? XmlReader.Create(characterStream, settings)
        : byteStream != null ? XmlReader.Create(byteStream, settings)
        : XmlReader.Create(systemId!, settings);
}

/// <summary>JAXP's DocumentBuilderFactory; its features are the parser's, a disallowed DOCTYPE the one that matters.</summary>
public sealed class JavaDocumentBuilderFactory
{
    public const string DisallowDoctypeDecl = "http://apache.org/xml/features/disallow-doctype-decl";

    private readonly Dictionary<string, bool> features = new();
    private bool namespaceAware;
    private bool xIncludeAware;
    private bool ignoringComments;

    public static JavaDocumentBuilderFactory NewInstance() => new();

    public static JavaDocumentBuilderFactory NewDefaultInstance() => new();

    public void SetFeature(string name, bool value) => features[name] = value;

    public bool GetFeature(string name) => features.TryGetValue(name, out var value) && value;

    /// <summary>System.Xml's DOM is always namespace aware.</summary>
    public void SetNamespaceAware(bool value) => namespaceAware = value;

    public bool IsNamespaceAware() => namespaceAware;

    /// <summary>System.Xml has no XInclude: the setting is kept, the documents are not included.</summary>
    public void SetXIncludeAware(bool value) => xIncludeAware = value;

    public bool IsXIncludeAware() => xIncludeAware;

    public void SetIgnoringComments(bool value) => ignoringComments = value;

    public void SetValidating(bool value) { }

    public void SetExpandEntityReferences(bool value) { }

    public void SetAttribute(string name, object? value) { }

    public JavaDocumentBuilder NewDocumentBuilder() => new(GetFeature(DisallowDoctypeDecl), ignoringComments);
}

/// <summary>JAXP's DocumentBuilder: XML to an XmlDocument.</summary>
public sealed class JavaDocumentBuilder
{
    private readonly bool disallowDoctype;
    private readonly bool ignoringComments;

    internal JavaDocumentBuilder(bool disallowDoctype, bool ignoringComments)
    {
        this.disallowDoctype = disallowDoctype;
        this.ignoringComments = ignoringComments;
    }

    private XmlReaderSettings Settings() => new()
    {
        DtdProcessing = disallowDoctype ? DtdProcessing.Prohibit : DtdProcessing.Ignore,
        XmlResolver = null,
        IgnoreComments = ignoringComments,
    };

    public XmlDocument Parse(JavaInputSource source)
    {
        using var reader = source.CreateReader(Settings());
        return Load(reader);
    }

    public XmlDocument Parse(Stream stream)
    {
        using var reader = XmlReader.Create(stream, Settings());
        return Load(reader);
    }

    public XmlDocument Parse(JavaFile file) => Parse(new FileInfo(file.GetAbsolutePath()));

    public XmlDocument Parse(FileInfo file)
    {
        using var stream = file.OpenRead();
        return Parse(stream);
    }

    public XmlDocument Parse(string uri)
    {
        using var reader = XmlReader.Create(uri, Settings());
        return Load(reader);
    }

    public XmlDocument NewDocument() => new();

    private static XmlDocument Load(XmlReader reader)
    {
        var document = new XmlDocument { XmlResolver = null };
        try
        {
            document.Load(reader);
        }
        catch (XmlException e)
        {
            throw new JavaSaxParseException(e.Message, e.LineNumber, e.LinePosition, e);
        }
        return document;
    }
}

/// <summary>DOM's operations that XmlDocument has under another shape.</summary>
public static class JavaDom
{
    /// <summary>Node.getNamespaceURI: null for no namespace, where XmlNode has the empty string.</summary>
    public static string? NamespaceUri(XmlNode node) => string.IsNullOrEmpty(node.NamespaceURI) ? null : node.NamespaceURI;

    /// <summary>
    /// Document.renameNode for an element: XmlDocument's names are fixed, so a new element with the name takes the
    /// old one's place, with its attributes and children. Returns the new element, as Java's returns the renamed node.
    /// </summary>
    public static XmlNode RenameNode(XmlDocument document, XmlNode node, string namespaceUri, string qualifiedName)
    {
        if (node is not XmlElement element) throw new NotSupportedException("renameNode of a " + node.NodeType);
        var renamed = document.CreateElement(qualifiedName, namespaceUri ?? string.Empty);
        foreach (XmlAttribute attribute in element.Attributes.Cast<XmlAttribute>().ToList())
        {
            // the old element's default namespace declaration is the old name's
            if (attribute.Name == "xmlns") continue;
            renamed.Attributes.Append((XmlAttribute) attribute.CloneNode(true));
        }
        while (element.FirstChild != null) renamed.AppendChild(element.FirstChild);
        element.ParentNode?.ReplaceChild(renamed, element);
        return renamed;
    }
}

// ---- JAXP: sources, results and transformation

/// <summary>JAXP's Source: XML to read.</summary>
public abstract class JavaXmlSource
{
    private string? systemId;

    public string? GetSystemId() => systemId;

    public void SetSystemId(string? id) => systemId = id;

    internal abstract XmlReader CreateReader(XmlReaderSettings settings);
}

/// <summary>JAXP's DOMSource.</summary>
public sealed class JavaDomSource : JavaXmlSource
{
    private XmlNode? node;

    public JavaDomSource() { }

    public JavaDomSource(XmlNode? node) => this.node = node;

    public XmlNode? GetNode() => node;

    public void SetNode(XmlNode? value) => node = value;

    internal override XmlReader CreateReader(XmlReaderSettings settings) => new XmlNodeReader(node!);
}

/// <summary>JAXP's StreamSource.</summary>
public sealed class JavaStreamSource : JavaXmlSource
{
    private readonly Stream? stream;
    private readonly TextReader? reader;

    public JavaStreamSource(Stream stream) => this.stream = stream;

    public JavaStreamSource(TextReader reader) => this.reader = reader;

    public JavaStreamSource(string systemId) => SetSystemId(systemId);

    public JavaStreamSource(FileInfo file) => SetSystemId(file.FullName);

    public JavaStreamSource(JavaFile file) => SetSystemId(file.GetAbsolutePath());

    public Stream? GetInputStream() => stream;

    public TextReader? GetReader() => reader;

    internal override XmlReader CreateReader(XmlReaderSettings settings) =>
        stream != null ? XmlReader.Create(stream, settings)
        : reader != null ? XmlReader.Create(reader, settings)
        : XmlReader.Create(GetSystemId()!, settings);
}

/// <summary>JAXP's Result: where XML goes.</summary>
public abstract class JavaXmlResult
{
    private string? systemId;

    public string? GetSystemId() => systemId;

    public void SetSystemId(string? id) => systemId = id;

    /// <summary>
    /// A writer of the XML; unless the settings omit it, the declaration first, with <paramref name="standalone"/>
    /// ("yes", "no"; null: none).
    /// </summary>
    internal abstract XmlWriter CreateWriter(XmlWriterSettings settings, string? standalone);

    /// <summary>After the writer has been closed.</summary>
    internal virtual void Written() { }
}

/// <summary>JAXP's DOMResult: a new XmlDocument, or the node given, gets the XML.</summary>
public sealed class JavaDomResult : JavaXmlResult
{
    private XmlNode? node;

    public JavaDomResult() { }

    public JavaDomResult(XmlNode? node) => this.node = node;

    public XmlNode? GetNode() => node;

    public void SetNode(XmlNode? value) => node = value;

    internal override XmlWriter CreateWriter(XmlWriterSettings settings, string? standalone)
    {
        node ??= new XmlDocument();
        return node.CreateNavigator()!.AppendChild();
    }
}

/// <summary>JAXP's StreamResult.</summary>
public sealed class JavaStreamResult : JavaXmlResult
{
    private readonly TextWriter? writer;
    private readonly Stream? stream;

    public JavaStreamResult(TextWriter writer) => this.writer = writer;

    public JavaStreamResult(Stream stream) => this.stream = stream;

    public JavaStreamResult(string systemId) => SetSystemId(systemId);

    public JavaStreamResult(FileInfo file) => SetSystemId(file.FullName);

    public JavaStreamResult(JavaFile file) => SetSystemId(file.GetAbsolutePath());

    public TextWriter? GetWriter() => writer;

    public Stream? GetOutputStream() => stream;

    internal override XmlWriter CreateWriter(XmlWriterSettings settings, string? standalone)
    {
        if (settings.OmitXmlDeclaration)
        {
            return writer != null ? XmlWriter.Create(writer, settings)
                : stream != null ? XmlWriter.Create(stream, settings) : XmlWriter.Create(GetSystemId()!, settings);
        }
        // Java's declaration: XmlWriter's has the TextWriter's encoding (a StringWriter's is UTF-16), the encoding in
        // lower case, and no standalone
        var encoding = settings.Encoding ?? new UTF8Encoding(false);
        if (encoding is UTF8Encoding) encoding = new UTF8Encoding(false); // Java writes no byte order mark
        var fragment = settings.Clone();
        fragment.OmitXmlDeclaration = true;
        var text = writer;
        if (text == null)
        {
            // a writer of our own, closed with the XmlWriter; the caller's stream stays open
            text = new StreamWriter(stream ?? new FileStream(GetSystemId()!, FileMode.Create), encoding, 1024,
                leaveOpen: stream != null);
            fragment.CloseOutput = true;
        }
        text.Write("<?xml version=\"1.0\" encoding=\"" + encoding.WebName.ToUpperInvariant() + "\""
                   + (standalone == null ? "" : " standalone=\"" + standalone + "\"") + "?>");
        if (settings.Indent) text.Write(settings.NewLineChars);
        return XmlWriter.Create(text, fragment);
    }
}

/// <summary>JAXP's OutputKeys.</summary>
public static class JavaOutputKeys
{
    public const string Indent = "indent";
    public const string Encoding = "encoding";
    public const string Method = "method";
    public const string OmitXmlDeclaration = "omit-xml-declaration";
    public const string Standalone = "standalone";

    /// <summary>Xalan's, which the JDK's transformer reads: the spaces of one level.</summary>
    public const string IndentAmount = "{http://xml.apache.org/xslt}indent-amount";
}

/// <summary>JAXP's TransformerException.</summary>
public class JavaTransformerException : Exception
{
    public JavaTransformerException(string message) : base(message) { }

    public JavaTransformerException(Exception? cause) : base(cause?.ToString(), cause) { }

    public JavaTransformerException(string message, Exception cause) : base(message, cause) { }
}

public class JavaTransformerConfigurationException : JavaTransformerException
{
    public JavaTransformerConfigurationException(string message) : base(message) { }

    public JavaTransformerConfigurationException(string message, Exception cause) : base(message, cause) { }
}

/// <summary>JAXP's TransformerFactory: an XSLT stylesheet compiled, or the identity.</summary>
public sealed class JavaTransformerFactory
{
    private readonly Dictionary<string, object?> attributes = new();

    public static JavaTransformerFactory NewInstance() => new();

    public static JavaTransformerFactory NewDefaultInstance() => new();

    /// <summary>The access properties of XMLConstants: System.Xml resolves nothing external here anyway.</summary>
    public void SetAttribute(string name, object? value) => attributes[name] = value;

    public object? GetAttribute(string name) => attributes.TryGetValue(name, out var value) ? value : null;

    public void SetFeature(string name, bool value) { }

    public JavaTransformer NewTransformer() => new(null);

    public JavaTransformer NewTransformer(JavaXmlSource stylesheet)
    {
        var xslt = new XslCompiledTransform();
        try
        {
            using var reader = stylesheet.CreateReader(JavaXmlParsing.Secure());
            xslt.Load(reader, XsltSettings.Default, null);
        }
        catch (Exception e) when (e is XsltException or XmlException)
        {
            throw new JavaTransformerConfigurationException(e.Message, e);
        }
        return new JavaTransformer(xslt);
    }
}

/// <summary>JAXP's Transformer, with the output properties indent, its amount, encoding and the XML declaration.</summary>
public sealed class JavaTransformer
{
    private readonly XslCompiledTransform? xslt;
    private readonly Dictionary<string, string> output = new();

    internal JavaTransformer(XslCompiledTransform? xslt) => this.xslt = xslt;

    public void SetOutputProperty(string name, string value) => output[name] = value;

    public string? GetOutputProperty(string name) => output.TryGetValue(name, out var value) ? value : null;

    public void SetParameter(string name, object? value) => throw new NotSupportedException("XSLT parameters");

    private XmlWriterSettings Settings()
    {
        var settings = xslt?.OutputSettings?.Clone() ?? new XmlWriterSettings();
        if (GetOutputProperty(JavaOutputKeys.Indent) is string indent) settings.Indent = indent == "yes";
        if (GetOutputProperty(JavaOutputKeys.IndentAmount) is string amount && int.TryParse(amount, out var spaces))
        {
            settings.IndentChars = new string(' ', spaces);
        }
        if (GetOutputProperty(JavaOutputKeys.OmitXmlDeclaration) is string omit) settings.OmitXmlDeclaration = omit == "yes";
        if (GetOutputProperty(JavaOutputKeys.Encoding) is string encoding) settings.Encoding = Encoding.GetEncoding(encoding);
        settings.CloseOutput = false;
        return settings;
    }

    public void Transform(JavaXmlSource source, JavaXmlResult result)
    {
        try
        {
            using (var reader = source.CreateReader(JavaXmlParsing.Secure()))
            // the JDK's transformer declares standalone="no" unless told otherwise
            using (var writer = result.CreateWriter(Settings(), GetOutputProperty(JavaOutputKeys.Standalone) ?? "no"))
            {
                if (xslt == null) writer.WriteNode(reader, true);
                else xslt.Transform(reader, writer);
            }
            result.Written();
        }
        catch (Exception e) when (e is XsltException or XmlException)
        {
            throw new JavaTransformerException(e.Message, e);
        }
    }
}

/// <summary>JAXP's XMLConstants.</summary>
public static class JavaXmlConstants
{
    public const string NullNsUri = "";
    public const string XmlNsUri = "http://www.w3.org/XML/1998/namespace";
    public const string XmlnsAttributeNsUri = "http://www.w3.org/2000/xmlns/";
    public const string W3cXmlSchemaNsUri = "http://www.w3.org/2001/XMLSchema";
    public const string W3cXmlSchemaInstanceNsUri = "http://www.w3.org/2001/XMLSchema-instance";
    public const string FeatureSecureProcessing = "http://javax.xml.XMLConstants/feature/secure-processing";
    public const string AccessExternalDtd = "http://javax.xml.XMLConstants/property/accessExternalDTD";
    public const string AccessExternalSchema = "http://javax.xml.XMLConstants/property/accessExternalSchema";
    public const string AccessExternalStylesheet = "http://javax.xml.XMLConstants/property/accessExternalStylesheet";
}

// ---- JAXP: validation

/// <summary>JAXP's SchemaFactory, for W3C XML Schema: schemas compiled into an XmlSchemaSet.</summary>
public sealed class JavaSchemaFactory
{
    private JavaSchemaFactory() { }

    public static JavaSchemaFactory NewInstance(string schemaLanguage)
    {
        if (schemaLanguage != JavaXmlConstants.W3cXmlSchemaNsUri)
        {
            throw new ArgumentException("no SchemaFactory for " + schemaLanguage);
        }
        return new JavaSchemaFactory();
    }

    public static JavaSchemaFactory NewDefaultInstance() => new();

    /// <summary>The access properties of XMLConstants: schemas are read without resolving external ones.</summary>
    public void SetProperty(string name, object? value) { }

    public void SetFeature(string name, bool value) { }

    public JavaSchema NewSchema(Uri uri)
    {
        using var stream = JavaResources.OpenStream(uri);
        return NewSchema(new JavaStreamSource(stream));
    }

    public JavaSchema NewSchema(FileInfo file) => NewSchema(new JavaStreamSource(file));

    public JavaSchema NewSchema(JavaFile file) => NewSchema(new JavaStreamSource(file));

    public JavaSchema NewSchema(params JavaXmlSource[] sources)
    {
        var set = new XmlSchemaSet { XmlResolver = null };
        try
        {
            foreach (var source in sources)
            {
                using var reader = source.CreateReader(JavaXmlParsing.Secure());
                set.Add(null, reader);
            }
            set.Compile();
        }
        catch (Exception e) when (e is XmlSchemaException or XmlException)
        {
            throw new JavaSaxParseException(e.Message, -1, -1, e);
        }
        return new JavaSchema(set);
    }
}

/// <summary>JAXP's Schema.</summary>
public sealed class JavaSchema
{
    internal JavaSchema(XmlSchemaSet set) => Set = set;

    internal XmlSchemaSet Set { get; }

    public JavaValidator NewValidator() => new(Set);
}

/// <summary>JAXP's Validator: without an error handler, an error is a SAXParseException, a warning is ignored.</summary>
public sealed class JavaValidator
{
    private readonly XmlSchemaSet set;

    internal JavaValidator(XmlSchemaSet set) => this.set = set;

    public void SetProperty(string name, object? value) { }

    public void SetFeature(string name, bool value) { }

    public void Validate(JavaXmlSource source)
    {
        if (source is JavaDomSource dom && dom.GetNode() is XmlNode node)
        {
            var document = node as XmlDocument ?? node.OwnerDocument!;
            document.Schemas = set;
            if (node is XmlDocument) document.Validate(OnEvent);
            else document.Validate(OnEvent, node);
            return;
        }
        var settings = JavaXmlParsing.Secure();
        settings.ValidationType = ValidationType.Schema;
        settings.Schemas = set;
        settings.ValidationEventHandler += OnEvent;
        using var reader = XmlReader.Create(source.CreateReader(JavaXmlParsing.Secure()), settings);
        try
        {
            while (reader.Read()) { }
        }
        catch (XmlException e)
        {
            throw new JavaSaxParseException(e.Message, e.LineNumber, e.LinePosition, e);
        }
    }

    private static void OnEvent(object? sender, ValidationEventArgs e)
    {
        if (e.Severity == XmlSeverityType.Warning) return;
        throw new JavaSaxParseException(e.Message, e.Exception?.LineNumber ?? -1, e.Exception?.LinePosition ?? -1,
            e.Exception);
    }
}
