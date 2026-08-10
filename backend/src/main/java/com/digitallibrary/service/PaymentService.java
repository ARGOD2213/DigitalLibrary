package com.digitallibrary.service;

import com.digitallibrary.dto.CheckoutRequest;
import com.digitallibrary.dto.PageResponse;
import com.digitallibrary.dto.PaymentResponse;

public interface PaymentService {
    PaymentResponse initiateCheckout(String userEmail, CheckoutRequest request);
    PaymentResponse handleWebhook(String gateway, String payload, String signature);
    PageResponse<PaymentResponse> getUserPayments(String userEmail, int page, int size);
}
