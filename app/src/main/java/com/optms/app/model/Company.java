package com.optms.app.model;

import java.sql.Timestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "companies")
public class Company {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "social_name")
    private String socialName;

    @Column(name = "document")
    private String document;

    @Column(name = "created_at")
    private Timestamp createdAt;

    @Column(name = "active")
    private Boolean active;

    @Column(name = "apikey")
    private String apikey;


}
