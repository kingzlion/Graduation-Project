package com.example.gis.service.impl;

import com.example.gis.entity.Rwjg;
import com.example.gis.entity.Zdgc;
import com.example.gis.entity.SupplyMerchant;
import com.example.gis.entity.NostalgiaComment;
import com.example.gis.repository.RwjgRepository;
import com.example.gis.repository.ZdgcRepository;
import com.example.gis.repository.SupplyMerchantRepository;
import com.example.gis.repository.NostalgiaCommentRepository;
import com.example.gis.service.ApiService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Service
public class ApiServiceImpl implements ApiService {

    private final ZdgcRepository zdgcRepository;
    private final RwjgRepository rwjgRepository;
    private final SupplyMerchantRepository supplyMerchantRepository;
    private final NostalgiaCommentRepository nostalgiaCommentRepository;

    public ApiServiceImpl(ZdgcRepository zdgcRepository,
                          RwjgRepository rwjgRepository,
                          SupplyMerchantRepository supplyMerchantRepository,
                          NostalgiaCommentRepository nostalgiaCommentRepository) {
        this.zdgcRepository = zdgcRepository;
        this.rwjgRepository = rwjgRepository;
        this.supplyMerchantRepository = supplyMerchantRepository;
        this.nostalgiaCommentRepository = nostalgiaCommentRepository;
    }

    @Override
    public Zdgc get(String id) {
        return zdgcRepository.findById(id).orElse(null);
    }

    @Override
    public List<Zdgc> list() {
        return zdgcRepository.findAll();
    }

    @Override
    public List<Zdgc> listByStatus(String status) {
        if (status == null || status.isEmpty()) {
            return zdgcRepository.findAll();
        }
        return zdgcRepository.findByStatus(status);
    }

