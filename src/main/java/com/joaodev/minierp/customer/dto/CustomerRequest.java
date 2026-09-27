package com.joaodev.minierp.customer.dto;

import com.joaodev.minierp.common.validation.ValidDocument;
import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Setter
@Getter
public class CustomerRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 150, message = "Name must have at most 150 characters")
    private String name;

    @NotBlank(message = "Document is required")
    @ValidDocument
    private String document;

    @Email(message = "Invalid email")
    @Size(max = 150, message = "Email must have at most 150 characters")
    private String email;

    @Size(max = 20, message = "Phone must have at most 20 characters")
    private String phone;

    private Boolean active;


}
