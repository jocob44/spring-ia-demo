package com.example.demo.controller;

import com.example.demo.service.CodeFileService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;


@RestController
@RequestMapping("/ai/self-healing")
public class SelfHealingController {

    private final ChatClient chatClient;
    private final CodeFileService fileService;

    public SelfHealingController(ChatClient.Builder builder, CodeFileService fileService) {
        this.chatClient = builder.build();
        this.fileService = fileService;
    }

    @PostMapping("/apply-fix")
    public String applyFix(@RequestParam String className) {
        try {
            // 1. Leer el código actual
            String currentCode = fileService.readClass(className);

            // 2. Pedirle a la IA la versión corregida (Sin explicaciones, solo código)
            String fixedCode = chatClient.prompt()
                    .system("""
                            Eres un agente de auto-corrección de código. 
                            Tu salida debe ser exclusivamente el código fuente corregido. 
                            No incluyas markdown (como ```java), ni saludos, ni explicaciones.
                            Aplica Clean Code, mejora el rendimiento y asegura que sea compatible con Spring Boot 3.3.
                            """)
                    .user("Corrige este código:\n" + currentCode)
                    .call()
                    .content();

            // 3. Aplicar la corrección al disco
            String cleanedCode = limpiarCodigoMarkdown(fixedCode); // Aseguramos que no queden etiquetas de markdown
            fileService.writeClass(className, cleanedCode);

            return "¡Éxito! El archivo " + className + ".java ha sido actualizado y se creó un backup .bak";

        } catch (Exception e) {
            return "Error en la auto-corrección: " + e.getMessage();
        }
    }

    public record TechnicalDebtReport(
            String className,
            double scoreCalidad, // 1 a 10
            String riesgoPrincipal,
            int horasAhorradasEstimadas, // Si se aplica el refactor
            String impactoNegocio,
            List<String> sugerenciasCriticas
    ) {}

    @GetMapping("/debt-report")
    public TechnicalDebtReport getDebtReport(@RequestParam String className) {
        try {
            String code = fileService.readClass(className);
            var converter = new BeanOutputConverter<>(TechnicalDebtReport.class);

            String auditTemplate = """
                Actúa como un Auditor de Software de Élite. 
                Analiza el código adjunto y calcula la deuda técnica acumulada.
                
                Contexto: Proyecto Spring Boot 3.3 de alta criticidad.
                
                Criterios de Evaluación:
                1. Complejidad Ciclomática (qué tan difícil es seguir la lógica).
                2. Acoplamiento (dependencias innecesarias).
                3. Seguridad (riesgos de inyección o exposición de datos).
                4. Mantenibilidad.

                {format}

                Código a auditar:
                ---
                {code}
                """;

            PromptTemplate promptTemplate = new PromptTemplate(auditTemplate);
            Prompt prompt = promptTemplate.create(Map.of(
                    "code", code,
                    "format", converter.getFormat()
            ));

            String response = chatClient.prompt(prompt).call().content();
            return converter.convert(response);

        } catch (IOException e) {
            throw new RuntimeException("No se pudo leer el archivo para la auditoría.");
        }
    }

    public record OrchestrationResult(
            String className,
            double scorePrevio,
            boolean fueCorregido,
            String accionTomada,
            String logAuditoria
    ) {}

    @PostMapping("/orchestrate")
    public OrchestrationResult orchestrateRefactor(@RequestParam String className) {
        try {
            // 1. Fase de Auditoría
            TechnicalDebtReport report = getDebtReport(className); // Reutilizamos tu lógica de deuda

            boolean necesitaIntervencion = report.scoreCalidad() < 7.0;
            String log;

            if (necesitaIntervencion) {
                // 2. Fase de Corrección (Solo si el score es bajo)
                applyFix(className); // Reutilizamos tu lógica de auto-corrección
                log = "Se detectó riesgo alto: " + report.riesgoPrincipal() + ". Refactorización aplicada con éxito.";
            } else {
                log = "El código cumple con los estándares (Score: " + report.scoreCalidad() + "). No se requiere acción.";
            }

            return new OrchestrationResult(
                    className,
                    report.scoreCalidad(),
                    necesitaIntervencion,
                    necesitaIntervencion ? "REFACTOR" : "NONE",
                    log
            );

        } catch (Exception e) {
            return new OrchestrationResult(className, 0, false, "ERROR", e.getMessage());
        }
    }

