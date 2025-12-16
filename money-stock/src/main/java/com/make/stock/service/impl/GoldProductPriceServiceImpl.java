package com.make.stock.service.impl;

import java.util.List;
        import com.make.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.stock.mapper.GoldProductPriceMapper;
import com.make.stock.domain.GoldProductPrice;
import com.make.stock.service.IGoldProductPriceService;

/**
 * 黄金价格Service业务层处理
 *
 * @author erqi
 * @date 2025-05-28
 */
@Service
public class GoldProductPriceServiceImpl implements IGoldProductPriceService {

    @Autowired
    private GoldProductPriceMapper goldProductPriceMapper;

    /**
     * 查询黄金价格
     *
     * @param id 黄金价格主键
     * @return 黄金价格
     */
    @Override
    public GoldProductPrice selectGoldProductPriceById(Long id) {
        return goldProductPriceMapper.selectGoldProductPriceById(id);
    }

    /**
     * 批量新增黄金价格
     *
     * @param goldProductPriceList 黄金价格列表
     * @return 结果
     */
    @Override
    public int batchInsertGoldProductPrice(List<GoldProductPrice> goldProductPriceList)
    {
        return goldProductPriceMapper.batchInsertGoldProductPrice(goldProductPriceList);
    }

    /**
     * 查询黄金价格列表
     *
     * @param goldProductPrice 黄金价格
     * @return 黄金价格
     */
    @Override
    public List<GoldProductPrice> selectGoldProductPriceList(GoldProductPrice goldProductPrice) {
        return goldProductPriceMapper.selectGoldProductPriceList(goldProductPrice);
    }

    /**
     * 新增黄金价格
     *
     * @param goldProductPrice 黄金价格
     * @return 结果
     */
    @Override
    public int insertGoldProductPrice(GoldProductPrice goldProductPrice) {
                goldProductPrice.setCreateTime(DateUtils.getNowDate());
            return goldProductPriceMapper.insertGoldProductPrice(goldProductPrice);
    }

    /**
     * 修改黄金价格
     *
     * @param goldProductPrice 黄金价格
     * @return 结果
     */
    @Override
    public int updateGoldProductPrice(GoldProductPrice goldProductPrice) {
                goldProductPrice.setUpdateTime(DateUtils.getNowDate());
        return goldProductPriceMapper.updateGoldProductPrice(goldProductPrice);
    }

    /**
     * 批量更新黄金价格
     *
     * @param goldProductPriceList 黄金价格列表
     * @return 结果
     */
    @Override
    public int batchUpdateGoldProductPrice(List<GoldProductPrice> goldProductPriceList)
    {
        return goldProductPriceMapper.batchUpdateGoldProductPrice(goldProductPriceList);
    }

    /**
     * 批量删除黄金价格
     *
     * @param ids 需要删除的黄金价格主键
     * @return 结果
     */
    @Override
    public int deleteGoldProductPriceByIds(Long[] ids) {
        return goldProductPriceMapper.deleteGoldProductPriceByIds(ids);
    }

    /**
     * 删除黄金价格信息
     *
     * @param id 黄金价格主键
     * @return 结果
     */
    @Override
    public int deleteGoldProductPriceById(Long id) {
        return goldProductPriceMapper.deleteGoldProductPriceById(id);
    }

    @Override
    public GoldProductPrice getGoldProductPriceByProductName(String name) {
        return goldProductPriceMapper.selectGoldProductPriceByName(name);
    }

}
