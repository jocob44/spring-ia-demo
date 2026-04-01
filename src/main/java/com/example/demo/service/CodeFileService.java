package com.example.demo.service;

import com.example.demo.exception.CodeFileException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.regex.Pattern;

@Service
public class CodeFileService {

    private static final Pattern VALID_CLASS_NAME = Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*");

    private final Path controllerBasePath;
    private final Path testBasePath;
    private final Path documentationPath;

    public CodeFileService(
            @Value("${app.codegen.base-path:src/main/java/com/example/demo/controller}") String basePath,
            @Value("${app.codegen.test-path:src/test/java/com/example/demo/controller}") String testPath,
            @Value("${app.codegen.docs-path:DOCUMENTATION.md}") String docsPath
    ) {
        this.controllerBasePath = Paths.get(basePath).toAbsolutePath().normalize();
        this.testBasePath = Paths.get(testPath).toAbsolutePath().normalize();
        this.documentationPath = Paths.get(docsPath).toAbsolutePath().normalize();
    }

    public String readClass(String className) {
        Path path = resolveJavaClassPath(controllerBasePath, className);
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new CodeFileException("No se pudo leer la clase: " + className, e);
        }
    }

    public void writeClass(String className, String newContent) {
        Path path = resolveJavaClassPath(controllerBasePath, className);

        try {
            Files.createDirectories(path.getParent());
            // Backup de seguridad: nombreClase.java.bak
            Files.copy(path, Paths.get(path.toString() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            // Sobreescribir con el nuevo codigo
            Files.writeString(path, newContent);
        } catch (IOException e) {
            throw new CodeFileException("No se pudo escribir la clase: " + className, e);
        }
    }

    public void writeTest(String className, String testContent) {
        Path path = resolveJavaClassPath(testBasePath, className + "Test");
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, testContent);
        } catch (IOException e) {
            throw new CodeFileException("No se pudo escribir el test para: " + className, e);
        }
    }

    public void writeDocumentation(String content) {
        try {
            Files.writeString(documentationPath, content);
        } catch (IOException e) {
            throw new CodeFileException("No se pudo escribir la documentacion generada.", e);
        }
    }

    private Path resolveJavaClassPath(Path basePath, String className) {
        String validatedClassName = validateClassName(className);
        Path resolvedPath = basePath.resolve(validatedClassName + ".java").normalize();
        if (!resolvedPath.startsWith(basePath)) {
            // Defensa contra path traversal si se altera la entrada de usuario.
            throw new CodeFileException("Nombre de clase invalido: acceso fuera del directorio permitido.");
        }
        return resolvedPath;
    }

    private String validateClassName(String className) {
        if (className == null || className.isBlank()) {
            throw new CodeFileException("El nombre de clase no puede estar vacio.");
        }
        if (!VALID_CLASS_NAME.matcher(className).matches()) {
            throw new CodeFileException("El nombre de clase contiene caracteres no permitidos.");
        }
        return className;
    }
}