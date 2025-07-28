package com.glasswallet.company.data.model;

import com.glasswallet.platform.data.models.PlatformUser;
import jakarta.persistence.*;
import lombok.Data;

import java.util.UUID;

@Entity
@Data
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private UUID id;
    private String name;


    private String platformId;


}
