package com.genc.billing_service.service;

import com.genc.billing_service.dto.*;
import com.genc.billing_service.model.*;
import com.genc.billing_service.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillingService {

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;

    // ==================== INVOICE OPERATIONS ====================

    // Generate invoice
    @Transactional
    public InvoiceDTO generateInvoice(InvoiceRequest request) {
        log.info("Generating invoice for patient {}", request.getPatientId());

        Invoice invoice = null;
        if (request.getAppointmentId() != null) {
            invoice = invoiceRepository.findByAppointmentId(request.getAppointmentId()).orElse(null);
        }
        if (invoice == null && request.getPatientId() != null) {
            invoice = invoiceRepository.findFirstByPatientIdAndPaymentStatusNotOrderByInvoiceDateDesc(
                    request.getPatientId(), PaymentStatus.PAID).orElse(null);
        }

        if (invoice == null) {
            String invoiceNumber = generateInvoiceNumber();
            invoice = Invoice.builder()
                    .invoiceNumber(invoiceNumber)
                    .patientId(request.getPatientId())
                    .appointmentId(request.getAppointmentId())
                    .invoiceDate(LocalDate.now())
                    .description(request.getDescription())
                    .paymentStatus(PaymentStatus.UNPAID)
                    .amountPaid(BigDecimal.ZERO)
                    .items(new java.util.ArrayList<>())
                    .build();
        }

        if (invoice.getItems() == null) {
            invoice.setItems(new java.util.ArrayList<>());
        }

        // Add or update invoice items (prevents duplicate items on same invoice)
        if (request.getItems() != null) {
            for (InvoiceItemRequest itemRequest : request.getItems()) {
                BigDecimal unitPrice = itemRequest.getUnitPrice() != null ? itemRequest.getUnitPrice() : BigDecimal.ZERO;
                int qty = itemRequest.getQuantity() != null && itemRequest.getQuantity() > 0 ? itemRequest.getQuantity() : 1;
                BigDecimal itemAmount = unitPrice.multiply(BigDecimal.valueOf(qty));

                InvoiceItem existingItem = null;
                for (InvoiceItem existing : invoice.getItems()) {
                    if (itemRequest.getServiceCode() != null && itemRequest.getServiceCode().equals(existing.getServiceCode())) {
                        existingItem = existing;
                        break;
                    } else if (itemRequest.getServiceName() != null && itemRequest.getServiceName().equalsIgnoreCase(existing.getServiceName())) {
                        existingItem = existing;
                        break;
                    }
                }

                if (existingItem != null) {
                    existingItem.setQuantity(qty);
                    existingItem.setUnitPrice(unitPrice);
                    existingItem.setAmount(itemAmount);
                    if (itemRequest.getDescription() != null) {
                        existingItem.setDescription(itemRequest.getDescription());
                    }
                } else {
                    InvoiceItem item = InvoiceItem.builder()
                            .invoice(invoice)
                            .serviceName(itemRequest.getServiceName())
                            .serviceCode(itemRequest.getServiceCode())
                            .quantity(qty)
                            .unitPrice(unitPrice)
                            .amount(itemAmount)
                            .description(itemRequest.getDescription())
                            .build();

                    invoice.getItems().add(item);
                }
            }
        }

        // Recalculate total amount from all items
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (InvoiceItem item : invoice.getItems()) {
            totalAmount = totalAmount.add(item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
        }

        invoice.setTotalAmount(totalAmount);
        invoice.setPatientPayable(totalAmount);

        Invoice saved = invoiceRepository.saveAndFlush(invoice);
        log.info("Invoice saved with number: {}, total items: {}, total amount: {}", saved.getInvoiceNumber(), saved.getItems().size(), saved.getTotalAmount());

        return mapToDTO(saved);
    }

    private String generateInvoiceNumber() {
        String prefix = "INV";
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String random = String.format("%04d", Math.abs(UUID.randomUUID().hashCode() % 10000));
        String invoiceNumber = prefix + date + random;

        while (invoiceRepository.existsByInvoiceNumber(invoiceNumber)) {
            random = String.format("%04d", Math.abs(UUID.randomUUID().hashCode() % 10000));
            invoiceNumber = prefix + date + random;
        }

        return invoiceNumber;
    }

    // Get invoice by ID
    public InvoiceDTO getInvoiceById(Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        return mapToDTO(invoice);
    }

    // Get invoice by number
    public InvoiceDTO getInvoiceByNumber(String invoiceNumber) {
        Invoice invoice = invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        return mapToDTO(invoice);
    }

    // Get patient invoices
    public List<InvoiceDTO> getPatientInvoices(Long patientId) {
        return invoiceRepository.findByPatientIdOrderByInvoiceDateDesc(patientId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Get all invoices
    public List<InvoiceDTO> getAllInvoices() {
        return invoiceRepository.findAll()
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Get unpaid invoices
    public List<InvoiceDTO> getUnpaidInvoices() {
        return invoiceRepository.findByPaymentStatusNotOrderByInvoiceDateDesc(PaymentStatus.PAID)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    // Delete invoice
    @Transactional
    public void deleteInvoice(Long invoiceId) {
        log.info("Deleting invoice with ID: {}", invoiceId);
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Invoice not found"));
        
        // Check if invoice has any payments
        if (invoice.getAmountPaid() != null && invoice.getAmountPaid().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Cannot delete invoice with existing payments");
        }
        
        invoiceRepository.delete(invoice);
        log.info("Invoice deleted successfully: {}", invoice.getInvoiceNumber());
    }

    // ==================== PAYMENT OPERATIONS ====================

    // Record payment
    @Transactional
    public PaymentDTO recordPayment(PaymentRequest request) {
        log.info("Recording payment for invoice {}", request.getInvoiceId());

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Invoice not found"));

        if (invoice.getPaymentStatus() == PaymentStatus.PAID) {
            throw new RuntimeException("Invoice is already fully paid");
        }

        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Payment amount must be greater than zero");
        }

        BigDecimal remainingAmount = invoice.getPatientPayable().subtract(invoice.getAmountPaid());
        if (request.getAmount().compareTo(remainingAmount) > 0) {
            throw new RuntimeException("Payment amount exceeds remaining balance");
        }

        Payment payment = Payment.builder()
                .invoice(invoice)
                .amount(request.getAmount())
                .paymentMethod(request.getPaymentMethod())
                .transactionReference(request.getTransactionReference())
                .notes(request.getNotes())
                .receivedBy(request.getReceivedBy())
                .build();

        Payment saved = paymentRepository.save(payment);

        // Update invoice
        BigDecimal newAmountPaid = invoice.getAmountPaid().add(request.getAmount());
        invoice.setAmountPaid(newAmountPaid);

        if (newAmountPaid.compareTo(invoice.getPatientPayable()) >= 0) {
            invoice.setPaymentStatus(PaymentStatus.PAID);
        } else {
            invoice.setPaymentStatus(PaymentStatus.PARTIAL);
        }

        invoiceRepository.save(invoice);
        log.info("Payment recorded successfully");

        return mapPaymentToDTO(saved);
    }

    // Get payments for invoice
    public List<PaymentDTO> getInvoicePayments(Long invoiceId) {
        return paymentRepository.findByInvoiceInvoiceId(invoiceId)
                .stream()
                .map(this::mapPaymentToDTO)
                .collect(Collectors.toList());
    }

    // ==================== MAPPING METHODS ====================

    private InvoiceDTO mapToDTO(Invoice invoice) {
        List<InvoiceItemDTO> items = invoice.getItems() != null ?
                invoice.getItems().stream().map(this::mapItemToDTO).collect(Collectors.toList()) :
                List.of();

        List<PaymentDTO> payments = invoice.getPayments() != null ?
                invoice.getPayments().stream().map(this::mapPaymentToDTO).collect(Collectors.toList()) :
                List.of();

        return InvoiceDTO.builder()
                .invoiceId(invoice.getInvoiceId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .patientId(invoice.getPatientId())
                .appointmentId(invoice.getAppointmentId())
                .invoiceDate(invoice.getInvoiceDate())
                .totalAmount(invoice.getTotalAmount())
                .patientPayable(invoice.getPatientPayable())
                .amountPaid(invoice.getAmountPaid())
                .paymentStatus(invoice.getPaymentStatus())
                .description(invoice.getDescription())
                .items(items)
                .payments(payments)
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    private InvoiceItemDTO mapItemToDTO(InvoiceItem item) {
        return InvoiceItemDTO.builder()
                .itemId(item.getItemId())
                .serviceName(item.getServiceName())
                .serviceCode(item.getServiceCode())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .amount(item.getAmount())
                .description(item.getDescription())
                .build();
    }

    private PaymentDTO mapPaymentToDTO(Payment payment) {
        return PaymentDTO.builder()
                .paymentId(payment.getPaymentId())
                .invoiceId(payment.getInvoice().getInvoiceId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .transactionReference(payment.getTransactionReference())
                .notes(payment.getNotes())
                .receivedBy(payment.getReceivedBy())
                .paymentDate(payment.getPaymentDate())
                .build();
    }

    // Delete all invoices for a patient (cascade deletion)
    @Transactional
    public void deleteByPatientId(Long patientId) {
        log.info("Cascade deleting all invoices for patient ID: {}", patientId);
        invoiceRepository.deleteByPatientId(patientId);
    }

}

