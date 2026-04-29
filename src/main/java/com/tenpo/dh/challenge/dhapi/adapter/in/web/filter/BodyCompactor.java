package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/**
 * Compacta un String a una representación de una sola línea sin espacios extra.
 * <p>
 * Si el input es JSON válido, se parsea y se re-serializa en formato compacto.
 * Si no es JSON válido, se normaliza colapsando los espacios en blanco.
 * <p>
 * Usado para sanitizar el request y response body antes de persistir en el audit log.
 */
@Component
public class BodyCompactor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /**
     * Compacta el body a una sola línea.
     *
     * @param body el contenido a compactar; puede ser {@code null} o vacío
     * @return body compactado, o el original si es {@code null} o blank
     */
    public String compact(String body) {
        if (body == null || body.isBlank()) {
            return body;
        }
        try {
            Object parsed = OBJECT_MAPPER.readValue(body, Object.class);
            return OBJECT_MAPPER.writeValueAsString(parsed);
        } catch (Exception e) {
            return body.replaceAll("\\s+", " ").strip();
        }
    }
}
