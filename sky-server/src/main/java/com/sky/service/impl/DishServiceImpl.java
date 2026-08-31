package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONTokener;
import cn.hutool.json.JSONUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.RedisConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.DishDTO;
import com.sky.dto.DishPageQueryDTO;
import com.sky.entity.Category;
import com.sky.entity.Dish;
import com.sky.entity.DishFlavor;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.*;
import com.sky.result.PageResult;
import com.sky.service.DishService;
import com.sky.vo.DishVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
public class DishServiceImpl implements DishService {

    @Autowired
    private DishMapper dishMapper;

    @Autowired
    private DishFlavorMapper dishFlavorMapper;

    @Autowired
    private SetmealDishMapper setmealDishMapper;
    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private SetmealMapper setmealMapper;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 新增菜品&口味
     * @param dishDTO
     */
    @Override
    @Transactional
    public void saveWithFlavor(DishDTO dishDTO) {
        //1.新增菜品
        //1.1将前端传入的数据封装为数据库的实体类dto->entity
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        dishMapper.insert(dish);

        //1.2插入数据后，会自动将生成的主键赋值给dish的id属性
        //获得dishId
        Long dishId = dish.getId();

        //2.新增口味
        List<DishFlavor> flavors = dishDTO.getFlavors();
        //2.1非空判断
        if(flavors!=null&& !flavors.isEmpty()){
            //2.2为每个口味指定对应菜品id
            flavors.forEach(flavor->flavor.setDishId(dishId));
            //2.3批量插入口味表
            dishFlavorMapper.insertBatch(flavors);
        }

        //3.修改完成后，清理redis缓存，保证双写一致性
        String key=RedisConstant.CATEGORY_DISH_KEY+dishDTO.getCategoryId();
        cleanCache(key);

    }

    /**
     * 菜品分页查询
     * @param dishPageQueryDTO
     * @return
     */
    @Override
    public PageResult pageQuery(DishPageQueryDTO dishPageQueryDTO) {
        //1.获取dto数据
        int page = dishPageQueryDTO.getPage();
        int pageSize = dishPageQueryDTO.getPageSize();
        //2.分页查询dish数据
        //2.1使用插件进行分页查询sql拼接
        PageHelper.startPage(page,pageSize);
        //2.2分页查询
        Page<DishVO> dishPage= dishMapper.queryPage(dishPageQueryDTO);
        //3.封装并返回数据
        return new PageResult(dishPage.getTotal(),dishPage.getResult());
    }

