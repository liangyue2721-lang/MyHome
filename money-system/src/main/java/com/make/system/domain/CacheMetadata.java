package com.make.system.domain;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.make.common.annotation.Excel;
import com.make.common.core.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * 缓存元数据对象 cache_metadata
 *
 * @author erqi
 * @date 2025-05-29
 */
public class CacheMetadata extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 缓存名称
     */
    @Excel(name = "缓存名称")
    private String cacheName;

    /**
     * 缓存键名
     */
    @Excel(name = "缓存键名")
    private String cacheKey;

    /**
     * 缓存内容
     */
    @Excel(name = "缓存内容")
    private String cacheValue;

    /**
     * 创建时间
     */
    private Date createdAt;

    /**
     * 更新时间
     */
    private Date updatedAt;

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setCacheName(String cacheName) {
        this.cacheName = cacheName;
    }

    public String getCacheName() {
        return cacheName;
    }

    public void setCacheKey(String cacheKey) {
        this.cacheKey = cacheKey;
    }

    public String getCacheKey() {
        return cacheKey;
    }

    public void setCacheValue(String cacheValue) {
        this.cacheValue = cacheValue;
    }

    public String getCacheValue() {
        return cacheValue;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("cacheName", getCacheName())
                .append("cacheKey", getCacheKey())
                .append("cacheValue", getCacheValue())
                .append("remark", getRemark())
                .append("createdAt", getCreatedAt())
                .append("updatedAt", getUpdatedAt())
                .toString();
    }
}
