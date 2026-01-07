package com.mycompany.tablemaster.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.validation")
public class AuthValidationProperties {

    private Email email = new Email();
    private Password password = new Password();
    private Name name = new Name();
    private Phone phone = new Phone();

    @Getter
    @Setter
    public static class Email {
        private int maxLength = 30;
    }

    @Getter
    @Setter
    public static class Password {
        private int minLength = 8;
        private int maxLength = 30;
        private boolean requireUppercase = true;
        private boolean requireLowercase = true;
        private boolean requireNumber = true;
        private boolean requireSpecialChar = true;
    }

    @Getter
    @Setter
    public static class Name {
        private int minLength = 2;
        private int maxLength = 30;
    }

    @Getter
    @Setter
    public static class Phone {
        private String pattern = "^01[0-9]-?[0-9]{3,4}-?[0-9]{4}$";
    }
}
