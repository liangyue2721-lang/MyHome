package com.make.finance.service;

import java.util.List;

import com.make.finance.domain.UtilityPayments;

/**
 * 用户水电费缴纳记录Service接口
 *
 * @author 贰柒
 * @date 2025-05-27
 */
public interface IUtilityPaymentsService {
    /**
     * 查询用户水电费缴纳记录
     *
     * @param id 用户水电费缴纳记录主键
     * @return 用户水电费缴纳记录
     */
    public UtilityPayments selectUtilityPaymentsById(Integer id);

    /**
     * 查询用户水电费缴纳记录列表
     *
     * @param utilityPayments 用户水电费缴纳记录
     * @return 用户水电费缴纳记录集合
     */
    public List<UtilityPayments> selectUtilityPaymentsList(UtilityPayments utilityPayments);

    /**
     * 新增用户水电费缴纳记录
     *
     * @param utilityPayments 用户水电费缴纳记录
     * @return 结果
     */
    public int insertUtilityPayments(UtilityPayments utilityPayments);

    /**
     * 修改用户水电费缴纳记录
     *
     * @param utilityPayments 用户水电费缴纳记录
     * @return 结果
     */
    public int updateUtilityPayments(UtilityPayments utilityPayments);

    /**
     * 批量删除用户水电费缴纳记录
     *
     * @param ids 需要删除的用户水电费缴纳记录主键集合
     * @return 结果
     */
    public int deleteUtilityPaymentsByIds(Integer[] ids);

    /**
     * 删除用户水电费缴纳记录信息
     *
     * @param id 用户水电费缴纳记录主键
     * @return 结果
     */
    public int deleteUtilityPaymentsById(Integer id);

    public List<UtilityPayments> queryUtilityPaymentsList(UtilityPayments utilityPayments);

}
