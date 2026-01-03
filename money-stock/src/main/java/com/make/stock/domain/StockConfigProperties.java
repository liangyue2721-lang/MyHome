package com.make.stock.domain;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * StockConfigProperties 类用于加载和管理股票相关API的配置信息。
 * <p>
 * 该类负责读取配置文件 stockConfig.properties 中存储的 API 配置，并提供获取和设置各个 API 地址的方法。
 * 支持链式存储，便于配置的快速设置。
 * </p>
 */
public class StockConfigProperties {
    private static final Logger log = LoggerFactory.getLogger(StockConfigProperties.class);

    /**
     * 股票API的基础URL
     */
    private static String apiUrl = null;

    /**
     * 股票发行信息API的URL
     */
    private static String stockIssueInfoApiUrl = null;

    // 各种API的URL配置
    private String shanghaiShenzhenBeijingAStockApi;
    private String shanghaiAStockApi;
    private String shenzhenAStockApi;
    private String beijingAStockApi;
    private String ipoStockApi;
    private String chiNextStockApi;
    private String starMarketStockApi;
    private String shanghaiStockConnectApi;
    private String shenzhenStockConnectApi;
    private String bStockApi;
    private String stStockApi;
    private String stock601138Api;

    /**
     * 构造方法是私有的，外部无法直接实例化此类
     */
    private StockConfigProperties() {
    }

    /**
     * 获取 StockConfigProperties 的单例实例。
     * <p>
     * 加载配置文件并初始化类的属性，支持链式调用方式。
     * </p>
     *
     * @return 当前类的实例
     */
    public static StockConfigProperties getInstance() {
        return new StockConfigProperties().load();
    }

    /**
     * 加载配置文件，并设置所有 API 配置。
     * <p>
     * 该方法会从 classpath 中加载名为 "stockConfig.properties" 的配置文件，读取其中存储的各个股票相关 API 地址配置，
     * 并将其存储到相应的实例变量中。支持链式调用，即返回当前类实例，便于连续设置多个属性。
     * </p>
     * <p>
     * 在配置文件中，API 地址以键值对形式存储，方法会根据这些键值对将配置文件中的内容加载到类的属性中：
     * <ul>
     *     <li>stockApi.url：股票API基础URL</li>
     *     <li>stockIssueInfoApi.url：股票发行信息API的URL</li>
     *     <li>ShanghaiShenzhenBeijingAStockApi：上海、深圳、北京A股API地址</li>
     *     <li>ShanghaiAStockApi：上海A股API地址</li>
     *     <li>ShenzhenAStockApi：深圳A股API地址</li>
     *     <li>BeijingAStockApi：北京A股API地址</li>
     *     <li>IpoStockApi：IPO股票API地址</li>
     *     <li>ChiNextStockApi：创业板API地址</li>
     *     <li>StarMarketStockApi：科创板API地址</li>
     *     <li>ShanghaiStockConnectApi：上海股市连接API地址</li>
     *     <li>ShenzhenStockConnectApi：深圳股市连接API地址</li>
     *     <li>BStockApi：B股API地址</li>
     *     <li>StStockApi：ST股票API地址</li>
     * </ul>
     * </p>
     * <p>
     * 如果配置文件读取失败，捕获到 `IOException` 异常并记录日志。此方法不会抛出异常，而是通过日志记录异常信息。
     * </p>
     *
     * @return 当前类的实例，支持链式调用
     */
    private StockConfigProperties load() {
        Properties properties = new Properties();

        // 获取当前类加载器，加载配置文件
        ClassLoader classLoader = StockConfigProperties.class.getClassLoader();
        try (InputStream inputStream = classLoader.getResourceAsStream("stockConfig.properties")) {
            // 如果配置文件存在，则加载
            if (inputStream != null) {
                properties.load(inputStream);

                // 加载各个API配置
                apiUrl = properties.getProperty("stockApi.url");
                stockIssueInfoApiUrl = properties.getProperty("stockIssueInfoApi.url");

                shanghaiShenzhenBeijingAStockApi = properties.getProperty("ShanghaiShenzhenBeijingAStockApi");
                shanghaiAStockApi = properties.getProperty("ShanghaiAStockApi");
                shenzhenAStockApi = properties.getProperty("ShenzhenAStockApi");
                beijingAStockApi = properties.getProperty("BeijingAStockApi");
                ipoStockApi = properties.getProperty("IPOStockApi");
                chiNextStockApi = properties.getProperty("ChiNextStockApi");
                starMarketStockApi = properties.getProperty("STARMarketStockApi");
                shanghaiStockConnectApi = properties.getProperty("ShanghaiStockConnectApi");
                shenzhenStockConnectApi = properties.getProperty("ShenzhenStockConnectApi");
                bStockApi = properties.getProperty("BStockApi");
                stStockApi = properties.getProperty("STStockApi");
                stock601138Api = properties.getProperty("Stock601138Api");
            }
        } catch (IOException ex) {
            // 如果配置文件加载失败，记录错误日志
            log.error("StockConfigProperties配置获取失败:", ex);
        }

        // 返回当前实例，以便支持链式调用
        return this;
    }


