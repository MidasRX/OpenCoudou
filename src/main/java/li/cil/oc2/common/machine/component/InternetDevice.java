/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.machine.component;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.config.Config;
import li.cil.oc2.common.machine.LuaMachineLog;
import li.cil.oc2.common.machine.lua.LuaUserdata;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * OpenComputers-style {@code internet} component, enabling {@code wget}/{@code oppm}/installers (the
 * "git clone" use case). {@code request} performs an HTTP(S) request and returns a non-blocking
 * userdata handle; {@code connect} opens a raw TCP socket handle. Outbound hosts are filtered against
 * {@link Config#deniedHosts}/{@link Config#allowedHosts}, and the whole component is gated on
 * {@link Config#internetCardEnabled}.
 */
@SuppressWarnings("unused")
public final class InternetDevice {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();

    @Callback(synchronize = false, description = "Returns whether HTTP requests are enabled.")
    public boolean isHttpEnabled() {
        return Config.internetCardEnabled;
    }

    @Callback(synchronize = false, description = "Returns whether TCP connections are enabled.")
    public boolean isTcpEnabled() {
        return Config.internetCardEnabled;
    }

    @Callback(synchronize = false, description = "Starts an HTTP request, returning a connection handle.")
    public Object request(@Parameter("url") final String url,
                          @Nullable @Parameter("postData") final String postData,
                          @Nullable @Parameter("headers") final Map<String, Object> headers) {
        if (!Config.internetCardEnabled) {
            throw new IllegalStateException("http requests are unavailable");
        }
        final URI uri;
        try {
            uri = URI.create(url);
        } catch (final Exception e) {
            throw new IllegalArgumentException("invalid address");
        }
        checkHostAllowed(uri.getHost());

        final HttpRequest.Builder builder = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30));
        if (headers != null) {
            headers.forEach((k, v) -> {
                try {
                    builder.header(k, String.valueOf(v));
                } catch (final Exception ignored) {
                }
            });
        }
        if (postData != null && !postData.isEmpty()) {
            builder.POST(HttpRequest.BodyPublishers.ofString(postData, StandardCharsets.UTF_8));
        } else {
            builder.GET();
        }

        LuaMachineLog.log("internet", "request %s", url);
        return new HttpRequestHandle(HTTP.sendAsync(builder.build(), HttpResponse.BodyHandlers.ofByteArray()));
    }

    @Callback(synchronize = false, description = "Opens a TCP connection, returning a socket handle.")
    public Object connect(@Parameter("address") final String address, @Nullable @Parameter("port") final Integer port) {
        if (!Config.internetCardEnabled) {
            throw new IllegalStateException("tcp connections are unavailable");
        }
        String host = address;
        int p = port != null ? port : 0;
        if (host.contains(":") && port == null) {
            final int idx = host.lastIndexOf(':');
            try {
                p = Integer.parseInt(host.substring(idx + 1));
                host = host.substring(0, idx);
            } catch (final NumberFormatException ignored) {
            }
        }
        checkHostAllowed(host);
        return new TcpConnectHandle(host, p);
    }

    ///////////////////////////////////////////////////////////////////

    private static void checkHostAllowed(@Nullable final String host) {
        if (host == null || host.isEmpty()) {
            throw new IllegalArgumentException("invalid address");
        }
        final InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (final Exception e) {
            throw new IllegalArgumentException("could not resolve address");
        }
        for (final InetAddress address : addresses) {
            for (final String cidr : Config.deniedHosts) {
                if (matchesCidr(address, cidr)) {
                    throw new IllegalStateException("address is blacklisted");
                }
            }
            if (!Config.allowedHosts.isEmpty()) {
                boolean allowed = false;
                for (final String cidr : Config.allowedHosts) {
                    if (matchesCidr(address, cidr)) {
                        allowed = true;
                        break;
                    }
                }
                if (!allowed) {
                    throw new IllegalStateException("address is not whitelisted");
                }
            }
        }
    }

    private static boolean matchesCidr(final InetAddress address, final String cidr) {
        try {
            final String[] parts = cidr.split("/");
            final InetAddress base = InetAddress.getByName(parts[0]);
            final byte[] addrBytes = address.getAddress();
            final byte[] baseBytes = base.getAddress();
            if (addrBytes.length != baseBytes.length) {
                return false;
            }
            final int prefix = parts.length > 1 ? Integer.parseInt(parts[1]) : addrBytes.length * 8;
            int bits = prefix;
            for (int i = 0; i < addrBytes.length; i++) {
                final int mask = bits >= 8 ? 0xFF : (bits <= 0 ? 0x00 : (0xFF << (8 - bits)) & 0xFF);
                if ((addrBytes[i] & mask) != (baseBytes[i] & mask)) {
                    return false;
                }
                bits -= 8;
            }
            return true;
        } catch (final Exception e) {
            return false;
        }
    }

    ///////////////////////////////////////////////////////////////////

    /** Non-blocking HTTP request handle. The body is buffered once the async request completes. */
    private static final class HttpRequestHandle implements LuaUserdata {
        private final CompletableFuture<HttpResponse<byte[]>> future;
        @Nullable private byte[] body;
        private int position;

        HttpRequestHandle(final CompletableFuture<HttpResponse<byte[]>> future) {
            this.future = future;
        }

        @Override
        public Map<String, Boolean> methods() {
            final Map<String, Boolean> m = new LinkedHashMap<>();
            m.put("finishConnect", true);
            m.put("read", true);
            m.put("write", true);
            m.put("response", true);
            m.put("close", true);
            return m;
        }

        @Override
        public List<Object> invoke(final String method, final List<Object> args) throws Exception {
            switch (method) {
                case "finishConnect":
                    if (future.isCompletedExceptionally()) {
                        throw new Exception("connection failed");
                    }
                    return List.of(future.isDone());
                case "read": {
                    if (!future.isDone()) {
                        return List.of(""); // not ready yet; caller retries
                    }
                    if (body == null) {
                        body = future.get().body();
                    }
                    if (position >= body.length) {
                        return single(null); // EOF -> nil
                    }
                    int n = body.length - position;
                    if (!args.isEmpty() && args.get(0) instanceof final Number num && !Double.isInfinite(num.doubleValue())) {
                        n = Math.min(n, Math.max(0, num.intValue()));
                    }
                    final String chunk = new String(body, position, n, StandardCharsets.UTF_8);
                    position += n;
                    return List.of(chunk);
                }
                case "write":
                    return List.of(0); // request body is set at creation time
                case "response": {
                    if (!future.isDone()) {
                        return single(null);
                    }
                    final HttpResponse<byte[]> response = future.get();
                    final Map<String, Object> headers = new LinkedHashMap<>();
                    response.headers().map().forEach((k, v) -> headers.put(k, String.join(",", v)));
                    return List.of(response.statusCode(), "", headers);
                }
                case "close":
                    future.cancel(true);
                    return List.of();
                default:
                    throw new Exception("no such method");
            }
        }
    }

    /** Raw TCP socket handle with non-blocking reads. */
    private static final class TcpConnectHandle implements LuaUserdata {
        @Nullable private Socket socket;
        @Nullable private Exception error;

        TcpConnectHandle(final String host, final int port) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 10000);
            } catch (final Exception e) {
                error = e;
            }
        }

        @Override
        public Map<String, Boolean> methods() {
            final Map<String, Boolean> m = new LinkedHashMap<>();
            m.put("finishConnect", true);
            m.put("read", true);
            m.put("write", true);
            m.put("close", true);
            return m;
        }

        @Override
        public List<Object> invoke(final String method, final List<Object> args) throws Exception {
            switch (method) {
                case "finishConnect":
                    if (error != null) {
                        throw new Exception(error.getMessage());
                    }
                    return List.of(socket != null && socket.isConnected());
                case "read": {
                    if (socket == null || socket.isClosed()) {
                        return single(null);
                    }
                    final InputStream in = socket.getInputStream();
                    final int available = in.available();
                    if (available <= 0) {
                        return List.of("");
                    }
                    int n = available;
                    if (!args.isEmpty() && args.get(0) instanceof final Number num && !Double.isInfinite(num.doubleValue())) {
                        n = Math.min(n, Math.max(0, num.intValue()));
                    }
                    final byte[] buffer = new byte[n];
                    final int read = in.read(buffer);
                    if (read < 0) {
                        return single(null);
                    }
                    return List.of(new String(buffer, 0, read, StandardCharsets.UTF_8));
                }
                case "write": {
                    if (socket == null || socket.isClosed() || args.isEmpty()) {
                        return List.of(0);
                    }
                    final byte[] data = String.valueOf(args.get(0)).getBytes(StandardCharsets.UTF_8);
                    final OutputStream out = socket.getOutputStream();
                    out.write(data);
                    out.flush();
                    return List.of(data.length);
                }
                case "close":
                    dispose();
                    return List.of();
                default:
                    throw new Exception("no such method");
            }
        }

        @Override
        public void dispose() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (final Exception ignored) {
                }
                socket = null;
            }
        }
    }

    private static List<Object> single(@Nullable final Object value) {
        final List<Object> list = new ArrayList<>(1);
        list.add(value);
        return list;
    }
}
