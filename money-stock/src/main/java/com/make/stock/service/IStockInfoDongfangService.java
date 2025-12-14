package com.make.stock.service;

import java.util.List;

import com.make.stock.domain.StockInfoDongfang;
import org.apache.ibatis.annotations.Param;

/**
 * 东方财富股票Service接口
 *
 * @author erqi
 * @date 2025-05-28
 */
public interface IStockInfoDongfangService {

    /**
     * 查询东方财富股票
     *
     * @param id 东方财富股票主键
     * @return 东方财富股票
     */
    public StockInfoDongfang selectStockInfoDongfangById(Long id);

    /**
     * 查询东方财富股票列表
     *
     * @param stockInfoDongfang 东方财富股票
     * @return 东方财富股票集合
     */
    public List<StockInfoDongfang> selectStockInfoDongfangList(StockInfoDongfang stockInfoDongfang);

    /**
     * 新增东方财富股票
     *
     * @param stockInfoDongfang 东方财富股票
     * @return 结果
     */
    public int insertStockInfoDongfang(StockInfoDongfang stockInfoDongfang);

    /**
     * 修改东方财富股票
     *
     * @param stockInfoDongfang 东方财富股票
     * @return 结果
     */
    public int updateStockInfoDongfang(StockInfoDongfang stockInfoDongfang);

    /**
     * 批量删除东方财富股票
     *
     * @param ids 需要删除的东方财富股票主键集合
     * @return 结果
     */
    public int deleteStockInfoDongfangByIds(Long[] ids);

    /**
     * 删除东方财富股票信息
     *
     * @param id 东方财富股票主键
     * @return 结果
     */
    public int deleteStockInfoDongfangById(Long id);

    /**
     * 根据东方股票代码查询单条股票信息
     *
     * <p>通过6位数字编码精确匹配获取最新股票数据，包含基本行情和财务指标</p>
     *
     * @param stockCode 东方股票代码（6位数字），例如{@code 600519}
     * @return 匹配的{@linkplain StockInfoDongfang}对象，若不存在则返回{@code null}
     * @throws IllegalArgumentException 当{@code stockCode}格式不符合要求时抛出
     * @apiNote 该方法会触发实时行情数据缓存更新
     * @see StockInfoDongfang#getStockCode()
     * @since 2.1.0 新增科创板股票支持
     */
    StockInfoDongfang selectByCode(String stockCode);

    /**
     * 批量查询东方股票信息
     *
     * <p>支持批量获取多支股票的基础信息，自动过滤无效代码并去重。
     * 返回结果按股票代码升序排列，包含以下核心字段：
     * <ul>
     *   <li>{@linkplain StockInfoDongfang#getStockCode() 股票代码}</li>
     * </ul>
     *
     * @param stockCodes 股票代码列表（非空），允许包含重复代码
     * @return 去重后的{@linkplain StockInfoDongfang}对象列表
     * @throws IllegalArgumentException 当{@code stockCodes}为空或包含非数字字符时抛出
     * @apiNote 该方法使用二级缓存策略（本地缓存+Redis）
     * @reference 参考《证券交易数据接口规范》（JR/T 0042-2023）
     * @since 3.0.0 新增港股通股票支持
     */
    List<StockInfoDongfang> queryIDByCodes(List<String> stockCodes);

    /**
     * 全量查询东方股票信息
     *
     * <p>获取系统中注册的所有东方股票基础数据，包含：
     * <ul>
     * </ul>
     *
     * <b>性能提示：</b>
     * <ul>
     *   <li>首次调用会加载全量数据到内存缓存</li>
     *   <li>数据更新频率为每日收盘后同步</li>
     * </ul>
     *
     * @return 包含所有东方股票的{@linkplain StockInfoDongfang}列表
     * @apiNote 该方法仅限内部系统调用，生产环境建议使用{@linkplain #queryByCodes(List)}
     * @since 1.0.0 初始版本
     */
    List<StockInfoDongfang> queryAllStockInfoDongfang();

    /**
     * 批量更新东方财富
     *
     * @return 结果
     */
    public int batchUpdateStockInfoDongfang(List<StockInfoDongfang> stockInfoDongFangs);

    /**
     * 批量新增东方财富
     *
     * @return 结果
     */
    public int batchInsertStockInfoDongfang(List<StockInfoDongfang> stockInfoDongFangs);

}
