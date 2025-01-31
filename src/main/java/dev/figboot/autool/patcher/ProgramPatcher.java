package dev.figboot.autool.patcher;

import dev.figboot.autool.Agent;
import dev.figboot.autool.config.PatchProperties;
import dev.figboot.autool.ui.ProgressUpdater;
import dev.figboot.autool.util.CountedInputStream;
import dev.figboot.autool.util.CountedOutputStream;
import dev.figboot.autool.util.DownloadUtil;
import dev.figboot.autool.util.Util;
import dev.figboot.autool.util.exception.CancelledException;
import io.sigpipe.jbsdiff.InvalidHeaderException;
import io.sigpipe.jbsdiff.Patch;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.lang.reflect.Method;
import java.net.Proxy;
import java.nio.file.Files;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.util.jar.JarFile;

public class ProgramPatcher {
    private final Proxy proxy;

    private final PatchProperties props;
    private final ProgressUpdater progress;

    private final File patcherDir;
    private final File origFile;
    private final File finalFile;

    public ProgramPatcher(PatchProperties props, Proxy proxy, File workDir, ProgressUpdater progress) {
        this.proxy = proxy;

        this.props = props;
        this.progress = progress;

        patcherDir = new File(workDir, "autool");
        origFile = new File(patcherDir, props.getOriginalName());
        finalFile = new File(patcherDir, props.getFinalName());
    }

    public void init() {
        try {
            Files.createDirectories(patcherDir.toPath());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public Method getMainMethod() {
        ensureFinalFile();

        progress.changeStatus("Loading patched file...");

        try {
            JarFile jf = new JarFile(finalFile);
            String mainClass;
            if (props.getMainClass() != null) {
                mainClass = props.getMainClass();
            } else {
                mainClass = jf.getManifest().getMainAttributes().getValue("Main-Class");
            }

            if (mainClass == null) {
                throw new RuntimeException("A main class could not be selected, please nag the developer.");
            }

            Class<?> clazz = Agent.loadClassFromJar(jf, finalFile, mainClass);
            return clazz.getDeclaredMethod("main", String[].class);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        //return null;
    }

    private void ensureFinalFile() {
        if (!checkFinalFile()) {
            System.out.println("The final file is not present or has the wrong hash, repatching.");
            patch();
        }
    }

    private boolean checkFinalFile() {
        if (!finalFile.isFile()) {
            return false;
        }

        progress.changeStatus("Checking final file integrity...");
        progress.setMaxProgress((int)props.getFinalSize());
        return Util.checkFileIntegrity(finalFile, props.getFinalHash(), progress);
    }

    private void ensureOriginalFile() {
        if (!checkOrigFile()) {
            String hash;
            progress.changeStatus("Downloading original file...");
            progress.setMaxProgress((int)props.getOriginalSize());
            try (OutputStream os = new FileOutputStream(origFile)) {
                hash = DownloadUtil.download(props.getOriginalURL(), proxy, progress, os);
            } catch (IOException ex) {
                throw new RuntimeException("Error downloading original file", ex);
            }

            if (!hash.equalsIgnoreCase(props.getOriginalHash())) {
                progress.error("The downloaded original file does not match the expected hash!");
                throw new CancelledException();
            }
        }
    }

    private boolean checkOrigFile() {
        if (!origFile.isFile()) {
            return false;
        }

        progress.changeStatus("Checking original file integrity...");
        progress.setMaxProgress((int)props.getOriginalSize());
        return Util.checkFileIntegrity(origFile, props.getOriginalHash(), progress);
    }

    private void patch() {
        ensureOriginalFile();

        byte[] origContents, patchContents;

        progress.changeStatus("Reading original file...");
        progress.setMaxProgress((int)props.getOriginalSize());
        try (InputStream is = new CountedInputStream(new FileInputStream(origFile), (p) -> progress.setProgress(p.intValue()))) {
            origContents = IOUtils.toByteArray(is);
        } catch (IOException ex) {
            throw new RuntimeException("Error reading original file", ex);
        }

        progress.changeStatus("Reading patch...");
        progress.setMaxProgress((int)props.getPatchSize());
        try (InputStream is = new CountedInputStream(getClass().getResourceAsStream(props.getPatchResource()), (p) -> progress.setProgress(p.intValue()))) {
            patchContents = IOUtils.toByteArray(is);
        } catch (IOException ex) {
            throw new RuntimeException("Error reading patch", ex);
        }

        progress.changeStatus("Patching original file...");
        progress.setMaxProgress((int)props.getFinalSize());
        MessageDigest digest;
        try (DigestOutputStream os = new DigestOutputStream(new FileOutputStream(finalFile), digest = Util.createSHA1Digest())) {
            CountedOutputStream cos = new CountedOutputStream(os, (p) -> progress.setProgress(p.intValue()));
            Patch.patch(origContents, patchContents, cos);

            String hash = Util.digestToString(digest, 40);
            if (!hash.equalsIgnoreCase(props.getFinalHash())) {
                progress.error("The patched final file does not match the expected hash!");
                throw new CancelledException();
            }
        } catch (IOException | CompressorException ex) {
            throw new RuntimeException("Error patching original file", ex);
        } catch (InvalidHeaderException ex) {
            throw new RuntimeException("The patch was in an invalid format?", ex);
        }
    }
}
