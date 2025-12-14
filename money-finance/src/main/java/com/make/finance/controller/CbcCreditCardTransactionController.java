package com.make.finance.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.make.common.utils.file.EasyExcelUtil;
import com.make.common.utils.file.FileUploadUtils;
import com.make.finance.domain.BankCardTransactions;
import com.make.finance.domain.dto.CbcCardTransaction;
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
import com.make.finance.domain.CbcCreditCardTransaction;
import com.make.finance.service.ICbcCreditCardTransactionService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 建行信用卡交易记录Controller
 *
 * @author 贰柒
 * @date 2025-05-26
 */
@RestController
@RequestMapping("/finance/ccb_credit_card_transaction")
public class CbcCreditCardTransactionController extends BaseController {

    // 定义上传文件的存储目录
    private static final String UPLOAD_DIR = "D:\\home\\app\\dataDir";

    /**
     * 定义文件的存储目录
     */
    private static final String BIZ_DIR = "D:\\home\\app\\dataBizDir";

    @Autowired
    private ICbcCreditCardTransactionService cbcCreditCardTransactionService;

    /**
     * 查询建行信用卡交易记录列表
     */
    @PreAuthorize("@ss.hasPermi('finance:ccb_credit_card_transaction:list')")
    @GetMapping("/list")
    public TableDataInfo list(CbcCreditCardTransaction cbcCreditCardTransaction) {
        startPage();
        List<CbcCreditCardTransaction> list = cbcCreditCardTransactionService.selectCbcCreditCardTransactionList(cbcCreditCardTransaction);
        return getDataTable(list);
    }

    /**
     * 导出建行信用卡交易记录列表
     */
    @PreAuthorize("@ss.hasPermi('finance:ccb_credit_card_transaction:export')")
    @Log(title = "建行信用卡交易记录", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, CbcCreditCardTransaction cbcCreditCardTransaction) {
        List<CbcCreditCardTransaction> list = cbcCreditCardTransactionService.selectCbcCreditCardTransactionList(cbcCreditCardTransaction);
        ExcelUtil<CbcCreditCardTransaction> util = new ExcelUtil<CbcCreditCardTransaction>(CbcCreditCardTransaction.class);
        util.exportExcel(response, list, "建行信用卡交易记录数据");
    }

    /**
     * 获取建行信用卡交易记录详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:ccb_credit_card_transaction:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(cbcCreditCardTransactionService.selectCbcCreditCardTransactionById(id));
    }

    /**
     * 新增建行信用卡交易记录
     */
    @PreAuthorize("@ss.hasPermi('finance:ccb_credit_card_transaction:add')")
    @Log(title = "建行信用卡交易记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CbcCreditCardTransaction cbcCreditCardTransaction) {
        return toAjax(cbcCreditCardTransactionService.insertCbcCreditCardTransaction(cbcCreditCardTransaction));
    }

    /**
     * 修改建行信用卡交易记录
     */
    @PreAuthorize("@ss.hasPermi('finance:ccb_credit_card_transaction:edit')")
    @Log(title = "建行信用卡交易记录", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody CbcCreditCardTransaction cbcCreditCardTransaction) {
        return toAjax(cbcCreditCardTransactionService.updateCbcCreditCardTransaction(cbcCreditCardTransaction));
    }

    /**
     * 删除建行信用卡交易记录
     */
    @PreAuthorize("@ss.hasPermi('finance:ccb_credit_card_transaction:remove')")
    @Log(title = "建行信用卡交易记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(cbcCreditCardTransactionService.deleteCbcCreditCardTransactionByIds(ids));
    }

    /**
     * 导入数据的方法，处理文件上传和解析。
     *
     * @param multipartFile 上传的文件，类型为 MultipartFile
     * @return AjaxResult 返回操作结果，包含成功或失败信息
     */
    @PreAuthorize("@ss.hasPermi('finance:ccb_credit_card_transaction:import')") // 权限校验，确保用户有权限进行导入操作
    @Log(title = "流水解析", businessType = BusinessType.IMPORT) // 日志记录，记录导入操作
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
            List<CbcCardTransaction> cbcCardTransactionList = EasyExcelUtil.readExcel(savedFile.getPath(), CbcCardTransaction.class);

            if (CollectionUtils.isEmpty(cbcCardTransactionList)) {
                return AjaxResult.error("导入失败：不支持的文件");
            } else {
                //存储数据库
                for (CbcCardTransaction cbcCardTransaction : cbcCardTransactionList) {
                    CbcCreditCardTransaction cbcCreditCardTransaction = new CbcCreditCardTransaction();
                    cbcCreditCardTransaction.setPostDate(cbcCardTransaction.getPostDate());
                    cbcCreditCardTransaction.setTradeDate(cbcCardTransaction.getTradeDate());
                    cbcCreditCardTransaction.setCardLast4(cbcCardTransaction.getCardLast4Digits());
                    cbcCreditCardTransaction.setDescription(cbcCardTransaction.getDescription());
                    cbcCreditCardTransaction.setRemark(cbcCardTransaction.getTransactionType());
                    cbcCreditCardTransaction.setTransAmount(cbcCardTransaction.getTransactionAmount());
                    cbcCreditCardTransaction.setSettleAmount(cbcCardTransaction.getSettlementAmount());

                    cbcCreditCardTransactionService.insertCbcCreditCardTransaction(cbcCreditCardTransaction);
                }
            }
            // 返回成功信息，包含导入的数据条数
            return AjaxResult.success("导入成功，数据条数：");
        } catch (IOException e) {
//             处理 IO 异常，返回错误信息
            return AjaxResult.error("导入失败：文件读取错误：" + e.getMessage());
        } finally {
//             可选：处理完后删除临时文件，如果文件不再需要
            if (savedFile != null && savedFile.exists()) {
                // savedFile.delete(); // 仅在必要时删除
            }
        }
    }
}
