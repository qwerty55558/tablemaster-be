package com.mycompany.tablemaster.messaging;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 인앱 블로킹 재시도. 컨슈머 스레드가 점유한 채 백오프하므로 prefetch=1, concurrency=1 설정과
 * 결합하면 큐의 순서가 깨지지 않는다 (TTL 지연큐는 메시지가 큐 끝으로 재진입해 순서 깨짐).
 */
@Component
@Slf4j
public class RetryExecutor {

    public static final int DEFAULT_MAX_ATTEMPTS = 3;
    public static final long DEFAULT_INITIAL_BACKOFF_MS = 1000L;
    public static final double DEFAULT_MULTIPLIER = 2.0;
    public static final long DEFAULT_MAX_BACKOFF_MS = 10_000L;

    @FunctionalInterface
    public interface Action {
        void run() throws Exception;
    }

    public Outcome execute(String context, Action action) {
        return execute(context, action, DEFAULT_MAX_ATTEMPTS,
                DEFAULT_INITIAL_BACKOFF_MS, DEFAULT_MULTIPLIER, DEFAULT_MAX_BACKOFF_MS);
    }

    public Outcome execute(String context, Action action, int maxAttempts,
                            long initialBackoffMs, double multiplier, long maxBackoffMs) {
        Throwable lastError = null;
        long backoff = initialBackoffMs;
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                action.run();
                if (attempt > 1) {
                    log.info("Retry succeeded: context={}, attempt={}", context, attempt);
                }
                return Outcome.success(attempt);
            } catch (Exception e) {
                lastError = e;
                log.warn("Attempt {}/{} failed: context={}, error={}",
                        attempt, maxAttempts, context, e.toString());
                if (attempt < maxAttempts) {
                    sleep(Math.min(backoff, maxBackoffMs));
                    backoff = (long) (backoff * multiplier);
                }
            }
        }
        log.error("All {} attempts exhausted: context={}", maxAttempts, context, lastError);
        return Outcome.exhausted(maxAttempts, lastError);
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public record Outcome(boolean succeeded, int attempts, Throwable lastError) {
        public static Outcome success(int attempts) {
            return new Outcome(true, attempts, null);
        }
        public static Outcome exhausted(int attempts, Throwable error) {
            return new Outcome(false, attempts, error);
        }
        public String failureSummary() {
            if (lastError == null) {
                return "exhausted after " + attempts + " attempts";
            }
            return lastError.getClass().getSimpleName() + ": " + lastError.getMessage();
        }
    }
}
