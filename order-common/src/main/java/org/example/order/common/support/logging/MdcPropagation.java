package org.example.order.common.support.logging;

import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * MdcPropagation
 * ------------------------------------------------------------------------
 * 목적
 * - 현재 스레드의 MDC 컨텍스트를 캡처하여, 다른 스레드에서 실행하는 작업(콜백/런너블 등)에 전파/복원한다.
 * - 비동기 콜백(CompletableFuture.whenComplete 등)에서 사용.
 */
public final class MdcPropagation {

    private MdcPropagation() {
    }

    /**
     * 현재 스레드의 MDC를 캡처한 뒤, 주어진 Runnable을 실행할 Runnable을 돌려준다.
     */
    public static Runnable wrap(Runnable task) {
        Map<String, String> captured = MDC.getCopyOfContextMap();

        return () -> withMdc(captured, () -> {
            task.run();

            return null;
        });
    }

    /**
     * 현재 스레드의 MDC를 캡처한 뒤, 주어진 Consumer<T>를 실행할 Consumer<T>를 돌려준다.
     */
    public static <T> Consumer<T> wrap(Consumer<T> consumer) {
        Map<String, String> captured = MDC.getCopyOfContextMap();

        return t -> withMdc(captured, () -> {
            consumer.accept(t);

            return null;
        });
    }

    /**
     * 현재 스레드의 MDC를 캡처한 뒤, 주어진 BiConsumer<T,U>를 실행할 BiConsumer<T,U>를 돌려준다.
     * (CompletableFuture.whenComplete 등에 사용)
     */
    public static <T, U> BiConsumer<T, U> wrap(BiConsumer<T, U> consumer) {
        Map<String, String> captured = MDC.getCopyOfContextMap();

        return (t, u) -> withMdc(captured, () -> {
            consumer.accept(t, u);

            return null;
        });
    }

    /**
     * 캡처된 MDC를 세팅하고 작업을 실행한 뒤, 이전 MDC로 복원한다.
     */
    private static <V> V withMdc(Map<String, String> captured, Callable<V> action) {
        Map<String, String> prev = MDC.getCopyOfContextMap();
        try {
            if (captured != null) {
                MDC.setContextMap(captured);
            } else {
                MDC.clear();
            }

            return action.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (prev != null) {
                MDC.setContextMap(prev);
            } else {
                MDC.clear();
            }
        }
    }
}
