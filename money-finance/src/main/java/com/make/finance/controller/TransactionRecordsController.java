package com.make.finance.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletResponse;

import com.make.common.utils.file.FileUploadUtils;
import com.make.finance.domain.dto.AliPayment;
import com.make.finance.domain.dto.WeChatTransaction;
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
 * @author è´°æ
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
            // 过滤和转换解析后的数据
            List<TransactionRecords> records = new ArrayList<>();
            if (savedFile.getName().startsWith("微信")) {
                // 处理保存的文件，解析 CSV 数据
                List<WeChatTransaction> weChatTransactions = CSVUtil.easyExcelParseWeChatCsv(savedFile);

                if (weChatTransactions.size() > 0) {
                    // 过滤和转换解析后的数据
                    List<TransactionRecords> recordWeChat = weChatTransactions.stream()
                            .filter(weChatTransaction -> weChatTransaction.getProduct() != null)
                            .map(this::convertToWeChatTransactionRecord)
                            .collect(Collectors.toList());
                    records.addAll(recordWeChat);

                }
            }
            if (savedFile.getName().startsWith("alipay")) {
                // 处理保存的文件，解析 CSV 数据
                List<AliPayment> aliPayTransactions = CSVUtil.easyExcelParseAlipayCsv(savedFile);
                if (aliPayTransactions.size() > 0) {
                    // 过滤和转换解析后的数据
                    List<TransactionRecords> recordWeChat = aliPayTransactions.stream()
                            .filter(aliPayTransaction -> aliPayTransaction.getTransactionType() != null) // 过滤掉产品为空的记录
                            .map(this::convertToAliPayTransactionRecord) // 转换为 TransactionRecords 对象
                            .collect(Collectors.toList()); // 收集到列表中
                    records.addAll(recordWeChat);

                }

            }


            // 如果转换后的记录不为空，进行后续处理
            if (!records.isEmpty()) {
                // transactionRecordsService.insertTransactionRecordsBatch(records); // 使用批量插入方法
                for (TransactionRecords record : records) {
                    if (record.getProduct().contains("一卡通充值") || record.getProduct().contains("12306消费") || record.getTransactionType().contains("滴滴") || record.getTransactionType().contains("中铁")) {
                        record.setProductType("交通出行");
                    }
                    if (record.getProduct().contains("衣") || record.getProduct().contains("口红") || record.getProduct().contains("唇")
                            || record.getProduct().contains("靴") || record.getProduct().contains("唯品会") || record.getTransactionType().contains("唯品会")) {
                        record.setProductType("服饰装扮");
                    }
                    if (record.getProduct().contains("扫二维码") || record.getTransactionType().contains("平台商户") || record.getTransactionType().contains("拼多多")) {
                        record.setProductType("商户消费");
                    }
                    if (record.getProduct().contains("交费")) {
                        record.setProductType("手机话费");
                    }
                    if (record.getProduct().contains("租房订单")) {
                        record.setProductType("租房费用");
                    }
                    if (record.getProduct().contains("转账") || record.getProduct().contains("红包") || record.getTransactionType().contains("转账") || record.getTransactionType().contains("红包")) {
                        record.setProductType("转账");
                    }
                    record.setUserId(userId);
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

    /**
     * 将 WeChatTransaction 转换为 TransactionRecords 的方法。
     *
     * @param weChatTransaction 待转换的 WeChatTransaction 对象
     * @return 转换后的 TransactionRecords 对象
     */
    private TransactionRecords convertToWeChatTransactionRecord(WeChatTransaction weChatTransaction) {
        // 创建新的 TransactionRecords 对象
        TransactionRecords transactionRecords = new TransactionRecords();

        // 设置交易时间，注意需要解析字符串为 Date
        if (weChatTransaction.getTransactionTime() != null) {
            try {
                // 假设交易时间是字符串格式，需要转换为 Date 类型
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = dateFormat.parse(weChatTransaction.getTransactionTime());
                transactionRecords.setTransactionTime(date);
            } catch (ParseException e) {
                e.printStackTrace(); // 处理解析异常
            }
        }

        // 设置其他字段
        transactionRecords.setTransactionType(weChatTransaction.getTransactionType());
        transactionRecords.setCounterparty(weChatTransaction.getCounterparty());
        transactionRecords.setProduct(weChatTransaction.getProduct());
        transactionRecords.setInOut(weChatTransaction.getInOut());

        // 金额转换，假设 WeChatTransaction 的金额是字符串，需转换为 BigDecimal
        String amtStr = (weChatTransaction != null) ? weChatTransaction.getAmount() : null;
        if (weChatTransaction.getAmount() != null) {
            BigDecimal amount;
            if (amtStr == null || amtStr.trim().isEmpty()) {
                amount = BigDecimal.ZERO;
            } else {
                // 移除所有非数字、非-、非.字符，例如 ￥、¥、逗号、空格等
                String cleaned = amtStr.trim().replaceAll("[^0-9\\.-]", "");
                if (cleaned.isEmpty()) {
                    amount = BigDecimal.ZERO;
                } else {
                    try {
                        amount = new BigDecimal(cleaned).setScale(6, RoundingMode.HALF_UP);
                    } catch (NumberFormatException ex) {
                        amount = BigDecimal.ZERO;
                    }
                }
            }

            transactionRecords.setAmount(amount);
//            transactionRecords.setAmount(new BigDecimal(weChatTransaction.getAmount().replace("¥", "")));
        }

        transactionRecords.setPaymentMethod(weChatTransaction.getPaymentMethod());
        transactionRecords.setTransactionStatus(weChatTransaction.getTransactionStatus());
        transactionRecords.setTransactionId(weChatTransaction.getTransactionId());
        transactionRecords.setMerchantId(weChatTransaction.getMerchantId());
        transactionRecords.setNote(weChatTransaction.getNote());
        transactionRecords.setSource("微信");
        transactionRecords.setProductType(weChatTransaction.getTransactionType());
        // 返回转换后的 TransactionRecords 对象
        return transactionRecords;
    }

    /**
     * 将 AliPayment 转换为 TransactionRecords 的方法。
     *
     * @param aliPayment 待转换的 WeChatTransaction 对象
     * @return 转换后的 TransactionRecords 对象
     */
    private TransactionRecords convertToAliPayTransactionRecord(AliPayment aliPayment) {
        // 创建新的 TransactionRecords 对象
        TransactionRecords transactionRecords = new TransactionRecords();

        // 设置交易时间，注意需要解析字符串为 Date
        if (aliPayment.getTransactionTime() != null) {
            try {
                // 假设交易时间是字符串格式，需要转换为 Date 类型
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
                Date date = dateFormat.parse(aliPayment.getTransactionTime());
                transactionRecords.setTransactionTime(date);
            } catch (ParseException e) {
                e.printStackTrace(); // 处理解析异常
            }
        }

        // 设置其他字段
        transactionRecords.setTransactionType(aliPayment.getTransactionType());
        transactionRecords.setCounterparty(aliPayment.getCounterparty());
        transactionRecords.setProduct(aliPayment.getProductDescription());
        transactionRecords.setInOut(aliPayment.getInOut());

        // 金额转换，假设 WeChatTransaction 的金额是字符串，需转换为 BigDecimal
        String amtStr = (aliPayment != null) ? aliPayment.getAmount() : null;
        if (aliPayment.getAmount() != null) {
            BigDecimal amount;
            if (amtStr == null || amtStr.trim().isEmpty()) {
                amount = BigDecimal.ZERO;
            } else {
                // 移除所有非数字、非-、非.字符，例如 ￥、¥、逗号、空格等
                String cleaned = amtStr.trim().replaceAll("[^0-9\\.-]", "");
                if (cleaned.isEmpty()) {
                    amount = BigDecimal.ZERO;
                } else {
                    try {
                        amount = new BigDecimal(cleaned).setScale(6, RoundingMode.HALF_UP);
                    } catch (NumberFormatException ex) {
                        amount = BigDecimal.ZERO;
                    }
                }
            }
            transactionRecords.setAmount(amount);
//            transactionRecords.setAmount(new BigDecimal(aliPayment.getAmount().replace("¥", "")));
        }

        transactionRecords.setPaymentMethod(aliPayment.getPaymentMethod());
        transactionRecords.setCounterpartyAccount(aliPayment.getCounterpartyAccount());
        transactionRecords.setTransactionStatus(aliPayment.getTransactionStatus());
        transactionRecords.setTransactionId(aliPayment.getTransactionOrderId());
        transactionRecords.setMerchantId(aliPayment.getMerchantOrderId());
        transactionRecords.setNote(aliPayment.getNote());
        transactionRecords.setSource("支付宝");
        transactionRecords.setProductType(aliPayment.getTransactionType());
        // 返回转换后的 TransactionRecords 对象
        return transactionRecords;
    }

}
