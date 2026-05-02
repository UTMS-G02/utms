package edu.iztech.utms.g02.utms_app.dal.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "staff")
@Getter
@Setter
@NoArgsConstructor
@SuperBuilder
public class Staff extends User {

    private Integer departmentId;
}