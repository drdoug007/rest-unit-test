package one.dastec.restunittest.js;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class UtilsJS {
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final Map<Integer, java.util.concurrent.ScheduledFuture<?>> timers = new ConcurrentHashMap<>();

    public void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public int setTimeout(org.graalvm.polyglot.Value callback, long ms) {
        int id = nextId.getAndIncrement();
        java.util.concurrent.ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                if (callback.canExecute()) {
                    callback.execute();
                }
            } catch (Exception e) {
                // Log error or handle it
            } finally {
                timers.remove(id);
            }
        }, ms, TimeUnit.MILLISECONDS);
        timers.put(id, future);
        return id;
    }

    public void clearTimeout(int id) {
        java.util.concurrent.ScheduledFuture<?> future = timers.remove(id);
        if (future != null) {
            future.cancel(false);
        }
    }

    public void shutdown() {
        scheduler.shutdownNow();
    }
}
