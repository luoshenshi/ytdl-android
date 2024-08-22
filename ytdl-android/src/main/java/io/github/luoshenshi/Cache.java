package io.github.luoshenshi;

import android.os.Build;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class Cache {
    private final Map<String, CompletableFuture<String>> cache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private final long timeout;

    public Cache(long timeout) {
        this.timeout = timeout;
    }

    public void set(String key, CompletableFuture<String> value) {
        ScheduledFuture<?> future = scheduler.schedule(() -> cache.remove(key), timeout, TimeUnit.MILLISECONDS);

        cache.put(key, value);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            value.whenComplete((result, error) -> {
                if (error != null) {
                    cache.remove(key);
                    future.cancel(false);
                }
            });
        }
    }

    public CompletableFuture<String> getOrSet(String key, Callable<CompletableFuture<String>> fn) throws Exception {
        CompletableFuture<String> value = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            value = cache.computeIfAbsent(key, k -> {
                try {
                    return fn.call();
                } catch (Exception e) {
                    throw new CompletionException(e);
                }
            });
        }
        set(key, value);
        return value;
    }

    public void clear() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            cache.values().forEach(future -> future.cancel(false));
        }
        cache.clear();
    }
}
