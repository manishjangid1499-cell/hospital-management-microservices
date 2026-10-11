package com.genc.pharmacy_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionRequest {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Encounter Record ID is required")
    private Long recordId;

    @NotBlank(message = "Medication name is required")
    @Size(min = 2, max = 100, message = "Medication name must be between 2 and 100 characters")
    private String medicationName;

    @Size(max = 50, message = "Dosage must not exceed 50 characters")
    private String dosage;

    @Size(max = 50, message = "Frequency must not exceed 50 characters")
    private String frequency;

    @Size(max = 50, message = "Duration must not exceed 50 characters")
    private String duration;

    @NotNull(message = "Quantity is required")
    @Positive(message = "Quantity must be greater than 0")
    private Integer quantity;

    @Size(max = 500, message = "Instructions must not exceed 500 characters")
    private String instructions;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}

