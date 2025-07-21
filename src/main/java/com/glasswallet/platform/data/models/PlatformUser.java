package com.glasswallet.platform.data.models;

import com.glasswallet.user.data.models.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;
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
    private String companyUserId;

    @Column(name = "platform_id", nullable = false)
    private String companyId; // e.g. "enum", "quickteller"

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

//    private List<User> values;

    private String token;
}
