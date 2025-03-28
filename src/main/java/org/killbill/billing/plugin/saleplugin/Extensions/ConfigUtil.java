package org.killbill.billing.plugin.saleplugin.Extensions;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigUtil {
    private static final Properties properties = new Properties();

    static {
        try (InputStream input = ConfigUtil.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                throw new RuntimeException("Config file not found");
            }
            properties.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error while access config file", e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }
}
