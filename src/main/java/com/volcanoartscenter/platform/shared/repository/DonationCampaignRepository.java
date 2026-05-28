package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.DonationCampaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationCampaignRepository extends JpaRepository<DonationCampaign, Long> {

    List<DonationCampaign> findByActiveTrueOrderByNameAsc();
}
