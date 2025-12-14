package com.make.finance.mapper;

import java.util.List;
import com.make.finance.domain.AssetRecord;

/**
 * 个人资产明细Mapper接口
 * 
 * @author 贰柒
 * @date 2025-05-28
 */
public interface AssetRecordMapper 
{
    /**
     * 查询个人资产明细
     * 
     * @param assetId 个人资产明细主键
     * @return 个人资产明细
     */
    public AssetRecord selectAssetRecordByAssetId(Long assetId);

    /**
     * 查询个人资产明细列表
     * 
     * @param assetRecord 个人资产明细
     * @return 个人资产明细集合
     */
    public List<AssetRecord> selectAssetRecordList(AssetRecord assetRecord);

    public List<AssetRecord> selectAssetRecordListByUserId(AssetRecord assetRecord);

    /**
     * 新增个人资产明细
     * 
     * @param assetRecord 个人资产明细
     * @return 结果
     */
    public int insertAssetRecord(AssetRecord assetRecord);

    /**
     * 修改个人资产明细
     * 
     * @param assetRecord 个人资产明细
     * @return 结果
     */
    public int updateAssetRecord(AssetRecord assetRecord);

    /**
     * 删除个人资产明细
     * 
     * @param assetId 个人资产明细主键
     * @return 结果
     */
    public int deleteAssetRecordByAssetId(Long assetId);

    /**
     * 批量删除个人资产明细
     * 
     * @param assetIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteAssetRecordByAssetIds(Long[] assetIds);
}
