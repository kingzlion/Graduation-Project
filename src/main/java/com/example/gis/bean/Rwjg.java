package com.example.gis.bean;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "RWJG")
public class Rwjg {
    @Id
    @Column(name = "id")
    private String id;

    @Column(name = "name")
    private String name;

    @Column(name = "jianjie")
    private String jianjie;

    @Column(name = "POINT_X")
    private String x;

    @Column(name = "POINT_Y")
    private String y;

    @Column(name = "src")
    private String src;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getJianjie() {
        return jianjie;
    }

    public String getX() {
        return x;
    }

    public String getY() {
        return y;
    }

    public String getSrc() {
        return src;
    }
}

