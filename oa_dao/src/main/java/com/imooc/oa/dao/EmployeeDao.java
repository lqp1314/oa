package com.imooc.oa.dao;

import com.imooc.oa.entity.Department;
import com.imooc.oa.entity.Employee;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository("employeeDao")
public interface EmployeeDao {
    void insert(Employee employee);
    void update(Employee employee);
    void delete(String sn);
    Employee select(String sn);
    List<Employee> selectAll();
	//业务  提交报销单的操作  查找待处理人
    //通过部门和职务 来获得相应的人员信息   需要去映射文件中调用  俩个参数 不明确 所以需要添加 @Param
	List<Employee> selectByDepartmentAndPost(@Param("dsn") String dsn,@Param("post") String post);
}
