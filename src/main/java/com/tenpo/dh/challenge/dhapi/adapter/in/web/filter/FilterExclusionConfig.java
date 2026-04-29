package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import java.util.List;

/**
 * Centraliza los prefijos de URL excluidos del procesamiento de filtros.
 * <p>
 * Estos paths omiten el rate limiting, la validación de headers y el audit logging.
 * En producción, solo el path {@code /actuator} es accesible externamente;
 * los demás son solo para desarrollo y documentación.
 */
public final class FilterExclusionConfig {

    public static final List<String> EXCLUDED_PREFIXES = List.of(
            "/actuator", "/swagger-ui", "/v3/api-docs", "/mock", "/webjars", "/mock/percentage");

    private FilterExclusionConfig() {}

    /**
     * Retorna {@code true} si el path dado empieza con alguno de los prefijos excluidos.
     *
     * @param path el path de la request a evaluar
     * @return {@code true} si el path debe omitir el procesamiento de filtros
     */
    public static boolean isExcluded(String path) {
        return EXCLUDED_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
