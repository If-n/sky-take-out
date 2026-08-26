package com.sky.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.result.Result;
import com.sky.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    @Override
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        //1.查询数据库对应的账号信息
        //1.1解析dto信息
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();
        //1.2查找账号
        Employee employee = employeeMapper.getByUsername(username);
        //1.3没有此账号，抛异常
        if(employee ==null){
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }
        //1.4有账号则下一步
        //2.对比密码
        //2.1处理密码md5
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        //2.2密码错误，抛出异常
        if(!password.equals(employee.getPassword())){
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }
        //2.3密码正确
        //3判断账号状态
        //3.1账号被锁定，抛异常
        if(employee.getStatus()==StatusConstant.DISABLE){
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }
        //3.2账号正常
        //4.放行
        return employee;
    }

    /**
     * 新增员工服务实现
     *
     * @param employeeDTO
     * @return
     */
    @Override
    public Result save(EmployeeDTO employeeDTO) {
        //0.获取当前登录的管理员
        Long adminId = BaseContext.getCurrentId();
        //非空判断
        if(adminId==null){
            //未登录直接返回
            return Result.error(MessageConstant.USER_NOT_LOGIN);
        }
        //1.将前端传入dto数据封装为entity实体类
        Employee employee = BeanUtil.copyProperties(employeeDTO, Employee.class);
        //2.添加未封装的默认数据
        //2.1设置账号默认状态可用
        employee.setStatus(StatusConstant.ENABLE);
        //2.2设置账号创建时间
        LocalDateTime now = LocalDateTime.now();
        employee.setCreateTime(now);
        //2.3设置账号数据更新时间
        employee.setUpdateTime(now);
        //2.4设置新增员工/更新员工的操作员
        employee.setCreateUser(adminId);
        employee.setUpdateUser(adminId);
        //2.5设置默认密码
        employee.setPassword(PasswordConstant.DEFAULT_PASSWORD);
        //3.插入employee表中，保存到数据库
        employeeMapper.insert(employee);
        //4.返回成功信息
        return Result.success();
    }

    /**
     * 员工分页查询服务实现
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public Result<PageResult> pageQuery(EmployeePageQueryDTO employeePageQueryDTO) {
        //1.获取请求参数
        int page = employeePageQueryDTO.getPage();
        int pageSize = employeePageQueryDTO.getPageSize();
        //2.使用mybatis的pagehelper将分页查询sql拼接到查询语句后面（底层是拦截器）
        PageHelper.startPage(page,pageSize);
        //3.分页查询语句(如果指定查询姓名则模糊查询)
        Page<Employee> employeePage=employeeMapper.pageQuery(employeePageQueryDTO);
        //4.解析查询结果
        //4.1查询总数
        long total = employeePage.getTotal();
        //4.2查询数据集合
        List<Employee> result = employeePage.getResult();
        //5.封装并返回
        return Result.success(new PageResult(total,result));
    }
}
