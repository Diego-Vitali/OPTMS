package com.optms.app.model;

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
@Table(name = "external_apikeys")
public class ExternalApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "custom_name")
    private String customName;

    @Column(name = "apikey")
    private String apikey;

    @Column(name = "companyId")
    private Long companyId;

    @Column(name = "active")
    private Boolean active;
}
