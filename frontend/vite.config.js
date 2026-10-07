import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
// The frontend talks to the Spring Boot backend on :8080. We proxy /api during
// dev so the browser sees a same-origin URL (avoids CORS entirely in dev), while
// SSE (text/event-stream) is proxied too.
//
// Target is 127.0.0.1 (not "localhost") on purpose: Node resolves "localhost"
// to IPv6 (::1) first, and if the backend is only answering on IPv4 the proxy
// hangs until timeout. Pinning IPv4 makes the dev networking deterministic.
export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            "/api": {
                target: "http://127.0.0.1:8080",
                changeOrigin: true,
            },
        },
    },
});
