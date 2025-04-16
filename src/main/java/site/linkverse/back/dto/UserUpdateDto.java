package site.linkverse.back.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import site.linkverse.back.enums.VisibilityType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDto {
    private String username;
    private String profileImage;
    private String bio;
    private VisibilityType profileVisibility;
    private String notificationSettings;
}