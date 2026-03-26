package com.bikeexchange.service.service;

import com.bikeexchange.model.OrderRuleConfig;
import com.bikeexchange.repository.OrderRuleConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderRuleConfigService {

    private static final double DEFAULT_COMMISSION_RATE = 0.02d;
    private static final long DEFAULT_SELLER_UPGRADE_FEE = 50000L;
    private static final int DEFAULT_RETURN_WINDOW_DAYS = 14;
    private static final long DEFAULT_BIKE_POST_FEE = 5000L;
    private static final long DEFAULT_INSPECTION_FEE = 200000L;

    @Autowired
    private OrderRuleConfigRepository repository;

    @Transactional(readOnly = true)
    public OrderRuleConfig getCurrentRules() {
        OrderRuleConfig config = repository.findById(OrderRuleConfig.SINGLETON_ID)
                .orElseGet(this::buildDefault);

        if (config.getCommissionRate() == null) {
            config.setCommissionRate(DEFAULT_COMMISSION_RATE);
        }
        if (config.getSellerUpgradeFee() == null) {
            config.setSellerUpgradeFee(DEFAULT_SELLER_UPGRADE_FEE);
        }
        if (config.getReturnWindowDays() == null || config.getReturnWindowDays() <= 0) {
            config.setReturnWindowDays(DEFAULT_RETURN_WINDOW_DAYS);
        }
        if (config.getBikePostFee() == null || config.getBikePostFee() <= 0) {
            config.setBikePostFee(DEFAULT_BIKE_POST_FEE);
        }
        if (config.getInspectionFee() == null || config.getInspectionFee() <= 0) {
            config.setInspectionFee(DEFAULT_INSPECTION_FEE);
        }
        return config;
    }

    @Transactional
    public OrderRuleConfig updateRules(Double commissionRate, Long sellerUpgradeFee, Integer returnWindowDays, Long bikePostFee, Long inspectionFee) {
        OrderRuleConfig config = repository.findById(OrderRuleConfig.SINGLETON_ID)
                .orElseGet(this::buildDefault);

        if (commissionRate != null) {
            validateCommissionRatePercent(commissionRate);
            config.setCommissionRate(percentToRatio(commissionRate));
        }

        if (sellerUpgradeFee != null) {
            validateSellerUpgradeFee(sellerUpgradeFee);
            config.setSellerUpgradeFee(sellerUpgradeFee);
        }

        if (returnWindowDays != null) {
            validateReturnWindowDays(returnWindowDays);
            config.setReturnWindowDays(returnWindowDays);
        }

        if (bikePostFee != null) {
            validateBikePostFee(bikePostFee);
            config.setBikePostFee(bikePostFee);
        }

        if (inspectionFee != null) {
            validateInspectionFee(inspectionFee);
            config.setInspectionFee(inspectionFee);
        }

        config.setId(OrderRuleConfig.SINGLETON_ID);
        return repository.save(config);
    }

    @Transactional(readOnly = true)
    public double getCommissionRate() {
        return getCurrentRules().getCommissionRate();
    }

    @Transactional(readOnly = true)
    public double getCommissionRatePercent() {
        return ratioToPercent(getCurrentRules().getCommissionRate());
    }

    @Transactional(readOnly = true)
    public long getSellerUpgradeFee() {
        return getCurrentRules().getSellerUpgradeFee();
    }

    @Transactional(readOnly = true)
    public int getReturnWindowDays() {
        return getCurrentRules().getReturnWindowDays();
    }

    @Transactional(readOnly = true)
    public long getBikePostFee() {
        return getCurrentRules().getBikePostFee();
    }

    @Transactional(readOnly = true)
    public long getInspectionFee() {
        return getCurrentRules().getInspectionFee();
    }

    private void validateCommissionRatePercent(Double value) {
        if (value < 0 || value > 100.0d) {
            throw new IllegalArgumentException("commissionRate must be between 0 and 100");
        }
    }

    public double ratioToPercent(Double ratioValue) {
        if (ratioValue == null) {
            return 0.0d;
        }
        return ratioValue * 100.0d;
    }

    public double percentToRatio(Double percentValue) {
        if (percentValue == null) {
            return DEFAULT_COMMISSION_RATE;
        }
        return percentValue / 100.0d;
    }

    private void validateReturnWindowDays(Integer value) {
        if (value < 1 || value > 60) {
            throw new IllegalArgumentException("returnWindowDays must be between 1 and 60");
        }
    }

    private void validateSellerUpgradeFee(Long value) {
        if (value < 0 || value > 10_000_000_000L) {
            throw new IllegalArgumentException("sellerUpgradeFee must be between 0 and 10000000000");
        }
    }

    private void validateBikePostFee(Long value) {
        if (value < 0 || value > 1_000_000_000L) {
            throw new IllegalArgumentException("bikePostFee must be between 0 and 1000000000");
        }
    }

    private void validateInspectionFee(Long value) {
        if (value < 0 || value > 10_000_000_000L) {
            throw new IllegalArgumentException("inspectionFee must be between 0 and 10000000000");
        }
    }

    private OrderRuleConfig buildDefault() {
        OrderRuleConfig config = new OrderRuleConfig();
        config.setId(OrderRuleConfig.SINGLETON_ID);
        config.setCommissionRate(DEFAULT_COMMISSION_RATE);
        config.setSellerUpgradeFee(DEFAULT_SELLER_UPGRADE_FEE);
        config.setReturnWindowDays(DEFAULT_RETURN_WINDOW_DAYS);
        config.setBikePostFee(DEFAULT_BIKE_POST_FEE);
        config.setInspectionFee(DEFAULT_INSPECTION_FEE);
        return config;
    }
}

