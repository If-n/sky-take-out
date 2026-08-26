package com.sky.service;

import com.sky.dto.CategoryPageQueryDTO;
import com.sky.result.PageResult;
import com.sky.result.Result;

public interface CategoryService {
    Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO);
}
