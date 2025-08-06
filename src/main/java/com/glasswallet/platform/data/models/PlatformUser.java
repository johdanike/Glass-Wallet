package com.glasswallet.platform.data.models;

import com.glasswallet.config.JsonbConverter;
import com.glasswallet.user.data.models.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "platform_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"platform_id", "platform_user_id"})
})
public class PlatformUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", columnDefinition = "VARCHAR(36)", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "platform_user_id", nullable = false)
    private String platformUserId;

    @Column(name = "platform_id", nullable = false)
    private String platformId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Convert(converter = JsonbConverter.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, String> kycDetails = new HashMap<>();

    private BigDecimal balanceFiat = BigDecimal.ZERO;
    private BigDecimal balanceSui = BigDecimal.ZERO;
    private String token;
    private String email;

    @Column(name = "pin", length = 4)
    private String pin;

    @Column(name = "is_activated")
    private boolean isActivated = false;

    @Column(name = "generated_platform_user_id", unique = true)
     private String generatedPlatformUserId;
}