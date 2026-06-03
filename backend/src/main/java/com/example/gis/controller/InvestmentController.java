package com.example.gis.controller;

import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/investment")
@CrossOrigin(origins = "*", maxAge = 3600)
public class InvestmentController {

    @GetMapping("/parcels")
    public Map<String, Object> getParcels() {
        Map<String, Object> geojson = new LinkedHashMap<>();
        geojson.put("type", "FeatureCollection");

        List<Map<String, Object>> features = new ArrayList<>();

        features.add(buildFeature("parcel_001", "容东商业综合体核心地块 A", "商业用地", 58000, 3.5,
                List.of(List.of(115.918,39.050), List.of(115.928,39.050), List.of(115.928,39.056), List.of(115.918,39.056), List.of(115.918,39.050))));
        features.add(buildFeature("parcel_002", "容东滨水科技研发总部用地", "科研用地", 32000, 2.0,
                List.of(List.of(115.924,39.044), List.of(115.934,39.044), List.of(115.934,39.050), List.of(115.924,39.050), List.of(115.924,39.044))));
        features.add(buildFeature("parcel_003", "昝岗站前综合商务区地块", "商务用地", 45000, 4.0,
                List.of(List.of(116.158,39.046), List.of(116.170,39.046), List.of(116.170,39.054), List.of(116.158,39.054), List.of(116.158,39.046))));

        geojson.put("features", features);
        return geojson;
    }

    @PostMapping("/intent")
    public Map<String, Object> submitIntent(@RequestBody Map<String, Object> body) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("code", 200);
        result.put("message", "投资意向提交成功，招商部门将尽快联系您！");
        return result;
    }

    private Map<String, Object> buildFeature(String id, String name, String landUse, int area, double far, List<List<Double>> coords) {
        Map<String, Object> feature = new LinkedHashMap<>();
        feature.put("type", "Feature");

        Map<String, Object> geometry = new LinkedHashMap<>();
        geometry.put("type", "Polygon");
        geometry.put("coordinates", List.of(coords));
        feature.put("geometry", geometry);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("id", id);
        properties.put("parcel_name", name);
        properties.put("land_use", landUse);
        properties.put("area_sqm", area);
        properties.put("far", far);
        properties.put("status", 0);
        feature.put("properties", properties);

        return feature;
    }
}
