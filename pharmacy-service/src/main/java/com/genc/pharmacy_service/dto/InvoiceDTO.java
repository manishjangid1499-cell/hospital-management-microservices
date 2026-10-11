package com.genc.pharmacy_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {

    private Long invoiceId;
    private String invoiceNumber;
    private Long patientId;
    private Long appointmentId;
    private BigDecimal totalAmount;
}
