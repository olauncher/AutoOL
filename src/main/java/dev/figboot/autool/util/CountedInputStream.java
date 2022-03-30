/*
 * AutoOL
 * Copyright (C) 2022  bigfoot547 <olauncher@figboot.dev>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see https://github.com/olauncher/autool .
 */

package dev.figboot.autool.util;

import java.io.IOException;
import java.io.InputStream;

public class CountedInputStream extends InputStream {
    private final InputStream parent;

    private final Object countLock = new Object();
    private long count;

    public CountedInputStream(InputStream parent) {
        this.parent = parent;
        count = 0;
    }

    protected void incCount(long l) {
        synchronized (countLock) {
            count += l;
        }
    }

    public long getCount() {
        synchronized (countLock) {
            return count;
        }
    }

    public long resetCount() {
        synchronized (countLock) {
            long temp = count;
            count = 0;
            return temp;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return parent.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return parent.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return parent.skip(n);
    }

    @Override
    public int available() throws IOException {
        return parent.available();
    }

    @Override
    public void close() throws IOException {
        parent.close();
    }

    @Override
    public synchronized void mark(int readlimit) {
        parent.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        parent.reset();
    }

    @Override
    public boolean markSupported() {
        return parent.markSupported();
    }

    @Override
    public int read() throws IOException {
        return parent.read();
    }
}
