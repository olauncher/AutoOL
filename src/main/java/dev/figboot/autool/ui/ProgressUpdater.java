package dev.figboot.autool.ui;

public interface ProgressUpdater {
    void changeStatus(String status);

    void setMaxProgress(int max);

    void setProgress(int progress);

    default boolean cancelOperation() {
        return false;
    }

    void shutdown() throws InterruptedException;

    void error(Object message);
}
