package com.make.stock.domain;

import java.math.BigDecimal;

import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 黄金价格对象 gold_product_price
 *
 * @author erqi
 * @date 2025-05-28
 */
public class GoldProductPrice extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 银行名称
     */
    @Excel(name = "银行名称")
    private String bank;

    /**
     * 产品名称
     */
    @Excel(name = "产品名称")
    private String product;

    /**
     * 产品价格
     */
    @Excel(name = "产品价格")
    private BigDecimal price;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getBank() {
        return bank;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public String getProduct() {
        return product;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("bank", getBank())
                .append("product", getProduct())
                .append("price", getPrice())
                .append("createTime", getCreateTime())
                .append("updateTime", getUpdateTime())
                .toString();
    }
}
