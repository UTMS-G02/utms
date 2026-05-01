package edu.iztech.utms.g02.utms_app.dal.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "students")
@Getter
@Setter
@NoArgsConstructor
public class Student extends User {

    @Column(nullable = false, unique = true, length = 11)
    private String tckn;

    private String phoneNumber;

    private LocalDate dateOfBirth;

    private LocalDateTime kvkkAcceptedAt;

    private Float gpa;

    private String currentUniversity;

    private String currentDepartment;
}