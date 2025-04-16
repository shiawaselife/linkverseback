package site.linkverse.back.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
import site.linkverse.back.enums.VisibilityType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table("users")
public class User {
    @Id
    private Long id;
    @Column("email")
    private String email;
    @Column("password")
    private String password;
    @Column("username")
    private String username;
    @Column("profile_image")
    private String profileImage;
    @Column("bio")
    private String bio;
    @Column("profile_visibility")
    private VisibilityType profileVisibility;
    @Column("created_at")
    private LocalDateTime createdAt;
    @Column("updated_at")
    private LocalDateTime updatedAt;
    @Column("last_login_at")
    private LocalDateTime lastLoginAt;
    @Column("email_verified")
    private boolean emailVerified;
    @Column("notification_settings")
    private String notificationSettings; // JSON 형태로 저장
}