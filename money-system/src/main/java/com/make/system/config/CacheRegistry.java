package com.make.system.config;

import com.make.system.domain.CacheMetadata;
import com.make.system.domain.SysCache;
import com.make.system.service.ICacheMetadataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 缓存元数据加载器，用于初始化预定义缓存列表。
 * 该类会在 Spring 容器初始化完成后，通过缓存元数据表加载缓存名称和备注。
 * 提供静态访问方式获取缓存定义列表。
 */
@Component
public class CacheRegistry {

    /**
     * 缓存元数据服务，通过 Spring 注入方式获取
     */
    @Autowired
    private ICacheMetadataService cacheMetadataService;

    /**
     * 静态缓存定义列表，用于展示或清理指定类型缓存
     */
    private static final List<SysCache> caches = new ArrayList<>();

    /**
     * 构造函数注入缓存元数据服务，并初始化缓存列表
     * @param cacheMetadataService 缓存元数据服务
     */
    public CacheRegistry(ICacheMetadataService cacheMetadataService) {
        this.cacheMetadataService = cacheMetadataService;
        initCaches();
    }

    /**
     * 初始化缓存定义列表，从缓存元数据表中读取并存入静态列表
     */
    private void initCaches() {
        // 查询所有缓存元数据记录
        List<CacheMetadata> metadataList = cacheMetadataService.selectCacheMetadataList(new CacheMetadata());

        // 遍历并转换为 SysCache 对象后加入静态列表
        for (CacheMetadata meta : metadataList) {
            caches.add(new SysCache(meta.getCacheName(), meta.getRemark()));
        }
    }

    /**
     * 获取初始化后的缓存定义列表
     * @return 预定义缓存列表
     */
    public static List<SysCache> getCaches() {
        return caches;
    }
}
