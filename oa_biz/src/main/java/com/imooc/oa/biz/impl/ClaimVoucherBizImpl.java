package com.imooc.oa.biz.impl;

import com.imooc.oa.biz.ClaimVoucherBiz;
import com.imooc.oa.dao.ClaimVoucherDao;
import com.imooc.oa.dao.ClaimVoucherItemDao;
import com.imooc.oa.dao.DealRecordDao;
import com.imooc.oa.dao.EmployeeDao;
import com.imooc.oa.entity.ClaimVoucher;
import com.imooc.oa.entity.ClaimVoucherItem;
import com.imooc.oa.entity.DealRecord;
import com.imooc.oa.entity.Employee;
import com.imooc.oa.global.Contant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
/*
  在 业务层 添加 表现层没有写完的东西 对对象进行封装和补充
*/
@Service("cliamVoucherBiz")
public class ClaimVoucherBizImpl implements ClaimVoucherBiz {
    @Autowired
    private ClaimVoucherDao claimVoucherDao;
    @Autowired
    private ClaimVoucherItemDao claimVoucherItemDao;
    @Autowired
    private DealRecordDao dealRecordDao;
    @Autowired
    private EmployeeDao employeeDao;

    public void save(ClaimVoucher claimVoucher, List<ClaimVoucherItem> items) {
        //设置报销单的创建时间 为系统的默认时间 时间格式已经处理好了 
		claimVoucher.setCreateTime(new Date());
		//待处理人 默认为创建人
        claimVoucher.setNextDealSn(claimVoucher.getCreateSn());
        //通过 常量类 设置 报销单不同的状态  此时为新创建的状态
        claimVoucher.setStatus(Contant.CLAIMVOUCHER_CREATED);
		//调用持久层  将当前报销单的信息保存到数据库中
        claimVoucherDao.insert(claimVoucher);

		//保存报销单的详细条目 信息
        for(ClaimVoucherItem item:items){
			//获取该条目信息的报销单编号   上一步中报销单编号已经通过mybatis自动注入了 可以直接回去
            item.setClaimVoucherId(claimVoucher.getId());
            claimVoucherItemDao.insert(item);
        }
    }
    
