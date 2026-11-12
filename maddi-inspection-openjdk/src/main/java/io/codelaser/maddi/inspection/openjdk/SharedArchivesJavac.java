package io.codelaser.maddi.inspection.openjdk;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.channels.SeekableByteChannel;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.tools.DiagnosticListener;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;

import com.sun.tools.javac.file.CacheFSInfo;
import com.sun.tools.javac.file.FSInfo;
import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;

/**
 * The javac-internal half of {@link SharedArchives}: loaded only when {@code com.sun.tools.javac.file} is exported.
 */
final class SharedArchivesJavac {
    private SharedArchivesJavac() {
    }

    /** As {@code JavacTool.getStandardFileManager}, with the shared archives' {@code FSInfo} in the context. */
    static StandardJavaFileManager newFileManager(Cache cache, DiagnosticListener<? super JavaFileObject> diagnostics,
                                                  Locale locale, Charset charset) {
        Context context = new Context();
        context.put(Locale.class, locale);
        if (diagnostics != null) context.put(DiagnosticListener.class, diagnostics);
        PrintWriter pw = charset == null
                ? new PrintWriter(System.err, true)
                : new PrintWriter(new OutputStreamWriter(System.err, charset), true);
        context.put(Log.errKey, pw);
        context.put(FSInfo.class, (Context.Factory<FSInfo>) c -> {
            FSInfo instance = new SharedFSInfo(cache);
            c.put(FSInfo.class, instance);
            return instance;
        });
        return new JavacFileManager(context, true, charset);
    }

    private static final class SharedFSInfo extends CacheFSInfo {
        private final Cache cache;

        SharedFSInfo(Cache cache) {
            this.cache = cache;
        }

        @Override
        public synchronized FileSystemProvider getJarFSProvider() {
            FileSystemProvider real = super.getJarFSProvider();
            return real == null ? null : cache.provider(real);
        }
    }

    /** The archive file systems, by absolute path and environment (read-only, multi-release version). */
    static final class Cache {
        private record Key(Path path, Map<String, ?> env) {
        }

        private final Map<Key, FileSystem> fileSystems = new ConcurrentHashMap<>();
        private volatile SharingProvider provider;
        private volatile boolean closed;

        FileSystemProvider provider(FileSystemProvider real) {
            SharingProvider p = provider;
            if (p == null || p.real != real) {
                synchronized (this) {
                    if (provider == null || provider.real != real) provider = new SharingProvider(real, this);
                    p = provider;
                }
            }
            return p;
        }

        FileSystem open(FileSystemProvider real, Path path, Map<String, ?> env) throws IOException {
            if (closed) throw new IOException("the inspector's archives are closed: " + path);
            Key key = new Key(path.toAbsolutePath().normalize(), Map.copyOf(env));
            try {
                return fileSystems.computeIfAbsent(key, k -> {
                    try {
                        return real.newFileSystem(path, env);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (UncheckedIOException e) {
                throw e.getCause(); // a ZipException stays one: javac's ArchiveContainer reports it as such
            }
        }

        int size() {
            return fileSystems.size();
        }

        void close() throws IOException {
            closed = true;
            IOException first = null;
            for (FileSystem fs : fileSystems.values()) {
                try {
                    fs.close();
                } catch (IOException e) {
                    if (first == null) first = e;
                }
            }
            fileSystems.clear();
            if (first != null) throw first;
        }
    }

    /**
     * The jar provider, except that {@code newFileSystem(Path, env)} hands out the shared file system, behind a view
     * whose {@code close} does nothing: javac closes an archive with its file manager, the cache with the inspector.
     */
    private static final class SharingProvider extends FileSystemProvider {
        private final FileSystemProvider real;
        private final Cache cache;

        SharingProvider(FileSystemProvider real, Cache cache) {
            this.real = real;
            this.cache = cache;
        }

        @Override
        public FileSystem newFileSystem(Path path, Map<String, ?> env) throws IOException {
            return new NonClosingFileSystem(cache.open(real, path, env));
        }

        @Override
        public String getScheme() {
            return real.getScheme();
        }

        @Override
        public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
            return real.newFileSystem(uri, env);
        }

        @Override
        public FileSystem getFileSystem(URI uri) {
            return real.getFileSystem(uri);
        }

        @Override
        public Path getPath(URI uri) {
            return real.getPath(uri);
        }

        @Override
        public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
                                                  FileAttribute<?>... attrs) throws IOException {
            return real.newByteChannel(path, options, attrs);
        }

        @Override
        public DirectoryStream<Path> newDirectoryStream(Path dir, DirectoryStream.Filter<? super Path> filter)
                throws IOException {
            return real.newDirectoryStream(dir, filter);
        }

        @Override
        public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
            real.createDirectory(dir, attrs);
        }

        @Override
        public void delete(Path path) throws IOException {
            real.delete(path);
        }

        @Override
        public void copy(Path source, Path target, CopyOption... options) throws IOException {
            real.copy(source, target, options);
        }

        @Override
        public void move(Path source, Path target, CopyOption... options) throws IOException {
            real.move(source, target, options);
        }

        @Override
        public boolean isSameFile(Path path, Path path2) throws IOException {
            return real.isSameFile(path, path2);
        }

        @Override
        public boolean isHidden(Path path) throws IOException {
            return real.isHidden(path);
        }

        @Override
        public FileStore getFileStore(Path path) throws IOException {
            return real.getFileStore(path);
        }

        @Override
        public void checkAccess(Path path, AccessMode... modes) throws IOException {
            real.checkAccess(path, modes);
        }

        @Override
        public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type, LinkOption... options) {
            return real.getFileAttributeView(path, type, options);
        }

        @Override
        public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type, LinkOption... options)
                throws IOException {
            return real.readAttributes(path, type, options);
        }

        @Override
        public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options)
                throws IOException {
            return real.readAttributes(path, attributes, options);
        }

        @Override
        public void setAttribute(Path path, String attribute, Object value, LinkOption... options) throws IOException {
            real.setAttribute(path, attribute, value, options);
        }
    }

    /** A shared archive as one file manager sees it: everything but {@code close}. */
    private static final class NonClosingFileSystem extends FileSystem {
        private final FileSystem delegate;

        NonClosingFileSystem(FileSystem delegate) {
            this.delegate = delegate;
        }

        @Override
        public FileSystemProvider provider() {
            return delegate.provider();
        }

        @Override
        public void close() {
            // shared: closed with the inspector (SharedArchives.close), not with one file manager
        }

        @Override
        public boolean isOpen() {
            return delegate.isOpen();
        }

        @Override
        public boolean isReadOnly() {
            return delegate.isReadOnly();
        }

        @Override
        public String getSeparator() {
            return delegate.getSeparator();
        }

        @Override
        public Iterable<Path> getRootDirectories() {
            return delegate.getRootDirectories();
        }

        @Override
        public Iterable<FileStore> getFileStores() {
            return delegate.getFileStores();
        }

        @Override
        public Set<String> supportedFileAttributeViews() {
            return delegate.supportedFileAttributeViews();
        }

        @Override
        public Path getPath(String first, String... more) {
            return delegate.getPath(first, more);
        }

        @Override
        public PathMatcher getPathMatcher(String syntaxAndPattern) {
            return delegate.getPathMatcher(syntaxAndPattern);
        }

        @Override
        public UserPrincipalLookupService getUserPrincipalLookupService() {
            return delegate.getUserPrincipalLookupService();
        }

        @Override
        public WatchService newWatchService() throws IOException {
            return delegate.newWatchService();
        }
    }
}
