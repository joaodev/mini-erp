package com.joaodev.minierp.common.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class DocumentUtilsTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "52998224725",
            "529.982.247-25",
            "11222333000181",
            "11.222.333/0001-81",
            "12ABC34501DE35",
            "12.ABC.345/01DE-35",
            "12.abc.345/01de-35"
    })
    void shouldAcceptValidCpfAndCnpj(String document) {
        assertThat(DocumentUtils.isValid(document)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {
            "   ",
            "123",
            "52998224726",
            "111.111.111-11",
            "11222333000182",
            "00000000000000",
            "ABCDEFGHIJK",
            "12ABC34501DE36"
    })
    void shouldRejectInvalidDocuments(String document) {
        assertThat(DocumentUtils.isValid(document)).isFalse();
    }

    @Test
    void shouldNormalizeDocumentKeepingOnlyLettersAndDigitsInUpperCase() {
        assertThat(DocumentUtils.normalize("529.982.247-25")).isEqualTo("52998224725");
        assertThat(DocumentUtils.normalize("12.abc.345/01de-35")).isEqualTo("12ABC34501DE35");
    }

    @Test
    void shouldReturnNullWhenNormalizingNull() {
        assertThat(DocumentUtils.normalize(null)).isNull();
    }
}
