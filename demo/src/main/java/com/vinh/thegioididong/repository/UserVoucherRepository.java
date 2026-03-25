package com.hutech.demo.repository;

import com.hutech.demo.model.User;
import com.hutech.demo.model.UserVoucher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserVoucherRepository extends JpaRepository<UserVoucher, Long> {

    List<UserVoucher> findByUserAndUsedOrderByCreatedAtDesc(User user, boolean used);

    Optional<UserVoucher> findByCodeAndUserAndUsedFalse(String code, User user);

    boolean existsByCode(String code);
}
