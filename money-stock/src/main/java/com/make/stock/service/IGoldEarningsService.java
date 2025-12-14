package com.make.stock.service;

import java.util.List;

import com.make.stock.domain .GoldEarnings;

/**
 * 攒金收益记录Service接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface IGoldEarningsService {

    /**
     * 查询攒金收益记录
     *
     * @param id 攒金收益记录主键
     * @return 攒金收益记录
     */
    public GoldEarnings selectGoldEarningsById(Long id);

    /**
     * 查询攒金收益记录列表
     *
     * @param goldEarnings 攒金收益记录
     * @return 攒金收益记录集合
     */
    public List<GoldEarnings> selectGoldEarningsList(GoldEarnings goldEarnings);

    /**
     * 新增攒金收益记录
     *
     * @param goldEarnings 攒金收益记录
     * @return 结果
     */
    public int insertGoldEarnings(GoldEarnings goldEarnings);

    /**
     * 修改攒金收益记录
     *
     * @param goldEarnings 攒金收益记录
     * @return 结果
     */
    public int updateGoldEarnings(GoldEarnings goldEarnings);

    /**
     * 批量删除攒金收益记录
     *
     * @param ids 需要删除的攒金收益记录主键集合
     * @return 结果
     */
    public int deleteGoldEarningsByIds(Long[] ids);

    /**
     * 删除攒金收益记录信息
     *
     * @param id 攒金收益记录主键
     * @return 结果
     */
    public int deleteGoldEarningsById(Long id);
}
