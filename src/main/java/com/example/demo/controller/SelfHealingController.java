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
}