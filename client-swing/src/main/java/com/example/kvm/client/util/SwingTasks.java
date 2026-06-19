package com.example.kvm.client.util;

import java.util.concurrent.Callable;
import java.util.function.Consumer;
import javax.swing.*;

public final class SwingTasks {
    private SwingTasks() {
    }

    public static <T> void run(Callable<T> job, Consumer<T> onSuccess, Consumer<Exception> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return job.call();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    onError.accept(cause instanceof Exception e ? e : new Exception(cause));
                }
            }
        }.execute();
    }
}
