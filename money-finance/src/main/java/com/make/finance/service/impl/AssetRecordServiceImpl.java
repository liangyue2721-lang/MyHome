package com.make.finance.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.make.common.core.redis.RedisCache;
import com.make.finance.domain.vo.LabelEntity;
import com.make.finance.enums.AssetStatusEnum;
import com.make.finance.enums.AssetTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.AssetRecordMapper;
import com.make.finance.domain.AssetRecord;
import com.make.finance.service.IAssetRecordService;

/**
 * 个人资产明细Service业务层处理
 *
 * @author 贰柒
 * @date 2025-05-28
 */
@Service
public class AssetRecordServiceImpl implements IAssetRecordService {

    @Autowired
    private AssetRecordMapper assetRecordMapper;
    @Autowired
    private RedisCache redisCache;

    /**
     * 查询个人资产明细
     *
     * @param assetId 个人资产明细主键
     * @return 个人资产明细
     */
    @Override
    public AssetRecord selectAssetRecordByAssetId(Long assetId) {
        return assetRecordMapper.selectAssetRecordByAssetId(assetId);
    }

    /**
     * 查询个人资产明细列表
     *
     * @param assetRecord 个人资产明细
     * @return 个人资产明细
     */
    @Override
    public List<AssetRecord> selectAssetRecordList(AssetRecord assetRecord) {
        String cacheKey = "finance:asset_" + "assetRecord";
//        List<AssetRecord> cacheData = redisCache.getCacheList(cacheKey);

//        if (!CollectionUtils.isEmpty(cacheData)) {
//            return cacheData;
//        }

        List<AssetRecord> dbData = assetRecordMapper.selectAssetRecordList(assetRecord);
//        if (!CollectionUtils.isEmpty(dbData)) {
//            redisCache.setCacheList(cacheKey, dbData);
//            // 设置这个键的过期时间为 1 小时
//            redisCache.setExpireTime(cacheKey, 1, TimeUnit.HOURS);
//        }
        return dbData;
    }

    /**
     * 新增个人资产明细
     *
     * @param assetRecord 个人资产明细
     * @return 结果
     */
    @Override
    public int insertAssetRecord(AssetRecord assetRecord) {
        return assetRecordMapper.insertAssetRecord(assetRecord);
    }

    /**
     * 修改个人资产明细
     *
     * @param assetRecord 个人资产明细
     * @return 结果
     */
    @Override
    public int updateAssetRecord(AssetRecord assetRecord) {
        String cacheKey = "finance:asset_" + "assetRecord";
        redisCache.deleteObject(cacheKey);
        return assetRecordMapper.updateAssetRecord(assetRecord);
    }

    /**
     * 批量删除个人资产明细
     *
     * @param assetIds 需要删除的个人资产明细主键
     * @return 结果
     */
    @Override
    public int deleteAssetRecordByAssetIds(Long[] assetIds) {
        return assetRecordMapper.deleteAssetRecordByAssetIds(assetIds);
    }

    /**
     * 删除个人资产明细信息
     *
     * @param assetId 个人资产明细主键
     * @return 结果
     */
    @Override
    public int deleteAssetRecordByAssetId(Long assetId) {
        return assetRecordMapper.deleteAssetRecordByAssetId(assetId);
    }

    @Override
    public List<LabelEntity> getAssetType() {
        Map<String, AssetTypeEnum> allSources = AssetTypeEnum.getAllDescriptions();
        List<LabelEntity> sourceOptions = new ArrayList<>();
        // 遍历枚举类中的所有收入来源，将其转换为LabelEntity对象并添加到列表中
        for (Map.Entry<String, AssetTypeEnum> entry : allSources.entrySet()) {
            LabelEntity labelEntity = new LabelEntity(entry.getKey(), String.valueOf(entry.getValue().getNum()));
            sourceOptions.add(labelEntity);
        }
        return sourceOptions;
    }

    @Override
    public List<LabelEntity> getAssetStatus() {
        Map<String, AssetStatusEnum> allSources = AssetStatusEnum.getAllDescriptions();
        List<LabelEntity> sourceOptions = new ArrayList<>();
        // 遍历枚举类中的所有收入来源，将其转换为LabelEntity对象并添加到列表中
        for (Map.Entry<String, AssetStatusEnum> entry : allSources.entrySet()) {
            LabelEntity labelEntity = new LabelEntity(entry.getKey(), String.valueOf(entry.getValue().getCode()));
            sourceOptions.add(labelEntity);
        }
        return sourceOptions;
    }

    @Override
    public List<AssetRecord> getRecordColumnChart(Long userId) {
        AssetRecord assetRecord = new AssetRecord();
        assetRecord.setUserId(userId);
        List<AssetRecord> assetRecords = assetRecordMapper.selectAssetRecordListByUserId(assetRecord);
        return assetRecords;
    }

}
