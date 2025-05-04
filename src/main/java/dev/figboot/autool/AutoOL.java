package dev.figboot.autool;

import dev.figboot.autool.config.PatchProperties;
import dev.figboot.autool.patcher.ProgramPatcher;
import dev.figboot.autool.ui.CLIProgressUpdater;
import dev.figboot.autool.ui.ProgressUpdater;
import dev.figboot.autool.ui.SwingProgressUpdater;
import dev.figboot.autool.util.OperatingSystem;
import dev.figboot.autool.util.exception.CancelledException;
import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.Proxy;

public class AutoOL {
    public static void main(String[] args) throws Exception { // no need to handle exceptions here
        Method mainMethod = realMain(args);

        System.gc();
        mainMethod.invoke(null, (Object)args);
    }

    static Method realMain(String[] args) {
        OptionParser parser = new OptionParser();
        ArgumentAcceptingOptionSpec<File> workDirArg = parser.accepts("workDir").withRequiredArg().ofType(File.class).defaultsTo(getWorkingDirectory());
        ArgumentAcceptingOptionSpec<String> proxyHostArg = parser.accepts("proxyHost").withRequiredArg();
        ArgumentAcceptingOptionSpec<Integer> proxyPortArg = parser.accepts("proxyPort").withRequiredArg().ofType(Integer.class).defaultsTo(8080);
        OptionSet opts = parser.parse(args);

        PatchProperties settings;
        try {
            settings = new PatchProperties();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }

        Proxy proxy = Proxy.NO_PROXY;
        String host = opts.valueOf(proxyHostArg);
        if (opts.valueOf(proxyHostArg) != null) {
            proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(host, opts.valueOf(proxyPortArg)));
        }

        File workDir = opts.valueOf(workDirArg);

        System.out.println("Using working directory: " + workDir.getAbsolutePath());
        System.out.println("Interactive: " + (settings.isInteractive() ? "yes" : "no"));

        ProgressUpdater progress;
        if (settings.isInteractive()) {
            progress = new SwingProgressUpdater();
        } else {
            progress = new CLIProgressUpdater();
        }

        ProgramPatcher patcher = new ProgramPatcher(settings, proxy, workDir, progress);

        Method method = null;
        try {
            patcher.init();
            method = patcher.getMainMethod();
        } catch (CancelledException ex) {
            System.out.println("Operation cancelled, exiting...");
            try {
                progress.shutdown();
            } catch (InterruptedException ignored) { }
            System.exit(0);
        } catch (Exception ex) {
            System.err.println("Error in patcher, exiting...:");
            ex.printStackTrace();
            try {
                progress.shutdown();
            } catch (InterruptedException ignored) { }
            System.exit(0);
        }

        try {
            progress.shutdown();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        System.getProperties().putAll(settings.getSystemProperties());
        return method;
    }

    // Code adapted from net.minecraft.launcher.Main
    private static File getWorkingDirectory() {
        String homeDir = System.getProperty("user.home", ".");

        switch (OperatingSystem.getOS()) {
            case WINDOWS: {
                String appData = System.getenv("AppData");
                return new File(appData == null ? homeDir : appData, ".minecraft");
            }
            case OSX:
                return new File(homeDir, "Library/Application Support/minecraft");
            case LINUX:
                return new File(homeDir, ".minecraft");
            default:
                return new File(homeDir, "minecraft");
        }
    }
}
