package edu.iztech.utms.g02.utms_app.dal.department.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "departments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Department {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer departmentId;

    @Column(nullable = false, unique = true)
    private String name;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = false)
    private Faculty faculty;

    // Pair 3: bölüm asil kontenjanı. Yayında bölüm-içi sıralamada ilk `quota` asil, gerisi yedek.
    // Additive + nullable (mevcut satırları bozmaz). Null = sınırsız (hepsi asil).
    @Column
    private Integer quota;
}