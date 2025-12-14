package com.make.finance.service.impl;

import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

import com.make.common.utils.ThreadPoolUtil;
import com.make.finance.domain.Income;
import com.make.finance.service.IIncomeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.SalaryRecordMapper;
import com.make.finance.domain.SalaryRecord;
import com.make.finance.service.ISalaryRecordService;

/**
 * 员工工资明细Service业务层处理
 *
 * @author erqi
 * @date 2025-05-29
 */
@Service
public class SalaryRecordServiceImpl implements ISalaryRecordService {

    /**
     * 日志记录器
     */
    private static final Logger log = LoggerFactory.getLogger(SalaryRecordServiceImpl.class);


    @Autowired
    private SalaryRecordMapper salaryRecordMapper;

    @Autowired
    private IIncomeService incomeService;

    /**
     * 查询员工工资明细
     *
     * @param id 员工工资明细主键
     * @return 员工工资明细
     */
    @Override
    public SalaryRecord selectSalaryRecordById(Long id) {
        return salaryRecordMapper.selectSalaryRecordById(id);
    }

    /**
     * 查询员工工资明细列表
     *
     * @param salaryRecord 员工工资明细
     * @return 员工工资明细
     */
    @Override
    public List<SalaryRecord> selectSalaryRecordList(SalaryRecord salaryRecord) {
        return salaryRecordMapper.selectSalaryRecordList(salaryRecord);
    }

    /**
     * 新增员工工资明细
     *
     * @param salaryRecord 员工工资明细
     * @return 结果
     */
    @Override
    public int insertSalaryRecord(SalaryRecord salaryRecord) {
        // 使用 ThreadPoolUtil 默认核心线程池执行异步任务
        ThreadPoolUtil.getCoreExecutor().submit(() -> {
            insertOrUpdateIncome(salaryRecord);
        });
        return salaryRecordMapper.insertSalaryRecord(salaryRecord);
    }

    /**
     * 修改员工工资明细
     *
     * @param salaryRecord 员工工资明细
     * @return 结果
     */
    @Override
    public int updateSalaryRecord(SalaryRecord salaryRecord) {
        salaryRecord.setNetSalary(
                salaryRecord.getPayableSalary()
                        .subtract(salaryRecord.getTotalDeduction())
                        .setScale(2, RoundingMode.HALF_UP));
        // 使用 ThreadPoolUtil 默认核心线程池执行异步任务
        ThreadPoolUtil.getCoreExecutor().submit(() -> {
            insertOrUpdateIncome(salaryRecord);
        });
        return salaryRecordMapper.updateSalaryRecord(salaryRecord);
    }

    /**
     * 批量删除员工工资明细
     *
     * @param ids 需要删除的员工工资明细主键
     * @return 结果
     */
    @Override
    public int deleteSalaryRecordByIds(Long[] ids) {
        return salaryRecordMapper.deleteSalaryRecordByIds(ids);
    }

    /**
     * 删除员工工资明细信息
     *
     * @param id 员工工资明细主键
     * @return 结果
     */
    @Override
    public int deleteSalaryRecordById(Long id) {
        return salaryRecordMapper.deleteSalaryRecordById(id);
    }

    /**
     * 复制指定ID的员工工资记录
     *
     * @param id 原始工资记录ID
     * @return 插入条数（1为成功，0为失败）
     */
    @Override
    public int copySalaryRecordById(Long id) {
        if (id == null) {
            log.warn("复制工资记录失败：ID为空");
            return 0;
        }

        try {
            SalaryRecord salaryRecord = salaryRecordMapper.selectSalaryRecordById(id);
            if (salaryRecord == null) {
                log.warn("复制工资记录失败：未找到ID为 {} 的记录", id);
                return 0;
            }

            // 清除原始记录中的主键 ID 和创建/修改信息
            salaryRecord.setId(null);
            salaryRecord.setCreateTime(null);
            salaryRecord.setIssueDate(new Date());
            salaryRecord.setUpdateTime(null);  // 可根据需要保留或重置

            int rows = salaryRecordMapper.insertSalaryRecord(salaryRecord);
            log.info("成功复制工资记录，原ID={}，新记录内容={}", id, salaryRecord);
            return rows;

        } catch (Exception e) {
            log.error("复制工资记录异常，ID={}，错误={}", id, e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 插入或更新用户的收入记录。
     *
     * 该方法根据用户名和当前时间查询是否存在相应的收入记录。如果记录存在，则更新收入数据；
     * 如果记录不存在，则插入一条新的收入记录。
     *
     * @param salaryRecord 需要插入或更新的收入记录，包括用户 ID 和其他收入相关信息
     */
    private void insertOrUpdateIncome(SalaryRecord salaryRecord) {
        // 1. 获取当前时间，设置为每个月的15号
        // 2. 根据用户 ID 和查询日期，查询是否存在收入记录
        Income income = incomeService.getIncomeByUserIdAndDate(salaryRecord.getUserId(), salaryRecord.getIssueDate());

        // 3. 如果收入记录已存在，则进行更新操作
        if (income != null) {
            // 更新收入记录
            income.setAmount(salaryRecord.getNetSalary()); // 假设 SalaryRecord 包含金额字段
            income.setUpdateTime(new Date()); // 更新时间为当前时间
            incomeService.updateIncome(income);  // 更新收入记录
        } else {
            // 4. 如果收入记录不存在，则进行插入操作
            income = new Income();
            income.setUserId(salaryRecord.getUserId());
            income.setAmount(salaryRecord.getNetSalary()); // 设置收入金额
            income.setSource("0");
            income.setIncomeDate(new Date()); // 设置收入时间
            incomeService.insertIncome(income);  // 插入新的收入记录
        }
    }

}
