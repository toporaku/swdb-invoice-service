package com.invoice.api.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.invoice.api.entity.Coupon;

@Repository
public interface RepoCoupon extends JpaRepository<Coupon, Integer> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
}
