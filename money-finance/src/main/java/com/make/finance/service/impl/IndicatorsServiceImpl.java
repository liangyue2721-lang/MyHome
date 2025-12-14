package com.make.finance.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.IndicatorsMapper;
import com.make.finance.domain.Indicators;
import com.make.finance.service.IIndicatorsService;

/**
 * 指标信息Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Service
public class IndicatorsServiceImpl implements IIndicatorsService {
    @Autowired
    private IndicatorsMapper indicatorsMapper;

    /**
     * 查询指标信息
     *
     * @param id 指标信息主键
     * @return 指标信息
     */
    @Override
    public Indicators selectIndicatorsById(Long id) {
        return indicatorsMapper.selectIndicatorsById(id);
    }

    /**
     * 查询指标信息列表
     *
     * @param indicators 指标信息
     * @return 指标信息
     */
    @Override
    public List<Indicators> selectIndicatorsList(Indicators indicators) {
        return indicatorsMapper.selectIndicatorsList(indicators);
    }

    /**
     * 新增指标信息
     *
     * @param indicators 指标信息
     * @return 结果
     */
    @Override
    public int insertIndicators(Indicators indicators) {
        return indicatorsMapper.insertIndicators(indicators);
    }

    /**
     * 修改指标信息
     *
     * @param indicators 指标信息
     * @return 结果
     */
    @Override
    public int updateIndicators(Indicators indicators) {
        return indicatorsMapper.updateIndicators(indicators);
    }

    /**
     * 批量删除指标信息
     *
     * @param ids 需要删除的指标信息主键
     * @return 结果
     */
    @Override
    public int deleteIndicatorsByIds(Long[] ids) {
        return indicatorsMapper.deleteIndicatorsByIds(ids);
    }

    /**
     * 删除指标信息信息
     *
     * @param id 指标信息主键
     * @return 结果
     */
    @Override
    public int deleteIndicatorsById(Long id) {
        return indicatorsMapper.deleteIndicatorsById(id);
    }
}
