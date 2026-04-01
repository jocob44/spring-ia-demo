package com.example.demo.controller;

import com.example.demo.controller.MyController.CodeReview;
import com.example.demo.controller.MyController.JavaClassResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MyControllerTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @TempDir
    Path tempDir;

    private MyController myController;
    private Path generatedOutputPath;
    private Path sourcePath;

    @BeforeEach
    void setUp() throws IOException {
        when(chatClientBuilder.build()).thenReturn(chatClient);
        generatedOutputPath = tempDir.resolve("generated");
        sourcePath = tempDir.resolve("sources").resolve("com/example/demo/controller");
        Files.createDirectories(generatedOutputPath);
        Files.createDirectories(sourcePath);
        myController = new MyController(chatClientBuilder, generatedOutputPath, sourcePath);
    }

    @Test
    public void testGeneration() {
        // Arrange
        when(chatClient.prompt().user(anyString()).call().content()).thenReturn("Respuesta del chat");

        // Act
        String result = myController.generation("Cuéntame un chiste de programadores");

        // Assert
        assertEquals("Respuesta del chat", result);
    }

    @Test
    public void testGenerateJavaEntity() {
        // Arrange
        when(chatClient.prompt(any()).call().content()).thenReturn("Código de la clase entidad");

        // Act
        String result = myController.generateJavaEntity("Entidad");

        // Assert
        assertEquals("Código de la clase entidad", result);
    }

    @Test
    public void testGenerateSmartFile() throws IOException {
        // Arrange
        when(chatClient.prompt(any()).call().content()).thenReturn("{\"fileName\":\"Clase.java\",\"content\":\"public class Clase {}\"}");

        // Act
        JavaClassResponse result = myController.generateSmartFile("Entidad");

        // Assert
        assertEquals("Clase.java", result.fileName());
        assertEquals("public class Clase {}", result.content());
        assertEquals("public class Clase {}", Files.readString(generatedOutputPath.resolve("Clase.java")));
    }

    @Test
    public void testRefactorCode() {
        // Arrange
        when(chatClient.prompt(any()).call().content()).thenReturn("{\"originalCode\":\"Código original\",\"improvedCode\":\"Código mejorado\",\"bugsEncontrados\":[\"Bug 1\"],\"explicacion\":\"Explicación\"}");

        // Act
        CodeReview result = myController.refactorCode("Código sucio");

        // Assert
        assertEquals("Código original", result.originalCode());
        assertEquals("Código mejorado", result.improvedCode());
        assertEquals(List.of("Bug 1"), result.bugsEncontrados());
        assertEquals("Explicación", result.explicacion());
    }

    @Test
    public void testAnalyzeLocalFile() throws IOException {
        // Arrange
        when(chatClient.prompt(any()).call().content()).thenReturn("{\"originalCode\":\"Código original\",\"improvedCode\":\"Código mejorado\",\"bugsEncontrados\":[\"Bug 1\"],\"explicacion\":\"Explicación\"}");
        Path path = sourcePath.resolve("Clase.java");
        Files.writeString(path, "public class Clase {}");

        // Act
        CodeReview result = myController.analyzeLocalFile("Clase");

        // Assert
        assertEquals("Código original", result.originalCode());
        assertEquals("Código mejorado", result.improvedCode());
        assertEquals(List.of("Bug 1"), result.bugsEncontrados());
        assertEquals("Explicación", result.explicacion());
    }

    @Test
    public void testAnalyzeLocalFileArchivoNoEncontrado() {
        // Act y Assert
        assertThrows(RuntimeException.class, () -> myController.analyzeLocalFile("ClaseNoExiste"));
    }
}