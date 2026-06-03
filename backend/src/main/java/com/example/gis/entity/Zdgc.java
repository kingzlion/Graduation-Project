package com.example.gis.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ZDGC")
public class Zdgc {

    @Id
    @Column(name = "id",length = 6)
    private String id;

    @Column(name = "name",length = 255)
    private String name;

    @Column(name = "x",length = 32)
    private String x;

    @Column(name = "y",length = 32)
    private String y;

    @Lob
    @Column(name = "jieshao")
    private String jieshao;

    @Column(name = "zoom")
    private Integer zoom;

    @Column(name = "src")
    private String src;

    // ========= 全生命周期字段 (FR-2.1) =========

    /** 工程状态：planning(红) / under_construction(黄) / completed(绿) */
    @Column(name = "status", length = 20)
    private String status;

    /** 总包单位 */
    @Column(name = "contractor", length = 255)
    private String contractor;

    /** 中标金额（万元） */
    @Column(name = "bid_amount", precision = 15, scale = 2)
    private BigDecimal bidAmount;

    /** 当前工况 */
    @Lob
    @Column(name = "work_condition")
    private String workCondition;

    /** 开工日期 */
    @Column(name = "start_date")
    private LocalDate startDate;

    /** 竣工日期 */
    @Column(name = "end_date")
    private LocalDate endDate;

    /** 工程分类（城市绿化/市政配套/交通物流/安置房） */
    @Column(name = "category", length = 50)
    private String category;

    /** 更新时间 */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========= 原有的 Getter/Setter =========

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getJieshao() {
        return jieshao;
    }

    public void setJieshao(String jieshao) {
        this.jieshao = jieshao;
    }

    public Integer getZoom() {
        return zoom;
    }

    public void setZoom(Integer zoom) {
        this.zoom = zoom;
    }

    public String getSrc() {
        return src;
    }

    public void setSrc(String src) {
        this.src = src;
    }

    // ========= 新增字段的 Getter/Setter =========

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getContractor() {
        return contractor;
    }

    public void setContractor(String contractor) {
        this.contractor = contractor;
    }

    public BigDecimal getBidAmount() {
        return bidAmount;
    }

    public void setBidAmount(BigDecimal bidAmount) {
        this.bidAmount = bidAmount;
    }

    public String getWorkCondition() {
        return workCondition;
    }

    public void setWorkCondition(String workCondition) {
        this.workCondition = workCondition;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
