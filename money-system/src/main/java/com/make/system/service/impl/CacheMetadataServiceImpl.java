package com.make.system.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.system.mapper.CacheMetadataMapper;
import com.make.system.domain.CacheMetadata;
import com.make.system.service.ICacheMetadataService;

/**
 * 缓存元数据Service业务层处理
 *
 * @author erqi
 * @date 2025-05-29
 */
@Service
public class CacheMetadataServiceImpl implements ICacheMetadataService {

    @Autowired
    private CacheMetadataMapper cacheMetadataMapper;

    /**
     * 查询缓存元数据
     *
     * @param id 缓存元数据主键
     * @return 缓存元数据
     */
    @Override
    public CacheMetadata selectCacheMetadataById(Long id) {
        return cacheMetadataMapper.selectCacheMetadataById(id);
    }

    /**
     * 查询缓存元数据列表
     *
     * @param cacheMetadata 缓存元数据
     * @return 缓存元数据
     */
    @Override
    public List<CacheMetadata> selectCacheMetadataList(CacheMetadata cacheMetadata) {
        return cacheMetadataMapper.selectCacheMetadataList(cacheMetadata);
    }

    /**
     * 新增缓存元数据
     *
     * @param cacheMetadata 缓存元数据
     * @return 结果
     */
    @Override
    public int insertCacheMetadata(CacheMetadata cacheMetadata) {
        return cacheMetadataMapper.insertCacheMetadata(cacheMetadata);
    }

    /**
     * 修改缓存元数据
     *
     * @param cacheMetadata 缓存元数据
     * @return 结果
     */
    @Override
    public int updateCacheMetadata(CacheMetadata cacheMetadata) {
        return cacheMetadataMapper.updateCacheMetadata(cacheMetadata);
    }

    /**
     * 批量删除缓存元数据
     *
     * @param ids 需要删除的缓存元数据主键
     * @return 结果
     */
    @Override
    public int deleteCacheMetadataByIds(Long[] ids) {
        return cacheMetadataMapper.deleteCacheMetadataByIds(ids);
    }

    /**
     * 删除缓存元数据信息
     *
     * @param id 缓存元数据主键
     * @return 结果
     */
    @Override
    public int deleteCacheMetadataById(Long id) {
        return cacheMetadataMapper.deleteCacheMetadataById(id);
    }
}
