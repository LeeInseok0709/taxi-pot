package com.taxipot.domain.rating.repository;

import com.taxipot.domain.rating.entity.Rating;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RatingRepository extends JpaRepository<Rating, Long> {

    List<Rating> findAllByPotIdAndReviewerId(Long potId, Long reviewerId);

    boolean existsByPotIdAndReviewerIdAndRevieweeId(Long potId, Long reviewerId, Long revieweeId);
}
