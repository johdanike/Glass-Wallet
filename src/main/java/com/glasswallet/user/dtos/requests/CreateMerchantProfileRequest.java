package com.glasswallet.user.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateMerchantProfileRequest {
    
    @NotBlank(message = "Business name is required")
    private String businessName;
    
    @NotBlank(message = "Business type is required")
    private String businessType;
    
    @NotBlank(message = "Bank account number is required")
    @Pattern(regexp = "\\d{10}", message = "Bank account number must be 10 digits")
    private String bankAccountNumber;
    
    @NotBlank(message = "Bank name is required")
    private String bankName;
    
    private String deviceId; // Optional for SoftPOS linking
}