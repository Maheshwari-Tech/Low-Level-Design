package code.creational.builder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class BuilderDemo {
    private BuilderDemo() {
    }

    private record HttpRequest(String method, URI uri, Map<String, String> headers) {
        private HttpRequest {
            Objects.requireNonNull(method, "method");
            Objects.requireNonNull(uri, "uri");
            headers = Map.copyOf(headers);
        }

        static Builder builder(URI uri) {
            return new Builder(uri);
        }

        private static final class Builder {
            private final URI uri;
            private final Map<String, String> headers = new LinkedHashMap<>();
            private String method = "GET";

            private Builder(URI uri) {
                this.uri = Objects.requireNonNull(uri, "uri");
            }

            Builder method(String value) {
                method = Objects.requireNonNull(value, "method").toUpperCase(Locale.ROOT);
                return this;
            }

            Builder header(String name, String value) {
                headers.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(value, "value"));
                return this;
            }

            HttpRequest build() {
                if (!uri.isAbsolute()) {
                    throw new IllegalStateException("request URI must be absolute");
                }
                return new HttpRequest(method, uri, headers);
            }
        }
    }

    public static void main(String[] args) {
        HttpRequest request = HttpRequest.builder(URI.create("https://example.test/orders"))
                .method("post")
                .header("Idempotency-Key", "order-42")
                .build();
        System.out.println(request);
    }
}
