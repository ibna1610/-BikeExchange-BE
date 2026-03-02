package com.bikeexchange.service;

import com.bikeexchange.exception.InsufficientBalanceException;
import com.bikeexchange.model.User;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.model.PointTransaction;
import com.bikeexchange.model.PointTransaction.TransactionType;
import com.bikeexchange.repository.PointTransactionRepository;
import com.bikeexchange.repository.UserWalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class WalletServiceTest {

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private PointTransactionRepository pointTxRepo;

    @InjectMocks
    private WalletService walletService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void requestWithdraw_insufficientBalance_throws() {
        UserWallet wallet = new UserWallet();
        wallet.setAvailablePoints(100L);
        when(walletRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class, () -> walletService.requestWithdraw(1L, 200L, "Bank", "Name", "123"));
    }
}
