package com.make.finance.controller;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import com.make.common.utils.file.EasyExcelUtil;
import com.make.common.utils.file.PdfUtil;
import com.alibaba.fastjson2.JSONObject;
import com.make.common.utils.file.FileUploadUtils;
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
import com.make.finance.domain.BankCardTransactions;
import com.make.finance.service.IBankCardTransactionsService;
import com.make.common.utils.poi.ExcelUtil;
import com.make.common.core.page.TableDataInfo;
import org.springframework.web.multipart.MultipartFile;

/**
 * 银行流水Controller
 *
 * @author erqi
 * @date 2025-05-27
 */
@RestController
@RequestMapping("/finance/bankTransactions")
public class BankCardTransactionsController extends BaseController {
    @Autowired
    private IBankCardTransactionsService bankCardTransactionsService;

    // 定义上传文件的存储目录
    private static final String UPLOAD_DIR = "D:\\home\\app\\dataDir";

    /**
     * 定义文件的存储目录
     */
    private static final String BIZ_DIR = "D:\\home\\app\\dataBizDir";

    /**
     * 查询银行卡流水解析列表
     */
    @PreAuthorize("@ss.hasPermi('finance:bankTransactions:list')")
    @GetMapping("/list")
    public TableDataInfo list(BankCardTransactions bankCardTransactions) {
        startPage();
        List<BankCardTransactions> list = bankCardTransactionsService.selectBankCardTransactionsList(bankCardTransactions);
        return getDataTable(list);
    }

    /**
     * 导出银行卡流水解析列表
     */
    @PreAuthorize("@ss.hasPermi('finance:bankTransactions:export')")
    @Log(title = "银行卡流水解析", businessType = BusinessType.EXPORT)
    @PostMapping("/export")
    public void export(HttpServletResponse response, BankCardTransactions bankCardTransactions) {
        List<BankCardTransactions> list = bankCardTransactionsService.selectBankCardTransactionsList(bankCardTransactions);
        ExcelUtil<BankCardTransactions> util = new ExcelUtil<BankCardTransactions>(BankCardTransactions.class);
        util.exportExcel(response, list, "银行卡流水解析数据");
    }

    /**
     * 获取银行卡流水解析详细信息
     */
    @PreAuthorize("@ss.hasPermi('finance:bankTransactions:query')")
    @GetMapping(value = "/{id}")
    public AjaxResult getInfo(@PathVariable("id") Long id) {
        return success(bankCardTransactionsService.selectBankCardTransactionsById(id));
    }

    /**
     * 新增银行卡流水解析
     */
    @PreAuthorize("@ss.hasPermi('finance:bankTransactions:add')")
    @Log(title = "银行卡流水解析", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BankCardTransactions bankCardTransactions) {
        return toAjax(bankCardTransactionsService.insertBankCardTransactions(bankCardTransactions));
    }

    /**
     * 修改银行卡流水解析
     */
    @PreAuthorize("@ss.hasPermi('finance:transactions:edit')")
    @Log(title = "银行卡流水解析", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody BankCardTransactions bankCardTransactions) {
        return toAjax(bankCardTransactionsService.updateBankCardTransactions(bankCardTransactions));
    }

    /**
     * 删除银行卡流水解析
     */
    @PreAuthorize("@ss.hasPermi('finance:bankTransactions:remove')")
    @Log(title = "银行卡流水解析", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        return toAjax(bankCardTransactionsService.deleteBankCardTransactionsByIds(ids));
    }

    /**
     * 导入数据的方法，处理文件上传和解析。
     *
     * @param multipartFile 上传的文件，类型为 MultipartFile
     * @return AjaxResult 返回操作结果，包含成功或失败信息
     */
    @PreAuthorize("@ss.hasPermi('finance:bankTransactions:import')") // 权限校验，确保用户有权限进行导入操作
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

            List<BankCardTransactions> bankCardTransactions = new ArrayList<>();

