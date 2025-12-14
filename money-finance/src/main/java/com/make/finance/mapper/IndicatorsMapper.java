package com.make.finance.mapper;

import java.util.List;

import com.make.finance.domain.Indicators;

/**
 * 指标信息Mapper接口
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public interface IndicatorsMapper {
    /**
     * 查询指标信息
     *
     * @param id 指标信息主键
     * @return 指标信息
     */
    public Indicators selectIndicatorsById(Long id);

    /**
     * 查询指标信息列表
     *
     * @param indicators 指标信息
     * @return 指标信息集合
     */
    public List<Indicators> selectIndicatorsList(Indicators indicators);

    /**
     * 新增指标信息
     *
     * @param indicators 指标信息
     * @return 结果
     */
    public int insertIndicators(Indicators indicators);

    /**
     * 修改指标信息
     *
     * @param indicators 指标信息
     * @return 结果
     */
    public int updateIndicators(Indicators indicators);

    /**
     * 删除指标信息
     *
     * @param id 指标信息主键
     * @return 结果
     */
    public int deleteIndicatorsById(Long id);

    /**
     * 批量删除指标信息
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteIndicatorsByIds(Long[] ids);
}