    /**
     * 设置 "上海深圳北京A股API" 地址
     *
     * @param shanghaiShenzhenBeijingAStockApi 上海深圳北京A股API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setShanghaiShenzhenBeijingAStockApi(String shanghaiShenzhenBeijingAStockApi) {
        this.shanghaiShenzhenBeijingAStockApi = shanghaiShenzhenBeijingAStockApi;
        return this;
    }

    /**
     * 设置 "上海A股API" 地址
     *
     * @param shanghaiAStockApi 上海A股API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setShanghaiAStockApi(String shanghaiAStockApi) {
        this.shanghaiAStockApi = shanghaiAStockApi;
        return this;
    }

    /**
     * 设置 "深圳A股API" 地址
     *
     * @param shenzhenAStockApi 深圳A股API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setShenzhenAStockApi(String shenzhenAStockApi) {
        this.shenzhenAStockApi = shenzhenAStockApi;
        return this;
    }

    /**
     * 设置 "北京A股API" 地址
     *
     * @param beijingAStockApi 北京A股API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setBeijingAStockApi(String beijingAStockApi) {
        this.beijingAStockApi = beijingAStockApi;
        return this;
    }

    /**
     * 设置 "IPO 股票API" 地址
     *
     * @param ipoStockApi IPO股票API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setIpoStockApi(String ipoStockApi) {
        this.ipoStockApi = ipoStockApi;
        return this;
    }

    /**
     * 设置 "创业板股票API" 地址
     *
     * @param chiNextStockApi 创业板股票API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setChiNextStockApi(String chiNextStockApi) {
        this.chiNextStockApi = chiNextStockApi;
        return this;
    }

    /**
     * 设置 "科创板股票API" 地址
     *
     * @param starMarketStockApi 科创板股票API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setStarMarketStockApi(String starMarketStockApi) {
        this.starMarketStockApi = starMarketStockApi;
        return this;
    }

    /**
     * 设置 "上海股市连接API" 地址
     *
     * @param shanghaiStockConnectApi 上海股市连接API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setShanghaiStockConnectApi(String shanghaiStockConnectApi) {
        this.shanghaiStockConnectApi = shanghaiStockConnectApi;
        return this;
    }

    /**
     * 设置 "深圳股市连接API" 地址
     *
     * @param shenzhenStockConnectApi 深圳股市连接API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setShenzhenStockConnectApi(String shenzhenStockConnectApi) {
        this.shenzhenStockConnectApi = shenzhenStockConnectApi;
        return this;
    }

    /**
     * 设置 "B股API" 地址
     *
     * @param bStockApi B股API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setbStockApi(String bStockApi) {
        this.bStockApi = bStockApi;
        return this;
    }

    /**
     * 设置 "ST股票API" 地址
     *
     * @param stStockApi ST股票API的地址
     * @return 当前类实例，支持链式调用
     */
    public StockConfigProperties setStStockApi(String stStockApi) {
        this.stStockApi = stStockApi;
        return this;
    }

