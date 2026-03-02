package com.bikeexchange.controller;

import com.bikeexchange.dto.request.UpgradeToSellerRequest;
import com.bikeexchange.model.User;
import com.bikeexchange.security.UserPrincipal;
import com.bikeexchange.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.containsString;

/**
 * Test cases for the UpgradeToSeller feature
 */
@WebMvcTest(UserController.class)
public class UserUpgradeToSellerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testBuyer;
    private UpgradeToSellerRequest validRequest;

    @BeforeEach
    void setUp() {
        // Create test user with BUYER role
        testBuyer = new User();
        testBuyer.setId(1L);
        testBuyer.setEmail("buyer@test.com");
        testBuyer.setFullName("Test Buyer");
        testBuyer.setPhone("0123456789");
        testBuyer.setAddress("Test Address");
        testBuyer.setRole(User.UserRole.BUYER);
        testBuyer.setStatus("ACTIVE");
        testBuyer.setIsVerified(false);
        testBuyer.setRating(5.0);
        testBuyer.setTotalBikesSold(0);

        // Create valid upgrade request
        validRequest = new UpgradeToSellerRequest(
            "My Bike Shop",
            "Professional bike sales",
            true
        );
    }

    @Test
    void testUpgradeToSellerSuccess() throws Exception {
        // Arrange
        User upgradedUser = testBuyer;
        upgradedUser.setRole(User.UserRole.SELLER);
        upgradedUser.setShopName("My Bike Shop");
        upgradedUser.setShopDescription("Professional bike sales");
        upgradedUser.setUpgradedToSellerAt(LocalDateTime.now());

        when(userService.upgradeToSeller(anyLong(), anyString(), anyString()))
            .thenReturn(upgradedUser);

        // Act & Assert
        MvcResult result = mockMvc.perform(post("/users/1/upgrade-to-seller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User successfully upgraded to seller"))
                .andExpect(jsonPath("$.data.role").value("SELLER"))
                .andExpect(jsonPath("$.data.shopName").value("My Bike Shop"))
                .andExpect(jsonPath("$.data.shopDescription").value("Professional bike sales"))
                .andReturn();
    }

    @Test
    void testUpgradeToSellerMissingShopName() throws Exception {
        // Arrange
        UpgradeToSellerRequest requestMissingShopName = new UpgradeToSellerRequest(
            "",  // Empty shop name
            "Professional bike sales",
            true
        );

        // Act & Assert
        mockMvc.perform(post("/users/1/upgrade-to-seller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMissingShopName)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Shop name is required"));
    }

    @Test
    void testUpgradeToSellerMissingShopDescription() throws Exception {
        // Arrange
        UpgradeToSellerRequest requestMissingDesc = new UpgradeToSellerRequest(
            "My Bike Shop",
            "",  // Empty description
            true
        );

        // Act & Assert
        mockMvc.perform(post("/users/1/upgrade-to-seller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestMissingDesc)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Shop description is required"));
    }

    @Test
    void testUpgradeToSellerTermsNotAccepted() throws Exception {
        // Arrange
        UpgradeToSellerRequest requestNoTerms = new UpgradeToSellerRequest(
            "My Bike Shop",
            "Professional bike sales",
            false  // Terms not accepted
        );

        // Act & Assert
        mockMvc.perform(post("/users/1/upgrade-to-seller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestNoTerms)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You must agree to the terms and conditions"));
    }

    @Test
    void testUpgradeToSellerAlreadySeller() throws Exception {
        // Arrange
        when(userService.upgradeToSeller(anyLong(), anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("User is already a seller"));

        // Act & Assert
        mockMvc.perform(post("/users/1/upgrade-to-seller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User is already a seller"));
    }

    @Test
    void testUpgradeToSellerNotBuyer() throws Exception {
        // Arrange
        when(userService.upgradeToSeller(anyLong(), anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("Only buyers can upgrade to seller status"));

        // Act & Assert
        mockMvc.perform(post("/users/1/upgrade-to-seller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Only buyers can upgrade to seller status"));
    }

    @Test
    void testUpgradeToSellerUserNotFound() throws Exception {
        // Arrange
        when(userService.upgradeToSeller(anyLong(), anyString(), anyString()))
            .thenThrow(new RuntimeException("User not found"));

        // Act & Assert
        mockMvc.perform(post("/users/999/upgrade-to-seller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void testUpgradeToSellerInvalidRole() throws Exception {
        // Arrange - Test that only BUYER role can upgrade
        User nonBuyerUser = new User();
        nonBuyerUser.setId(2L);
        nonBuyerUser.setRole(User.UserRole.INSPECTOR);

        when(userService.upgradeToSeller(2L, "Shop", "Description"))
            .thenThrow(new IllegalArgumentException("Only buyers can upgrade to seller status"));

        // Act & Assert
        mockMvc.perform(post("/users/2/upgrade-to-seller")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only buyers can upgrade to seller status"));
    }
}
