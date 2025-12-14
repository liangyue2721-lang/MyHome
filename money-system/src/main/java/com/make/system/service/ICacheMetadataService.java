package com.make.system.service;

import java.util.List;

import com.make.system.domain.CacheMetadata;

/**
 * 缓存元数据Service接口
 *
 * @author erqi
 * @date 2025-05-29
 */
public interface ICacheMetadataService {

    /**
     * 查询缓存元数据
     *
     * @param id 缓存元数据主键
     * @return 缓存元数据
     */
    public CacheMetadata selectCacheMetadataById(Long id);

    /**
     * 查询缓存元数据列表
     *
     * @param cacheMetadata 缓存元数据
     * @return 缓存元数据集合
     */
    public List<CacheMetadata> selectCacheMetadataList(CacheMetadata cacheMetadata);

    /**
     * 新增缓存元数据
     *
     * @param cacheMetadata 缓存元数据
     * @return 结果
     */
    public int insertCacheMetadata(CacheMetadata cacheMetadata);

    /**
     * 修改缓存元数据
     *
     * @param cacheMetadata 缓存元数据
     * @return 结果
     */
    public int updateCacheMetadata(CacheMetadata cacheMetadata);

    /**
     * 批量删除缓存元数据
     *
     * @param ids 需要删除的缓存元数据主键集合
     * @return 结果
     */
    public int deleteCacheMetadataByIds(Long[] ids);

    /**
     * 删除缓存元数据信息
     *
     * @param id 缓存元数据主键
     * @return 结果
     */
    public int deleteCacheMetadataById(Long id);
}
