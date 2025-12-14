package com.make.stock.service.impl;

import java.util.List;

import com.make.stock.domain.StockIssueInfo;
import com.make.stock.mapper.StockIssueInfoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.StockListingNoticeMapper;
import com.make.stock.domain.StockListingNotice;
import com.make.stock.service.IStockListingNoticeService;

/**
 * 证券上市通知Service业务层处理
 *
 * @author erqi
 * @date 2025-07-31
 */
@Service
public class StockListingNoticeServiceImpl implements IStockListingNoticeService {

    @Autowired
    private StockListingNoticeMapper stockListingNoticeMapper;

    @Autowired
    private StockIssueInfoMapper stockIssueInfoMapper;

    /**
     * 查询证券上市通知
     *
     * @param id 证券上市通知主键
     * @return 证券上市通知
     */
    @Override
    public StockListingNotice selectStockListingNoticeById(String id) {
        return stockListingNoticeMapper.selectStockListingNoticeById(id);
    }

    /**
     * 查询证券上市通知列表
     *
     * @param stockListingNotice 证券上市通知
     * @return 证券上市通知
     */
    @Override
    public List<StockListingNotice> selectStockListingNoticeList(StockListingNotice stockListingNotice) {
//        List<StockListingNotice> stockListingNotices = stockListingNoticeMapper.selectStockListingNoticeList(stockListingNotice);
//        for (StockListingNotice stockListingNotice2 : stockListingNotices){
//            String securityCode = stockListingNotice2.getSecurityCode();
//            StockIssueInfo stockIssueInfo = stockIssueInfoMapper.selectStockIssueInfoByApplyCode(securityCode);
//            if (stockIssueInfo!=null){
//                stockListingNotice2.setListingDate(stockIssueInfo.getListingDate());
//                stockListingNoticeMapper.updateStockListingNotice(stockListingNotice2);
//            }else {
//                stockListingNotice2.setListingDate(null);
//            }
//
//        }
        return stockListingNoticeMapper.selectStockListingNoticeList(stockListingNotice);
    }

    /**
     * 新增证券上市通知
     *
     * @param stockListingNotice 证券上市通知
     * @return 结果
     */
    @Override
    public int insertStockListingNotice(StockListingNotice stockListingNotice) {
        return stockListingNoticeMapper.insertStockListingNotice(stockListingNotice);
    }

    /**
     * 修改证券上市通知
     *
     * @param stockListingNotice 证券上市通知
     * @return 结果
     */
    @Override
    public int updateStockListingNotice(StockListingNotice stockListingNotice) {
        return stockListingNoticeMapper.updateStockListingNotice(stockListingNotice);
    }

    /**
     * 批量删除证券上市通知
     *
     * @param ids 需要删除的证券上市通知主键
     * @return 结果
     */
    @Override
    public int deleteStockListingNoticeByIds(String[] ids) {
        return stockListingNoticeMapper.deleteStockListingNoticeByIds(ids);
    }

    /**
     * 删除证券上市通知信息
     *
     * @param id 证券上市通知主键
     * @return 结果
     */
    @Override
    public int deleteStockListingNoticeById(String id) {
        return stockListingNoticeMapper.deleteStockListingNoticeById(id);
    }
}
