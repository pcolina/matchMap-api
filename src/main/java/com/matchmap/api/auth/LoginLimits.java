package com.matchmap.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

record LoginLimits(
    @NotBlank @Size(max = MAX_EMAIL_LENGTH) @Email String email,
    @NotNull @Size(min = 6, max = 20) @Pattern(regexp = "\\S*") String password) {

  static final int MAX_EMAIL_LENGTH = 254;
}
