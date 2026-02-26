package com.example.demo.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
public class MyController {

    private final ChatClient chatClient;

    // Inyección por constructor: La forma más segura y testeable
    public MyController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/ai")
    public String generation(@RequestParam(value = "userInput", defaultValue = "Cuéntame un chiste de programadores") String userInput) {
        return this.chatClient.prompt()
                .user(userInput)
                .call()
                .content();
    }

    @GetMapping("/generate-entity")
    public String generateJavaEntity(@RequestParam String entidad) {
        // Definimos la plantilla con instrucciones de experto
        String templateText = """
            Eres un Arquitecto de Software Senior especializado en Java y Spring Boot.
            Genera una clase Entidad JPA para la entidad: {entidad}.
            
            Requisitos:
            1. Usa Java 17+ (records o clases con Lombok).
            2. Incluye anotaciones de Jakarta Persistence (@Entity, @Id, @GeneratedValue).
            3. Añade al menos 4 atributos lógicos para esta entidad.
            4. Incluye comentarios JavaDoc breves.
            5. Devuelve SOLO el código de la clase, sin explicaciones.
            """;

        // Creamos el template y pasamos los parámetros
        PromptTemplate promptTemplate = new PromptTemplate(templateText);
        Prompt prompt = promptTemplate.create(Map.of("entidad", entidad));

        return chatClient.prompt(prompt)
                .call()
                .content();
    }
    public record JavaClassResponse(String fileName, String content) {}

    @GetMapping("/generate-file")
    public JavaClassResponse generateSmartFile(@RequestParam String entidad) throws IOException {
        // 1. Configuramos el convertidor para nuestro Record
        var converter = new BeanOutputConverter<>(JavaClassResponse.class);

        String templateText = """
            Genera una clase Java profesional para la entidad: {entidad}.
            
            {format}
            
            Asegúrate de que 'fileName' termine en .java y que 'content' sea el código fuente.
            """;

        // 2. Pasamos el formato automático que genera el convertidor al template
        PromptTemplate promptTemplate = new PromptTemplate(templateText);
        Prompt prompt = promptTemplate.create(Map.of(
                "entidad", entidad,
                "format", converter.getFormat() // Esto le explica a la IA cómo hacer el JSON
        ));

        // 3. Llamamos a Llama 3.3 y convertimos la respuesta directamente al Record
        String response = chatClient.prompt(prompt)
                .call()
                .content();
        JavaClassResponse object = converter.convert(response);
        Files.writeString(Path.of("src/main/java/" + object.fileName()), object.content());
        return converter.convert(response);
    }
    public record CodeReview(
            String originalCode,
            String improvedCode,
            List<String> bugsEncontrados,
            String explicacion
    ) {}
    @PostMapping("/refactor")
    public CodeReview refactorCode(@RequestBody String dirtyCode) {
        var converter = new BeanOutputConverter<>(CodeReview.class);

        String reviewTemplate = """
            Eres un experto en Seguridad Java y Clean Code. 
            Analiza el siguiente código y realiza un refactoring profundo.
            
            Código a analizar:
            {code}
            
            Instrucciones:
            1. Busca vulnerabilidades (SQL Injection, fugas de memoria).
            2. Aplica patrones de diseño modernos.
            3. Mejora la legibilidad.
            
            {format}
            """;

        PromptTemplate promptTemplate = new PromptTemplate(reviewTemplate);
        Prompt prompt = promptTemplate.create(Map.of(
                "code", dirtyCode,
                "format", converter.getFormat()
        ));

        String response = chatClient.prompt(prompt)
                .call()
                .content();

        return converter.convert(response);
    }

    private String leerCodigoFuente(String nombreClase) throws IOException {
        // Ajusta esta ruta a la base de tu proyecto
        Path path = Paths.get("src/main/java/com/example/demo/controller/", nombreClase + ".java");
        return Files.readString(path);
    }

    @GetMapping("/analyze-my-code")
    public CodeReview analyzeLocalFile(@RequestParam String className) {
        try {
            // 1. Leemos el archivo real de tu disco
            String localCode = leerCodigoFuente(className);

            var converter = new BeanOutputConverter<>(CodeReview.class);

            // 2. Prompt con contexto real
            String template = """
                Estás analizando un archivo real del proyecto actual del usuario.
                
                Archivo: {className}.java
                Código Fuente:
                ---
                {code}
                ---
                
                Realiza una auditoría técnica. Evalúa:
                1. Cumplimiento de estándares de Spring Boot 3.3.
                2. Uso correcto de ChatClient de Spring AI.
                3. Manejo de excepciones y escalabilidad.
                
                {format}
                """;

            PromptTemplate promptTemplate = new PromptTemplate(template);
            Prompt prompt = promptTemplate.create(Map.of(
                    "className", className,
                    "code", localCode,
                    "format", converter.getFormat()
            ));

            String response = chatClient.prompt(prompt).call().content();
            return converter.convert(response);

        } catch (IOException e) {
            throw new RuntimeException("No pude encontrar el archivo: " + className);
        }
    }
}