package com.make.finance.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.servlet.http.HttpServletResponse;

import com.make.common.utils.file.FileUploadUtils;
import com.make.finance.utils.CSVUtil;
import com.make.finance.utils.OSValidatorUtil;
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
import com.make.finance.domain.TransactionRecords;
import com.make.finance.service.ITransactionRecordsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 微信支付宝流水Controller
 *
 * @author è´°æŸ’
 * @date 2025-05-27
 */
@RestController
@RequestMapping("/finance/weChatRecords")
public class TransactionRecordsController extends BaseController {

    @Autowired
    private ITransactionRecordsService transactionRecordsService;


    // 定义linux上传文件的存储目录
    private static final String LINUX_UPLOAD_DIR = "/home/app/dataDir";
    // 定义windows上传文件的存储目录
    private static final String WIN_UPLOAD_DIR = "D:\\home\\app\\dataDir";


    /**
     * 查询微信支付宝流水列表
     */
    @PreAuthorize("@ss.hasPermi('finance:weChatRecords:list')")
    @GetMapping("/list")
    public TableDataInfo list(TransactionRecords transactionRecords) {
        startPage();
        boolean equals = transactionRecords.getUserId().equals(1L);
        if (equals) {
            transactionRecords.setUserId(null);
        }
        List<TransactionRecords> list = transactionRecordsService.selectTransactionRecordsList(transactionRecords);
        return getDataTable(list);
    }

    /**
     * 导出微信支付宝流水列表
     */
    @PreAuthorize("@ss.hasPermi('finance:weChatRecords:export')")
    @Log(title = "微信支付宝流水", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, TransactionRecords transactionRecords) {
        List<TransactionRecords> list = transactionRecordsService.selectTransactionRecordsList(transactionRecords);
        ExcelUtil<TransactionRecords> util = new ExcelUtil<TransactionRecords>(TransactionRecords.class);
        util.exportExcel(response, list, "微信支付宝流水数据");
    }

    /**
     * 获取微信支付宝流水详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:weChatRecords:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(transactionRecordsService.selectTransactionRecordsById(id));
    }

    /**
     * 新增微信支付宝流水
     */
    @PreAuthorize("@ss.hasPermi('finance:weChatRecords:add')")
    @Log(title = "微信支付宝流水", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TransactionRecords transactionRecords) {
        return toAjax(transactionRecordsService.insertTransactionRecords(transactionRecords));
    }

    /**
     * 修改微信支付宝流水
     */
    @PreAuthorize("@ss.hasPermi('finance:weChatRecords:edit')")
    @Log(title = "微信支付宝流水", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody TransactionRecords transactionRecords) {
        return toAjax(transactionRecordsService.updateTransactionRecords(transactionRecords));
    }

    /**
     * 删除微信支付宝流水
     */
    @PreAuthorize("@ss.hasPermi('finance:weChatRecords:remove')")
    @Log(title = "微信支付宝流水", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(transactionRecordsService.deleteTransactionRecordsByIds(ids));
    }

    /**
     * 导入数据的方法，处理文件上传和解析。
     *
     * @param multipartFile 上传的文件，类型为 MultipartFile
     * @return AjaxResult 返回操作结果，包含成功或失败信息
     */
    @PreAuthorize("@ss.hasPermi('finance:weChatRecords:import')") // 权限校验，确保用户有权限进行导入操作
    @Log(title = "流水解析", businessType = BusinessType.IMPORT) // 日志记录，记录导入操作
    @PostMapping("/import") // 映射 POST 请求到 /import 路径
    public AjaxResult importData(@RequestParam("file") MultipartFile multipartFile,
                                 @RequestParam("userId") Long userId) {
        // 检查上传的文件是否为空
        if (multipartFile.isEmpty()) {
            return AjaxResult.error("上传文件不能为空");
        }

        File savedFile = null;

        boolean windows = OSValidatorUtil.isWindows();
        String savePath;
        if (windows) {
            savePath = WIN_UPLOAD_DIR;
        } else {
            savePath = LINUX_UPLOAD_DIR;
        }
        try {
            // 调用工具类将文件上传到指定目录
            FileUploadUtils.upload(savePath, multipartFile);

            // 创建文件保存路径
            Path uploadPath = Paths.get(savePath, multipartFile.getOriginalFilename());
            savedFile = uploadPath.toFile();

            // 将上传的文件保存到指定路径
            multipartFile.transferTo(savedFile);

            // 使用统一的智能解析器
            // 自动解析并应用分类逻辑和 userId
            List<TransactionRecords> records = CSVUtil.parse(savedFile, userId);

            // 如果转换后的记录不为空，进行后续处理
            if (!records.isEmpty()) {
                for (TransactionRecords record : records) {
                    transactionRecordsService.insertTransactionRecords(record); // 使用批量插入方法
                }
            }

            // 返回成功信息，包含导入的数据条数
            return AjaxResult.success("导入成功，数据条数：" + records.size());
        } catch (IOException e) {
            // 处理 IO 异常，返回错误信息
            return AjaxResult.error("导入失败：文件读取错误：" + e.getMessage());
        } finally {
            // 可选：处理完后删除临时文件，如果文件不再需要
            if (savedFile != null && savedFile.exists()) {
                // savedFile.delete(); // 仅在必要时删除
            }
        }
    }

}
