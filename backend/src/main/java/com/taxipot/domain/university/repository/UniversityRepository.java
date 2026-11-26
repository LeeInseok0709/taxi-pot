package com.taxipot.domain.university.repository;

import com.taxipot.domain.university.entity.University;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UniversityRepository extends JpaRepository<University, Long> {

    List<University> findAllByIsActiveTrueOrderByNameAsc();

    Optional<University> findByNameAndIsActiveTrue(String name);
}
