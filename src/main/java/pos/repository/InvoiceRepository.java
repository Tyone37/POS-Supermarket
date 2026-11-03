package pos.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pos.entity.Invoice;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    Optional<Invoice> findByOrderId(Long orderId);

    Optional<Invoice> findByPaymentId(Long paymentId);

    Optional<Invoice> findByInvoiceNo(String invoiceNo);
}
