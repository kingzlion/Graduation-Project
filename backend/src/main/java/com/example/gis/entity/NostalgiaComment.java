package com.example.gis.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "NOSTALGIA_COMMENT")
public class NostalgiaComment {

    @Id
    @Column(name = "id", length = 64)
    private String id;

    @Column(name = "village_id", length = 64, nullable = false)
    private String villageId;

    @Column(name = "author", length = 128)
    private String author;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "created_at", length = 64)
    private String createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getVillageId() {
        return villageId;
    }

    public void setVillageId(String villageId) {
        this.villageId = villageId;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
