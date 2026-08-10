package com.digitallibrary.service.impl;

import com.digitallibrary.dto.CheckoutRequest;
import com.digitallibrary.dto.PageResponse;
import com.digitallibrary.dto.PaymentResponse;
import com.digitallibrary.entity.*;
import com.digitallibrary.exception.ResourceNotFoundException;
import com.digitallibrary.repository.*;
import com.digitallibrary.service.AwsNotificationService;
import com.digitallibrary.service.CommissionService;
import com.digitallibrary.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final BookRepository bookRepository;
    private final AppUserRepository appUserRepository;
    private final CommissionService commissionService;
    private final AwsNotificationService awsNotificationService;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              BookRepository bookRepository,
                              AppUserRepository appUserRepository,
                              CommissionService commissionService,
                              AwsNotificationService awsNotificationService) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.bookRepository = bookRepository;
        this.appUserRepository = appUserRepository;
        this.commissionService = commissionService;
        this.awsNotificationService = awsNotificationService;
    }

    @Override
    @Transactional
    public PaymentResponse initiateCheckout(String userEmail, CheckoutRequest request) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Book> books = bookRepository.findAllById(request.getBookIds());
        if (books.isEmpty()) {
            throw new ResourceNotFoundException("No valid books found for checkout");
        }

        BigDecimal totalAmount = BigDecimal.ZERO;
        for (Book book : books) {
            totalAmount = totalAmount.add(book.getPrice());
        }

        // Create order
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = new Order(orderNumber, user, totalAmount);
        order.setStatus("PENDING");
        Order savedOrder = orderRepository.save(order);

        // Create order items and calculate commissions
        for (Book book : books) {
            OrderItem item = new OrderItem(savedOrder, book, book.getVendorProfile(),
                    book.getPrice(), BigDecimal.ZERO, BigDecimal.ZERO);
            savedOrder.getItems().add(item);
            commissionService.calculateAndRecordCommission(item);
        }
        orderRepository.save(savedOrder);

        // Calculate platform fee
        BigDecimal platformFee = BigDecimal.ZERO;
        for (OrderItem item : savedOrder.getItems()) {
            platformFee = platformFee.add(item.getPlatformCommission());
        }
        savedOrder.setPlatformFee(platformFee);
        orderRepository.save(savedOrder);

        // Create payment record (simulating gateway — in production, this would create a Stripe/Razorpay session)
        String transactionId = "TXN-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        String gateway = request.getPaymentGateway() != null ? request.getPaymentGateway() : "MOCK";
        
        Payment payment = new Payment(user, gateway, transactionId, totalAmount, "INR", "PENDING");
        payment.setOrder(savedOrder);
        payment.setPaymentIntentId("pi_" + UUID.randomUUID().toString().replace("-", "").substring(0, 24));

        // For MOCK gateway, immediately succeed
        if ("MOCK".equalsIgnoreCase(gateway)) {
            payment.setStatus("SUCCESS");
            savedOrder.setStatus("PAID");
            orderRepository.save(savedOrder);

            // Update book sales count
            for (Book book : books) {
                book.setTotalSales(book.getTotalSales() + 1);
                bookRepository.save(book);
            }

            // Send email receipt
            String subject = "Payment Confirmation - Digital Library";
            String body = "Your payment of " + totalAmount + " INR for order " + orderNumber + " was successful. Transaction ID: " + transactionId;
            awsNotificationService.sendEmail(user.getEmail(), subject, body);
        }

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Checkout initiated for user {} with order {} and payment {}", userEmail, orderNumber, transactionId);
        return PaymentResponse.fromEntity(savedPayment);
    }

    @Override
    @Transactional
    public PaymentResponse handleWebhook(String gateway, String payload, String signature) {
        // In production: verify webhook signature from Stripe/Razorpay
        // For now, we simulate webhook processing with signature validation placeholder
        log.info("Received {} webhook. Signature: {}", gateway, signature);

        // Parse the payload — in production this would be JSON from Stripe/Razorpay
        // For now we'll use the payload as the transaction ID
        Payment payment = paymentRepository.findByTransactionId(payload)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found for transaction: " + payload));

        payment.setStatus("SUCCESS");
        payment.setRawResponse("webhook_verified_at=" + LocalDateTime.now());

        if (payment.getOrder() != null) {
            Order order = payment.getOrder();
            order.setStatus("PAID");
            orderRepository.save(order);

            // Update book sales
            for (OrderItem item : order.getItems()) {
                Book book = item.getBook();
                book.setTotalSales(book.getTotalSales() + 1);
                bookRepository.save(book);
            }
        }

        Payment savedPayment = paymentRepository.save(payment);

        // Send confirmation email
        String subject = "Payment Confirmed - Digital Library";
        String body = "Your payment has been confirmed. Transaction: " + payment.getTransactionId();
        awsNotificationService.sendEmail(payment.getUser().getEmail(), subject, body);

        return PaymentResponse.fromEntity(savedPayment);
    }

    @Override
    public PageResponse<PaymentResponse> getUserPayments(String userEmail, int page, int size) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Page<Payment> payments = paymentRepository.findByUserId(
                user.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.fromPage(payments.map(PaymentResponse::fromEntity));
    }
}
