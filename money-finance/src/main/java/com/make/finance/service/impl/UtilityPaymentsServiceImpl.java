package com.make.finance.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.UtilityPaymentsMapper;
import com.make.finance.domain.UtilityPayments;
import com.make.finance.service.IUtilityPaymentsService;

/**
 * 用户水电费缴纳记录Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-27
 */
@Service
public class UtilityPaymentsServiceImpl implements IUtilityPaymentsService {
    @Autowired
    private UtilityPaymentsMapper utilityPaymentsMapper;

    /**
     * 查询用户水电费缴纳记录
     *
     * @param id 用户水电费缴纳记录主键
     * @return 用户水电费缴纳记录
     */
    @Override
    public UtilityPayments selectUtilityPaymentsById(Integer id) {
        return utilityPaymentsMapper.selectUtilityPaymentsById(id);
    }

    /**
     * 查询用户水电费缴纳记录列表
     *
     * @param utilityPayments 用户水电费缴纳记录
     * @return 用户水电费缴纳记录
     */
    @Override
    public List<UtilityPayments> selectUtilityPaymentsList(UtilityPayments utilityPayments) {
        return utilityPaymentsMapper.selectUtilityPaymentsList(utilityPayments);
    }

    /**
     * 新增用户水电费缴纳记录
     *
     * @param utilityPayments 用户水电费缴纳记录
     * @return 结果
     */
    @Override
    public int insertUtilityPayments(UtilityPayments utilityPayments) {
        return utilityPaymentsMapper.insertUtilityPayments(utilityPayments);
    }

    /**
     * 修改用户水电费缴纳记录
     *
     * @param utilityPayments 用户水电费缴纳记录
     * @return 结果
     */
    @Override
    public int updateUtilityPayments(UtilityPayments utilityPayments) {
        return utilityPaymentsMapper.updateUtilityPayments(utilityPayments);
    }

    /**
     * 批量删除用户水电费缴纳记录
     *
     * @param ids 需要删除的用户水电费缴纳记录主键
     * @return 结果
     */
    @Override
    public int deleteUtilityPaymentsByIds(Integer[] ids) {
        return utilityPaymentsMapper.deleteUtilityPaymentsByIds(ids);
    }

    /**
     * 删除用户水电费缴纳记录信息
     *
     * @param id 用户水电费缴纳记录主键
     * @return 结果
     */
    @Override
    public int deleteUtilityPaymentsById(Integer id) {
        return utilityPaymentsMapper.deleteUtilityPaymentsById(id);
    }

    @Override
    public List<UtilityPayments> queryUtilityPaymentsList(UtilityPayments utilityPayments) {
        if (utilityPayments.getPaymentDate() != null) {
            Date paymentDate = utilityPayments.getPaymentDate();

            // 使用 Calendar 提取年份
            Calendar cal = Calendar.getInstance();
            cal.setTime(paymentDate);

            int year = cal.get(Calendar.YEAR);

            // 构建年初日期：yyyy-01-01
            cal.set(year, Calendar.JANUARY, 1, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            Date startDate = cal.getTime();

            // 构建年末日期：yyyy-12-31
            cal.set(year, Calendar.DECEMBER, 31, 23, 59, 59);
            cal.set(Calendar.MILLISECOND, 999);
            Date endDate = cal.getTime();

            utilityPayments.setStartDate(new java.sql.Date(startDate.getTime()));
            utilityPayments.setEndDate(new java.sql.Date(endDate.getTime()));
        }
        return utilityPaymentsMapper.queryUtilityPaymentsList(utilityPayments);
    }
}
