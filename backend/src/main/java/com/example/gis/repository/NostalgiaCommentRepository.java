package com.example.gis.repository;

import com.example.gis.entity.NostalgiaComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NostalgiaCommentRepository extends JpaRepository<NostalgiaComment, String> {
    List<NostalgiaComment> findByVillageIdOrderByCreatedAtDesc(String villageId);
}
