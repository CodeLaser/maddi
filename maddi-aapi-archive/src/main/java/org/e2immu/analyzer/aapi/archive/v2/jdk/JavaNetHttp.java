package org.e2immu.analyzer.aapi.archive.v2.jdk;
import org.e2immu.annotation.*;
import org.e2immu.annotation.method.GetSet;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.BiPredicate;
import java.util.function.Supplier;

public class JavaNetHttp {
    public static final String PACKAGE_NAME = "java.net.http";
    //public abstract class HttpRequest
    @ImmutableContainer
    class HttpRequest$ {
        //public interface BodyPublisher implements Flow.Publisher<ByteBuffer>
        //@Container[M]
        class BodyPublisher {long contentLength() { return 0L; } }

        //public static class BodyPublishers
        @Independent
        class BodyPublishers {
            //@Independent[T]
            static HttpRequest.BodyPublisher fromPublisher(Flow.Publisher<? extends ByteBuffer> publisher) {
                return null;
            }

            //@Independent[T]
            static HttpRequest.BodyPublisher fromPublisher(
                Flow.Publisher<? extends ByteBuffer> publisher,
                long contentLength) { return null; }

            //@Independent[T]
            static HttpRequest.BodyPublisher ofString(String body) { return null; }

            //@Independent[T]
            static HttpRequest.BodyPublisher ofString(String s, Charset charset) { return null; }

            //@Independent[T]
            static HttpRequest.BodyPublisher ofInputStream(
                /*@IgnoreModifications[T]*/ Supplier<? extends InputStream> streamSupplier) { return null; }

            //@Independent[T]
            static HttpRequest.BodyPublisher ofByteArray(byte [] buf) { return null; }

            //@Independent[T]
            static HttpRequest.BodyPublisher ofByteArray(byte [] buf, int offset, int length) { return null; }

            //@Independent[T]
            static HttpRequest.BodyPublisher ofFile(
                /*@Immutable(hc=true)[T] @Independent(hc=true)[T] @NotModified[T]*/ Path path) { return null; }

            //@Independent[T]
            static HttpRequest.BodyPublisher ofByteArrays(Iterable<byte []> iter) { return null; }

            //@Independent[T]
            static HttpRequest.BodyPublisher noBody() { return null; }

            //@Independent[T]
            static HttpRequest.BodyPublisher concat(HttpRequest.BodyPublisher ... publishers) { return null; }
        }

        //public interface Builder
        @Container(builds = HttpRequest.class)
        class Builder {
            @Fluent @Commutable
            HttpRequest.Builder uri(URI uRI) { return null; }
            @Fluent @Commutable
            HttpRequest.Builder expectContinue(boolean b) { return null; }
            @Fluent @Commutable
            HttpRequest.Builder version(HttpClient.Version version) { return null; }
            @Fluent @Commutable(seq = "header,0", multi = "headers")
            HttpRequest.Builder header(String string, String string1) { return null; }
            @Fluent @Commutable(seq = "header")
            HttpRequest.Builder headers(String ... string) { return null; }
            @Fluent @Commutable
            HttpRequest.Builder timeout(Duration duration) { return null; }
            @Fluent @Commutable(seq = "header,0")
            HttpRequest.Builder setHeader(String string, String string1) { return null; }
            @Fluent @Commutable
            HttpRequest.Builder GET() { return null; }
            @Fluent @Commutable
            HttpRequest.Builder POST(HttpRequest.BodyPublisher bodyPublisher) { return null; }
            @Fluent @Commutable
            HttpRequest.Builder PUT(HttpRequest.BodyPublisher bodyPublisher) { return null; }
            @Fluent @Commutable
            HttpRequest.Builder DELETE() { return null; }
            @Fluent @Commutable
            HttpRequest.Builder HEAD() { return null; }
            @Fluent @Commutable
            HttpRequest.Builder method(String string, HttpRequest.BodyPublisher bodyPublisher) { return null; }
            HttpRequest build() { return null; }
            HttpRequest.Builder copy() { return null; }
        }
        @Independent @NotNull @GetSet(equivalent = true)
        static HttpRequest.Builder newBuilder(URI uri) { return null; }
        static HttpRequest.Builder newBuilder(
            HttpRequest request,
            /*@IgnoreModifications[T]*/ BiPredicate<String, String> filter) { return null; }
        @Independent @NotNull
        static HttpRequest.Builder newBuilder() { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Optional<HttpRequest.BodyPublisher> bodyPublisher() { return null; }
        String method() { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Optional<Duration> timeout() { return null; }
        boolean expectContinue() { return false; }
        URI uri() { return null; }
        //@Immutable(hc=true)[T] @Independent(hc=true)[T]
        Optional<HttpClient.Version> version() { return null; }
        HttpHeaders headers() { return null; }
        //override from java.lang.Object
        //@NotModified[H]
        public boolean equals(/*@Immutable(hc=true)[T] @Independent[M] @NotModified[T]*/ Object obj) { return false; }

        //override from java.lang.Object
        //@NotModified[H]
        public int hashCode() { return 0; }
    }

    interface HttpResponse$ {

        @Independent
        interface BodySubscribers {

            HttpResponse.BodySubscriber<Void> fromSubscriber(Flow.Subscriber<? super List<ByteBuffer>> subscriber);
        }

        @Independent
        interface BodyHandlers {
            HttpResponse.BodyHandler<Void> fromSubscriber(Flow.Subscriber<? super List<ByteBuffer>> subscriber);
        }
    }

    @ImmutableContainer
    interface HttpClient$ {

        @Container(builds = HttpClient.class)
        interface Builder {

            @Fluent
            @Commutable
            HttpRequest.Builder connectTimeout(Duration duration);

            @Fluent
            @Commutable
            HttpRequest.Builder followRedirects(HttpClient.Redirect policy);

            @Fluent
            @Commutable
            HttpRequest.Builder version(HttpClient.Version version);
        }

        @NotNull
        @Independent
        HttpClient.Builder newBuilder();

        @Independent
        HttpClient newHttpClient();

        @NotModified
        <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler);
    }
}
