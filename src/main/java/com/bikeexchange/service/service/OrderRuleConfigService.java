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
        if (config.getReturnWindowDays() == null) {
            config.setReturnWindowDays(DEFAULT_RETURN_WINDOW_DAYS);
        }
        return config;
    }

    @Transactional
    public OrderRuleConfig updateRules(Double commissionRate, Long sellerUpgradeFee, Integer returnWindowDays) {
        OrderRuleConfig config = repository.findById(OrderRuleConfig.SINGLETON_ID)
                .orElseGet(this::buildDefault);

        if (commissionRate != null) {
            validateCommissionRate(commissionRate);
        }

        if (sellerUpgradeFee != null) {
            validateSellerUpgradeFee(sellerUpgradeFee);
        }

        if (commissionRate != null) {
            config.setCommissionRate(commissionRate);
        }

        if (sellerUpgradeFee != null) {
            config.setSellerUpgradeFee(sellerUpgradeFee);
        }

        if (returnWindowDays != null) {
            validateReturnWindowDays(returnWindowDays);
            config.setReturnWindowDays(returnWindowDays);
        }

        config.setId(OrderRuleConfig.SINGLETON_ID);
        return repository.save(config);
    }

    @Transactional(readOnly = true)
    public double getCommissionRate() {
        return getCurrentRules().getCommissionRate();
    }

    @Transactional(readOnly = true)
    public long getSellerUpgradeFee() {
        return getCurrentRules().getSellerUpgradeFee();
    }

    @Transactional(readOnly = true)
    public int getReturnWindowDays() {
        return getCurrentRules().getReturnWindowDays();
    }

    private void validateCommissionRate(Double value) {
        if (value < 0 || value > 1.0d) {
            throw new IllegalArgumentException("commissionRate must be between 0 and 1.0");
        }
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

    private OrderRuleConfig buildDefault() {
        OrderRuleConfig config = new OrderRuleConfig();
        config.setId(OrderRuleConfig.SINGLETON_ID);
        config.setCommissionRate(DEFAULT_COMMISSION_RATE);
        config.setSellerUpgradeFee(DEFAULT_SELLER_UPGRADE_FEE);
        config.setReturnWindowDays(DEFAULT_RETURN_WINDOW_DAYS);
        return config;
    }
}