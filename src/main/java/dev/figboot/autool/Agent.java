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

package dev.figboot.autool;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

public class Agent {
    private static Instrumentation inst;

    public static void premain(String args, Instrumentation inst) {
        Agent.inst = inst;
    }

    public static void agentmain(String args, Instrumentation inst) {
        Agent.inst = inst;
    }

    public static void addSystemClassLoaderJar(File file) {
        if (inst == null) {
            try {
                URLClassLoader ucl = (URLClassLoader)ClassLoader.getSystemClassLoader();
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(ucl, file.toURI().toURL());
                method.setAccessible(false);
            } catch (ClassCastException ex) {
                throw new RuntimeException("Your system class loader is not a URLClassLoader! Please use another JRE.");
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Error adding the jar file to the classpath", ex);
            }
        } else {
            try {
                inst.appendToSystemClassLoaderSearch(new JarFile(file));
            } catch (IOException ex) {
                throw new RuntimeException("Error loading the jar file", ex);
            } finally {
                inst = null;
            }
        }
    }
}