    public String getbStockApi() {
        return bStockApi;
    }

    public String getStock601138Api() {
        return stock601138Api;
    }

    public void setStock601138Api(String stock601138Api) {
        this.stock601138Api = stock601138Api;
    }

// 获取方法

    public String getShanghaiShenzhenBeijingAStockApi() {
        return shanghaiShenzhenBeijingAStockApi;
    }

    public String getShanghaiAStockApi() {
        return shanghaiAStockApi;
    }

    public String getShenzhenAStockApi() {
        return shenzhenAStockApi;
    }

    public String getBeijingAStockApi() {
        return beijingAStockApi;
    }

    public String getIpoStockApi() {
        return ipoStockApi;
    }

    public String getChiNextStockApi() {
        return chiNextStockApi;
    }

    public String getStarMarketStockApi() {
        return starMarketStockApi;
    }

    public String getShanghaiStockConnectApi() {
        return shanghaiStockConnectApi;
    }

    public String getShenzhenStockConnectApi() {
        return shenzhenStockConnectApi;
    }

    public String getBStockApi() {
        return bStockApi;
    }

    public String getStStockApi() {
        return stStockApi;
    }

    // 静态配置获取方法

    /**
     * 获取股票API的基础URL
     *
     * @return 股票API基础URL
     */
    public static String getApiUrl() {
        return apiUrl;
    }

    /**
     * 获取股票发行信息API的URL
     *
     * @return 股票发行信息API的URL
     */
    public static String getStockIssueInfoApiUrl() {
        return stockIssueInfoApiUrl;
    }

    // 静态配置设置方法

    /**
     * 设置股票API的基础URL
     *
     * @param apiUrl 股票API的基础URL
     */
    public static void setApiUrl(String apiUrl) {
        StockConfigProperties.apiUrl = apiUrl;
    }

    /**
     * 设置股票发行信息API的URL
     *
     * @param stockIssueInfoApiUrl 股票发行信息API的URL
     */
    public static void setStockIssueInfoApiUrl(String stockIssueInfoApiUrl) {
        StockConfigProperties.stockIssueInfoApiUrl = stockIssueInfoApiUrl;
    }

