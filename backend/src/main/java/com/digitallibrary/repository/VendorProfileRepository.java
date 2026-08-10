package com.digitallibrary.repository;

import com.digitallibrary.entity.VendorProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VendorProfileRepository extends JpaRepository<VendorProfile, Long> {
    Optional<VendorProfile> findByUserId(Long userId);
    Optional<VendorProfile> findByUserEmail(String email);
    Page<VendorProfile> findByStatus(String status, Pageable pageable);
    boolean existsByUserId(Long userId);
}
