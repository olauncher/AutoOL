package dev.figboot.autool.ui;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class CLIProgressUpdater implements ProgressUpdater {
    private final ScheduledExecutorService scheduler;

    private final Object progressLock = new Object();
    private String status;
    private int maxProgress;
    private int progress;

    public CLIProgressUpdater() {
        scheduler = new ScheduledThreadPoolExecutor(1);
        scheduler.scheduleAtFixedRate(this::updateProgress, 1000L, 500L, TimeUnit.MILLISECONDS);

        status = "";
        progress = 0;
        maxProgress = 1;
    }

    @Override
    public void changeStatus(String status) {
        synchronized (progressLock) {
            this.status = status;
            progress = 0;
            updateProgress();
        }
    }

    @Override
    public void setMaxProgress(int max) {
        synchronized (progressLock) {
            this.maxProgress = max;
        }
    }

    @Override
    public void setProgress(int progress) {
        synchronized (progressLock) {
            this.progress = progress;
        }
    }

    private void updateProgress() {
        int percentComplete;
        String status;
        synchronized (progressLock) {
            status = this.status;
            percentComplete = 100 * progress / maxProgress;
        }

        System.out.format("%s (%d%% complete)\n", status, percentComplete);
    }

    @Override
    public void shutdown() throws InterruptedException {
        scheduler.shutdown();
        if (!scheduler.awaitTermination(5000, TimeUnit.SECONDS)) {
            System.err.println("The executor did not shut down in time :(");
        }
    }

    @Override
    public void error(Object message) {
        System.err.println(message);
    }
}
