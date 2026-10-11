package com.genc.pharmacy_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceRequest {

    private Long patientId;
    private Long appointmentId;
    private String description;
    private List<InvoiceItemRequest> items;
}
