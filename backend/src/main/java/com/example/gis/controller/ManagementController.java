package com.example.gis.controller;

import com.example.gis.entity.Rwjg;
import com.example.gis.entity.SupplyMerchant;
import com.example.gis.entity.Zdgc;
import com.example.gis.service.ManagementService;
import com.example.gis.utils.Result;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/manage")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ManagementController {

    private final ManagementService managementService;

    public ManagementController(ManagementService managementService) {
        this.managementService = managementService;
    }

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("zdgc", managementService.listZdgc());
        summary.put("rwjg", managementService.listRwjg());
        return Result.success(summary);
    }

    @PostMapping("/zdgc")
    public Result<Zdgc> saveZdgc(@RequestBody Zdgc zdgc) {
        return Result.success("保存成功", managementService.saveZdgc(zdgc));
    }

    @PostMapping("/rwjg")
    public Result<Rwjg> saveRwjg(@RequestBody Rwjg rwjg) {
        return Result.success("保存成功", managementService.saveRwjg(rwjg));
    }

    @DeleteMapping("/zdgc/{id}")
    public Result<String> deleteZdgc(@PathVariable String id) {
        managementService.deleteZdgc(id);
        return Result.success("删除成功");
    }

    @DeleteMapping("/rwjg/{id}")
    public Result<String> deleteRwjg(@PathVariable String id) {
        managementService.deleteRwjg(id);
        return Result.success("删除成功");
    }

    // ========= 商家管理 (FR-2.3) =========

    /** 商家列表（可按审核状态筛选） */
    @GetMapping("/merchant/list")
    public Result<?> listMerchants(@RequestParam(required = false) String status) {
        return Result.success(managementService.listMerchants(status));
    }

    /** 新增/编辑商家 */
    @PostMapping("/merchant/save")
    public Result<SupplyMerchant> saveMerchant(@RequestBody SupplyMerchant merchant) {
        try {
            return Result.success("保存成功", managementService.saveMerchant(merchant));
        } catch (Exception e) {
            return Result.error("保存商家失败: " + e.getMessage(), 500);
        }
    }

    /** 删除商家 */
    @DeleteMapping("/merchant/{id}")
    public Result<String> deleteMerchant(@PathVariable String id) {
        try {
            managementService.deleteMerchant(id);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error("删除失败: " + e.getMessage(), 500);
        }
    }

    /** 入驻审核 */
    @PostMapping("/merchant/audit")
    public Result<SupplyMerchant> auditMerchant(@RequestParam String id,
                                                 @RequestParam boolean approved,
                                                 @RequestParam(required = false) String reason) {
        try {
            SupplyMerchant merchant = managementService.auditMerchant(id, approved, reason);
            String msg = approved ? "入驻审核通过" : "入驻审核拒绝";
            return Result.success(msg, merchant);
        } catch (Exception e) {
            return Result.error("审核操作失败: " + e.getMessage(), 500);
        }
    }

    /** 设置/取消 VIP */
    @PostMapping("/merchant/set-vip")
    public Result<SupplyMerchant> setVip(@RequestParam String id,
                                          @RequestParam boolean isVip) {
        try {
            SupplyMerchant merchant = managementService.setVip(id, isVip);
            String msg = isVip ? "已设为 VIP 商家" : "已取消 VIP";
            return Result.success(msg, merchant);
        } catch (Exception e) {
            return Result.error("设置 VIP 失败: " + e.getMessage(), 500);
        }
    }
}
