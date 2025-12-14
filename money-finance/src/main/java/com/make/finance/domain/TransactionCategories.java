package com.make.finance.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 交易分类关键词对象 transaction_categories
 *
 * @author 贰柒
 * @date 2025-05-28
 */
public class TransactionCategories extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 交易分类的主键 */
    private Long id;

    /** 交易分类名称 */
    @Excel(name = "交易分类名称")
    private String categoryName;

    /** 与交易分类关联的关键词 */
    @Excel(name = "与交易分类关联的关键词")
    private String keyword;

    /** 交易分类类型 */
    @Excel(name = "交易分类类型")
    private String category;

    /** 更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Excel(name = "更新时间", width = 30, dateFormat = "yyyy-MM-dd")
    private Date updatedAt;

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getId()
    {
        return id;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setKeyword(String keyword)
    {
        this.keyword = keyword;
    }

    public String getKeyword()
    {
        return keyword;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getCategory()
    {
        return category;
    }

    public void setUpdatedAt(Date updatedAt)
    {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt()
    {
        return updatedAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("categoryName", getCategoryName())
            .append("keyword", getKeyword())
            .append("category", getCategory())
            .append("updatedAt", getUpdatedAt())
            .toString();
    }
}
