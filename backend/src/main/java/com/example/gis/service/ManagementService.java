package com.example.gis.service;

import com.example.gis.entity.Rwjg;
import com.example.gis.entity.SupplyMerchant;
import com.example.gis.entity.Zdgc;

import java.util.List;
import java.util.Optional;

public interface ManagementService {

    // ========= 工程管理 =========

    List<Zdgc> listZdgc();

    List<Rwjg> listRwjg();

    Optional<Zdgc> findZdgc(String id);

    Optional<Rwjg> findRwjg(String id);

    Zdgc saveZdgc(Zdgc zdgc);

    Rwjg saveRwjg(Rwjg rwjg);

    void deleteZdgc(String id);

    void deleteRwjg(String id);

    // ========= 商家管理 (FR-2.3) =========

    /** 商家列表（支持按审核状态筛选） */
    List<SupplyMerchant> listMerchants(String status);

    /** 全部商家 */
    List<SupplyMerchant> listAllMerchants();

    Optional<SupplyMerchant> findMerchant(String id);

    SupplyMerchant saveMerchant(SupplyMerchant merchant);

    void deleteMerchant(String id);

    /** 入驻审核 */
    SupplyMerchant auditMerchant(String id, boolean approved, String reason);

    /** 设置 VIP */
    SupplyMerchant setVip(String id, boolean isVip);
}
