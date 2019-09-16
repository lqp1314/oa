package com.imooc.oa.controller;

import com.imooc.oa.biz.ClaimVoucherBiz;
import com.imooc.oa.dto.ClaimVoucherInfo;
import com.imooc.oa.entity.DealRecord;
import com.imooc.oa.entity.Employee;
import com.imooc.oa.global.Contant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;
import java.util.Map;
@Controller("claimVoucherController")
@RequestMapping("/claim_voucher")
public class ClaimVoucherController {
    @Autowired
    private ClaimVoucherBiz claimVoucherBiz;
	/*
	* 在打开填写报销单页面的时候 需要先向页面传入信息 所以需要添加map 用来保存和显示信息   这个很重要 也很常见
	*打开添加报销单界面  在此过程中 需要先传入 对象模型和常量值（花销类型）  使用的集合是map
	*/
    @RequestMapping("/to_add")
    public String toAdd(Map<String,Object> map){
		//去数据字典中 获得所有的花销类型
        map.put("items", Contant.getItems());
        //先建立一个对象模型   空壳的对象模型 用来存放报销单的相应数据
        map.put("info",new ClaimVoucherInfo());
        return "claim_voucher_add";
    }
    @RequestMapping("/add")
	//ClaimVoucherInfo info  如果不加注解的话@RequestParam(value = "info") 需要和map里面的info 保持一致
    public String add(HttpSession session, ClaimVoucherInfo info){
		//需要去调用session域  去获得创建人的编号 就是职工的工号
        Employee employee = (Employee)session.getAttribute("employee");
        info.getClaimVoucher().setCreateSn(employee.getSn());
        claimVoucherBiz.save(info.getClaimVoucher(),info.getItems());
		//return "redirect:detail?id="+info.getClaimVoucher().getId();
      //通过deal 控制器 来获得 需要使用的 id 属性  所以不用在url上面添加 id了      
	  return "redirect:deal";
    }
	//详情 也是需要在打开该页面的时候 需要向该详情页面传递信息 需要时候 map来存储信息
    @RequestMapping("/detail")
    public String detail(int id, Map<String,Object> map){
       //报销单  基本信息     
	   map.put("claimVoucher",claimVoucherBiz.get(id));
         //报销单具体的项目 费用明细
		map.put("items",claimVoucherBiz.getItems(id));
		//处理流程
        map.put("records",claimVoucherBiz.getRecords(id));
        return "claim_voucher_detail";
    }
	//获取个人报销单  
    @RequestMapping("/self")
    public String self(HttpSession session, Map<String,Object> map){
         //通过session域中获得用户的信息
		Employee employee = (Employee)session.getAttribute("employee");
        map.put("list",claimVoucherBiz.getForSelf(employee.getSn()));
        return "claim_voucher_self";
    }
	//待处理报销单
    @RequestMapping("/deal")
    public String deal(HttpSession session, Map<String,Object> map){
        Employee employee = (Employee)session.getAttribute("employee");
        map.put("list",claimVoucherBiz.getForDeal(employee.getSn()));
        return "claim_voucher_deal";
    }
    
	//去修改      //修改报销单操作   和 添加报销单类似   但是具体操作不同  先 点击去修改操作    传递id的作用是为了 获得报销单和报销单条目信息

    @RequestMapping("/to_update")
	//传递报销单编号
    public String toUpdate(int id,Map<String,Object> map){
        map.put("items", Contant.getItems());
		//因为是去修改 所以先创建一个空的info对象  用来存入新的对象值 
        ClaimVoucherInfo info =new ClaimVoucherInfo();
		//存入 报销单 和 报销单条目信息
        info.setClaimVoucher(claimVoucherBiz.get(id));
        info.setItems(claimVoucherBiz.getItems(id));
        map.put("info",info);
        return "claim_voucher_update";
    }
	//修改
    @RequestMapping("/update")
    public String update(HttpSession session, ClaimVoucherInfo info){
        Employee employee = (Employee)session.getAttribute("employee");
        info.getClaimVoucher().setCreateSn(employee.getSn());
        claimVoucherBiz.update(info.getClaimVoucher(),info.getItems());
        return "redirect:deal";
    }
	
	//提交
    @RequestMapping("/submit")
    public String submit(int id){
        claimVoucherBiz.submit(id);
        return "redirect:deal";
    }
    //去 审核 + 打款 操作
    @RequestMapping("/to_check")
    public String toCheck(int id,Map<String,Object> map){
		//通过id 获得报销单
        map.put("claimVoucher",claimVoucherBiz.get(id));
		//通过id 获得报销单的详情条目
        map.put("items",claimVoucherBiz.getItems(id));
		//通过id 获得处理信息的集合
        map.put("records",claimVoucherBiz.getRecords(id));
        //先创建 处理记录的 对象   /创建空间       
	   DealRecord dealRecord =new DealRecord();
	   // 设置 报销单id 
        dealRecord.setClaimVoucherId(id);
        map.put("record",dealRecord);
        return "claim_voucher_check";
    }
    @RequestMapping("/check")
    public String check(HttpSession session, DealRecord dealRecord){
        Employee employee = (Employee)session.getAttribute("employee");
		//设置处理流程的 处理人编号
        dealRecord.setDealSn(employee.getSn());
		
		// 去service层 更新处理流程
        claimVoucherBiz.deal(dealRecord);
        return "redirect:deal";
    }
}