    @Override
    public List<Zdgc> listByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return zdgcRepository.findAll();
        }
        return zdgcRepository.findByCategory(category);
    }

    @Override
    public List<Rwjg> listRwjg() {
        return rwjgRepository.findAll();
    }

    @Override
    public Zdgc saveZdgc(Zdgc zdgc) {
        if (zdgc.getId() == null || zdgc.getId().isEmpty()) {
            zdgc.setId(UUID.randomUUID().toString().substring(0, 6));
        }
        zdgc.setUpdatedAt(LocalDateTime.now());
        return zdgcRepository.save(zdgc);
    }

    @Override
    public Rwjg saveRwjg(Rwjg rwjg) {
        if (rwjg.getId() == null || rwjg.getId().isEmpty()) {
            rwjg.setId(UUID.randomUUID().toString().substring(0, 6));
        }
        return rwjgRepository.save(rwjg);
    }

    @Override
    public List<SupplyMerchant> listMerchants() {
        return supplyMerchantRepository.findAll();
    }

    @Override
    public List<SupplyMerchant> listMerchantsSorted() {
        return supplyMerchantRepository.findAllByOrderByIsVipDescMerchantNameAsc();
    }

    @Override
    public SupplyMerchant saveMerchant(SupplyMerchant merchant) {
        if (merchant.getId() == null || merchant.getId().isEmpty()) {
            merchant.setId(UUID.randomUUID().toString());
        }
        merchant.setUpdatedAt(LocalDateTime.now());
        return supplyMerchantRepository.save(merchant);
    }

    @Override
    public SupplyMerchant registerMerchant(SupplyMerchant merchant) {
        merchant.setId(UUID.randomUUID().toString());
        merchant.setVerified(false);      // 新入驻默认未审核
        merchant.setIsVip(false);         // 新入驻默认非 VIP
        merchant.setRating(0.0);
        merchant.setCreatedAt(LocalDateTime.now());
        merchant.setUpdatedAt(LocalDateTime.now());
        return supplyMerchantRepository.save(merchant);
    }

    @Override
    public List<SupplyMerchant> findNearbyMerchants(double longitude, double latitude, double maxDistanceKm) {
        return findNearbyMerchantsEnhanced(longitude, latitude, maxDistanceKm, null);
    }

    @Override
    public List<SupplyMerchant> findNearbyMerchantsEnhanced(double longitude, double latitude,
                                                             double maxDistanceKm, String businessType) {
        List<SupplyMerchant> all;
        if (businessType != null && !businessType.isEmpty()) {
            all = supplyMerchantRepository.findByBusinessType(businessType);
        } else {
            all = supplyMerchantRepository.findAll();
        }

        List<SupplyMerchant> nearby = new ArrayList<>();
        for (SupplyMerchant merchant : all) {
            if (merchant.getX() != null && merchant.getY() != null) {
                try {
                    double mLon = Double.parseDouble(merchant.getX());
                    double mLat = Double.parseDouble(merchant.getY());
                    double distance = calculateHaversineDistance(latitude, longitude, mLat, mLon);
                    if (distance <= maxDistanceKm) {
                        merchant.setDistance(Math.round(distance * 10.0) / 10.0); // 保留一位小数
                        nearby.add(merchant);
                    }
                } catch (NumberFormatException e) {
                    // Ignore malformed coordinates
                }
            }
        }

        // 按距离排序
        nearby.sort(Comparator.comparingDouble(SupplyMerchant::getDistance));
        return nearby;
    }

    @Override
    public Map<String, Object> getMerchantDirectory() {
        Map<String, Object> result = new LinkedHashMap<>();

        // VIP 商家置顶
        List<SupplyMerchant> vipMerchants = supplyMerchantRepository.findAll()
                .stream()
                .filter(m -> Boolean.TRUE.equals(m.getIsVip()))
                .collect(Collectors.toList());
        result.put("vipMerchants", vipMerchants);

        // 按业态分类聚合
        List<Object[]> categoryCounts = supplyMerchantRepository.countByBusinessType();
        List<Map<String, Object>> categories = new ArrayList<>();
        for (Object[] row : categoryCounts) {
            Map<String, Object> cat = new LinkedHashMap<>();
            String type = (String) row[0];
            Long count = (Long) row[1];
            cat.put("type", type);
            cat.put("count", count);
            // 每个分类取前 4 个推荐商家
            List<SupplyMerchant> topMerchants = supplyMerchantRepository.findByBusinessType(type)
                    .stream()
                    .sorted((a, b) -> {
                        boolean aVip = Boolean.TRUE.equals(a.getIsVip());
                        boolean bVip = Boolean.TRUE.equals(b.getIsVip());
                        return bVip == aVip ? 0 : (bVip ? 1 : -1);
                    })
                    .limit(4)
                    .collect(Collectors.toList());
            cat.put("merchants", topMerchants);
            categories.add(cat);
        }
        result.put("categories", categories);

        return result;
    }

    @Override
    public List<SupplyMerchant> searchMerchants(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return supplyMerchantRepository.findAllByOrderByIsVipDescMerchantNameAsc();
        }
        return supplyMerchantRepository
                .findByMerchantNameContainingOrBusinessTypeContaining(keyword, keyword);
    }

    @Override
    public Map<String, Object> getMerchantDetail(String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        SupplyMerchant merchant = supplyMerchantRepository.findById(id).orElse(null);
        if (merchant == null) {
            return result;
        }
        result.put("merchant", merchant);

        // 周边工程匹配（使用商家坐标）
        List<Map<String, Object>> nearbyProjects = new ArrayList<>();
        if (merchant.getX() != null && merchant.getY() != null) {
            try {
                double mLon = Double.parseDouble(merchant.getX());
                double mLat = Double.parseDouble(merchant.getY());
                double radius = merchant.getServiceRadius() != null ? merchant.getServiceRadius() : 30.0;

                List<Zdgc> allProjects = zdgcRepository.findAll();
                for (Zdgc project : allProjects) {
                    if (project.getX() != null && project.getY() != null) {
                        double pLon = Double.parseDouble(project.getX());
                        double pLat = Double.parseDouble(project.getY());
                        double distance = calculateHaversineDistance(mLat, mLon, pLat, pLon);
                        if (distance <= radius) {
                            Map<String, Object> projectInfo = new LinkedHashMap<>();
                            projectInfo.put("id", project.getId());
                            projectInfo.put("name", project.getName());
                            projectInfo.put("status", project.getStatus());
                            projectInfo.put("contractor", project.getContractor());
                            projectInfo.put("distance", Math.round(distance * 10.0) / 10.0);
                            nearbyProjects.add(projectInfo);
                        }
                    }
                }
                nearbyProjects.sort(Comparator.comparingDouble(p -> (Double) ((Map) p).get("distance")));
            } catch (NumberFormatException e) {
                // Ignore
            }
        }
        result.put("nearbyProjects", nearbyProjects);

        return result;
    }

    private double calculateHaversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.0; // Earth radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    @Override
    public List<NostalgiaComment> listComments(String villageId) {
        return nostalgiaCommentRepository.findByVillageIdOrderByCreatedAtDesc(villageId);
    }

    @Override
    public NostalgiaComment saveComment(NostalgiaComment comment) {
        if (comment.getId() == null || comment.getId().isEmpty()) {
            comment.setId(UUID.randomUUID().toString());
        }
        if (comment.getCreatedAt() == null || comment.getCreatedAt().isEmpty()) {
            comment.setCreatedAt(LocalDateTime.now().toString());
        }
        return nostalgiaCommentRepository.save(comment);
    }
}
