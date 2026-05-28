package edu.iztech.utms.g02.utms_app.dal.application.entity;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationTimestampTest {

    @Test
    void updatedAt_hasHibernateUpdateTimestampAnnotation() throws NoSuchFieldException {
        Field updatedAtField = Application.class.getDeclaredField("updatedAt");

        assertThat(updatedAtField.getType()).isEqualTo(LocalDateTime.class);
        assertThat(updatedAtField.isAnnotationPresent(CreationTimestamp.class)).isTrue();
        assertThat(updatedAtField.isAnnotationPresent(UpdateTimestamp.class)).isTrue();
    }
}
