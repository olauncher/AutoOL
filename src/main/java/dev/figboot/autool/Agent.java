package dev.figboot.autool;

import java.io.File;
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

    private static Class<?> loadClassFromNewLoader(File file, String name) throws ClassNotFoundException, MalformedURLException {
        URLClassLoader ucl = new URLClassLoader(new URL[]{file.toURI().toURL()}, ClassLoader.getSystemClassLoader());
        return ucl.loadClass(name);
    }

    public static Class<?> loadClassFromJar(JarFile jarFile, File file, String name) throws ClassNotFoundException {
        if (inst == null) {
            /*try {
                URLClassLoader ucl = (URLClassLoader)ClassLoader.getSystemClassLoader();
                Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(ucl, file.toURI().toURL());
                method.setAccessible(false);

                return Class.forName(name);
            } catch (ClassNotFoundException ex) {
                throw ex;
            } catch (ClassCastException ex) {
                throw new RuntimeException("The system class loader is not a URLClassLoader!");
            } catch (MalformedURLException ex) {
                throw new RuntimeException(ex);
            } catch (ReflectiveOperationException ex) {
                throw new RuntimeException("Error adding the jar file to the classpath", ex);
            }*/

            try {
                return loadClassFromNewLoader(file, name);
            } catch (MalformedURLException ex) {
                throw new RuntimeException("Error creating URLClassLoader", ex);
            }
        } else {
            try {
                inst.appendToSystemClassLoaderSearch(jarFile);

                return Class.forName(name);
            } finally {
                inst = null;
            }
        }
    }
}
