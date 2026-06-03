package com.example.gis.controller;

import com.example.gis.entity.Rwjg;
import com.example.gis.entity.Zdgc;
import com.example.gis.entity.SupplyMerchant;
import com.example.gis.entity.NostalgiaComment;
import com.example.gis.service.ApiService;
import com.example.gis.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ApiController {

    private final ApiService apiService;

    public ApiController(ApiService apiService) {
        this.apiService = apiService;
    }

    @PostMapping("/get")
    public Result<Zdgc> get(@RequestParam String id) {
        Zdgc zdgc = apiService.get(id);
        if (zdgc != null) {
            return Result.success(zdgc);
        }
        return Result.error("数据错误", 404);
    }

    @GetMapping("/get")
    public Result<Zdgc> getGet(@RequestParam String id) {
        return get(id);
    }

    @PostMapping("/zdgc/list")
    public Result<List<Zdgc>> listZdgcPost(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        if (status != null && !status.isEmpty()) {
            return Result.success(apiService.listByStatus(status));
        }
        if (category != null && !category.isEmpty()) {
            return Result.success(apiService.listByCategory(category));
        }
        return Result.success(apiService.list());
    }

    @GetMapping("/zdgc/list")
    public Result<List<Zdgc>> listZdgcGet(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category) {
        return listZdgcPost(status, category);
    }

    /** 工程全生命周期详情 (FR-2.1) */
    @GetMapping("/zdgc/detail")
    public Result<Zdgc> getZdgcDetail(@RequestParam String id) {
        Zdgc zdgc = apiService.get(id);
        if (zdgc != null) {
            return Result.success(zdgc);
        }
        return Result.error("工程不存在", 404);
    }

    @PostMapping("/rwjg/list")
    public Result<List<Rwjg>> apiRwjgPost() {
        return Result.success(apiService.listRwjg());
    }

    @GetMapping("/rwjg/list")
    public Result<List<Rwjg>> apiRwjgGet() {
        return Result.success(apiService.listRwjg());
    }

    // ==========================================
    // B端商机与供应商 API
    // ==========================================

    @GetMapping("/merchant/list")
    public Result<List<SupplyMerchant>> listMerchantsGet() {
        return Result.success(apiService.listMerchantsSorted());
    }

    @PostMapping("/merchant/list")
    public Result<List<SupplyMerchant>> listMerchantsPost() {
        return Result.success(apiService.listMerchantsSorted());
    }

    @PostMapping("/merchant/save")
    public Result<SupplyMerchant> saveMerchant(@RequestBody SupplyMerchant merchant) {
        try {
            SupplyMerchant saved = apiService.saveMerchant(merchant);
            return Result.success("保存成功", saved);
        } catch (Exception e) {
            return Result.error("保存商家失败: " + e.getMessage(), 500);
        }
    }

    /** 商家自主入驻 (FR-2.3) */
    @PostMapping("/merchant/register")
    public Result<SupplyMerchant> registerMerchant(@RequestBody SupplyMerchant merchant) {
        try {
            SupplyMerchant saved = apiService.registerMerchant(merchant);
            return Result.success("入驻申请已提交，等待审核", saved);
        } catch (Exception e) {
            return Result.error("入驻申请失败: " + e.getMessage(), 500);
        }
    }

    /** 周边检索（增强版：支持业态过滤+距离计算）(FR-2.2) */
    @GetMapping("/merchant/nearby")
    public Result<List<SupplyMerchant>> getNearbyMerchants(
            @RequestParam double longitude,
            @RequestParam double latitude,
            @RequestParam(defaultValue = "30.0") double distance,
            @RequestParam(required = false) String businessType) {
        try {
            List<SupplyMerchant> merchants = apiService.findNearbyMerchantsEnhanced(
                    longitude, latitude, distance, businessType);
            return Result.success(merchants);
        } catch (Exception e) {
            return Result.error("附近商家匹配失败: " + e.getMessage(), 500);
        }
    }

    /** 黄页首页 (FR-2.3) */
    @GetMapping("/merchant/directory")
    public Result<Map<String, Object>> getMerchantDirectory() {
        try {
            return Result.success(apiService.getMerchantDirectory());
        } catch (Exception e) {
            return Result.error("获取黄页失败: " + e.getMessage(), 500);
        }
    }

    /** 黄页搜索 (FR-2.3) */
    @GetMapping("/merchant/search")
    public Result<List<SupplyMerchant>> searchMerchants(
            @RequestParam(required = false) String keyword) {
        try {
            return Result.success(apiService.searchMerchants(keyword));
        } catch (Exception e) {
            return Result.error("搜索失败: " + e.getMessage(), 500);
        }
    }

    /** 商家详情（含周边工程匹配）(FR-2.3) */
    @GetMapping("/merchant/detail")
    public Result<Map<String, Object>> getMerchantDetail(@RequestParam String id) {
        try {
            Map<String, Object> detail = apiService.getMerchantDetail(id);
            if (detail.isEmpty()) {
                return Result.error("商家不存在", 404);
            }
            return Result.success(detail);
        } catch (Exception e) {
            return Result.error("获取商家详情失败: " + e.getMessage(), 500);
        }
    }

    // ==========================================
    // AI Agent 数据上报/同步 API
    // ==========================================

    @PostMapping("/agent/zdgc")
    public Result<Zdgc> agentReportZdgc(@RequestBody Zdgc zdgc) {
        try {
            Zdgc saved = apiService.saveZdgc(zdgc);
            return Result.success("项目数据同步成功", saved);
        } catch (Exception e) {
            return Result.error("同步失败: " + e.getMessage(), 500);
        }
    }

    @PostMapping("/agent/rwjg")
    public Result<Rwjg> agentReportRwjg(@RequestBody Rwjg rwjg) {
        try {
            Rwjg saved = apiService.saveRwjg(rwjg);
            return Result.success("人文景观数据同步成功", saved);
        } catch (Exception e) {
            return Result.error("同步失败: " + e.getMessage(), 500);
        }
    }

    // ==========================================
    // C端 情感留言与回忆板块 API
    // ==========================================

    @GetMapping("/comment/list")
    public Result<List<NostalgiaComment>> listComments(@RequestParam String villageId) {
        try {
            return Result.success(apiService.listComments(villageId));
        } catch (Exception e) {
            return Result.error("拉取留言失败: " + e.getMessage(), 500);
        }
    }

    @PostMapping("/comment/save")
    public Result<NostalgiaComment> saveComment(@RequestBody NostalgiaComment comment) {
        try {
            NostalgiaComment saved = apiService.saveComment(comment);
            return Result.success("发表回忆成功", saved);
        } catch (Exception e) {
            return Result.error("发表回忆失败: " + e.getMessage(), 500);
        }
    }
}
