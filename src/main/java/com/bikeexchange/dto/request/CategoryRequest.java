package com.bikeexchange.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

import com.bikeexchange.model.Category;

@Data
@Schema(description = "Request create category")
public class CategoryRequest {

    private String name;

    private String description;

    private String imgUrl;

    private LocalDateTime createdAt;

    public Category toEntity() {
        Category category = new Category();
        category.setName(this.name);
        category.setDescription(this.description);
        category.setImgUrl(this.imgUrl);
        return category;
    }
}
