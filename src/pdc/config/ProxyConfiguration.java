package pdc.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import pdc.logger.HttpProxyLogger;

/**
 * Created by heyimnowi on 5/24/17.
 */

public class ProxyConfiguration {
    private Properties properties;
    private File file;

    private static ProxyConfiguration instance;

    public static ProxyConfiguration getInstance() {
        if (instance == null)
            instance = new ProxyConfiguration();
        return instance;
    }

    private ProxyConfiguration() {
        this.properties = new Properties();
        try {
            String current = new java.io.File(".").getCanonicalPath();
            this.file = new File(current + "/src/resources/config.properties");
            //this.file = new File("./config.properties");
            System.out.print(this.file);
            FileInputStream fis = new FileInputStream(file);
            System.out.print(fis);
            properties.load(fis);

        } catch (Exception e) {
            HttpProxyLogger.getInstance().error("Cannot open proxy configuration file");
        }
    }

    public String getProperty(String property) {
        if (properties.get(property) == null)
            return "";
        return properties.get(property).toString();
    }

    public void setProperty(String property, String value) throws IOException {
        String finalValue = "";
        if (property.startsWith("filter_")) {
            Set<String> propertyValues = propertyToSetOfValues(property);
            propertyValues.add(value);
            finalValue = propertiesFromSet(propertyValues);
        } else {
            finalValue = value;
        }
        properties.setProperty(property, finalValue);
        flushPropertiesToFile();

    }

    public void unsetProperty(String property, String value) throws IOException {
        if (property.startsWith("filter_")) {
            Set<String> propertyValues = propertyToSetOfValues(property);
            propertyValues.remove(value);
            properties.setProperty(property, propertiesFromSet(propertyValues));
            flushPropertiesToFile();
        }
    }

    private void flushPropertiesToFile() throws IOException {
        FileOutputStream fos = new FileOutputStream(this.file);
        properties.store(fos, "");
     }

    private Set<String> propertyToSetOfValues(String property) {
        String[] previousPropertyValues = properties.getProperty(property).split(",");
        Set<String> propertyValues = new HashSet<String>();
        for (String prop : previousPropertyValues) {
            propertyValues.add(prop);
        }
        return propertyValues;
    }

    public boolean hasProperty(String property) {
        return properties.containsKey(property);
    }

    public String getAllProperties() {
        return propertiesFromSet(properties.keySet());
    }

    private String propertiesFromSet(Set<?> set) {
        StringBuilder all = new StringBuilder();
        for (Object key : set) {
            all.append(key.toString() + ",");
        }
        all.deleteCharAt(all.length() - 1);
        return all.toString();
    }
}
