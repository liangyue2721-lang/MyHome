package com.make.finance.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSON;
import com.make.common.utils.SecurityUtils;
import com.make.finance.domain.vo.LoanRepaymentsChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.make.common.annotation.Log;
import com.make.common.core.controller.BaseController;
import com.make.common.core.domain.AjaxResult;
import com.make.common.enums.BusinessType;
import com.make.finance.domain.LoanRepayments;
import com.make.finance.service.ILoanRepaymentsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 贷款剩余计算Controller
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@RestController
@RequestMapping("/finance/loan_repayments")
public class LoanRepaymentsController extends BaseController {

    // 日志记录器
    private static final Logger log = LoggerFactory.getLogger(LoanRepaymentsController.class);

    @Autowired
    private ILoanRepaymentsService loanRepaymentsService;

    /**
     * 查询贷款剩余计算列表
     */
    @PreAuthorize("@ss.hasPermi('finance:loan_repayments:list')")
    @GetMapping("/list")
    public TableDataInfo list(LoanRepayments loanRepayments) {
        startPage();
        List<LoanRepayments> list = loanRepaymentsService.selectLoanRepaymentsList(loanRepayments);
        return getDataTable(list);
    }

    /**
     * 导出贷款剩余计算列表
     */
    @PreAuthorize("@ss.hasPermi('finance:loan_repayments:export')")
    @Log(title = "贷款剩余计算", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, LoanRepayments loanRepayments) {
        List<LoanRepayments> list = loanRepaymentsService.selectLoanRepaymentsList(loanRepayments);
        ExcelUtil<LoanRepayments> util = new ExcelUtil<LoanRepayments>(LoanRepayments.class);
        util.exportExcel(response, list, "贷款剩余计算数据");
    }

    /**
     * 获取贷款剩余计算详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:loan_repayments:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(loanRepaymentsService.selectLoanRepaymentsById(id));
    }

    /**
     * 新增贷款剩余计算
     */
    @PreAuthorize("@ss.hasPermi('finance:loan_repayments:add')")
    @Log(title = "贷款剩余计算", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody LoanRepayments loanRepayments) {
        return toAjax(loanRepaymentsService.insertLoanRepayments(loanRepayments));
    }

    /**
     * 修改贷款剩余计算
     */
    @PreAuthorize("@ss.hasPermi('finance:loan_repayments:edit')")
    @Log(title = "贷款剩余计算", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody LoanRepayments loanRepayments) {
        return toAjax(loanRepaymentsService.updateLoanRepayments(loanRepayments));
    }

    /**
     * 删除贷款剩余计算
     */
    @PreAuthorize("@ss.hasPermi('finance:loan_repayments:remove')")
    @Log(title = "贷款剩余计算", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(loanRepaymentsService.deleteLoanRepaymentsByIds(ids));
    }

    /**
     * 导入数据的方法，处理文件上传和解析。
     *
     * @param multipartFile 上传的文件，类型为 MultipartFile
     * @return AjaxResult 返回操作结果，包含成功或失败信息
     */
    @PreAuthorize("@ss.hasPermi('finance:loan_repayments:import')") // 权限校验，确保用户有权限进行导入操作
    @Log(title = "更新贷款", businessType = BusinessType.IMPORT) // 日志记录，记录导入操作
    @PostMapping("/import") // 映射 POST 请求到 /import 路径
    public AjaxResult importData(@RequestParam("file") MultipartFile multipartFile) {
        // 检查上传的文件是否为空
        if (multipartFile.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }

        try {
            // 直接读取流
            List<Map<Integer, String>> list = EasyExcel.read(multipartFile.getInputStream())
                    .sheet()
                    .doReadSync();

            if (list == null || list.isEmpty()) {
                return AjaxResult.error("导入失败：文件为空");
            }

            Long userId = SecurityUtils.getUserId();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            // 支持常见格式
            DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd")
            };

            int successCount = 0;
            for (Map<Integer, String> row : list) {
                // 跳过可能的标题行，尝试解析第一列是否为数字
                // 如果第一列包含非数字字符（如 "期数"），则跳过
                if (row.get(0) == null || !row.get(0).matches("\\d+")) {
                    continue;
                }

                try {
                    // 0: 期数
                    Long term = Long.valueOf(row.get(0));

                    // 1: 还款日期
                    String dateStr = row.get(1);
                    LocalDate date = null;
                    if (dateStr != null) {
                        for (DateTimeFormatter fmt : formatters) {
                            try {
                                date = LocalDate.parse(dateStr, fmt);
                                break;
                            } catch (Exception e) {
                                // ignore
                            }
                        }
                    }
                    if (date == null) {
                        log.warn("无法解析日期: {}", dateStr);
                        continue;
                    }

                    // 2: 应还本金
                    BigDecimal principal = new BigDecimal(row.get(2).replace(",", ""));
                    // 3: 应还利息
                    BigDecimal interest = new BigDecimal(row.get(3).replace(",", ""));
                    // 4: 贴息金额 (跳过)
                    // 5: 本息合计
                    BigDecimal total = new BigDecimal(row.get(5).replace(",", ""));

                    LoanRepayments loanRepaymentsObj = new LoanRepayments(
                            term,
                            date,
                            principal,
                            interest,
                            total
                    );
                    loanRepaymentsObj.setUserId(userId);

                    // Update
                    int update = loanRepaymentsService.updateLoanRepaymentsById(loanRepaymentsObj);
                    if (update > 0) {
                        successCount++;
                    }
                } catch (Exception e) {
                    log.error("导入行处理失败: {}", JSON.toJSONString(row), e);
                }
            }

            return AjaxResult.success("导入成功，更新条数：" + successCount);
        } catch (IOException e) {
            return AjaxResult.error("导入失败：IO错误 " + e.getMessage());
        } catch (Exception e) {
            log.error("导入异常", e);
            return AjaxResult.error("导入失败：" + e.getMessage());
        }
    }

    @PreAuthorize("@ss.hasPermi('finance:loan_repayments:list')")
    @GetMapping("/getLoanRepaymentsLineChart")
    public List<LoanRepaymentsChart> getLoanRepaymentsLineChart() {
        return loanRepaymentsService.queryLoanRepaymentsChartList(SecurityUtils.getUserId());
    }
}
