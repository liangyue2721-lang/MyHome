package com.make.finance.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.make.common.utils.file.EasyExcelUtil;
import com.make.common.utils.file.FileUploadUtils;
import com.make.finance.domain.vo.LoanRepaymentsChart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
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


    // 定义上传文件的存储目录
    private static final String UPLOAD_DIR = "D:\\home\\app\\dataDir";

    /**
     * 定义文件的存储目录
     */
    private static final String BIZ_DIR = "D:\\home\\app\\dataBizDir";


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
    @PreAuthorize("@ss.hasPermi('stock:repayments:import')") // 权限校验，确保用户有权限进行导入操作
    @Log(title = "更新贷款", businessType = BusinessType.IMPORT) // 日志记录，记录导入操作
    @PostMapping("/import") // 映射 POST 请求到 /import 路径
    public AjaxResult importData(@RequestParam("file") MultipartFile multipartFile) {
        // 检查上传的文件是否为空
        if (multipartFile.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }

        File savedFile = null;
        try {
            // 调用工具类将文件上传到指定目录
            FileUploadUtils.upload(UPLOAD_DIR, multipartFile);

            // 创建文件保存路径
            Path uploadPath = Paths.get(UPLOAD_DIR, multipartFile.getOriginalFilename());
            savedFile = uploadPath.toFile();

            // 将上传的文件保存到指定路径
            multipartFile.transferTo(savedFile);
            //解析Excel
            List<Object> loanRepayments = EasyExcelUtil.readExcel(savedFile.getPath());
            //数据转实体

            if (CollectionUtils.isEmpty(loanRepayments)) {
                return AjaxResult.error("导入失败：不支持的文件");
            } else {
                //根据ID，更新数据库
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
                for (Object loanRepayment : loanRepayments) {
                    try {
                        JSONObject jsonObject = JSONObject.from(loanRepayment);
                        LoanRepayments loanRepaymentsObj = new LoanRepayments(
                                Long.valueOf(jsonObject.get(0).toString()),
                                LocalDate.parse(jsonObject.get(1).toString(), formatter),
                                new BigDecimal(jsonObject.get(2).toString().replace(",", "")),
                                new BigDecimal(jsonObject.get(3).toString().replace(",", "")),
                                new BigDecimal(jsonObject.get(4).toString().replace(",", ""))
                        );
                        int update = loanRepaymentsService.updateLoanRepaymentsById(loanRepaymentsObj);
                        log.info("导入更新贷款月供金额成功{}{}", update, JSON.toJSONString(loanRepaymentsObj));
                    } catch (Exception e) {
                        log.error("导入更新贷款月供金额失败{}{}", e, JSON.toJSONString(loanRepayment));
                    }
                }
            }
            // 返回成功信息，包含导入的数据条数
            return AjaxResult.success("导入成功，数据条数：");
        } catch (IOException e) {
            // 处理 IO 异常，返回错误信息
            return AjaxResult.error("导入失败：文件读取错误：" + e.getMessage());
        } finally {
            //  可选：处理完后删除临时文件，如果文件不再需要
            if (savedFile != null && savedFile.exists()) {
                // savedFile.delete(); // 仅在必要时删除
            }
        }
    }

    @PreAuthorize("@ss.hasPermi('finance:loan_repayments:list')")
    @GetMapping("/getLoanRepaymentsLineChart/{userId}")
    public List<LoanRepaymentsChart> getLoanRepaymentsLineChart(@PathVariable Long userId) {
        return loanRepaymentsService.queryLoanRepaymentsChartList(userId);
    }
}
