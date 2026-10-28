package com.githubassistant.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Lightweight per-process limits for public, cost-bearing API routes. */
@Component
public class ApiRateLimitInterceptor implements HandlerInterceptor {
    private record Window(long expiresAt, int count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanup = new AtomicLong();
    private final int indexLimit;
    private final int queryLimit;
    private final long windowMillis;

    public ApiRateLimitInterceptor(
            @Value("${app.rate-limit.index-per-hour:3}") int indexLimit,
            @Value("${app.rate-limit.queries-per-minute:20}") int queryLimit) {
        this.indexLimit = Math.max(1, indexLimit);
        this.queryLimit = Math.max(1, queryLimit);
        this.windowMillis = 60_000L;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        String path = request.getRequestURI();
        String method = request.getMethod();
        boolean indexing = "POST".equals(method) && "/repositories".equals(path);
        boolean query = path.matches("/repositories/[^/]+/(ask|explore|search)")
                && ("POST".equals(method) || "GET".equals(method));
        query = query || ("POST".equals(method) && path.matches("/repositories/[^/]+/saved-locations"));
        if (!indexing && !query) return true;

        long now = System.currentTimeMillis();
        long duration = indexing ? 3_600_000L : windowMillis;
        String route = indexing ? "index" : "query";
        // Use the socket peer address; do not trust caller-supplied forwarding headers.
        String key = request.getRemoteAddr() + ':' + route;
        if (!windows.containsKey(key) && windows.size() >= 20_000) {
            cleanupOccasionally(now);
            if (!windows.containsKey(key) && windows.size() >= 20_000) {
                writeLimited(response, 60);
                return false;
            }
        }
        int limit = indexing ? indexLimit : queryLimit;
        Window updated = windows.compute(key, (ignored, current) -> {
            if (current == null || current.expiresAt() <= now) return new Window(now + duration, 1);
            return new Window(current.expiresAt(), Math.min(limit + 1, current.count() + 1));
        });
        if (updated.count() <= limit) {
            cleanupOccasionally(now);
            return true;
        }

        long seconds = Math.max(1, (updated.expiresAt() - now + 999) / 1000);
        writeLimited(response, seconds);
        cleanupOccasionally(now);
        return false;
    }

    private void writeLimited(HttpServletResponse response, long seconds) throws IOException {
        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", Long.toString(seconds));
        response.getWriter().write("{\"error\":\"Request limit reached. Please wait before trying again.\"}");
    }

    private void cleanupOccasionally(long now) {
        long previous = lastCleanup.get();
        if (now - previous < 60_000L || !lastCleanup.compareAndSet(previous, now)) return;
        windows.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
    }
}
