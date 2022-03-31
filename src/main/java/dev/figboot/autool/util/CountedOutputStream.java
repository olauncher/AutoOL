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
import java.io.OutputStream;

public class CountedOutputStream extends OutputStream {
    private final OutputStream parent;

    private final Object countLock = new Object();
    private long count;

    public CountedOutputStream(OutputStream parent) {
        this.parent = parent;
        this.count = 0;
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
