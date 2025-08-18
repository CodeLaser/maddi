package org.e2immu.analyzer.aapi.archive.v2.jdk;
import java.io.InputStream;
import java.net.*;
import java.nio.charset.Charset;

public class JavaNet {
    public static final String PACKAGE_NAME = "java.net";
    //public class InetAddress implements Serializable
    class InetAddress$ {
        boolean isMulticastAddress() { return false; }
        boolean isAnyLocalAddress() { return false; }
        boolean isLoopbackAddress() { return false; }
        boolean isLinkLocalAddress() { return false; }
        boolean isSiteLocalAddress() { return false; }
        boolean isMCGlobal() { return false; }
        boolean isMCNodeLocal() { return false; }
        boolean isMCLinkLocal() { return false; }
        boolean isMCSiteLocal() { return false; }
        boolean isMCOrgLocal() { return false; }
        boolean isReachable(int timeout) { return false; }
        boolean isReachable(NetworkInterface netif, int ttl, int timeout) { return false; }
        String getHostName() { return null; }
        String getCanonicalHostName() { return null; }
        byte [] getAddress() { return null; }
        //frequency 1
        String getHostAddress() { return null; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        static InetAddress getByAddress(String host, byte [] addr) { return null; }
        static InetAddress getByName(String host) { return null; }
        static InetAddress [] getAllByName(String host) { return null; }
        static InetAddress getLoopbackAddress() { return null; }
        static InetAddress ofLiteral(String ipAddressLiteral) { return null; }
        static InetAddress getByAddress(byte [] addr) { return null; }
        //frequency 1
        static InetAddress getLocalHost() { return null; }
    }

    //public final class URI implements Comparable<URI>, Serializable
    class URI$ {
        //frequency 38
        URI$(String str) { }
        URI$(String scheme, String userInfo, String host, int port, String path, String query, String fragment) { }
        URI$(String scheme, String authority, String path, String query, String fragment) { }
        URI$(String scheme, String host, String path, String fragment) { }
        URI$(String scheme, String ssp, String fragment) { }
        //frequency 2
        static URI create(String str) { return null; }
        URI parseServerAuthority() { return null; }
        //frequency 1
        URI normalize() { return null; }
        URI resolve(URI uri) { return null; }
        URI resolve(String str) { return null; }
        URI relativize(URI uri) { return null; }
        URL toURL() { return null; }
        String getScheme() { return null; }
        boolean isAbsolute() { return false; }
        boolean isOpaque() { return false; }
        String getRawSchemeSpecificPart() { return null; }
        String getSchemeSpecificPart() { return null; }
        String getRawAuthority() { return null; }
        String getAuthority() { return null; }
        String getRawUserInfo() { return null; }
        String getUserInfo() { return null; }
        String getHost() { return null; }
        int getPort() { return 0; }
        String getRawPath() { return null; }
        //frequency 1
        String getPath() { return null; }
        String getRawQuery() { return null; }
        String getQuery() { return null; }
        String getRawFragment() { return null; }
        String getFragment() { return null; }
        //override from java.lang.Object
        //frequency 1
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object ob) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ URI that) { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        String toASCIIString() { return null; }
    }

    //public final class URL implements Serializable
    class URL$ {
        URL$(String protocol, String host, int port, String file) { }
        URL$(String protocol, String host, String file) { }
        URL$(String protocol, String host, int port, String file, URLStreamHandler handler) { }
        URL$(String spec) { }
        URL$(URL context, String spec) { }
        URL$(URL context, String spec, URLStreamHandler handler) { }
        static URL of(URI uri, URLStreamHandler handler) { return null; }
        String getQuery() { return null; }
        //frequency 1
        String getPath() { return null; }
        String getUserInfo() { return null; }
        String getAuthority() { return null; }
        int getPort() { return 0; }
        int getDefaultPort() { return 0; }
        String getProtocol() { return null; }
        String getHost() { return null; }
        String getFile() { return null; }
        String getRef() { return null; }
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }
        boolean sameFile(URL other) { return false; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        String toExternalForm() { return null; }
        URI toURI() { return null; }
        URLConnection openConnection() { return null; }
        URLConnection openConnection(Proxy proxy) { return null; }
        InputStream openStream() { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Object getContent() { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Object getContent(Class<?> [] classes) { return null; }
        static void setURLStreamHandlerFactory(URLStreamHandlerFactory fac) { }
    }

    //public class URLDecoder
    class URLDecoder$ {
        static String decode(String s) { return null; }
        static String decode(String s, String enc) { return null; }
        //frequency 1
        static String decode(String s, Charset charset) { return null; }
    }

    //public class URLEncoder
    class URLEncoder$ {
        static String encode(String s) { return null; }
        static String encode(String s, String enc) { return null; }
        //frequency 1
        static String encode(String s, Charset charset) { return null; }
    }
}
