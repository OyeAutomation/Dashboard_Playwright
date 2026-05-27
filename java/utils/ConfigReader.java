package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigReader {
    private static final Properties properties = new Properties();

    static {
        try (InputStream file = ConfigReader.class
                .getClassLoader()
                .getResourceAsStream("config.properties")) {
            if (file == null) {
                throw new IllegalStateException("config.properties not found on classpath");
            }
            properties.load(file);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static String getProperty(String key) {
        String systemValue = System.getProperty(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String envValue = System.getenv(toEnvKey(key));
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        String propertyValue = properties.getProperty(key);
        return propertyValue == null ? null : propertyValue.trim();
    }

    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value == null || value.isBlank() ? defaultValue : value;
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = getProperty(key);
        return value == null || value.isBlank()
                ? defaultValue
                : Boolean.parseBoolean(value);
    }

    public static int getInt(String key, int defaultValue) {
        String value = getProperty(key);
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value);
    }

    private static String toEnvKey(String key) {
        return key.replace('.', '_')
                .replace('-', '_')
                .toUpperCase();
    }
}
