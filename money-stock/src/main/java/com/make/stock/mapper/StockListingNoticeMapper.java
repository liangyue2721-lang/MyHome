package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.StockListingNotice;

/**
 * 证券上市通知Mapper接口
 *
 * @author erqi
 * @date 2025-07-31
 */
public interface StockListingNoticeMapper {

    /**
     * 查询证券上市通知
     *
     * @param id 证券上市通知主键
     * @return 证券上市通知
     */
    public StockListingNotice selectStockListingNoticeById(String id);

    /**
     * 查询证券上市通知列表
     *
     * @param stockListingNotice 证券上市通知
     * @return 证券上市通知集合
     */
    public List<StockListingNotice> selectStockListingNoticeList(StockListingNotice stockListingNotice);

    /**
     * 新增证券上市通知
     *
     * @param stockListingNotice 证券上市通知
     * @return 结果
     */
    public int insertStockListingNotice(StockListingNotice stockListingNotice);

    /**
     * 修改证券上市通知
     *
     * @param stockListingNotice 证券上市通知
     * @return 结果
     */
    public int updateStockListingNotice(StockListingNotice stockListingNotice);

    /**
     * 删除证券上市通知
     *
     * @param id 证券上市通知主键
     * @return 结果
     */
    public int deleteStockListingNoticeById(String id);

    /**
     * 批量删除证券上市通知
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteStockListingNoticeByIds(String[] ids);
}
