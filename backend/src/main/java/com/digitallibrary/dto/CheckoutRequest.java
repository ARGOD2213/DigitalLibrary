package com.digitallibrary.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class CheckoutRequest {

    @NotEmpty(message = "At least one book ID is required")
    private List<Long> bookIds;

    @NotNull(message = "Payment gateway is required")
    private String paymentGateway; // STRIPE, RAZORPAY, MOCK

    public CheckoutRequest() {}

    public List<Long> getBookIds() { return bookIds; }
    public void setBookIds(List<Long> bookIds) { this.bookIds = bookIds; }

    public String getPaymentGateway() { return paymentGateway; }
    public void setPaymentGateway(String paymentGateway) { this.paymentGateway = paymentGateway; }
}
