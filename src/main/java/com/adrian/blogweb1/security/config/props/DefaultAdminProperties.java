package com.adrian.blogweb1.security.config.props;


import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;


// ¡OJO! Ya no usamos @Component ni @Setter
@ConfigurationProperties(prefix = "default.admin")
@Getter
@Validated
public class DefaultAdminProperties {

    @NotEmpty
    private final String username; // Los campos ahora son 'final'

    @NotEmpty
    private final String password;

    @NotEmpty
    private final String email;

    // Usamos un constructor para la inyección de valores
    public DefaultAdminProperties(String username, String password, String email) {
        this.username = username;
        this.password = password;
        this.email = email;
    }
}
