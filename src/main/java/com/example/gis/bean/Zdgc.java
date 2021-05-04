package com.example.gis.bean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "ZDGC")
public class Zdgc {

    @Id
    @Column(name = "id",length = 6)
    private String id;

    @Column(name = "name",length = 10)
    private String name;

    @Column(name = "x",length = 14)
    private String x;

    @Column(name = "y",length = 13)
    private String y;

    @Column(name = "jieshao",length = 200)
    private String jieshao;

    @Column(name = "zoom")
    private Integer zoom;

    @Column(name = "src")
    private String src;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String getJieshao() {
        return jieshao;
    }

    public Integer getZoom() {
        return zoom;
    }

    public String getSrc() {
        return src;
    }
}

