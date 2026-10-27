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
import java.net.*;
import java.nio.charset.Charset;

import io.codelaser.maddi.annotation.ImmutableContainer;
import io.codelaser.maddi.annotation.Nullable;
import io.codelaser.maddi.annotation.NotNull;

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
        @Nullable java.net.PasswordAuthentication getPasswordAuthentication() { return null; }
        @Nullable static java.net.PasswordAuthentication requestPasswordAuthentication(java.net.Authenticator p0, java.lang.String p1, java.net.InetAddress p2, int p3, java.lang.String p4, java.lang.String p5, java.lang.String p6, java.net.URL p7, java.net.Authenticator.RequestorType p8) { return null; }
        @Nullable static java.net.PasswordAuthentication requestPasswordAuthentication(java.net.InetAddress p0, int p1, java.lang.String p2, java.lang.String p3, java.lang.String p4) { return null; }
        @Nullable static java.net.PasswordAuthentication requestPasswordAuthentication(java.lang.String p0, java.net.InetAddress p1, int p2, java.lang.String p3, java.lang.String p4, java.lang.String p5) { return null; }
        @Nullable static java.net.PasswordAuthentication requestPasswordAuthentication(java.lang.String p0, java.net.InetAddress p1, int p2, java.lang.String p3, java.lang.String p4, java.lang.String p5, java.net.URL p6, java.net.Authenticator.RequestorType p7) { return null; }
    }

    //public java.net.BindException (shadow created for its nullability, generated)
    class BindException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        BindException$(@Nullable java.lang.String p0) { }
    }

    //public java.net.ConnectException (shadow created for its nullability, generated)
    class ConnectException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ConnectException$(@Nullable java.lang.String p0) { }
    }

    //public java.net.ContentHandler (shadow created for its nullability, generated)
    class ContentHandler$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.Object getContent(java.net.URLConnection p0, java.lang.Class[] p1) { return null; }
    }

    //public java.net.CookieHandler (shadow created for its nullability, generated)
    class CookieHandler$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        java.util.Map<java.lang.String, java.util.List<java.lang.String>> get(@NotNull java.net.URI p0, @NotNull java.util.Map<java.lang.String, java.util.List<java.lang.String>> p1) { return null; }
        @Nullable static java.net.CookieHandler getDefault() { return null; }
        void put(@NotNull java.net.URI p0, @NotNull java.util.Map<java.lang.String, java.util.List<java.lang.String>> p1) { }
        static void setDefault(@Nullable java.net.CookieHandler p0) { }
    }

    //public java.net.CookieManager (shadow created for its nullability, generated)
    class CookieManager$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        CookieManager$(@Nullable java.net.CookieStore p0, @Nullable java.net.CookiePolicy p1) { }
        java.util.Map<java.lang.String, java.util.List<java.lang.String>> get(@NotNull java.net.URI p0, @NotNull java.util.Map<java.lang.String, java.util.List<java.lang.String>> p1) { return null; }
        void put(@NotNull java.net.URI p0, @NotNull java.util.Map<java.lang.String, java.util.List<java.lang.String>> p1) { }
        void setCookiePolicy(@Nullable java.net.CookiePolicy p0) { }
    }

    //public java.net.CookiePolicy (shadow created for its nullability, generated)
    class CookiePolicy$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        boolean shouldAccept(@NotNull java.net.URI p0, @NotNull java.net.HttpCookie p1) { return false; }
    }

    //public java.net.CookieStore (shadow created for its nullability, generated)
    class CookieStore$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        void add(@Nullable java.net.URI p0, @NotNull java.net.HttpCookie p1) { }
        java.util.List<java.net.HttpCookie> get(@NotNull java.net.URI p0) { return null; }
        boolean remove(@Nullable java.net.URI p0, @NotNull java.net.HttpCookie p1) { return false; }
    }

    //public java.net.DatagramSocket (shadow created for its nullability, generated)
    class DatagramSocket$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        DatagramSocket$(@NotNull java.net.DatagramSocketImpl p0) { }
        DatagramSocket$(@Nullable java.net.SocketAddress p0) { }
        DatagramSocket$(int p0, @Nullable java.net.InetAddress p1) { }
        void bind(@Nullable java.net.SocketAddress p0) { }
        void connect(@NotNull java.net.InetAddress p0, int p1) { }
        void connect(@NotNull java.net.SocketAddress p0) { }
        @Nullable java.nio.channels.DatagramChannel getChannel() { return null; }
        @Nullable java.net.InetAddress getInetAddress() { return null; }
        @Nullable java.net.InetAddress getLocalAddress() { return null; }
        @Nullable java.net.SocketAddress getLocalSocketAddress() { return null; }
        <T> T getOption(@NotNull java.net.SocketOption<T> p0) { return null; }
        @Nullable java.net.SocketAddress getRemoteSocketAddress() { return null; }
        void joinGroup(@NotNull java.net.SocketAddress p0, @Nullable java.net.NetworkInterface p1) { }
        void leaveGroup(@NotNull java.net.SocketAddress p0, @Nullable java.net.NetworkInterface p1) { }
        void receive(@NotNull java.net.DatagramPacket p0) { }
        void send(@NotNull java.net.DatagramPacket p0) { }
        static void setDatagramSocketImplFactory(@NotNull java.net.DatagramSocketImplFactory p0) { }
        <T> java.net.DatagramSocket setOption(@NotNull java.net.SocketOption<T> p0, T p1) { return null; }
    }

    //public java.net.HttpCookie (shadow created for its nullability, generated)
    class HttpCookie$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static boolean domainMatches(@Nullable java.lang.String p0, @Nullable java.lang.String p1) { return false; }
        @Nullable java.lang.String getComment() { return null; }
        @Nullable java.lang.String getCommentURL() { return null; }
        @Nullable java.lang.String getDomain() { return null; }
        @Nullable java.lang.String getPath() { return null; }
        @Nullable java.lang.String getPortlist() { return null; }
        static java.util.List<java.net.HttpCookie> parse(@NotNull java.lang.String p0) { return null; }
        void setComment(@Nullable java.lang.String p0) { }
        void setCommentURL(@Nullable java.lang.String p0) { }
        void setDomain(@Nullable java.lang.String p0) { }
        void setPath(@Nullable java.lang.String p0) { }
        void setPortlist(@Nullable java.lang.String p0) { }
    }

    //public java.net.HttpURLConnection (shadow created for its nullability, generated)
    class HttpURLConnection$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.io.InputStream getErrorStream() { return null; }
        @Nullable java.lang.String getHeaderField(int p0) { return null; }
        @Nullable java.lang.String getHeaderFieldKey(int p0) { return null; }
    }

    //public java.net.Inet4Address (shadow created for its nullability, generated)
    class Inet4Address$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static java.net.Inet4Address ofLiteral(@NotNull java.lang.String p0) { return null; }
        static java.net.Inet4Address ofPosixLiteral(@NotNull java.lang.String p0) { return null; }
    }

    //public java.net.Inet6Address (shadow created for its nullability, generated)
    class Inet6Address$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        static java.net.Inet6Address getByAddress(@Nullable java.lang.String p0, @NotNull byte[] p1, @Nullable java.net.NetworkInterface p2) { return null; }
        static java.net.Inet6Address getByAddress(@Nullable java.lang.String p0, @NotNull byte[] p1, int p2) { return null; }
        @Nullable java.net.NetworkInterface getScopedInterface() { return null; }
        static java.net.InetAddress ofLiteral(@NotNull java.lang.String p0) { return null; }
    }

    //public java.net.InetSocketAddress (shadow created for its nullability, generated)
    class InetSocketAddress$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        InetSocketAddress$(@Nullable java.net.InetAddress p0, int p1) { }
        InetSocketAddress$(@NotNull java.lang.String p0, int p1) { }
        static java.net.InetSocketAddress createUnresolved(@NotNull java.lang.String p0, int p1) { return null; }
        @Nullable java.net.InetAddress getAddress() { return null; }
    }

    //public java.net.JarURLConnection (shadow created for its nullability, generated)
    class JarURLConnection$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.util.jar.Attributes getAttributes() { return null; }
        @Nullable java.security.cert.Certificate[] getCertificates() { return null; }
        @Nullable java.util.jar.JarEntry getJarEntry() { return null; }
        @Nullable java.util.jar.Attributes getMainAttributes() { return null; }
        @Nullable java.util.jar.Manifest getManifest() { return null; }
    }

    //public java.net.MalformedURLException (shadow created for its nullability, generated)
    class MalformedURLException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        MalformedURLException$(@Nullable java.lang.String p0) { }
    }

    //public java.net.MulticastSocket (shadow created for its nullability, generated)
    class MulticastSocket$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        MulticastSocket$(@Nullable java.net.SocketAddress p0) { }
        void joinGroup(@NotNull java.net.InetAddress p0) { }
        void joinGroup(@NotNull java.net.SocketAddress p0, @Nullable java.net.NetworkInterface p1) { }
        void leaveGroup(@NotNull java.net.InetAddress p0) { }
        void leaveGroup(@NotNull java.net.SocketAddress p0, @Nullable java.net.NetworkInterface p1) { }
    }

    //public java.net.NetworkInterface (shadow created for its nullability, generated)
    class NetworkInterface$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable static java.net.NetworkInterface getByIndex(int p0) { return null; }
        @Nullable static java.net.NetworkInterface getByInetAddress(@NotNull java.net.InetAddress p0) { return null; }
        @Nullable static java.net.NetworkInterface getByName(@NotNull java.lang.String p0) { return null; }
        @Nullable java.lang.String getDisplayName() { return null; }
        @Nullable byte[] getHardwareAddress() { return null; }
        @Nullable java.net.NetworkInterface getParent() { return null; }
    }

    //public java.net.NoRouteToHostException (shadow created for its nullability, generated)
    class NoRouteToHostException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        NoRouteToHostException$(@Nullable java.lang.String p0) { }
    }

    //public java.net.PortUnreachableException (shadow created for its nullability, generated)
    class PortUnreachableException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        PortUnreachableException$(@Nullable java.lang.String p0) { }
    }

    //public java.net.ProtocolException (shadow created for its nullability, generated)
    class ProtocolException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ProtocolException$(@Nullable java.lang.String p0) { }
    }

    //public java.net.ServerSocket (shadow created for its nullability, generated)
    class ServerSocket$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        ServerSocket$(@NotNull java.net.SocketImpl p0) { }
        ServerSocket$(int p0, int p1, @Nullable java.net.InetAddress p2) { }
        void bind(@Nullable java.net.SocketAddress p0) { }
        void bind(@Nullable java.net.SocketAddress p0, int p1) { }
        @Nullable java.nio.channels.ServerSocketChannel getChannel() { return null; }
        @Nullable java.net.InetAddress getInetAddress() { return null; }
        @Nullable java.net.SocketAddress getLocalSocketAddress() { return null; }
        <T> T getOption(@NotNull java.net.SocketOption<T> p0) { return null; }
        void implAccept(@NotNull java.net.Socket p0) { }
        <T> java.net.ServerSocket setOption(@NotNull java.net.SocketOption<T> p0, T p1) { return null; }
        static void setSocketFactory(@NotNull java.net.SocketImplFactory p0) { }
    }

    //public java.net.Socket (shadow created for its nullability, generated)
    class Socket$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        Socket$(@NotNull java.net.InetAddress p0, int p1) { }
        Socket$(@NotNull java.net.InetAddress p0, int p1, @Nullable java.net.InetAddress p2, int p3) { }
        Socket$(@NotNull java.net.InetAddress p0, int p1, boolean p2) { }
        Socket$(@NotNull java.net.Proxy p0) { }
        Socket$(@Nullable java.net.SocketImpl p0) { }
        Socket$(@Nullable java.lang.String p0, int p1) { }
        Socket$(@Nullable java.lang.String p0, int p1, @Nullable java.net.InetAddress p2, int p3) { }
        Socket$(@Nullable java.lang.String p0, int p1, boolean p2) { }
        void bind(@Nullable java.net.SocketAddress p0) { }
        void connect(@NotNull java.net.SocketAddress p0) { }
        void connect(@NotNull java.net.SocketAddress p0, int p1) { }
        @Nullable java.nio.channels.SocketChannel getChannel() { return null; }
        @Nullable java.net.InetAddress getInetAddress() { return null; }
        @Nullable java.net.SocketAddress getLocalSocketAddress() { return null; }
        <T> T getOption(@NotNull java.net.SocketOption<T> p0) { return null; }
        @Nullable java.net.SocketAddress getRemoteSocketAddress() { return null; }
        <T> java.net.Socket setOption(@NotNull java.net.SocketOption<T> p0, T p1) { return null; }
        static void setSocketImplFactory(@Nullable java.net.SocketImplFactory p0) { }
    }

    //public java.net.SocketException (shadow created for its nullability, generated)
    class SocketException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SocketException$(@Nullable java.lang.String p0) { }
        SocketException$(@Nullable java.lang.String p0, @Nullable java.lang.Throwable p1) { }
        SocketException$(@Nullable java.lang.Throwable p0) { }
    }

    //public java.net.SocketTimeoutException (shadow created for its nullability, generated)
    class SocketTimeoutException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        SocketTimeoutException$(@Nullable java.lang.String p0) { }
    }

    //public java.net.URISyntaxException (shadow created for its nullability, generated)
    class URISyntaxException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        URISyntaxException$(@NotNull java.lang.String p0, @NotNull java.lang.String p1) { }
        URISyntaxException$(@NotNull java.lang.String p0, @NotNull java.lang.String p1, int p2) { }
    }

    //public java.net.URLClassLoader (shadow created for its nullability, generated)
    class URLClassLoader$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        URLClassLoader$(@Nullable java.lang.String p0, @NotNull java.net.URL[] p1, @Nullable java.lang.ClassLoader p2) { }
        URLClassLoader$(@Nullable java.lang.String p0, @NotNull java.net.URL[] p1, @Nullable java.lang.ClassLoader p2, @Nullable java.net.URLStreamHandlerFactory p3) { }
        URLClassLoader$(@NotNull java.net.URL[] p0) { }
        URLClassLoader$(@NotNull java.net.URL[] p0, @Nullable java.lang.ClassLoader p1) { }
        URLClassLoader$(@NotNull java.net.URL[] p0, @Nullable java.lang.ClassLoader p1, @Nullable java.net.URLStreamHandlerFactory p2) { }
        void addURL(@Nullable java.net.URL p0) { }
        java.lang.Package definePackage(@NotNull java.lang.String p0, @NotNull java.util.jar.Manifest p1, @Nullable java.net.URL p2) { return null; }
        java.lang.Class<?> findClass(@NotNull java.lang.String p0) { return null; }
        @Nullable java.net.URL findResource(@NotNull java.lang.String p0) { return null; }
        java.util.Enumeration<java.net.URL> findResources(@NotNull java.lang.String p0) { return null; }
        java.security.PermissionCollection getPermissions(@NotNull java.security.CodeSource p0) { return null; }
        @Nullable java.io.InputStream getResourceAsStream(@NotNull java.lang.String p0) { return null; }
        static java.net.URLClassLoader newInstance(@NotNull java.net.URL[] p0) { return null; }
        static java.net.URLClassLoader newInstance(@NotNull java.net.URL[] p0, @Nullable java.lang.ClassLoader p1) { return null; }
    }

    //public java.net.URLConnection (shadow created for its nullability, generated)
    class URLConnection$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.lang.Object getContent(java.lang.Class<?>[] p0) { return null; }
        @Nullable java.lang.String getContentEncoding() { return null; }
        @Nullable java.lang.String getContentType() { return null; }
        @Nullable static java.lang.String getDefaultRequestProperty(java.lang.String p0) { return null; }
        @Nullable java.lang.String getHeaderField(java.lang.String p0) { return null; }
        @Nullable java.lang.String getHeaderField(int p0) { return null; }
        @Nullable java.lang.String getHeaderFieldKey(int p0) { return null; }
        @Nullable java.security.Permission getPermission() { return null; }
        @Nullable java.lang.String getRequestProperty(java.lang.String p0) { return null; }
        @Nullable static java.lang.String guessContentTypeFromStream(java.io.InputStream p0) { return null; }
    }

    //public java.net.URLStreamHandlerFactory (shadow created for its nullability, generated)
    class URLStreamHandlerFactory$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        @Nullable java.net.URLStreamHandler createURLStreamHandler(@NotNull java.lang.String p0) { return null; }
    }

    //public java.net.UnknownHostException (shadow created for its nullability, generated)
    class UnknownHostException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnknownHostException$(@Nullable java.lang.String p0) { }
    }

    //public java.net.UnknownServiceException (shadow created for its nullability, generated)
    class UnknownServiceException$ {
        // nullability from JSpecify's JDK (generated, jspecify-jdk-nullness.py)
        UnknownServiceException$(@Nullable java.lang.String p0) { }
    }
}
