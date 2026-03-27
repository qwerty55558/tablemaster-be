package com.mycompany.tablemaster.dto.auth;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSettingsUpdateRequest {

    @NotNull
    private Boolean emailNotificationEnabled;

    @NotNull
    private Boolean pushNotificationEnabled;

    @NotNull
    private Boolean marketingNotificationEnabled;
}
