package com.make.finance.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import lombok.Data;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

/**
 * 贷款折线图展示对象 loan_repayments
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Data
public class LoanRepaymentsChart  {

    /**
     * 还款日期
     */
    @JsonFormat(pattern = "yyyy")
    private Date repaymentDate;

    /**
     * 本息合计
     */
    @Excel(name = "本息合计")
    private BigDecimal totalPrincipalAndInterest;


}
