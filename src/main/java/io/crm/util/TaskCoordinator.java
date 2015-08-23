package io.crm.util;

import io.crm.intfs.Runnable;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;

import java.util.function.Consumer;

import static io.crm.util.ExceptionUtil.fail;
import static io.crm.util.ExceptionUtil.sallowRun;
import static io.crm.util.ExceptionUtil.then;

/**
 * Created by someone on 12-Aug-2015.
 */
final public class TaskCoordinator {
    private final Message message;
    private io.crm.intfs.Runnable onSuccess;
    private Consumer<Throwable> onError;
    private Consumer<TaskCoordinator> onComplete;
    private int count;
    private Throwable error;

    TaskCoordinator(int count, Message message, Runnable onSuccess, Consumer<Throwable> onError, Consumer<TaskCoordinator> onComplete) {
        this.count = count;
        this.message = message;
        this.onSuccess = onSuccess;
        this.onError = onError;
        this.onComplete = onComplete;
    }

    public <T> Handler<AsyncResult<T>> add(Consumer<T> consumer) {
        return r -> {
            System.out.println("--START--");
            count--;
            System.out.println("TASK_COORDINATOR@" + this.hashCode() + " THREAD: " + Thread.currentThread().getId() + " : " + Thread.currentThread().getName());
            if (r.failed()) {
                error = r.cause();
                if (message != null) fail(message, r.cause());
            } else {
                if (consumer != null) sallowRun(() -> consumer.accept(r.result()));
            }

            if (count <= 0) {
                if (error == null) {
                    if (onSuccess != null) sallowRun(() -> onSuccess.run());
                } else {
                    if (onError != null) sallowRun(() -> onError.accept(r.cause()));
                }
                if (onComplete != null) onComplete.accept(this);
            }
            System.out.println("--END--");
        };
    }

    public boolean isError() {
        return error != null;
    }

    public boolean isSuccess() {
        return count <= 0 && error == null;
    }

    public boolean isComplete() {
        return count <= 0;
    }

    @Override
    public String toString() {
        return String.format("[%s complete: %s error: %s]", this.getClass().getSimpleName(), isComplete(), error);
    }

    public TaskCoordinator onSuccess(Runnable onSuccess) {
        this.onSuccess = onSuccess;
        return this;
    }

    public TaskCoordinator onError(Consumer<Throwable> onError) {
        this.onError = onError;
        return this;
    }

    public TaskCoordinator onComplete(Consumer<TaskCoordinator> onComplete) {
        this.onComplete = onComplete;
        return this;
    }

    public static void main(String... args) {
        System.out.println(new TaskCoordinatorBuilder().get());
    }
}
