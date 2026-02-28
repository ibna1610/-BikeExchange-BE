package com.bikeexchange.service;

import com.bikeexchange.config.VNPAYConfig;
import com.bikeexchange.model.PointTransaction;
import com.bikeexchange.repository.PointTransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class VnPayService {
    @Autowired
    private VNPAYConfig config;
    @Autowired
    private WalletService walletService;
    @Autowired
    private PointTransactionRepository pointTxRepo;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    public Map<String, String> createPaymentParams(Long amountVnd, Long userId, String ipAddr) {
        String txnRef = generateTxnRef(userId);
        Map<String, String> vnpParams = new LinkedHashMap<>();
        vnpParams.put("vnp_Version", VNPAYConfig.VERSION);
        vnpParams.put("vnp_Command", VNPAYConfig.COMMAND);
        vnpParams.put("vnp_TmnCode", config.getTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amountVnd * 100));
        vnpParams.put("vnp_CurrCode", VNPAYConfig.CURR_CODE);
        vnpParams.put("vnp_TxnRef", txnRef);
        vnpParams.put("vnp_OrderInfo", "WALLET_DEPOSIT_" + userId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", config.getReturnUrl());
        vnpParams.put("vnp_IpAddr", ipAddr);
        LocalDateTime now = LocalDateTime.now();
        vnpParams.put("vnp_CreateDate", now.format(FMT));
        vnpParams.put("vnp_ExpireDate", now.plusMinutes(15).format(FMT));
        return vnpParams;
    }

    public String generatePaymentUrl(Long amountVnd, Long userId, String ipAddr) {
        Map<String, String> vnpParams = createPaymentParams(amountVnd, userId, ipAddr);
        String query = config.buildSignedQuery(vnpParams);
        return config.getPayUrl() + "?" + query;
    }

    public boolean verifySignature(Map<String, String> queryParams) {
        Map<String, String> toHash = new HashMap<>();
        for (Map.Entry<String, String> e : queryParams.entrySet()) {
            String k = e.getKey();
            if (!"vnp_SecureHash".equalsIgnoreCase(k) && !"vnp_SecureHashType".equalsIgnoreCase(k)) {
                toHash.put(k, e.getValue());
            }
        }
        List<String> fieldNames = new ArrayList<>(toHash.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (int i = 0; i < fieldNames.size(); i++) {
            String field = fieldNames.get(i);
            String value = toHash.get(field);
            String encodedName = java.net.URLEncoder.encode(field, java.nio.charset.StandardCharsets.UTF_8);
            String encodedValue = java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
            hashData.append(encodedName).append("=").append(encodedValue);
            if (i < fieldNames.size() - 1) hashData.append("&");
        }
        String expected = config.hmacSHA512(config.getHashSecret(), hashData.toString());
        String provided = queryParams.getOrDefault("vnp_SecureHash", "");
        return expected.equalsIgnoreCase(provided);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean depositIfNotProcessed(Long userId, Long amountVnd, String referenceId) {
        Optional<PointTransaction> existed = pointTxRepo.findByReferenceId(referenceId);
        if (existed.isPresent() && existed.get().getStatus() == PointTransaction.TransactionStatus.SUCCESS) {
            return false;
        }
        Long points = amountVnd / 1000;
        walletService.depositPoints(userId, points, referenceId);
        return true;
    }

    private String generateTxnRef(Long userId) {
        String ts = LocalDateTime.now().format(FMT);
        String rnd = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        return "WALLET-" + userId + "-" + ts + "-" + rnd;
    }
}
