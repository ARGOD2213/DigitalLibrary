package com.digitallibrary.service.impl;

import com.digitallibrary.entity.Book;
import com.digitallibrary.entity.Commission;
import com.digitallibrary.entity.OrderItem;
import com.digitallibrary.entity.VendorProfile;
import com.digitallibrary.repository.CommissionRepository;
import com.digitallibrary.service.CommissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class CommissionServiceImpl implements CommissionService {

    private static final Logger log = LoggerFactory.getLogger(CommissionServiceImpl.class);

    private final CommissionRepository commissionRepository;

    public CommissionServiceImpl(CommissionRepository commissionRepository) {
        this.commissionRepository = commissionRepository;
    }

    @Override
    @Transactional
    public void calculateAndRecordCommission(OrderItem orderItem) {
        Book book = orderItem.getBook();
        if (book == null) {
            log.warn("OrderItem {} has no associated book, skipping commission calculation.", orderItem.getId());
            return;
        }

        VendorProfile vendor = book.getVendorProfile();
        if (vendor == null) {
            log.info("Book {} has no vendor profile, skipping commission calculation. (Platform owned)", book.getId());
            orderItem.setPlatformCommission(orderItem.getPrice());
            orderItem.setVendorEarning(BigDecimal.ZERO);
            return;
        }

        BigDecimal price = orderItem.getPrice();
        BigDecimal commissionRate = vendor.getCommissionRate(); // Platform's cut percentage, e.g. 10.00
        
        if (commissionRate == null) {
            commissionRate = new BigDecimal("10.00"); // default fallback
        }

        BigDecimal platformCommission = price.multiply(commissionRate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal vendorEarning = price.subtract(platformCommission);

        orderItem.setPlatformCommission(platformCommission);
        orderItem.setVendorEarning(vendorEarning);
        orderItem.setVendorProfile(vendor);

        Commission commission = new Commission(vendor, orderItem, price, platformCommission, vendorEarning);
        commissionRepository.save(commission);

        log.info("Recorded commission for OrderItem {}: Platform = {}, Vendor = {}", 
                 orderItem.getId(), platformCommission, vendorEarning);
    }
}
