package com.example.gis.service.impl;

import com.example.gis.entity.Rwjg;
import com.example.gis.entity.SupplyMerchant;
import com.example.gis.entity.Zdgc;
import com.example.gis.repository.RwjgRepository;
import com.example.gis.repository.SupplyMerchantRepository;
import com.example.gis.repository.ZdgcRepository;
import com.example.gis.service.ManagementService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ManagementServiceImpl implements ManagementService {

    private final ZdgcRepository zdgcRepository;
    private final RwjgRepository rwjgRepository;
    private final SupplyMerchantRepository supplyMerchantRepository;

    public ManagementServiceImpl(ZdgcRepository zdgcRepository,
                                 RwjgRepository rwjgRepository,
                                 SupplyMerchantRepository supplyMerchantRepository) {
        this.zdgcRepository = zdgcRepository;
        this.rwjgRepository = rwjgRepository;
        this.supplyMerchantRepository = supplyMerchantRepository;
    }

    // ========= 工程管理 =========

    @Override
    public List<Zdgc> listZdgc() {
        return zdgcRepository.findAll();
    }

    @Override
    public List<Rwjg> listRwjg() {
        return rwjgRepository.findAll();
    }

    @Override
    public Optional<Zdgc> findZdgc(String id) {
        return zdgcRepository.findById(id);
    }

    @Override
    public Optional<Rwjg> findRwjg(String id) {
        return rwjgRepository.findById(id);
    }

    @Override
    public Zdgc saveZdgc(Zdgc zdgc) {
        if (zdgc.getId() == null || zdgc.getId().isEmpty()) {
            zdgc.setId(java.util.UUID.randomUUID().toString().substring(0, 6));
        }
        zdgc.setUpdatedAt(LocalDateTime.now());
        return zdgcRepository.save(zdgc);
    }

    @Override
    public Rwjg saveRwjg(Rwjg rwjg) {
        return rwjgRepository.save(rwjg);
    }

    @Override
    public void deleteZdgc(String id) {
        zdgcRepository.deleteById(id);
    }

    @Override
    public void deleteRwjg(String id) {
        rwjgRepository.deleteById(id);
    }

    // ========= 商家管理 (FR-2.3) =========

    @Override
    public List<SupplyMerchant> listMerchants(String status) {
        List<SupplyMerchant> all = supplyMerchantRepository.findAllByOrderByIsVipDescMerchantNameAsc();
        if (status != null && !status.isEmpty()) {
            if ("pending".equals(status)) {
                // 待审核：verified 为 false
                return all.stream()
                        .filter(m -> !Boolean.TRUE.equals(m.getVerified()))
                        .collect(Collectors.toList());
            } else if ("approved".equals(status)) {
                return all.stream()
                        .filter(m -> Boolean.TRUE.equals(m.getVerified()))
                        .collect(Collectors.toList());
            }
        }
        return all;
    }

    @Override
    public List<SupplyMerchant> listAllMerchants() {
        return supplyMerchantRepository.findAllByOrderByIsVipDescMerchantNameAsc();
    }

    @Override
    public Optional<SupplyMerchant> findMerchant(String id) {
        return supplyMerchantRepository.findById(id);
    }

    @Override
    public SupplyMerchant saveMerchant(SupplyMerchant merchant) {
        if (merchant.getId() == null || merchant.getId().isEmpty()) {
            merchant.setId(java.util.UUID.randomUUID().toString());
            if (merchant.getCreatedAt() == null) {
                merchant.setCreatedAt(LocalDateTime.now());
            }
        }
        merchant.setUpdatedAt(LocalDateTime.now());
        return supplyMerchantRepository.save(merchant);
    }

    @Override
    public void deleteMerchant(String id) {
        supplyMerchantRepository.deleteById(id);
    }

    @Override
    public SupplyMerchant auditMerchant(String id, boolean approved, String reason) {
        SupplyMerchant merchant = supplyMerchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商家不存在: " + id));
        merchant.setVerified(approved);
        merchant.setUpdatedAt(LocalDateTime.now());
        return supplyMerchantRepository.save(merchant);
    }

    @Override
    public SupplyMerchant setVip(String id, boolean isVip) {
        SupplyMerchant merchant = supplyMerchantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("商家不存在: " + id));
        merchant.setIsVip(isVip);
        merchant.setUpdatedAt(LocalDateTime.now());
        return supplyMerchantRepository.save(merchant);
    }
}
