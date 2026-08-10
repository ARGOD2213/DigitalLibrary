package com.digitallibrary.service;

import com.digitallibrary.entity.OrderItem;

public interface CommissionService {
    void calculateAndRecordCommission(OrderItem orderItem);
}
