package com.example.gis.controller;

import com.example.gis.entity.CitizenReport;
import com.example.gis.repository.CitizenReportRepository;
import com.example.gis.utils.Result;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/citizen")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CitizenController {

    private final CitizenReportRepository repository;

    @Value("${citizen.upload-dir:uploads/citizen}")
    private String uploadDir;

    public CitizenController(CitizenReportRepository repository) {
        this.repository = repository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
        } catch (IOException e) {
            // ignore
        }
    }

    /** 提交随手拍（含图片上传） */
    @PostMapping("/report")
    public Result<CitizenReport> report(
            @RequestParam(required = false) MultipartFile image,
            @RequestParam String description,
            @RequestParam(defaultValue = "其他") String issueType,
            @RequestParam(defaultValue = "热心市民") String reporterName,
            @RequestParam(required = false) String contactPhone,
            @RequestParam(required = false) String x,
            @RequestParam(required = false) String y) {

        CitizenReport report = new CitizenReport();
        report.setId(UUID.randomUUID().toString());
        report.setIssueType(issueType);
        report.setDescription(description);
        report.setReporterName(reporterName);
        report.setContactPhone(contactPhone);
        report.setX(x);
        report.setY(y);
        report.setCreatedAt(LocalDateTime.now());

        // 保存上传图片
        if (image != null && !image.isEmpty()) {
            try {
                String fileName = UUID.randomUUID() + "_" + image.getOriginalFilename();
                Path target = Paths.get(uploadDir, fileName);
                Files.copy(image.getInputStream(), target);
                report.setImageUrl("/uploads/citizen/" + fileName);
            } catch (IOException e) {
                return Result.error("图片上传失败: " + e.getMessage(), 500);
            }
        }

        repository.save(report);
        return Result.success("上报成功，感谢支持！", report);
    }

    /** 获取随手拍列表 */
    @GetMapping("/list")
    public Result<List<CitizenReport>> list() {
        return Result.success(repository.findAllByOrderByCreatedAtDesc());
    }
}
