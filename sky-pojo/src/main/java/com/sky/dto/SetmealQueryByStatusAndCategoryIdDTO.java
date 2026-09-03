package com.sky.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetmealQueryByStatusAndCategoryIdDTO {
    //分类id
    private Integer categoryId;

    //状态 0表示禁用 1表示启用
    private Integer status;
}
