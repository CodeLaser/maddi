/*
 * maddi: a modification analyzer for duplication detection and immutability.
 * Copyright 2020-2026, CodeLaser BV, https://github.com/CodeLaser/maddi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.codelaser.maddi.aapi.archive.jdk;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.channels.DatagramChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.security.CodeSource;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.cert.Certificate;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.Manifest;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Modified;
import io.codelaser.maddi.annotation.NotModified;
import io.codelaser.maddi.annotation.NotNull;
import io.codelaser.maddi.annotation.Nullable;

public class JavaNet {
    public static final String PACKAGE_NAME = "java.net";
    //public class InetAddress implements Serializable
    // immutable value; not final (Inet4/6Address subtypes), so this lands as @Immutable(hc=true)
    @ImmutableContainer
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
        boolean isReachable(@Nullable NetworkInterface netif, int ttl, int timeout) { return false; }
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
        static InetAddress getByAddress(@Nullable String host, @NotNull byte [] addr) { return null; }
        static InetAddress getByName(@Nullable String host) { return null; }
        static InetAddress [] getAllByName(@Nullable String host) { return null; }
        static InetAddress getLoopbackAddress() { return null; }
        static InetAddress ofLiteral(@NotNull String ipAddressLiteral) { return null; }
        static InetAddress getByAddress(@NotNull byte [] addr) { return null; }
        //frequency 1
        static InetAddress getLocalHost() { return null; }
    }

    //public final class URI implements Comparable<URI>, Serializable
    @ImmutableContainer
    class URI$ {
        //frequency 38
        URI$(@NotNull String str) { }
        URI$(@Nullable String scheme, @Nullable String userInfo, @Nullable String host, int port, @Nullable String path, @Nullable String query, @Nullable String fragment) { }
        URI$(@Nullable String scheme, @Nullable String authority, @Nullable String path, @Nullable String query, @Nullable String fragment) { }
        URI$(@Nullable String scheme, @Nullable String host, @Nullable String path, @Nullable String fragment) { }
        URI$(@Nullable String scheme, @Nullable String ssp, @Nullable String fragment) { }
        //frequency 2
        static URI create(@NotNull String str) { return null; }
        URI parseServerAuthority() { return null; }
        //frequency 1
        URI normalize() { return null; }
        URI resolve(@NotNull URI uri) { return null; }
        URI resolve(@NotNull String str) { return null; }
        URI relativize(@NotNull URI uri) { return null; }
        URL toURL() { return null; }
        @Nullable String getScheme() { return null; }
        boolean isAbsolute() { return false; }
        boolean isOpaque() { return false; }
        String getRawSchemeSpecificPart() { return null; }
        String getSchemeSpecificPart() { return null; }
        @Nullable String getRawAuthority() { return null; }
        @Nullable String getAuthority() { return null; }
        @Nullable String getRawUserInfo() { return null; }
        @Nullable String getUserInfo() { return null; }
        @Nullable String getHost() { return null; }
        int getPort() { return 0; }
        @Nullable String getRawPath() { return null; }
        //frequency 1
        @Nullable String getPath() { return null; }
        @Nullable String getRawQuery() { return null; }
        @Nullable String getQuery() { return null; }
        @Nullable String getRawFragment() { return null; }
        @Nullable String getFragment() { return null; }
        //override from java.lang.Object
        //frequency 1
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object ob) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }

        //override from java.lang.Comparable
        //@NotModified[H]
        int compareTo(/*@Independent[M] @NotModified[H] @NotNull[H]*/ @NotNull URI that) { return 0; }

        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        String toASCIIString() { return null; }
    }

    //public final class URL implements Serializable
    @ImmutableContainer
    class URL$ {
        URL$(@NotNull String protocol, @Nullable String host, int port, @NotNull String file) { }
        URL$(@NotNull String protocol, @Nullable String host, @NotNull String file) { }
        URL$(@NotNull String protocol, @Nullable String host, int port, @NotNull String file, @Nullable URLStreamHandler handler) { }
        URL$(@NotNull String spec) { }
        URL$(@Nullable URL context, @NotNull String spec) { }
        URL$(@Nullable URL context, @NotNull String spec, @Nullable URLStreamHandler handler) { }
        static URL of(@NotNull URI uri, @Nullable URLStreamHandler handler) { return null; }
        @Nullable String getQuery() { return null; }
        //frequency 1
        String getPath() { return null; }
        @Nullable String getUserInfo() { return null; }
        @Nullable String getAuthority() { return null; }
        int getPort() { return 0; }
        int getDefaultPort() { return 0; }
        String getProtocol() { return null; }
        String getHost() { return null; }
        String getFile() { return null; }
        @Nullable String getRef() { return null; }
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }
        boolean sameFile(@NotNull URL other) { return false; }
        //override from java.lang.Object
        //@NotModified[H] @NotNull[H]
        public String toString() { return null; }
        String toExternalForm() { return null; }
        URI toURI() { return null; }
        URLConnection openConnection() { return null; }
        URLConnection openConnection(@NotNull Proxy proxy) { return null; }
        InputStream openStream() { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Object getContent() { return null; }

        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        @Nullable Object getContent(@NotNull Class<?> [] classes) { return null; }
        static void setURLStreamHandlerFactory(@NotNull URLStreamHandlerFactory fac) { }
    }

    //public class URLDecoder
    class URLDecoder$ {
        static String decode(@NotNull String s) { return null; }
        static String decode(@NotNull String s, @NotNull String enc) { return null; }
        //frequency 1
        static String decode(@NotNull String s, @NotNull Charset charset) { return null; }
    }

    //public class URLEncoder
    class URLEncoder$ {
        static String encode(@NotNull String s) { return null; }
        static String encode(@NotNull String s, @NotNull String enc) { return null; }
        //frequency 1
        static String encode(@NotNull String s, @NotNull Charset charset) { return null; }
    }

    //public java.net.Authenticator (shadow created for its nullability, generated)
    class Authenticator$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable PasswordAuthentication getPasswordAuthentication() { return null; }
        @Nullable static PasswordAuthentication requestPasswordAuthentication(Authenticator p0, String p1, InetAddress p2, int p3, String p4, String p5, String p6, URL p7, Authenticator.RequestorType p8) { return null; }
        @Nullable static PasswordAuthentication requestPasswordAuthentication(InetAddress p0, int p1, String p2, String p3, String p4) { return null; }
        @Nullable static PasswordAuthentication requestPasswordAuthentication(String p0, InetAddress p1, int p2, String p3, String p4, String p5) { return null; }
        @Nullable static PasswordAuthentication requestPasswordAuthentication(String p0, InetAddress p1, int p2, String p3, String p4, String p5, URL p6, Authenticator.RequestorType p7) { return null; }
    }

    //public java.net.BindException (shadow created for its nullability, generated)
    class BindException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        BindException$(@Nullable String p0) { }
    }

    //public java.net.ConnectException (shadow created for its nullability, generated)
    class ConnectException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ConnectException$(@Nullable String p0) { }
    }

    //public java.net.ContentHandler (shadow created for its nullability, generated)
    class ContentHandler$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified @Nullable Object getContent(URLConnection p0, Class[] p1) { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified Object getContent(URLConnection p0) { return null; }
    }

    //public java.net.CookieHandler (shadow created for its nullability, generated)
    class CookieHandler$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @NotModified Map<String, List<String>> get(@NotNull URI p0, @NotNull Map<String, List<String>> p1) { return null; }
        @Nullable static CookieHandler getDefault() { return null; }
        @Modified void put(@NotNull URI p0, @NotNull Map<String, List<String>> p1) { }
        static void setDefault(@Nullable CookieHandler p0) { }
    }

    //public java.net.CookieManager (shadow created for its nullability, generated)
    class CookieManager$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        CookieManager$(@Nullable CookieStore p0, @Nullable CookiePolicy p1) { }
        @NotModified Map<String, List<String>> get(@NotNull URI p0, @NotNull Map<String, List<String>> p1) { return null; }
        @Modified void put(@NotNull URI p0, @NotNull Map<String, List<String>> p1) { }
        @Modified void setCookiePolicy(@Nullable CookiePolicy p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified CookieStore getCookieStore() { return null; }
    }

    //public java.net.CookiePolicy (shadow created for its nullability, generated)
    class CookiePolicy$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean shouldAccept(@NotNull URI p0, @NotNull HttpCookie p1) { return false; }
    }

    //public java.net.CookieStore (shadow created for its nullability, generated)
    class CookieStore$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Modified void add(@Nullable URI p0, @NotNull HttpCookie p1) { }
        @NotModified List<HttpCookie> get(@NotNull URI p0) { return null; }
        @Modified boolean remove(@Nullable URI p0, @NotNull HttpCookie p1) { return false; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified boolean removeAll() { return false; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified List<HttpCookie> getCookies() { return null; }
        @NotModified List<URI> getURIs() { return null; }
    }

    //public java.net.DatagramSocket (shadow created for its nullability, generated)
    class DatagramSocket$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DatagramSocket$(@NotNull DatagramSocketImpl p0) { }
        DatagramSocket$(@Nullable SocketAddress p0) { }
        DatagramSocket$(int p0, @Nullable InetAddress p1) { }
        void bind(@Nullable SocketAddress p0) { }
        void connect(@NotNull InetAddress p0, int p1) { }
        void connect(@NotNull SocketAddress p0) { }
        @NotModified @Nullable DatagramChannel getChannel() { return null; }
        @NotModified @Nullable InetAddress getInetAddress() { return null; }
        @NotModified @Nullable InetAddress getLocalAddress() { return null; }
        @NotModified @Nullable SocketAddress getLocalSocketAddress() { return null; }
        @NotModified <T> T getOption(@NotNull SocketOption<T> p0) { return null; }
        @NotModified @Nullable SocketAddress getRemoteSocketAddress() { return null; }
        void joinGroup(@NotNull SocketAddress p0, @Nullable NetworkInterface p1) { }
        void leaveGroup(@NotNull SocketAddress p0, @Nullable NetworkInterface p1) { }
        void receive(@NotNull DatagramPacket p0) { }
        void send(@NotNull DatagramPacket p0) { }
        static void setDatagramSocketImplFactory(@NotNull DatagramSocketImplFactory p0) { }
        @Modified <T> DatagramSocket setOption(@NotNull SocketOption<T> p0, T p1) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setBroadcast(boolean p0) { }
        @Modified void setReceiveBufferSize(int p0) { }
        @Modified void setReuseAddress(boolean p0) { }
        @Modified void setSendBufferSize(int p0) { }
        @Modified void setSoTimeout(int p0) { }
        @Modified void setTrafficClass(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified boolean getBroadcast() { return false; }
        @NotModified int getLocalPort() { return 0; }
        @NotModified int getPort() { return 0; }
        @NotModified int getReceiveBufferSize() { return 0; }
        @NotModified boolean getReuseAddress() { return false; }
        @NotModified int getSendBufferSize() { return 0; }
        @NotModified int getSoTimeout() { return 0; }
        @NotModified int getTrafficClass() { return 0; }
        @NotModified boolean isBound() { return false; }
        @NotModified boolean isClosed() { return false; }
        @NotModified boolean isConnected() { return false; }
    }

    //public java.net.HttpCookie (shadow created for its nullability, generated)
    class HttpCookie$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static boolean domainMatches(@Nullable String p0, @Nullable String p1) { return false; }
        @NotModified @Nullable String getComment() { return null; }
        @NotModified @Nullable String getCommentURL() { return null; }
        @NotModified @Nullable String getDomain() { return null; }
        @NotModified @Nullable String getPath() { return null; }
        @NotModified @Nullable String getPortlist() { return null; }
        static List<HttpCookie> parse(@NotNull String p0) { return null; }
        @Modified void setComment(@Nullable String p0) { }
        @Modified void setCommentURL(@Nullable String p0) { }
        @Modified void setDomain(@Nullable String p0) { }
        @Modified void setPath(@Nullable String p0) { }
        @Modified void setPortlist(@Nullable String p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setDiscard(boolean p0) { }
        @Modified void setHttpOnly(boolean p0) { }
        @Modified void setMaxAge(long p0) { }
        @Modified void setSecure(boolean p0) { }
        @Modified void setValue(String p0) { }
        @Modified void setVersion(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified boolean getDiscard() { return false; }
        @NotModified long getMaxAge() { return 0; }
        @NotModified String getName() { return null; }
        @NotModified boolean getSecure() { return false; }
        @NotModified String getValue() { return null; }
        @NotModified int getVersion() { return 0; }
        @NotModified boolean hasExpired() { return false; }
        @NotModified boolean isHttpOnly() { return false; }
    }

    //public java.net.HttpURLConnection (shadow created for its nullability, generated)
    class HttpURLConnection$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable InputStream getErrorStream() { return null; }
        @Nullable String getHeaderField(int p0) { return null; }
        @Nullable String getHeaderFieldKey(int p0) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setAuthenticator(Authenticator p0) { }
        @Modified void setChunkedStreamingMode(int p0) { }
        @Modified void setFixedLengthStreamingMode(int p0) { }
        @Modified void setFixedLengthStreamingMode(long p0) { }
        @Modified void setInstanceFollowRedirects(boolean p0) { }
        @Modified void setRequestMethod(String p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified boolean getInstanceFollowRedirects() { return false; }
        @NotModified Permission getPermission() { return null; }
        @NotModified String getRequestMethod() { return null; }
    }

    //public java.net.Inet4Address (shadow created for its nullability, generated)
    class Inet4Address$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static Inet4Address ofLiteral(@NotNull String p0) { return null; }
        static Inet4Address ofPosixLiteral(@NotNull String p0) { return null; }
    }

    //public java.net.Inet6Address (shadow created for its nullability, generated)
    class Inet6Address$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static Inet6Address getByAddress(@Nullable String p0, @NotNull byte[] p1, @Nullable NetworkInterface p2) { return null; }
        static Inet6Address getByAddress(@Nullable String p0, @NotNull byte[] p1, int p2) { return null; }
        @NotModified @Nullable NetworkInterface getScopedInterface() { return null; }
        static InetAddress ofLiteral(@NotNull String p0) { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified int getScopeId() { return 0; }
        @NotModified boolean isIPv4CompatibleAddress() { return false; }
    }

    //public java.net.InetSocketAddress (shadow created for its nullability, generated)
    class InetSocketAddress$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InetSocketAddress$(@Nullable InetAddress p0, int p1) { }
        InetSocketAddress$(@NotNull String p0, int p1) { }
        static InetSocketAddress createUnresolved(@NotNull String p0, int p1) { return null; }
        @NotModified @Nullable InetAddress getAddress() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getHostName() { return null; }
        @NotModified String getHostString() { return null; }
        @NotModified int getPort() { return 0; }
        @NotModified boolean isUnresolved() { return false; }
    }

    //public java.net.JarURLConnection (shadow created for its nullability, generated)
    class JarURLConnection$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable Attributes getAttributes() { return null; }
        @Nullable Certificate[] getCertificates() { return null; }
        @Nullable JarEntry getJarEntry() { return null; }
        @Nullable Attributes getMainAttributes() { return null; }
        @Nullable Manifest getManifest() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified String getEntryName() { return null; }
        @NotModified URL getJarFileURL() { return null; }
    }

    //public java.net.MalformedURLException (shadow created for its nullability, generated)
    class MalformedURLException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        MalformedURLException$(@Nullable String p0) { }
    }

    //public java.net.MulticastSocket (shadow created for its nullability, generated)
    class MulticastSocket$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        MulticastSocket$(@Nullable SocketAddress p0) { }
        void joinGroup(@NotNull InetAddress p0) { }
        void joinGroup(@NotNull SocketAddress p0, @Nullable NetworkInterface p1) { }
        void leaveGroup(@NotNull InetAddress p0) { }
        void leaveGroup(@NotNull SocketAddress p0, @Nullable NetworkInterface p1) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setInterface(InetAddress p0) { }
        @Modified void setLoopbackMode(boolean p0) { }
        @Modified void setNetworkInterface(NetworkInterface p0) { }
        @Modified void setTimeToLive(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified InetAddress getInterface() { return null; }
        @NotModified boolean getLoopbackMode() { return false; }
        @NotModified NetworkInterface getNetworkInterface() { return null; }
        @NotModified int getTimeToLive() { return 0; }
    }

    //public java.net.NetworkInterface (shadow created for its nullability, generated)
    class NetworkInterface$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable static NetworkInterface getByIndex(int p0) { return null; }
        @Nullable static NetworkInterface getByInetAddress(@NotNull InetAddress p0) { return null; }
        @Nullable static NetworkInterface getByName(@NotNull String p0) { return null; }
        @NotModified @Nullable String getDisplayName() { return null; }
        @NotModified @Nullable byte[] getHardwareAddress() { return null; }
        @NotModified @Nullable NetworkInterface getParent() { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified int getIndex() { return 0; }
        @NotModified Enumeration<InetAddress> getInetAddresses() { return null; }
        @NotModified List<InterfaceAddress> getInterfaceAddresses() { return null; }
        @NotModified int getMTU() { return 0; }
        @NotModified String getName() { return null; }
        @NotModified Enumeration<NetworkInterface> getSubInterfaces() { return null; }
        @NotModified boolean isLoopback() { return false; }
        @NotModified boolean isPointToPoint() { return false; }
        @NotModified boolean isUp() { return false; }
        @NotModified boolean isVirtual() { return false; }
    }

    //public java.net.NoRouteToHostException (shadow created for its nullability, generated)
    class NoRouteToHostException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        NoRouteToHostException$(@Nullable String p0) { }
    }

    //public java.net.PortUnreachableException (shadow created for its nullability, generated)
    class PortUnreachableException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PortUnreachableException$(@Nullable String p0) { }
    }

    //public java.net.ProtocolException (shadow created for its nullability, generated)
    class ProtocolException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ProtocolException$(@Nullable String p0) { }
    }

    //public java.net.ServerSocket (shadow created for its nullability, generated)
    class ServerSocket$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ServerSocket$(@NotNull SocketImpl p0) { }
        ServerSocket$(int p0, int p1, @Nullable InetAddress p2) { }
        void bind(@Nullable SocketAddress p0) { }
        void bind(@Nullable SocketAddress p0, int p1) { }
        @NotModified @Nullable ServerSocketChannel getChannel() { return null; }
        @NotModified @Nullable InetAddress getInetAddress() { return null; }
        @NotModified @Nullable SocketAddress getLocalSocketAddress() { return null; }
        @NotModified <T> T getOption(@NotNull SocketOption<T> p0) { return null; }
        void implAccept(@NotNull Socket p0) { }
        @Modified <T> ServerSocket setOption(@NotNull SocketOption<T> p0, T p1) { return null; }
        static void setSocketFactory(@NotNull SocketImplFactory p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setPerformancePreferences(int p0, int p1, int p2) { }
        @Modified void setReceiveBufferSize(int p0) { }
        @Modified void setReuseAddress(boolean p0) { }
        @Modified void setSoTimeout(int p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified int getLocalPort() { return 0; }
        @NotModified int getReceiveBufferSize() { return 0; }
        @NotModified boolean getReuseAddress() { return false; }
        @NotModified int getSoTimeout() { return 0; }
        @NotModified boolean isBound() { return false; }
        @NotModified boolean isClosed() { return false; }
    }

    //public java.net.Socket (shadow created for its nullability, generated)
    class Socket$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Socket$(@NotNull InetAddress p0, int p1) { }
        Socket$(@NotNull InetAddress p0, int p1, @Nullable InetAddress p2, int p3) { }
        Socket$(@NotNull InetAddress p0, int p1, boolean p2) { }
        Socket$(@NotNull Proxy p0) { }
        Socket$(@Nullable SocketImpl p0) { }
        Socket$(@Nullable String p0, int p1) { }
        Socket$(@Nullable String p0, int p1, @Nullable InetAddress p2, int p3) { }
        Socket$(@Nullable String p0, int p1, boolean p2) { }
        void bind(@Nullable SocketAddress p0) { }
        void connect(@NotNull SocketAddress p0) { }
        void connect(@NotNull SocketAddress p0, int p1) { }
        @NotModified @Nullable SocketChannel getChannel() { return null; }
        @NotModified @Nullable InetAddress getInetAddress() { return null; }
        @NotModified @Nullable SocketAddress getLocalSocketAddress() { return null; }
        @NotModified <T> T getOption(@NotNull SocketOption<T> p0) { return null; }
        @NotModified @Nullable SocketAddress getRemoteSocketAddress() { return null; }
        @Modified <T> Socket setOption(@NotNull SocketOption<T> p0, T p1) { return null; }
        static void setSocketImplFactory(@Nullable SocketImplFactory p0) { }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void setKeepAlive(boolean p0) { }
        @Modified void setOOBInline(boolean p0) { }
        @Modified void setPerformancePreferences(int p0, int p1, int p2) { }
        @Modified void setReceiveBufferSize(int p0) { }
        @Modified void setReuseAddress(boolean p0) { }
        @Modified void setSendBufferSize(int p0) { }
        @Modified void setSoLinger(boolean p0, int p1) { }
        @Modified void setSoTimeout(int p0) { }
        @Modified void setTcpNoDelay(boolean p0) { }
        @Modified void setTrafficClass(int p0) { }
        @Modified void shutdownInput() { }
        @Modified void shutdownOutput() { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified InputStream getInputStream() { return null; }
        @NotModified boolean getKeepAlive() { return false; }
        @NotModified InetAddress getLocalAddress() { return null; }
        @NotModified int getLocalPort() { return 0; }
        @NotModified boolean getOOBInline() { return false; }
        @NotModified OutputStream getOutputStream() { return null; }
        @NotModified int getPort() { return 0; }
        @NotModified int getReceiveBufferSize() { return 0; }
        @NotModified boolean getReuseAddress() { return false; }
        @NotModified int getSendBufferSize() { return 0; }
        @NotModified int getSoLinger() { return 0; }
        @NotModified int getSoTimeout() { return 0; }
        @NotModified boolean getTcpNoDelay() { return false; }
        @NotModified int getTrafficClass() { return 0; }
        @NotModified boolean isBound() { return false; }
        @NotModified boolean isClosed() { return false; }
        @NotModified boolean isConnected() { return false; }
        @NotModified boolean isInputShutdown() { return false; }
        @NotModified boolean isOutputShutdown() { return false; }
    }

    //public java.net.SocketException (shadow created for its nullability, generated)
    class SocketException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SocketException$(@Nullable String p0) { }
        SocketException$(@Nullable String p0, @Nullable Throwable p1) { }
        SocketException$(@Nullable Throwable p0) { }
    }

    //public java.net.SocketTimeoutException (shadow created for its nullability, generated)
    class SocketTimeoutException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SocketTimeoutException$(@Nullable String p0) { }
    }

    //public java.net.URISyntaxException (shadow created for its nullability, generated)
    class URISyntaxException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        URISyntaxException$(@NotNull String p0, @NotNull String p1) { }
        URISyntaxException$(@NotNull String p0, @NotNull String p1, int p2) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified int getIndex() { return 0; }
        @NotModified String getInput() { return null; }
        @NotModified String getReason() { return null; }
    }

    //public java.net.URLClassLoader (shadow created for its nullability, generated)
    class URLClassLoader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        URLClassLoader$(@Nullable String p0, @NotNull URL[] p1, @Nullable ClassLoader p2) { }
        URLClassLoader$(@Nullable String p0, @NotNull URL[] p1, @Nullable ClassLoader p2, @Nullable URLStreamHandlerFactory p3) { }
        URLClassLoader$(@NotNull URL[] p0) { }
        URLClassLoader$(@NotNull URL[] p0, @Nullable ClassLoader p1) { }
        URLClassLoader$(@NotNull URL[] p0, @Nullable ClassLoader p1, @Nullable URLStreamHandlerFactory p2) { }
        void addURL(@Nullable URL p0) { }
        Package definePackage(@NotNull String p0, @NotNull Manifest p1, @Nullable URL p2) { return null; }
        Class<?> findClass(@NotNull String p0) { return null; }
        @Nullable URL findResource(@NotNull String p0) { return null; }
        Enumeration<URL> findResources(@NotNull String p0) { return null; }
        PermissionCollection getPermissions(@NotNull CodeSource p0) { return null; }
        @Nullable InputStream getResourceAsStream(@NotNull String p0) { return null; }
        static URLClassLoader newInstance(@NotNull URL[] p0) { return null; }
        static URLClassLoader newInstance(@NotNull URL[] p0, @Nullable ClassLoader p1) { return null; }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified URL[] getURLs() { return null; }
    }

    //public java.net.URLConnection (shadow created for its nullability, generated)
    class URLConnection$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable Object getContent(Class<?>[] p0) { return null; }
        @Nullable String getContentEncoding() { return null; }
        @Nullable String getContentType() { return null; }
        @Nullable static String getDefaultRequestProperty(String p0) { return null; }
        @Nullable String getHeaderField(String p0) { return null; }
        @Nullable String getHeaderField(int p0) { return null; }
        @Nullable String getHeaderFieldKey(int p0) { return null; }
        @NotModified @Nullable Permission getPermission() { return null; }
        @NotModified @Nullable String getRequestProperty(String p0) { return null; }
        @Nullable static String guessContentTypeFromStream(InputStream p0) { return null; }

        // a modifying method, declared so (generated from the assumed-defaults list)
        @Modified void addRequestProperty(String p0, String p1) { }
        @Modified void setAllowUserInteraction(boolean p0) { }
        @Modified void setConnectTimeout(int p0) { }
        @Modified void setDefaultUseCaches(boolean p0) { }
        @Modified void setDoInput(boolean p0) { }
        @Modified void setDoOutput(boolean p0) { }
        @Modified void setIfModifiedSince(long p0) { }
        @Modified void setReadTimeout(int p0) { }
        @Modified void setRequestProperty(String p0, String p1) { }
        @Modified void setUseCaches(boolean p0) { }

        // a query, declared so (generated from the assumed-defaults list)
        @NotModified boolean getAllowUserInteraction() { return false; }
        @NotModified int getConnectTimeout() { return 0; }
        @NotModified boolean getDefaultUseCaches() { return false; }
        @NotModified boolean getDoInput() { return false; }
        @NotModified boolean getDoOutput() { return false; }
        @NotModified long getIfModifiedSince() { return 0; }
        @NotModified int getReadTimeout() { return 0; }
        @NotModified Map<String, List<String>> getRequestProperties() { return null; }
        @NotModified URL getURL() { return null; }
        @NotModified boolean getUseCaches() { return false; }
    }

    //public java.net.URLStreamHandlerFactory (shadow created for its nullability, generated)
    class URLStreamHandlerFactory$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable URLStreamHandler createURLStreamHandler(@NotNull String p0) { return null; }
    }

    //public java.net.UnknownHostException (shadow created for its nullability, generated)
    class UnknownHostException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnknownHostException$(@Nullable String p0) { }
    }

    //public java.net.UnknownServiceException (shadow created for its nullability, generated)
    class UnknownServiceException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnknownServiceException$(@Nullable String p0) { }
    }
}
