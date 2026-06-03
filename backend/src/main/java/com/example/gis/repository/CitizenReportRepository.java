package com.example.gis.repository;

import com.example.gis.entity.CitizenReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CitizenReportRepository extends JpaRepository<CitizenReport, String> {
    List<CitizenReport> findAllByOrderByCreatedAtDesc();
}
