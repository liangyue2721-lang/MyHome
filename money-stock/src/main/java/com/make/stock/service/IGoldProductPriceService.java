package com.make.stock.service;

import java.util.List;

import com.make.stock.domain .GoldProductPrice;

/**
 * 黄金价格Service接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface IGoldProductPriceService {

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
     * 修改黄金价格
     *
     * @param goldProductPrice 黄金价格
     * @return 结果
     */
    public int updateGoldProductPrice(GoldProductPrice goldProductPrice);

    /**
     * 批量删除黄金价格
     *
     * @param ids 需要删除的黄金价格主键集合
     * @return 结果
     */
    public int deleteGoldProductPriceByIds(Long[] ids);

    /**
     * 删除黄金价格信息
     *
     * @param id 黄金价格主键
     * @return 结果
     */
    public int deleteGoldProductPriceById(Long id);

    /**
     * 根据黄金产品名称查询实时价格信息
     *
     * @param name 产品名称（支持模糊查询），例如{@code "999足金"}或{@code "24K金条"}
     * @return 包含实时价格的{@linkplain GoldProductPrice}对象，若未找到则返回{@code null}
     * @throws IllegalArgumentException 当{@code name}为空或包含非法字符时抛出
     * @apiNote 该方法会触发上海黄金交易所数据源实时同步
     * @reference 参考《贵金属交易数据接口规范》（JR/T 0213-2024）第3.4.1条
     * @since 3.2.0 新增熊猫普制币价格支持
     * @example
     * <pre>{@code
     * GoldProductPrice price = goldService.selectGoldProductPriceByName("999足金");
     * // 输出：GoldProductPrice{name='999足金', price=625.45, unit='元/克', updateTime=2025-05-29T09:15:00}
     * }</pre>
     */
    GoldProductPrice getGoldProductPriceByProductName(String name);
}
