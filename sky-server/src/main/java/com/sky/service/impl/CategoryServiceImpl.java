package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.dto.CategoryPageQueryDTO;
import com.sky.entity.Category;
import com.sky.mapper.CategoryMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 分类分页查询服务实现
     * @param categoryPageQueryDTO
     * @return
     */
    @Override
    public Result<PageResult> page(CategoryPageQueryDTO categoryPageQueryDTO) {
        //1.获取分页查询参数
        int page = categoryPageQueryDTO.getPage();
        int pageSize = categoryPageQueryDTO.getPageSize();
        //2.使用pagehelper在sql中拼接分页条件参数
        PageHelper.startPage(page,pageSize);

        //3.查询数据库
        Page<Category> result= categoryMapper.page(categoryPageQueryDTO);

        //4.解析数据并返回
        long total = result.getTotal();
        List<Category> categories = result.getResult();

        return Result.success(new PageResult(total,categories));
    }
}
