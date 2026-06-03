package com.example.gis.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.time.LocalDateTime;

@Entity
@Table(name = "SUPPLY_MERCHANT")
public class SupplyMerchant {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "merchant_name", length = 255, nullable = false)
    private String merchantName;

    @Column(name = "business_type", length = 100)
    private String businessType;

    @Column(name = "contact_person", length = 50)
    private String contactPerson;

    @Column(name = "contact_phone", length = 50)
    private String contactPhone;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "POINT_X", length = 32)
    private String x;

    @Column(name = "POINT_Y", length = 32)
    private String y;

    @Column(name = "service_radius")
    private Double serviceRadius;

    @Column(name = "is_vip")
    private Boolean isVip;

    // ========= 新增字段 (FR-2.3 黄页展示) =========

    /** 商家 Logo URL */
    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    /** 商家简介 */
    @Column(name = "description", length = 2000)
    private String description;

    /** 营业时间 */
    @Column(name = "business_hours", length = 100)
    private String businessHours;

    /** 评分 1-5 */
    @Column(name = "rating")
    private Double rating;

    /** 是否认证入驻 */
    @Column(name = "verified")
    private Boolean verified;

    /** 入驻时间 */
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 距离当前检索点的距离（非持久化，仅 API 响应用） */
    @Transient
    private Double distance;

    // ========= 原有的 Getter/Setter =========

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMerchantName() {
        return merchantName;
    }

    public void setMerchantName(String merchantName) {
        this.merchantName = merchantName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getContactPerson() {
        return contactPerson;
    }

    public void setContactPerson(String contactPerson) {
        this.contactPerson = contactPerson;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getX() {
        return x;
    }

    public void setX(String x) {
        this.x = x;
    }

    public String getY() {
        return y;
    }

    public void setY(String y) {
        this.y = y;
    }

    public Double getServiceRadius() {
        return serviceRadius;
    }

    public void setServiceRadius(Double serviceRadius) {
        this.serviceRadius = serviceRadius;
    }

    public Boolean getIsVip() {
        return isVip;
    }

    public void setIsVip(Boolean isVip) {
        this.isVip = isVip;
    }

    // ========= 新增字段的 Getter/Setter =========

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBusinessHours() {
        return businessHours;
    }

    public void setBusinessHours(String businessHours) {
        this.businessHours = businessHours;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    /** 安全获取 x 坐标数值 */
    @Transient
    public Double getXDouble() {
        try {
            return x != null ? Double.parseDouble(x) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 安全获取 y 坐标数值 */
    @Transient
    public Double getYDouble() {
        try {
            return y != null ? Double.parseDouble(y) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