	//通过id获得报销单的数据
    public ClaimVoucher get(int id) {
        return claimVoucherDao.select(id);
    }
    //通过报销单的id 获得该员工下的所有报销单详细信息
    public List<ClaimVoucherItem> getItems(int cvid) {
        return claimVoucherItemDao.selectByClaimVoucher(cvid);
    }
    //获得该职工下的报销单的处理记录
    public List<DealRecord> getRecords(int cvid) {
        return dealRecordDao.selectByClaimVoucher(cvid);
    }
    //获得个人报销单 
    public List<ClaimVoucher> getForSelf(String sn) {
        return claimVoucherDao.selectByCreateSn(sn);
    }
   //获取待处理报销单
    public List<ClaimVoucher> getForDeal(String sn) {
        return claimVoucherDao.selectByNextDealSn(sn);
    }
  //修改报销单
    public void update(ClaimVoucher claimVoucher, List<ClaimVoucherItem> items) {
        claimVoucher.setNextDealSn(claimVoucher.getCreateSn());
        claimVoucher.setStatus(Contant.CLAIMVOUCHER_CREATED);
        claimVoucherDao.update(claimVoucher);
    //在更新报销单的时候  报销单的条目 可能发生变化 所以要对报销单的新老条目进行对比  添加 或者删除
       //获得原来的报销单条目     
	  List<ClaimVoucherItem> olds = claimVoucherItemDao.selectByClaimVoucher(claimVoucher.getId());
      //删除修改后 消失的旧条目      
	  for(ClaimVoucherItem old:olds){
            boolean isHave=false;
            for(ClaimVoucherItem item:items){
             //与新的报销单条目 一一进行对比  如果相等 则为true  不相等为false				
			 if(item.getId()==old.getId()){
                    isHave=true;
                    break;
                }
            }
			//如果为false 表示 该条目 在新的修改中 删除了 所以 要去数据库删除这个id
            if(!isHave){
                claimVoucherItemDao.delete(old.getId());
            }
        }
		
		//更新报销单条目 如果item.getId()>0 表示该条目已经存在  则更新它  如果不等于0  则添加新的条目
        for(ClaimVoucherItem item:items){
            //为item 设置报销单id  如果不加 在修改数据时新增item会发生空指针异常
			item.setClaimVoucherId(claimVoucher.getId());
            if(item.getId()!=null&&item.getId()>0){
                claimVoucherItemDao.update(item);
            }else{
                claimVoucherItemDao.insert(item);
            }
        }

    }
  // 提交操作
    public void submit(int id) {
		//获得报销单
        ClaimVoucher claimVoucher = claimVoucherDao.select(id);
		//获得员工信息
        Employee employee = employeeDao.select(claimVoucher.getCreateSn());
         //报销单更新
		 //更新状态  修改为已提交的状态
        claimVoucher.setStatus(Contant.CLAIMVOUCHER_SUBMIT);
         //更新待处理人编号   去职工的dao层  获取部门经理的sn  通过部门和职位查询  查询的结果可能是list集合 所以只获得第一个employee的sn   list<Employee>.get(0).getSn()
		claimVoucher.setNextDealSn(employeeDao.selectByDepartmentAndPost(employee.getDepartmentSn(),Contant.POST_FM).get(0).getSn());
		//更新报销单
        claimVoucherDao.update(claimVoucher);
       //记录的保存
        DealRecord dealRecord = new DealRecord();
        dealRecord.setDealWay(Contant.DEAL_SUBMIT);
        dealRecord.setDealSn(employee.getSn());
        dealRecord.setClaimVoucherId(id);
		//处理状态
        dealRecord.setDealResult(Contant.CLAIMVOUCHER_SUBMIT);
        dealRecord.setDealTime(new Date());
        //备注
		dealRecord.setComment("无");
        dealRecordDao.insert(dealRecord);
    }
    	//报销单的处理
		//审核（打回 通过 拒绝） 和 打款 放在一起  执行了 
    public void deal(DealRecord dealRecord) {
        ClaimVoucher claimVoucher = claimVoucherDao.select(dealRecord.getClaimVoucherId());
        //获得当前处理人   通过记录表       
	   Employee employee = employeeDao.select(dealRecord.getDealSn());
        //处理时间 系统当前时间
		dealRecord.setDealTime(new Date());
         
		 //如果审核通过
        if(dealRecord.getDealWay().equals(Contant.DEAL_PASS)){
            //如果金额小于5000 || 或者 审核人的职位是总经理的话   都直接给 财务处理         
  		   if(claimVoucher.getTotalAmount()<=Contant.LIMIT_CHECK || employee.getPost().equals(Contant.POST_GM)){
                claimVoucher.setStatus(Contant.CLAIMVOUCHER_APPROVED);
                //待处理人 设为 财务  不用设置其dsn  财务就一个
				claimVoucher.setNextDealSn(employeeDao.selectByDepartmentAndPost(null,Contant.POST_CASHIER).get(0).getSn());
                 //报销单的状态 为已审核
                dealRecord.setDealResult(Contant.CLAIMVOUCHER_APPROVED);
            }else{
				//大于5000  交给总经理
                 //报销单的状态 ：设为 待复审				
                claimVoucher.setStatus(Contant.CLAIMVOUCHER_RECHECK);
				//待处理人改为总经理
                claimVoucher.setNextDealSn(employeeDao.selectByDepartmentAndPost(null,Contant.POST_GM).get(0).getSn());
                 //处理记录 设为 待复审
                dealRecord.setDealResult(Contant.CLAIMVOUCHER_RECHECK);
            }
        }
		//如果记录为 被打回  更改状态和 待处理人    打回为创建人
		else if(dealRecord.getDealWay().equals(Contant.DEAL_BACK)){
			//设置状态为已打回
            claimVoucher.setStatus(Contant.CLAIMVOUCHER_BACK);
			//设置待处理人为  创建者
            claimVoucher.setNextDealSn(claimVoucher.getCreateSn());
             
			 //处理记录为已打回
            dealRecord.setDealResult(Contant.CLAIMVOUCHER_BACK);
        }
		//如果记录为拒绝 就不用设置待处理人了  这条记录就没有用了 终止了
		else if(dealRecord.getDealWay().equals(Contant.DEAL_REJECT)){
			//设置状态为 已终止
            claimVoucher.setStatus(Contant.CLAIMVOUCHER_TERMINATED);
			//不用设置待处理人  因为该记录 被拒绝 无效了
            claimVoucher.setNextDealSn(null);
			//设置状态为 已终止
            dealRecord.setDealResult(Contant.CLAIMVOUCHER_TERMINATED);
		}
		//如果处理方式为打款操作 财务人员的 打款操作
		else if(dealRecord.getDealWay().equals(Contant.DEAL_PAID)){
            //设置为已打款           
		   claimVoucher.setStatus(Contant.CLAIMVOUCHER_PAID);
            //打款操作 就不用设置待处理人了
			claimVoucher.setNextDealSn(null);

            dealRecord.setDealResult(Contant.CLAIMVOUCHER_PAID);
        }
        //处理后 更新报销单
        claimVoucherDao.update(claimVoucher);
		//更新 处理流程
        dealRecordDao.insert(dealRecord);
    }

}
