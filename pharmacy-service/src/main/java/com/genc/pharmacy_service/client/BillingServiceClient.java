package com.genc.pharmacy_service.client;

import com.genc.pharmacy_service.dto.ApiResponse;
import com.genc.pharmacy_service.dto.InvoiceDTO;
import com.genc.pharmacy_service.dto.InvoiceRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "billing-service")
public interface BillingServiceClient {

    @PostMapping("/api/billing/invoices")
    ApiResponse<InvoiceDTO> generateInvoice(@RequestBody InvoiceRequest request);
}
