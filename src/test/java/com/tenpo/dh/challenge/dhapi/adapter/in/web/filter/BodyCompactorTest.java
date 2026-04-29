package com.tenpo.dh.challenge.dhapi.adapter.in.web.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BodyCompactorTest {

    private BodyCompactor compactor;

    @BeforeEach
    void setUp() {
        compactor = new BodyCompactor();
    }

    @Test
    void compact_validJson_returnsCompactSingleLine() {
        String prettyJson = "{\n  \"num1\": 5.0,\n  \"num2\": 5.0\n}";

        String result = compactor.compact(prettyJson);

        assertThat(result).doesNotContain("\n");
        assertThat(result).doesNotContain("  ");
        assertThat(result).contains("\"num1\"");
        assertThat(result).contains("5.0");
    }

    @Test
    void compact_alreadyCompactJson_returnsUnchanged() {
        String compact = "{\"num1\":5.0,\"num2\":5.0}";

        String result = compactor.compact(compact);

        assertThat(result).isEqualTo(compact);
    }

    @Test
    void compact_nonJsonText_collapsesWhitespace() {
        String text = "  plain   text  with   spaces  ";

        String result = compactor.compact(text);

        assertThat(result).doesNotStartWith(" ");
        assertThat(result).doesNotEndWith(" ");
        assertThat(result).isEqualTo("plain text with spaces");
    }

    @Test
    void compact_nullBody_returnsNull() {
        assertThat(compactor.compact(null)).isNull();
    }

    @Test
    void compact_blankBody_returnsBlank() {
        assertThat(compactor.compact("   ")).isEqualTo("   ");
    }

    @Test
    void compact_emptyString_returnsEmpty() {
        assertThat(compactor.compact("")).isEqualTo("");
    }
}