    /**
     * 批量删除菜品
     * @param ids
     */
    @Override
    public void deleteBatch(List<Long> ids) {
        //1.判断菜品是否能被删除
        //1.1判断菜品状态，起售则不可删除
        for (Long id : ids) {
            //查询每个菜品状态，如果有起售状态则抛异常
            Dish dish=dishMapper.getById(id);
            if(dish.getStatus()== StatusConstant.ENABLE){
                throw new DeletionNotAllowedException(MessageConstant.DISH_ON_SALE);
            }
        }
        //1.2判断每个菜品是否有关联套餐，如果有则不可删除
        List<Long> mealIds =setmealDishMapper.getSetmealIdsbyDishIds(ids);
        if(mealIds!=null&&!mealIds.isEmpty()){
            throw new DeletionNotAllowedException(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        }
        //可以删除
        /*for (Long id : ids) {
            //2.删除dish表中的数据
            dishMapper.deleteById(id);
            //3.删除口味表中的数据
            dishFlavorMapper.deleteByDishId(id);
        }*/

        //批量删除，减少sql语句
        dishMapper.deleteByIds(ids);
        dishFlavorMapper.deleteByDishIds(ids);

        //2.删除后，清理全部菜品数据缓存
        String key=RedisConstant.CATEGORY_DISH_KEY+"*";
        cleanCache(key);

    }

    /**
     * 根据id查询菜品数据（包括口味）
     * @param id
     * @return
     */
    @Override
    public DishVO getByIdWithFlavors(Long id) {
        //1.根据id查询菜品数据
        Dish dish = dishMapper.getById(id);
        //1.1根据categoryId查询categoryName
        Category category= categoryMapper.getById(dish.getCategoryId());
        String categoryName = category.getName();
        //2.根据dishId查询口味数据
        List<DishFlavor> flavors= dishFlavorMapper.getByDishId(id);
        //3.封装为DishVO并返回
        DishVO dishVO = BeanUtil.copyProperties(dish, DishVO.class);
        dishVO.setFlavors(flavors);
        dishVO.setCategoryName(categoryName);
        log.info("菜品查询结果：{}",dishVO);
        return dishVO;
    }

    /**
     * 菜品信息修改
     * @param dishDTO
     * @return
     */
    @Override
    public void updateWithFlavors(DishDTO dishDTO) {
        //1.修改dish表的dish信息
        Dish dish = BeanUtil.copyProperties(dishDTO, Dish.class);
        dishMapper.update(dish);
        //2.修改flavor表的菜品口味信息
        //2.1根据dishId删除原来所有口味数据
        Long dishId = dish.getId();
        dishFlavorMapper.deleteByDishId(dishId);
        //2.2插入新增数据
        //2.2.1获取口味数据集合
        List<DishFlavor> flavors = dishDTO.getFlavors();
        //2.2.2批量新增数据
        //非空判断
        if(flavors!=null&& !flavors.isEmpty()){
            //为每个口味指定对应菜品id
            flavors.forEach(flavor->flavor.setDishId(dishId));
            //批量插入口味表
            dishFlavorMapper.insertBatch(flavors);
        }

        //3.修改菜品信息后清理对应的分类查询缓存（可能涉及到旧分类数据和新分类数据，所以直接全部清理）
        String key=RedisConstant.CATEGORY_DISH_KEY+"*";
        cleanCache(key);
    }

    /**
     * 条件查询菜品和口味
     * @param dish
     * @return
     */
    public List<DishVO> listWithFlavor(Dish dish) {
        //先在redis中查询数据
        String key= RedisConstant.CATEGORY_DISH_KEY+dish.getCategoryId();
        String listJson = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(listJson)){
            //查到直接返回
            List<DishVO> list = JSONUtil.toList(listJson, DishVO.class);
            return list;
        }
        //未查到，则查询数据库并存入redis
        List<Dish> dishList = dishMapper.list(dish);
        List<DishVO> dishVOList = new ArrayList<>();

        for (Dish d : dishList) {
            DishVO dishVO = new DishVO();
            BeanUtils.copyProperties(d,dishVO);

            //根据菜品id查询对应的口味
            List<DishFlavor> flavors = dishFlavorMapper.getByDishId(d.getId());

            dishVO.setFlavors(flavors);
            dishVOList.add(dishVO);
        }
        //存入redis
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(dishVOList));
        return dishVOList;
    }


    /**
     * 清理redis缓存
     * @param keyPattern
     */
    private void cleanCache(String keyPattern){
        Set<String> keys = stringRedisTemplate.keys(keyPattern);
        stringRedisTemplate.delete(keys);

    }


    /**
     * 菜品起售停售
     *
     * @param status
     * @param id
     */
    @Override
    @Transactional
    public void startOrStop(Integer status, Long id) {
        Dish dish = Dish.builder()
                .id(id)
                .status(status)
                .build();
        dishMapper.update(dish);

        if (status == StatusConstant.DISABLE) {
            // 如果是停售操作，还需要将包含当前菜品的套餐也停售
            List<Long> dishIds = new ArrayList<>();
            dishIds.add(id);
            // select setmeal_id from setmeal_dish where dish_id in (?,?,?)
            List<Long> setmealIds = setmealDishMapper.getSetmealIdsbyDishIds(dishIds);
            if (setmealIds != null && setmealIds.size() > 0) {
                for (Long setmealId : setmealIds) {
                    Setmeal setmeal = Setmeal.builder()
                            .id(setmealId)
                            .status(StatusConstant.DISABLE)
                            .build();
                    setmealMapper.update(setmeal);
                }
            }
        }

        //修改菜品信息后，需要清理redis缓存（精准清理需要重新查询，不如全部清理）
        String key=RedisConstant.CATEGORY_DISH_KEY+"*";
        cleanCache(key);
    }

}
