package com.digitallibrary.repository;

import com.digitallibrary.entity.Commission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CommissionRepository extends JpaRepository<Commission, Long> {
    Page<Commission> findByVendorProfileId(Long vendorProfileId, Pageable pageable);
    Page<Commission> findByVendorProfileUserEmail(String email, Pageable pageable);
}
