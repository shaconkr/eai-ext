package kr.shacon.edi;

import org.xml.sax.InputSource;

import java.io.*;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

class Utils {

    private static ClassLoader getClassLoader() {
        return Objects.requireNonNullElse(Thread.currentThread().getContextClassLoader(), Utils.class.getClassLoader());
    }

    public static Class<?> loadClass(String fqn) {
        return loadClass(getClassLoader(), fqn);
    }

    public static Class<?> loadClass(ClassLoader loader, String fqn) {
        try {
            return Class.forName(fqn, true, loader);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public static Object newInstanceOf(String fqn) {
        return newInstanceOf(getClassLoader(), fqn);
    }

    public static Object newInstanceOf(ClassLoader loader, String fqn) {
        try {
            return loadClass(loader, fqn).getConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static InputSource toInputSource(File file) {
        try {
            return new InputSource(file.toURI().toURL().toExternalForm());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static InputSource toInputSource(Reader reader) {
        return new InputSource(reader);
    }

    public static List<RecordDefinition.Property> collectProperties(RecordDefinition definition) {
        List<RecordDefinition.Property> properties = new ArrayList<>(definition.getProperties());
        for (RecordDefinition subDefinition : definition.getSubRecords()) {
            properties.addAll(collectProperties(subDefinition));
        }
        return properties;
    }

    @SuppressWarnings("unchecked")
    public static <E> void createAndStoreMissingInstancesOf(List<RecordDefinition.Property> properties, Function<RecordDefinition.Property, String> mapper, Map<String, E> store) {
        properties.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .filter(classNameOrLabel -> !store.containsKey(classNameOrLabel))
                .distinct()
                .forEach(classNameOrLabel -> store.put(classNameOrLabel, (E) newInstanceOf(classNameOrLabel)));
    }

    public static InputStreamReader getReader(String path, boolean flag) {
        InputStreamReader isr = null;
        try {
            if (flag) {
                isr = new FileReader(path);
            } else {
                isr = new InputStreamReader(getClassLoader().getResourceAsStream(path));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return isr;
    }

}
