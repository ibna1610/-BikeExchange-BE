package com.bikeexchange.service.service;

import com.bikeexchange.model.OrderRuleConfig;
import com.bikeexchange.repository.OrderRuleConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderRuleConfigService {

    private static final double DEFAULT_COMMISSION_RATE = 0.02d;
    private static final long DEFAULT_SELLER_UPGRADE_FEE = 50000L;
    private static final long DEFAULT_BIKE_POST_FEE = 5000L;
    private static final long DEFAULT_INSPECTION_FEE = 200000L;

    @Value("${app.order-rule.defaults.return-window.days:14}")
    private int defaultReturnWindowDays;

    @Value("${app.order-rule.defaults.return-window.hours:0}")
    private int defaultReturnWindowHours;

    @Value("${app.order-rule.defaults.return-window.minutes:0}")
    private int defaultReturnWindowMinutes;

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
        // Chỉ set default nếu null, cho phép 0 ngày
        if (config.getReturnWindowDays() == null) {
            config.setReturnWindowDays(defaultReturnWindowDays);
        }
        if (config.getReturnWindowHours() == null || config.getReturnWindowHours() < 0) {
            config.setReturnWindowHours(defaultReturnWindowHours);
        }
        if (config.getReturnWindowMinutes() == null || config.getReturnWindowMinutes() < 0) {
            config.setReturnWindowMinutes(defaultReturnWindowMinutes);
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
    public OrderRuleConfig updateRules(Double commissionRate,
                                       Long sellerUpgradeFee,
                                       Integer returnWindowDays,
                                       Integer returnWindowHours,
                                       Integer returnWindowMinutes,
                                       Long bikePostFee,
                                       Long inspectionFee) {
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

        if (returnWindowHours != null) {
            validateReturnWindowHours(returnWindowHours);
            config.setReturnWindowHours(returnWindowHours);
        }

        if (returnWindowMinutes != null) {
            validateReturnWindowMinutes(returnWindowMinutes);
            config.setReturnWindowMinutes(returnWindowMinutes);
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
    public int getReturnWindowHours() {
        return getCurrentRules().getReturnWindowHours();
    }

    @Transactional(readOnly = true)
    public int getReturnWindowMinutes() {
        return getCurrentRules().getReturnWindowMinutes();
    }

    @Transactional(readOnly = true)
    public long getReturnWindowTotalMinutes() {
        OrderRuleConfig config = getCurrentRules();
        return (long) config.getReturnWindowDays() * 24L * 60L
                + (long) config.getReturnWindowHours() * 60L
                + config.getReturnWindowMinutes();
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
        if (value < 0 || value > 60) {
            throw new IllegalArgumentException("returnWindowDays must be between 0 and 60");
        }
    }

    private void validateReturnWindowHours(Integer value) {
        if (value < 0 || value > 23) {
            throw new IllegalArgumentException("returnWindowHours must be between 0 and 23");
        }
    }

    private void validateReturnWindowMinutes(Integer value) {
        if (value < 0 || value > 59) {
            throw new IllegalArgumentException("returnWindowMinutes must be between 0 and 59");
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
        config.setReturnWindowDays(defaultReturnWindowDays);
        config.setReturnWindowHours(defaultReturnWindowHours);
        config.setReturnWindowMinutes(defaultReturnWindowMinutes);
        config.setBikePostFee(DEFAULT_BIKE_POST_FEE);
        config.setInspectionFee(DEFAULT_INSPECTION_FEE);
        return config;
    }
}

