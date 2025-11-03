package monitor.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public class ResourceExtractor {
    public static void extractResource(String resourceName, Path destination) throws IOException {
        try (InputStream in = ResourceExtractor.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new FileNotFoundException("Not found resource: " + resourceName);
            }
            Files.copy(in, destination, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("Extracted: " + resourceName);
        }
    }
}