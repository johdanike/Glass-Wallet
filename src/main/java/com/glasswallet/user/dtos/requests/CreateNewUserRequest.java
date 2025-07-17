package com.glasswallet.user.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;


@Data
public class CreateNewUserRequest {
    @NotEmpty(message = "first name cannot be empty")
    @Pattern(
            regexp = "^[a-zA-ZÀ-ÖØ-öø-ÿ'\\- ]+$",
            message = "First name contains invalid characters"
    )
    private String firstName;

    @NotEmpty(message = "last name cannot be empty")
    @Pattern(
            regexp = "^[a-zA-ZÀ-ÖØ-öø-ÿ'\\- ]+$",
            message = "Last name contains invalid characters"
    )
    private String lastName;

    @Pattern(regexp = "^\\+?[0-9\\s]{7,14}$",
            message = "Invalid phone number format")
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    @NotEmpty(message = "password cannot be empty")
    private String password;

}
