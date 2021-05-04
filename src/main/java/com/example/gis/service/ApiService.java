package com.example.gis.service;

import com.example.gis.bean.Rwjg;
import com.example.gis.bean.Zdgc;
import com.example.gis.utils.Result;

import java.util.List;

public interface ApiService {
    Zdgc get(String id);

    List<Rwjg> listRwjg();
}
