package com.make.finance.mapper;

import java.util.List;

import com.make.finance.domain.SalaryRecord;

/**
 * 员工工资明细Mapper接口
 *
 * @author erqi
 * @date 2025-05-29
 */
public interface SalaryRecordMapper {

    /**
     * 查询员工工资明细
     *
     * @param id 员工工资明细主键
     * @return 员工工资明细
     */
    public SalaryRecord selectSalaryRecordById(Long id);

    /**
     * 查询员工工资明细列表
     *
     * @param salaryRecord 员工工资明细
     * @return 员工工资明细集合
     */
    public List<SalaryRecord> selectSalaryRecordList(SalaryRecord salaryRecord);

    /**
     * 新增员工工资明细
     *
     * @param salaryRecord 员工工资明细
     * @return 结果
     */
    public int insertSalaryRecord(SalaryRecord salaryRecord);

    /**
     * 修改员工工资明细
     *
     * @param salaryRecord 员工工资明细
     * @return 结果
     */
    public int updateSalaryRecord(SalaryRecord salaryRecord);

    /**
     * 删除员工工资明细
     *
     * @param id 员工工资明细主键
     * @return 结果
     */
    public int deleteSalaryRecordById(Long id);

    /**
     * 批量删除员工工资明细
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteSalaryRecordByIds(Long[] ids);
}
