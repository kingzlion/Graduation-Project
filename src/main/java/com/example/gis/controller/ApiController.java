package com.example.gis.controller;

import com.example.gis.bean.Rwjg;
import com.example.gis.bean.Zdgc;
import com.example.gis.service.ApiService;
import com.example.gis.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ApiController {

    @Autowired
    private ApiService apiService;

    /**
     * 通过id查询点的坐标
     * @param id
     * @return
     */
    @CrossOrigin(origins = "*",maxAge = 3600)
    @PostMapping("/get")
    public Result get(String id){
        Zdgc zdgc = apiService.get(id);
        if(zdgc!=null){
            return Result.success(2000,zdgc);
        }
        return Result.custom("数据错误",2001);
    }

    /**
     * 获取所有点的坐标
     * @return
     */
    @CrossOrigin(origins = "*",maxAge = 3600)
    @PostMapping("/rwjg/list")
    public Result apiRwjg(){
        List<Rwjg> list = apiService.listRwjg();
        return Result.success(200,list);
    }
}
