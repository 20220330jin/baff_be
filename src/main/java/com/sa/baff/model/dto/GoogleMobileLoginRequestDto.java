package com.sa.baff.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GoogleMobileLoginRequestDto {
    private String idToken;
    private String email;
    private String name;

}
