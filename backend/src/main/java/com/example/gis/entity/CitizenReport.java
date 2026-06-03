package com.example.gis.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Lob;
import java.time.LocalDateTime;

@Entity
@Table(name = "CITIZEN_REPORT")
public class CitizenReport {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "issue_type", length = 50)
    private String issueType;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "reporter_name", length = 100)
    private String reporterName;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "x", length = 32)
    private String x;

    @Column(name = "y", length = 32)
    private String y;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getX() { return x; }
    public void setX(String x) { this.x = x; }

    public String getY() { return y; }
    public void setY(String y) { this.y = y; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
