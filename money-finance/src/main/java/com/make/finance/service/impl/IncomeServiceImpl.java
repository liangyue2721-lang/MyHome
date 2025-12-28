package com.make.finance.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.make.finance.domain.vo.LabelEntity;
import com.make.finance.enums.IncomeSourceEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.IncomeMapper;
import com.make.finance.domain.Income;
import com.make.finance.service.IIncomeService;

/**
 * 收入Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Service
public class IncomeServiceImpl implements IIncomeService {

    @Autowired
    private IncomeMapper incomeMapper;

    /**
     * 查询收入
     *
     * @param incomeId 收入主键
     * @return 收入
     */
    @Override
    public Income selectIncomeByIncomeId(Long incomeId) {
        return incomeMapper.selectIncomeByIncomeId(incomeId);
    }

    /**
     * 查询收入列表
     *
     * @param income 收入
     * @return 收入
     */
    @Override
    public List<Income> selectIncomeList(Income income) {
        return incomeMapper.selectIncomeList(income);
    }

    /**
     * 新增收入
     *
     * @param income 收入
     * @return 结果
     */
    @Override
    public int insertIncome(Income income) {
        return incomeMapper.insertIncome(income);
    }

    /**
     * 修改收入
     *
     * @param income 收入
     * @return 结果
     */
    @Override
    public int updateIncome(Income income) {
        return incomeMapper.updateIncome(income);
    }

    /**
     * 批量删除收入
     *
     * @param incomeIds 需要删除的收入主键
     * @return 结果
     */
    @Override
    public int deleteIncomeByIncomeIds(Long[] incomeIds) {
        return incomeMapper.deleteIncomeByIncomeIds(incomeIds);
    }

    /**
     * 删除收入信息
     *
     * @param incomeId 收入主键
     * @return 结果
     */
    @Override
    public int deleteIncomeByIncomeId(Long incomeId) {
        return incomeMapper.deleteIncomeByIncomeId(incomeId);
    }

    @Override
    public BigDecimal getCurrentMonthIncomeTotal(Long id, LocalDate startDate, LocalDate endDate) {
        return incomeMapper.selectCurrentMonthIncomeTotal(id, startDate, endDate);
    }

    @Override
    public Income getIncomeByUserIdAndDate(Long userId, Date date) {
        return incomeMapper.selectIncomeByUserIdAndDate(userId, date);
    }

    @Override
    public List<Map<String, Object>> selectIncomeStats(Income income) {
        return incomeMapper.selectIncomeStats(income);
    }


    /**
     * 获取收入来源选项列表，用于前端下拉框展示
     * <p>
     * 本方法通过枚举类 IncomeSource 获取所有预定义的收入来源选项，
     * 并将其转换为具有标准标签结构的对象列表。转换规则为：
     * - 使用枚举项的显示名称(label)作为前端展示文本
     * - 使用枚举项的编码值(value)作为表单提交值
     *
     * @return List<LabelEntity> 收入来源选项列表，每个元素包含：
     * - label: 显示在前端的下拉选项文本（对应枚举显示名称）
     * - value: 选项对应的值（对应枚举编码值）
     * @example 返回示例：
     * [
     * {"label": "工资收入", "value": "salary"},
     * {"label": "投资理财", "value": "investment"},
     * {"label": "经营所得", "value": "business"}
     * ]
     * @apiNote 该方法通常用于以下场景：
     * 1. 表单中收入来源下拉框的选项数据源
     * 2. 需要展示标准化收入来源选项的业务场景
     * @implNote 实现细节：
     * 1. 通过 IncomeSource.getAllSources() 获取枚举键值对集合
     * 2. 遍历转换每个枚举项为 LabelEntity 对象
     * 3. 保持选项顺序与枚举定义顺序一致
     * @version 1.0
     * @author 系统作者
     * @see IncomeSourceEnum 收入来源枚举类，包含所有预定义的来源类型
     * @see LabelEntity 标准标签实体，用于统一前端选项数据结构
     */
    @Override
    public List<LabelEntity> getSourceOptions() {
        Map<String, String> allSources = IncomeSourceEnum.getAllSources();
        List<LabelEntity> sourceOptions = new ArrayList<>();
        // 遍历枚举类中的所有收入来源，将其转换为LabelEntity对象并添加到列表中
        for (Map.Entry<String, String> entry : allSources.entrySet()) {
            LabelEntity labelEntity = new LabelEntity(entry.getValue(), entry.getKey());
            sourceOptions.add(labelEntity);
        }
        return sourceOptions;
    }


}