            // Check if it's a PDF
            if (savedFile.getName().toLowerCase().endsWith(".pdf")) {
                String bankType = "";
                if (savedFile.getName().contains("招商")) {
                    bankType = "CMB";
                } else if (savedFile.getName().contains("建设")) {
                    bankType = "CCB";
                }

                if (!bankType.isEmpty()) {
                    List<Map<String, Object>> pdfData = PdfUtil.parsePdf(savedFile, bankType);
                    if ("CMB".equals(bankType)) {
                        bankCardTransactions = convertCMBMapListToTransactions(pdfData);
                    } else if ("CCB".equals(bankType)) {
                        bankCardTransactions = convertCCBMapListToTransactions(pdfData);
                    }
                }
            } else {
                //解析Excel
                List<Object> objects = EasyExcelUtil.readExcel(savedFile.getPath());
                //数据转实体
                if (savedFile.getName().contains("招商")) {
                    bankCardTransactions = convertCMBJSONArrayToBankCardTransactionsList(objects);
                } else if (savedFile.getName().contains("建设")) {
                    bankCardTransactions = convertJSYHJSONArrayToBankCardTransactionsList(objects);
                }
            }

            if (CollectionUtils.isEmpty(bankCardTransactions)) {
                return AjaxResult.error("导入失败：不支持的文件或文件为空");
            } else {
                //存储数据库
                for (BankCardTransactions bankCardTransaction : bankCardTransactions) {
                    BigDecimal amount = bankCardTransaction.getAmount();
                    if (amount.compareTo(BigDecimal.ZERO) >= 0) {
                        bankCardTransaction.setTransactionType("收入");
                    } else {
                        bankCardTransaction.setTransactionType("支出");
                    }
                    bankCardTransactionsService.insertBankCardTransactions(bankCardTransaction);
                }
            }
            // 返回成功信息，包含导入的数据条数
            return AjaxResult.success("导入成功，数据条数：");
        } catch (IOException e) {
//             处理 IO 异常，返回错误信息
            return AjaxResult.error("导入失败：文件读取错误：" + e.getMessage());
        } catch (ParseException e) {
            //             处理 IO 异常，返回错误信息
            return AjaxResult.error("导入失败：文件处理错误：" + e.getMessage());
        } finally {
//             可选：处理完后删除临时文件，如果文件不再需要
            if (savedFile != null && savedFile.exists()) {
                // savedFile.delete(); // 仅在必要时删除
            }
        }
    }

    /**
     * Converts a JSONObject to a BankCardTransactions object.
     *
     * @param jsonObject The JSONObject containing transaction information.
     * @return The BankCardTransactions object created from the JSONObject.
     */
    public static BankCardTransactions convertCMBJSONObjectToBankCardTransactions(JSONObject jsonObject) throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        BankCardTransactions bankCardTransactions = new BankCardTransactions();
        bankCardTransactions.setAccountNo("6214831061297492");
        bankCardTransactions.setSubBranch("北京回龙观支行");
        bankCardTransactions.setBank("招商银行");
        bankCardTransactions.setDate(dateFormat.parse(jsonObject.get(0).toString()));
        bankCardTransactions.setCurrency(jsonObject.get(1).toString());
        bankCardTransactions.setAmount(new BigDecimal(jsonObject.get(2).toString().replace(",", "")));
        bankCardTransactions.setBalance(new BigDecimal(jsonObject.get(3).toString().replace(",", "")));
        bankCardTransactions.setTransaction(jsonObject.get(4).toString());
        bankCardTransactions.setCounterParty(jsonObject.get(5).toString());
        if (jsonObject.get(6) != null) {
            bankCardTransactions.setCounterParty(jsonObject.get(5).toString() + " " + jsonObject.get(6).toString());
        }

        return bankCardTransactions;
    }

    /**
     * Converts a JSONObject to a BankCardTransactions object.
     *
     * @param jsonObject The JSONObject containing transaction information.
     * @return The BankCardTransactions object created from the JSONObject.
     */
    public static BankCardTransactions convertJSYHJSONObjectToBankCardTransactions(JSONObject jsonObject) throws ParseException {
        BankCardTransactions bankCardTransactions = new BankCardTransactions();
        bankCardTransactions.setAccountNo("6217000210014252752");
        bankCardTransactions.setSubBranch("河北分行");
        bankCardTransactions.setBank("建设银行");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        bankCardTransactions.setDate(sdf.parse(jsonObject.get(4).toString()));
        bankCardTransactions.setCurrency("CNY");
        bankCardTransactions.setAmount(new BigDecimal(jsonObject.get(5).toString().replace(",", "")));
        bankCardTransactions.setBalance(new BigDecimal(jsonObject.get(6).toString().replace(",", "")));
        bankCardTransactions.setTransaction(jsonObject.get(1).toString());
        bankCardTransactions.setCounterParty(jsonObject.get(8).toString());
        if (jsonObject.get(7) != null) {
            bankCardTransactions.setNote(jsonObject.get(7).toString());
        }
        return bankCardTransactions;
    }

    /**
     * Converts a JSONArray to a list of BankCardTransactions objects.
     *
     * @param objects The JSONArray containing transaction information.
     * @return A list of BankCardTransactions objects created from the JSONArray.
     */
    public static List<BankCardTransactions> convertCMBJSONArrayToBankCardTransactionsList(List<Object> objects) throws ParseException {
        List<BankCardTransactions> transactionsList = new ArrayList<>();

        for (Object object : objects) {
            JSONObject jsonObject = JSONObject.from(object);
            BankCardTransactions transaction = convertCMBJSONObjectToBankCardTransactions(jsonObject);
            transactionsList.add(transaction);
        }
        return transactionsList;
    }

    /**
     * Converts a JSONArray to a list of BankCardTransactions objects.
     *
     * @param objects The JSONArray containing transaction information.
     * @return A list of BankCardTransactions objects created from the JSONArray.
     */
    public static List<BankCardTransactions> convertJSYHJSONArrayToBankCardTransactionsList(List<Object> objects) throws ParseException {
        List<BankCardTransactions> transactionsList = new ArrayList<>();
        int count = 0;
        for (Object object : objects) {
            if (count > 3) {
                JSONObject jsonObject = JSONObject.from(object);
                BankCardTransactions transaction = convertJSYHJSONObjectToBankCardTransactions(jsonObject);
                transactionsList.add(transaction);
            }
            count++;
        }
        return transactionsList;
    }

    private List<BankCardTransactions> convertCMBMapListToTransactions(List<Map<String, Object>> list) throws ParseException {
        List<BankCardTransactions> transactions = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        for (Map<String, Object> map : list) {
            BankCardTransactions tx = new BankCardTransactions();
            // NOTE: Hardcoded account details to match existing Excel import logic for CMB
            tx.setAccountNo("6214831061297492");
            tx.setSubBranch("北京回龙观支行");
            tx.setBank("招商银行");
            tx.setDate(sdf.parse((String) map.get("date")));
            tx.setCurrency((String) map.get("currency"));
            tx.setAmount(new BigDecimal((String) map.get("amount")));
            tx.setBalance(new BigDecimal((String) map.get("balance")));
            tx.setTransaction((String) map.get("transactionType"));
            tx.setCounterParty((String) map.get("counterParty"));
            transactions.add(tx);
        }
        return transactions;
    }

    private List<BankCardTransactions> convertCCBMapListToTransactions(List<Map<String, Object>> list) throws ParseException {
        List<BankCardTransactions> transactions = new ArrayList<>();
        // CCB uses yyyyMMdd in PDF based on heuristics in PdfUtil, but date in Map is string.
        // PdfUtil for CCB returns "yyyyMMdd" as "date".
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        for (Map<String, Object> map : list) {
            BankCardTransactions tx = new BankCardTransactions();
            // NOTE: Hardcoded account details to match existing Excel import logic for CCB
            tx.setAccountNo("6217000210014252752");
            tx.setSubBranch("河北分行");
            tx.setBank("建设银行");

            String dateStr = (String) map.get("date");
            // Basic validation to avoid ParseException if parsing failed
            if (dateStr != null) {
                tx.setDate(sdf.parse(dateStr));
            }

            tx.setCurrency("CNY");
            if (map.get("amount") != null) {
                tx.setAmount(new BigDecimal((String) map.get("amount")));
            }
            if (map.get("balance") != null) {
                tx.setBalance(new BigDecimal((String) map.get("balance")));
            }
            tx.setTransaction((String) map.get("transactionType"));
            tx.setCounterParty((String) map.get("counterParty"));
            transactions.add(tx);
        }
        return transactions;
    }
}
