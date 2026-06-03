package com.example.gis.repository;

import com.example.gis.entity.SupplyMerchant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SupplyMerchantRepository extends JpaRepository<SupplyMerchant, String> {

    /** VIP 商家优先，按名称排序 */
    List<SupplyMerchant> findAllByOrderByIsVipDescMerchantNameAsc();

    /** 按业态查询 */
    List<SupplyMerchant> findByBusinessType(String businessType);

    /** 模糊搜索（黄页搜索） */
    List<SupplyMerchant> findByMerchantNameContainingOrBusinessTypeContaining(String name, String type);

    /** 已认证商家列表 */
    List<SupplyMerchant> findByVerifiedTrue();

    /** 统计业态分类 */
    @Query("SELECT s.businessType, COUNT(s) FROM SupplyMerchant s GROUP BY s.businessType ORDER BY COUNT(s) DESC")
    List<Object[]> countByBusinessType();
}
