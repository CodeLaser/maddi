// maddi: the compatibility library's JAXB, for jaxb-xml-serialization.csmap.
//
// XmlSerializer writes a type's public properties; JAXB's adapter, which stands between a field's type and one that
// XML can say, is called by the property the translation adds for the field (see JaxbXmlSerialization).
namespace Maddi.JavaCompat;

/// <summary>JAXB's XmlAdapter: <typeparamref name="V"/>, what XmlSerializer writes, for <typeparamref name="B"/>.</summary>
public abstract class JavaXmlAdapter<V, B>
{
    public abstract B Unmarshal(V v);

    public abstract V Marshal(B b);
}
