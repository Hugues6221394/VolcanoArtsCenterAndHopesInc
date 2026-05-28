package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Donation;
import com.volcanoartscenter.platform.shared.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {
    List<Donation> findByUserOrderByCreatedAtDesc(User user);
    Optional<Donation> findByReference(String reference);
    Optional<Donation> findByStripeSubscriptionId(String stripeSubscriptionId);
}
