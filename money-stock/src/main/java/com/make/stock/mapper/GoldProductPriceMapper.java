package com.make.stock.mapper;

import java.util.List;

import com.make.stock.domain.GoldProductPrice;

/**
 * 黄金价格Mapper接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface GoldProductPriceMapper {

    /**
     * 查询黄金价格
     *
     * @param id 黄金价格主键
     * @return 黄金价格
     */
    public GoldProductPrice selectGoldProductPriceById(Long id);

    /**
     * 查询黄金价格列表
     *
     * @param goldProductPrice 黄金价格
     * @return 黄金价格集合
     */
    public List<GoldProductPrice> selectGoldProductPriceList(GoldProductPrice goldProductPrice);

    /**
     * 新增黄金价格
     *
     * @param goldProductPrice 黄金价格
     * @return 结果
     */
    public int insertGoldProductPrice(GoldProductPrice goldProductPrice);

    /**
     * 批量新增黄金价格
     *
     * @param goldProductPriceList 黄金价格列表
     * @return 结果
     */
    public int batchInsertGoldProductPrice(List<GoldProductPrice> goldProductPriceList);

    /**
     * 修改黄金价格
     *
     * @param goldProductPrice 黄金价格
     * @return 结果
     */
    public int updateGoldProductPrice(GoldProductPrice goldProductPrice);

    /**
     * 批量更新黄金价格
     *
     * @param goldProductPriceList 黄金价格列表
     * @return 结果
     */
    public int batchUpdateGoldProductPrice(List<GoldProductPrice> goldProductPriceList);

    /**
     * 删除黄金价格
     *
     * @param id 黄金价格主键
     * @return 结果
     */
    public int deleteGoldProductPriceById(Long id);

    /**
     * 批量删除黄金价格
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteGoldProductPriceByIds(Long[] ids);

    /**
     * 根据黄金产品名称查询实时价格信息
     *
     * <pre>{@code
     * GoldProductPrice price = goldService.selectGoldProductPriceByName("999足金");
     * // 输出：GoldProductPrice{name='999足金', price=625.45, unit='元/克', updateTime=2025-05-29T09:15:00}
     * }</pre>
     */
    GoldProductPrice selectGoldProductPriceByName(String name);
}