    /**
     * f43 (2107)
     * 可能表示当前价格，单位为“分”或转换后为21.07元。
     *
     * f44 (2149)
     * 可能表示今日最高价，对应21.49元。
     *
     * f45 (2085)
     * 可能表示今日最低价，对应20.85元。
     *
     * f46 (2137)
     * 可能表示收盘价或最新成交价，对应21.37元。
     *
     * f47 (1059091)
     * 可能表示成交量，单位为“手”（例如105.91万手）。
     *
     * f48 (2231416864.0)
     * 可能表示成交额，单位为元（约22.31亿元）。
     *
     * f49 (471126)
     * 可能表示成交笔数或其他成交相关数据（具体含义需参考文档）。
     *
     * f50 (55)
     * 可能代表涨跌数值、振幅或其他百分比指标，但数值55需要结合具体定义判断。
     *
     * f51 (2331)
     * 可能为市盈率、估值指标或参考价格（单位、含义需参考说明）。
     *
     * f52 (1907)
     * 可能代表市净率、流通市值或其它估值数据。
     *
     * f57 ("601138")
     * 明确表示股票代码。
     *
     * f58 ("工业富联")
     * 股票名称。
     *
     * f59 (2)
     * 可能表示股票类别或交易市场标识（例如上证、深证的区分）。
     *
     * f60 (2119)
     * 可能为当前实时成交价，或其他参考价格，与f43、f46可能有不同意义（例如实时价与收盘价）。
     *
     * f71 (2107)
     * 可能表示昨日收盘价或前一个交易日的参考价。
     *
     * f84 (19858549824.0)
     * 可能表示总市值，单位可能经过换算后为“4184亿左右”（例如除以1亿）。
     *
     * f85 (19857264319.0)
     * 可能表示流通市值或其他市值指标。
     *
     * f86 (1740989507)
     * 可能为净利润、营业收入等财务数据，或其他指标。
     *
     * f92 (7.6889947)
     * 可能表示某项估值指标，如市盈率（TTM）或市净率，但与界面显示（18.43）不完全对应，需确认具体定义。
     *
     * f107 (1)
     * 可能是涨跌状态标识（例如1代表上涨，0或-1代表下跌）。
     *
     * f108 (1.169091862)
     * 可能为涨跌幅（百分比），但实际界面显示为负值时可能为跌幅。
     *
     * f111 (2)
     * 可能表示交易状态、股票类别或其它代码标识。
     *
     * f116 (418419644791.68)
     * 可能表示总市值的原始数据，经过单位转换后显示为“4184亿”。
     *
     * f117 (418392559201.33)
     * 可能为流通市值或类似指标。
     *
     * f152 (2)
     * 可能表示行业分类代码或板块标识。
     *
     * f161 (587965)
     * 可能表示成交笔数或累计成交数据中的一项。
     *
     * f162 (1802)、f163 (1989)、f164 (1802)
     * 这组字段可能代表分时或历史数据中的成交量、成交额或均价等细分数据。
     *
     * f167 (274)
     * 可能表示主力资金净流入（单位：万元或其他单位）。
     *
     * f168 (53)
     * 可能表示散户资金流入或其他资金指标。
     *
     * f169 (-12)
     * 可能表示某时段内的资金净流出，负值表示流出。
     *
     * f170 (-57)
     * 可能是另一个时段的资金净流量数据。
     *
     * f171 (302)
     * 可能是综合净流入数值或累计数据。
     *
     * f177 (577)
     * 可能表示特定时段累计的资金流向或买卖力量指标。
     *
     * f191 (-3682) 与 f192 (-3607)
     * 可能表示较长周期内的资金流出数据（例如日内、日均或累计资金变化）。
     *
     * f256 ("-")、f260 ("-")、f261 ("-")、f262 ("-")、f269 ("-")、f285 ("-")、f295 ("-")
     * 这些字段可能表示没有数据或不适用的项，用“-”表示缺失或无效数据。
     *
     * f257 (0)、f270 (0)、f286 (0)、f288 (0)
     * 可能为保留字段或表示该项数据为0。
     *
     * f277 (19858549824.0)
     * 可能是市值数据的重复项或备用市值指标，与f84类似。
     *
     * f278 (1969530023.0)
     * 可能表示另一个市值相关数据，如流通市值或净资产。
     *
     * f279 (1)
     * 可能为标识字段（例如是否上市或特殊交易状态）。
     *
     * f292 (5)
     * 可能代表交易笔数、涨停次数或板块内排名等统计数据。
     *
     * f294 (0)
     * 可能是对应的统计数据，为0表示无成交或无涨跌。
     *
     * f301 (2107)
     * 可能重复表示当前价格或参考价格，与f43一致。
     *
     * f31 (2111)、f33 (2110)、f35 (2109)、f37 (2108)、f39 (2107)
     * 这组字段可能是分时数据中不同时间点的价格，或是历史价格序列中的数据。
     *
     * f32 (268)、f34 (1342)、f36 (1056)、f38 (641)、f40 (3395)
     * 这组字段可能代表对应时段内的成交量、成交额或成交笔数等统计数据。
     *
     * f19 (2106)、f20 (907)、f17 (2105)、f18 (773)、f15 (2104)、f16 (285)、f13 (2103)、f14 (662)、f11 (2102)、f12 (468)
     * 同样，这组字段可能是历史行情或分时数据，分别代表价格、成交量或其他指标的不同时间点记录。
     *
     * f734 ("")
     * 为空字符串，可能表示预留或无数据。
     *
     * f747 ("-")
     * 同样用“-”表示无数据或不适用。
     *
     * f748 (0)
     * 可能为备用字段或未使用字段，其值为0。
     */
}
