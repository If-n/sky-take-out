package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.service.EmployeeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

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
}
