package com.example.gis.repository;

import com.example.gis.entity.Zdgc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ZdgcRepository extends JpaRepository<Zdgc, String> {

    /** 按工程状态筛选 */
    List<Zdgc> findByStatus(String status);

    /** 按工程分类筛选 */
    List<Zdgc> findByCategory(String category);

    /** 按状态和分类联合筛选 */
    List<Zdgc> findByStatusAndCategory(String status, String category);
}
