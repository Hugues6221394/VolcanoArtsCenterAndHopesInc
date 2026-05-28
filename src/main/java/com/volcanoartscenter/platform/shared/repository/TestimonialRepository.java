package com.volcanoartscenter.platform.shared.repository;

import com.volcanoartscenter.platform.shared.model.Testimonial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestimonialRepository extends JpaRepository<Testimonial, Long> {
    List<Testimonial> findByPublishedTrueOrderByIdDesc();
}
