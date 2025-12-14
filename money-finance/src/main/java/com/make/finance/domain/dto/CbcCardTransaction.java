package com.make.finance.domain.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;
@Data
public class CbcCardTransaction {

    @ExcelProperty("交易日")
    @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date tradeDate;

    @ExcelProperty("银行记账日")
    @DateTimeFormat(fallbackPatterns = "yyyy-MM-dd")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private Date postDate;

    @ExcelProperty("卡号后四位")
    private String cardLast4Digits;

    @ExcelProperty("交易描述")
    private String description;

    @ExcelProperty("交易币种")
    private String transactionCurrency;

    @ExcelProperty("交易金额")
    private BigDecimal transactionAmount;

    @ExcelProperty("结算币种")
    private String settlementCurrency;

    @ExcelProperty("结算金额")
    private BigDecimal settlementAmount;

    @ExcelProperty("交易类型")
    private String transactionType;
}
