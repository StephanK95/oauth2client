package com.klamann.oauth2client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@Scope("prototype")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Details {

    private String accessToken;
    private String scope;
    private String state;
    private String refreshToken;
}
