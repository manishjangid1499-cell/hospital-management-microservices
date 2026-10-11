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
public class InvoiceItemRequest {

    private String serviceName;
    private String serviceCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private String description;
}
