package com.bikeexchange.service;

import com.bikeexchange.dto.request.BikeCreateRequest;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.Brand;
import com.bikeexchange.model.User;
import com.bikeexchange.repository.BikeRepository;
import com.bikeexchange.repository.BrandRepository;
import com.bikeexchange.repository.CategoryRepository;
import com.bikeexchange.repository.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;
import java.util.Collections;

@ExtendWith(MockitoExtension.class)
public class BikeServiceTest {

    @Mock
    private BikeRepository bikeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private HistoryService historyService;

    @InjectMocks
    private BikeService bikeService;

    @Test
    public void createBike_setsDraftAndNoInspection() {
        User seller = new User();
        seller.setId(1L);

        BikeCreateRequest request = new BikeCreateRequest();
        request.setTitle("Bike");
        request.setDescription("Desc");
        request.setBrand("Giant");
        request.setModel("Escape");
        request.setYear(2022);
        request.setPricePoints(5000L);
        request.setCondition("Like New");
        request.setBikeType("Road");

        Brand brand = new Brand();
        brand.setId(10L);
        brand.setName("Giant");

        Mockito.when(userRepository.findById(1L)).thenReturn(Optional.of(seller));
        Mockito.when(brandRepository.findByName("Giant")).thenReturn(Optional.empty());
        Mockito.when(brandRepository.save(Mockito.any(Brand.class))).thenReturn(brand);
        Mockito.when(bikeRepository.save(Mockito.any(Bike.class))).thenAnswer(invocation -> {
            Bike b = invocation.getArgument(0);
            b.setId(10L);
            return b;
        });

        Bike saved = bikeService.createBike(1L, request);

        Assertions.assertEquals(Bike.BikeStatus.DRAFT, saved.getStatus());
        Assertions.assertEquals(Bike.InspectionStatus.NONE, saved.getInspectionStatus());
    }

    @Test
    public void searchAdvanced_withPriceAndFrame_filters() {
        Page<Bike> page = new PageImpl<>(Collections.singletonList(new Bike()));
        Mockito.when(bikeRepository.filterBikesAdvanced(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any()))
                .thenReturn(page);

        Page<Bike> result = bikeService.searchBikesAdvanced(null, null, null, 100L, 500L, 2020, "54cm", PageRequest.of(0, 10));
        Assertions.assertEquals(1, result.getTotalElements());
    }
}
