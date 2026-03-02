package com.bikeexchange.service;

import com.bikeexchange.exception.InsufficientBalanceException;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.UserWallet;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.OrderRepository;
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

public class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserWalletRepository walletRepository;

    @Mock
    private BikeRepository bikeRepository;

    @Mock
    private PointTransactionRepository pointTxRepo;

    @InjectMocks
    private OrderService orderService;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void createOrder_insufficientBalance_throws() {
        Bike bike = new Bike();
        bike.setId(1L);
        bike.setPricePoints(1000L);
        bike.setStatus(com.bikeexchange.model.Bike.BikeStatus.ACTIVE);

        when(bikeRepository.findByIdForUpdate(anyLong())).thenReturn(Optional.of(bike));

        UserWallet wallet = new UserWallet();
        wallet.setAvailablePoints(500L);

        when(walletRepository.findByUserIdForUpdate(anyLong())).thenReturn(Optional.of(wallet));

        assertThrows(InsufficientBalanceException.class, () -> orderService.createOrder(2L, 1L, "key-1"));
    }
}
