package com.imooc.oa.biz;

import com.imooc.oa.entity.ClaimVoucher;
import com.imooc.oa.entity.ClaimVoucherItem;
import com.imooc.oa.entity.DealRecord;

import java.util.List;

public interface ClaimVoucherBiz {
    //保存  报销单和 报销单条目     1-n
    void save(ClaimVoucher claimVoucher, List<ClaimVoucherItem> items);

    ClaimVoucher get(int id);
    List<ClaimVoucherItem> getItems(int cvid);
	//获得处理流程的 集合
    List<DealRecord> getRecords(int cvid);
   
   //个人报销单  传入职工的编号
    List<ClaimVoucher> getForSelf(String sn);
    //待处理报销单   传入职工的编号 
	List<ClaimVoucher> getForDeal(String sn);
     
	 //修改报销单 和添加报销单 一样 只是里面的内容不同
    void update(ClaimVoucher claimVoucher, List<ClaimVoucherItem> items);
	//提交操作  
    void submit(int id);
	//审核 和 打款 放在一起  执行了 
    void deal(DealRecord dealRecord);
}
