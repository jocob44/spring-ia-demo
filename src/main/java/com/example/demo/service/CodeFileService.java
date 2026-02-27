package com.example.demo.service;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
public class CodeFileService {

    private final String BASE_PATH = "src/main/java/com/example/demo/controller/";
    private final String TEST_PATH = "src/test/java/com/example/demo/controller/"; // Ruta de tests

    public String readClass(String className) throws IOException, IOException {
        return Files.readString(Paths.get(BASE_PATH, className + ".java"));
    }

    public void writeClass(String className, String newContent) throws IOException {
        Path path = Paths.get(BASE_PATH, className + ".java");

        // Backup de seguridad: nombreClase.java.bak
        Files.copy(path, Paths.get(path.toString() + ".bak"), StandardCopyOption.REPLACE_EXISTING);

        // Sobreescribir con el nuevo código
        Files.writeString(path, newContent);
    }

    public void writeTest(String className, String testContent) throws IOException {
        // Aseguramos que la carpeta de tests exista
        Path directory = Paths.get(TEST_PATH);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }

        Path path = Paths.get(TEST_PATH, className + "Test.java");
        Files.writeString(path, testContent);
    }

    public void writeDocumentation(String content) throws IOException {
        Path path = Paths.get("DOCUMENTATION.md");
        Files.writeString(path, content);
    }
}