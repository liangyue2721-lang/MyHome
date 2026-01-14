package com.make.stock.service.scheduled.impl;

/**
 * 股票市场解析工具类
 * <p>
 * 根据股票代码的前缀数字判断所属的交易市场，并返回对应的市场编码。
 * 支持上海证券交易所、深圳证券交易所和北京证券交易所的股票代码识别。
 * </p>
 */
public final class StockMarketResolver {

    /**
     * 私有构造函数，防止实例化
     * <p>由于该类只提供静态方法，因此将其构造函数设为私有以防止外部实例化。</p>
     */
    private StockMarketResolver() {}

    /**
     * 根据股票代码获取市场编码
     * <p>
     * 根据中国股市规则，通过分析股票代码的前两位数字来确定其所属市场：
     * <ul>
     *   <li>60xxx 或 68xxx 开头的为上海证券交易所股票，返回市场编码"1"</li>
     *   <li>00xxx 或 30xxx 开头的为深圳证券交易所股票，返回市场编码"0"</li>
     *   <li>43xxx、83xxx、87xxx 或 88xxx 开头的为北京证券交易所股票，返回市场编码"2"</li>
     *   <li>无法识别的代码返回null</li>
     * </ul>
     * </p>
     *
     * @param stockCode 股票代码字符串
     * @return 市场编码字符串，无法识别时返回null
     */
    public static String getMarketCode(String stockCode) {
        // 参数校验：如果股票代码为空或长度不足2位，返回null
        if (stockCode == null || stockCode.length() < 2) {
            return null;
        }

        // 提取股票代码前两位字符
        String prefix = stockCode.substring(0, 2);

        // 判断是否为上海证券交易所股票（60开头或68开头）
        if (prefix.startsWith("60") || prefix.startsWith("68")) {
            return "1";
        }

        // 判断是否为深圳证券交易所股票（00开头或30开头）
        if (prefix.startsWith("00") || prefix.startsWith("30")) {
            return "0";
        }

        // 判断是否为北京证券交易所股票（43、83、87或88开头）
        if (prefix.startsWith("43") || prefix.startsWith("83")
                || prefix.startsWith("87") || prefix.startsWith("88")) {
            return "2";
        }

        // 无法识别的股票代码返回null
        return null;
    }
}