package dev.figboot.autool.config;

import lombok.Getter;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PatchProperties {
    private static final String SYSTEM_PROPERTY_PREFIX = "sysprop.";
    private static final String CONFIG_RES = "/patch.properties";

    private final Properties properties;

    @Getter private URL originalURL;
    @Getter private String originalHash;
    @Getter private String originalName;
    @Getter private long originalSize;

    @Getter private String patchResource;
    @Getter private long patchSize;

    @Getter private String finalHash;
    @Getter private String finalName;
    @Getter private long finalSize;

    @Getter private boolean interactive;

    @Getter private String mainClass;

    private final Map<String, String> systemPropertiesMutable = new HashMap<>();
    @Getter private final Map<String, String> systemProperties = Collections.unmodifiableMap(systemPropertiesMutable);

    public PatchProperties() throws IOException {
        properties = new Properties();
        properties.load(getClass().getResourceAsStream(CONFIG_RES));
        populate();
    }

    private void populate() {
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            String keyStr = entry.getKey().toString();
            if (keyStr.startsWith(SYSTEM_PROPERTY_PREFIX)) {
                systemPropertiesMutable.put(keyStr.substring(SYSTEM_PROPERTY_PREFIX.length()), entry.getValue().toString());
                continue;
            }

            try {
                Method method = getClass().getDeclaredMethod(keyStr, String.class);

                if (method.isAnnotationPresent(ConfigMethod.class)) {
                    method.invoke(this, entry.getValue().toString());
                } else {
                    System.err.println("Not calling method '" + method.getName() + "', as it is not annotated with @ConfigMethod.");
                }
            } catch (NoSuchMethodException ex) {
                throw new RuntimeException("Invalid patch properties", ex);
            } catch (ReflectiveOperationException ex) {
                System.err.println("Error while loading patch properties:");
                ex.printStackTrace();
            }
        }
    }

    @ConfigMethod
    private void origurl(String url) throws MalformedURLException {
        originalURL = new URL(url);
    }

    @ConfigMethod
    private void orighash(String hash) {
        originalHash = hash;
    }

    @ConfigMethod
    private void origname(String origname) {
        originalName = origname;
    }

    @ConfigMethod
    private void origsz(String origsz) {
        originalSize = Long.parseLong(origsz);
    }

    @ConfigMethod
    private void patchres(String patchres) {
        patchResource = patchres;
    }

    @ConfigMethod
    private void patchsz(String patchsz) {
        this.patchSize = Long.parseLong(patchsz);
    }

    @ConfigMethod
    private void finalhash(String finalhash) {
        finalHash = finalhash;
    }

    @ConfigMethod
    private void finalname(String finalname) {
        finalName = finalname;
    }

    @ConfigMethod
    private void finalsz(String finalsz) {
        finalSize = Long.parseLong(finalsz);
    }

    @ConfigMethod
    private void interactive(String interactive) {
        this.interactive = interactive.equalsIgnoreCase("true");

        String prop = System.getProperties().getProperty("dev.figboot.autool.AutoOL.interactive");
        if (prop != null) {
            this.interactive = prop.equalsIgnoreCase("true");
        }
    }

    @ConfigMethod
    private void mainclass(String mainclass) {
        this.mainClass = mainclass;
    }
}
