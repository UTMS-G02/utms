package edu.iztech.utms.g02.utms_app.dal.evaluation.entity;

import jakarta.persistence.Column;
import org.hibernate.annotations.CreationTimestamp;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PublishedResultTest {

    @Test
    void publishedAt_hasCreationTimestampAnnotation() throws NoSuchFieldException {
        Field field = PublishedResult.class.getDeclaredField("publishedAt");

        assertThat(field.getType()).isEqualTo(LocalDateTime.class);
        assertThat(field.isAnnotationPresent(CreationTimestamp.class)).isTrue();
    }

    @Test
    void finalDecision_columnLengthFitsAcceptedAndRejected() throws NoSuchFieldException {
        Field field = PublishedResult.class.getDeclaredField("finalDecision");
        Column column = field.getAnnotation(Column.class);

        assertThat(column).isNotNull();
        assertThat(column.length()).isGreaterThanOrEqualTo("ACCEPTED".length());
        assertThat(column.length()).isGreaterThanOrEqualTo("REJECTED".length());
        assertThat(column.nullable()).isFalse();
    }

    @Test
    void builder_setsAllFields() {
        PublishedResult result = PublishedResult.builder()
                .id(1)
                .finalDecision("ACCEPTED")
                .build();

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getFinalDecision()).isEqualTo("ACCEPTED");
        assertThat(result.getPublishedAt()).isNull();
        assertThat(result.getApplication()).isNull();
        assertThat(result.getPublishedBy()).isNull();
    }

    @Test
    void setter_updatesFinalDecision() {
        PublishedResult result = new PublishedResult();
        result.setFinalDecision("REJECTED");

        assertThat(result.getFinalDecision()).isEqualTo("REJECTED");
    }
}
