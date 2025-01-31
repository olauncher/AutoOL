package dev.figboot.autool.util;

import java.io.IOException;
import java.io.OutputStream;
import java.util.function.Consumer;

public class CountedOutputStream extends OutputStream {
    private final OutputStream parent;

    private final Object countLock = new Object();
    private long count;

    private final Consumer<Long> postIncCallback;

    public CountedOutputStream(OutputStream parent, Consumer<Long> postIncCallback) {
        this.postIncCallback = postIncCallback;
        this.parent = parent;
        this.count = 0;
    }

    public CountedOutputStream(OutputStream parent) {
        this(parent, null);
    }

    protected void incCount(long l) {
        long cpy;

        synchronized (countLock) {
            cpy = count;
            count += l;
        }

        if (postIncCallback != null) postIncCallback.accept(cpy);
    }

    public long getCount() {
        synchronized (countLock) {
            return count;
        }
    }

    public void resetCount() {
        synchronized (countLock) {
            count = 0;
        }
    }

    public long getCountAndReset() {
        synchronized (countLock) {
            long temp = count;
            count = 0;
            return temp;
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        parent.write(b);
        incCount(b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        parent.write(b, off, len);
        incCount(len);
    }

    @Override
    public void flush() throws IOException {
        parent.flush();
    }

    @Override
    public void close() throws IOException {
        parent.close();
    }

    @Override
    public void write(int b) throws IOException {
        parent.write(b);
        incCount(1);
    }
}