    private String limpiarCodigoMarkdown(String contenido) {
        if (contenido == null) return "";

        // Elimina la etiqueta de apertura ```java o ```
        String limpio = contenido.replaceAll("(?i)```java", "");
        limpio = limpio.replaceAll("(?i)```", "");

        // Elimina espacios en blanco o saltos de línea al inicio y final
        return limpio.trim();
    }

    @PostMapping("/orchestrate-full")
    public OrchestrationResult orchestrateWithTest(@RequestParam String className) {
        try {
            // 1. Auditoría
            TechnicalDebtReport report = getDebtReport(className);

            // 2. Refactor (si es necesario)
            if (report.scoreCalidad() < 7.0) {
                applyFix(className);
            }

            // 3. GENERACIÓN DE TEST (La pieza final)
            String updatedCode = fileService.readClass(className);

            String testPrompt = """
                Eres un experto en QA Automation. Genera un test unitario con JUnit 5 y Mockito.
                
                Para la clase: {className}
                Código: {code}
                
                Requisitos:
                1. Cubre los casos de éxito y error.
                2. Usa @ExtendWith(MockitoExtension.class).
                3. Devuelve SOLO el código, sin markdown ni explicaciones.
                """;

            String rawTest = chatClient.prompt()
                    .user(u -> u.text(testPrompt)
                            .params(Map.of(
                                    "className", className,
                                    "code", updatedCode
                            )))
                    .call()
                    .content();

            // Limpiamos y guardamos el test
            String cleanTest = limpiarCodigoMarkdown(rawTest);
            fileService.writeTest(className, cleanTest);

            return new OrchestrationResult(
                    className,
                    report.scoreCalidad(),
                    true,
                    "REFACTOR + TEST_GENERATED",
                    "Código saneado y test unitario creado en src/test/java"
            );

        } catch (Exception e) {
            return new OrchestrationResult(className, 0, false, "ERROR", e.getMessage());
        }
    }

    @PostMapping("/generate-docs")
    public String generateDocs(@RequestParam String className) {
        try {
            // 1. Leer el código del controlador
            String code = fileService.readClass(className);

            // 2. Prompt diseñado para documentación técnica
            String docPrompt = """
                Actúa como un Technical Writer especializado en APIs REST.
                Analiza el siguiente código de Spring Boot y genera una documentación técnica en Markdown.
                
                Código:
                {code}
                
                La documentación debe incluir:
                1. Nombre del Controlador y su propósito general.
                2. Un listado de Endpoints (Verbo HTTP + URL).
                3. Para cada endpoint: Parámetros de entrada, tipo de retorno y una breve descripción.
                4. Un ejemplo de uso con 'curl'.
                
                Usa un tono profesional y limpio. No incluyas bloques de código Java, solo el Markdown.
                """;

            // 3. Ejecutar con Llama 3.3
            String markdownDoc = chatClient.prompt()
                    .user(u -> u.text(docPrompt)
                            .params(Map.of("code", code)))
                    .call()
                    .content();

            // 4. Limpiar (por si acaso) y Guardar
            String cleanDoc = limpiarCodigoMarkdown(markdownDoc);
            fileService.writeDocumentation(cleanDoc);

            return "Documentación generada con éxito en DOCUMENTATION.md";

        } catch (Exception e) {
            return "Error generando documentación: " + e.getMessage();
        }
    }
}