package com.bikeexchange.controller;

import com.bikeexchange.dto.request.SellerPostCreateRequest;
import com.bikeexchange.dto.request.SellerPostUpdateRequest;
import com.bikeexchange.model.Bike;
import com.bikeexchange.model.Brand;
import com.bikeexchange.model.Post;
import com.bikeexchange.model.User;
import com.bikeexchange.service.PostService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerPostController.class)
@AutoConfigureMockMvc(addFilters = false)
public class SellerPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PostService postService;

    private Post samplePost() {
        Bike bike = new Bike();
        bike.setId(10L);
        Brand brand = new Brand();
        brand.setName("Giant");
        bike.setBrand(brand);

        User seller = new User();
        seller.setId(1L);

        Post post = new Post();
        post.setId(100L);
        post.setBike(bike);
        post.setSeller(seller);
        post.setCaption("Desc");
        post.setListingType(Post.ListingType.STANDARD);
        post.setStatus(Post.PostStatus.ACTIVE);
        return post;
    }

    @Test
    public void listSellerPosts_returnsPage() throws Exception {
        Post post = samplePost();
        Mockito.when(postService.listPosts(Mockito.eq(1L), Mockito.anyList(), Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        mockMvc.perform(get("/seller/posts")
                        .param("sellerId", "1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(100));
    }

    @Test
    public void listPosts_withoutSellerId_returnsAll() throws Exception {
        Post post = samplePost();
        Mockito.when(postService.listPosts(Mockito.isNull(), Mockito.anyList(), Mockito.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(post)));

        mockMvc.perform(get("/seller/posts")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(100));
    }

    @Test
    public void getSellerPost_returnsDetail() throws Exception {
        Post post = samplePost();
        Mockito.when(postService.getSellerPost(100L, 1L)).thenReturn(post);

        mockMvc.perform(get("/seller/posts/100")
                        .param("sellerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    public void createSellerPost_returnsCreated() throws Exception {
        Post post = samplePost();
        Mockito.when(postService.createPost(Mockito.eq(1L), Mockito.any(SellerPostCreateRequest.class)))
                .thenReturn(post);

        SellerPostCreateRequest request = new SellerPostCreateRequest();
        request.setBikeId(10L);
        request.setCaption("Desc");
        request.setListingType("STANDARD");

        mockMvc.perform(post("/seller/posts")
                        .param("sellerId", "1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    public void updateSellerPost_returnsUpdated() throws Exception {
        Post post = samplePost();
        Mockito.when(postService.updatePost(Mockito.eq(100L), Mockito.eq(1L), Mockito.any(SellerPostUpdateRequest.class)))
                .thenReturn(post);

        SellerPostUpdateRequest request = new SellerPostUpdateRequest();
        request.setCaption("Updated");

        mockMvc.perform(put("/seller/posts/100")
                        .param("sellerId", "1")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100));
    }

    @Test
    public void deleteSellerPost_returnsSuccess() throws Exception {
        mockMvc.perform(delete("/seller/posts/100")
                        .param("sellerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
