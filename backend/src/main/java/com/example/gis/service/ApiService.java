package com.example.gis.service;

import com.example.gis.entity.Rwjg;
import com.example.gis.entity.Zdgc;
import com.example.gis.entity.SupplyMerchant;
import com.example.gis.entity.NostalgiaComment;

import java.util.List;
import java.util.Map;

public interface ApiService {
    Zdgc get(String id);

    List<Zdgc> list();

    /** 按状态筛选工程列表 */
    List<Zdgc> listByStatus(String status);

    /** 按分类筛选工程列表 */
    List<Zdgc> listByCategory(String category);

    List<Rwjg> listRwjg();

    Zdgc saveZdgc(Zdgc zdgc);

    Rwjg saveRwjg(Rwjg rwjg);

    // ========= 商家 API =========

    List<SupplyMerchant> listMerchants();

    /** VIP 优先排序的商家列表 */
    List<SupplyMerchant> listMerchantsSorted();

    SupplyMerchant saveMerchant(SupplyMerchant merchant);

    /** 商家自主入驻注册 */
    SupplyMerchant registerMerchant(SupplyMerchant merchant);

    /** 周边检索（基本版） */
    List<SupplyMerchant> findNearbyMerchants(double longitude, double latitude, double maxDistanceKm);

    /** 周边检索（增强版：业态过滤+距离计算） */
    List<SupplyMerchant> findNearbyMerchantsEnhanced(double longitude, double latitude,
                                                      double maxDistanceKm, String businessType);

    /** 黄页首页（分类聚合 + VIP 推荐） */
    Map<String, Object> getMerchantDirectory();

    /** 黄页搜索 */
    List<SupplyMerchant> searchMerchants(String keyword);

    /** 商家详情（含周边工程匹配） */
    Map<String, Object> getMerchantDetail(String id);

    // ========= 留言 API =========

    List<NostalgiaComment> listComments(String villageId);

    NostalgiaComment saveComment(NostalgiaComment comment);
}
