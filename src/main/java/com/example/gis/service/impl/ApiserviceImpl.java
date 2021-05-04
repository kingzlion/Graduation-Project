package com.example.gis.service.impl;

import com.example.gis.bean.Rwjg;
import com.example.gis.bean.Zdgc;
import com.example.gis.repository.ApiRepository;
import com.example.gis.repository.RwjgRepository;
import com.example.gis.service.ApiService;
import com.example.gis.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ApiserviceImpl implements ApiService {

    @Autowired
    private ApiRepository apiRepository;

    @Autowired
    private RwjgRepository rwjgRepository;

    /**
     * 通过id获取坐标信息
     * @param id
     * @return
     */
    @Override
    public Zdgc get(String id) {
        Optional<Zdgc> optional = apiRepository.findById(id);
        if(optional!=null && optional.isPresent()){
           return optional.get();
        }
        return null;
    }

    @Override
    public List<Rwjg> listRwjg() {
        return rwjgRepository.findAll();
    }
}
