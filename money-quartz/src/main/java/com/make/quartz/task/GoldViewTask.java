package com.make.quartz.task;

import com.make.stock.domain.GoldProductPrice;
import com.make.stock.service.IGoldProductPriceService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import javax.annotation.Resource;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 黄金价格数据抓取定时任务
 *
 * <p>本任务用于定时抓取指定网站的银行金条价格数据，并将数据更新到数据库。</p>
 *
 * <p>主要功能：</p>
 * <ol>
 *   <li>从目标网站抓取最新的金条价格数据</li>
 *   <li>解析HTML表格数据并转换为结构化数据</li>
 *   <li>将数据持久化到数据库</li>
 * </ol>
 */
@Component("goldTask")
public class GoldViewTask {
    private static final Logger logger = LoggerFactory.getLogger(GoldViewTask.class);

    /**
     * 价格正则表达式预编译
     */
    private static final Pattern PRICE_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)");

    /**
     * 目标网站URL
     */
    private static final String TARGET_URL = "http://www.huangjinjiage.cn/baike/112572.html";

    /**
     * 表格定位CSS选择器
     */
    private static final String TABLE_SELECTOR =
            "h2:contains(各个银行黄金金条今日价格查询) + .table-box table";

    @Resource
    private IGoldProductPriceService goldProductPriceService;

    /**
     * 更新投资金条价格数据
     *
     * <p>执行流程：</p>
     * <ol>
     *   <li>连接目标网站获取HTML内容</li>
     *   <li>解析表格数据并转换为对象列表</li>
     *   <li>执行数据库更新操作</li>
     * </ol>
     *
     * @throws IOException 当网络请求或HTML解析失败时抛出
     */
    public void updateStockProfitData() throws IOException {
        StopWatch watch = new StopWatch();
        watch.start();

        try {
            Document document = Jsoup.connect(TARGET_URL).get();
            List<GoldPrice> prices = parseHtmlTable(document);
            processDatabaseUpdate(prices);

            logger.info("成功更新{}条金价数据，耗时：{}ms",
                    prices.size(), watch.getTotalTimeMillis());
        } finally {
            watch.stop();
        }
    }

    /**
     * 解析HTML表格数据
     *
     * @param document Jsoup文档对象
     * @return 解析后的金价数据列表
     */
    private List<GoldPrice> parseHtmlTable(Document document) {
        List<GoldPrice> prices = new ArrayList<>();
        Element table = document.selectFirst(TABLE_SELECTOR);

        if (table != null) {
            Elements rows = table.select("tbody > tr"); // 选择<tbody>中的所有<tr>元素
            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() >= 4) {
                    prices.add(createGoldPrice(cols));
                }
            }
        }
        return prices;
    }

    /**
     * 创建金价对象
     *
     * @param columns 表格列元素
     * @return GoldPrice对象
     */
    private GoldPrice createGoldPrice(Elements columns) {
        return new GoldPrice(
                columns.get(0).text(),
                columns.get(1).text(),
                parsePrice(columns.get(2).text()),
                columns.get(3).text()
        );
    }

    /**
     * 解析包含价格的字符串，提取其中的数值部分。
     *
     * @param priceStr 包含价格的原始字符串（可能包含单位等字符）
     * @return 解析后的数值价格；如果解析失败，则返回 0.0
     */
    private BigDecimal parsePrice(String priceStr) {
        Matcher matcher = PRICE_PATTERN.matcher(priceStr);
        if (matcher.find()) {
            try {
                return new BigDecimal(matcher.group(1));
            } catch (NumberFormatException e) {
                logger.warn("价格解析失败: {}", priceStr);
            }
        }
        return BigDecimal.ZERO;
    }

    /**
     * 处理数据库更新
     *
     * @param prices 需要更新的金价列表
     */
    private void processDatabaseUpdate(List<GoldPrice> prices) {
        for (GoldPrice goldPrice : prices) {
            GoldProductPrice goldProductPrice = goldProductPriceService
                    .getGoldProductPriceByProductName(goldPrice.getBank());
            if (goldProductPrice != null) {
                goldProductPrice.setPrice(goldPrice.getPrice());
                goldProductPriceService.updateGoldProductPrice(goldProductPrice);
            } else {
                goldProductPrice = new GoldProductPrice();
                goldProductPrice.setBank(goldPrice.getBank());
                goldProductPrice.setProduct(goldPrice.getProduct());
                goldProductPrice.setPrice(goldPrice.getPrice());
                goldProductPriceService.insertGoldProductPrice(goldProductPrice);
            }
        }
    }
}

/**
 * 金价数据临时载体
 *
 * <p>用于JSON序列化/反序列化的中间对象</p>
 */
class GoldPrice {
    private String bank;
    private String product;
    private BigDecimal price;
    private String updateTime;

    /**
     * 全参数构造方法
     *
     * @param bank       银行/品牌名称
     * @param product    产品类型
     * @param price      当前价格（元/克）
     * @param updateTime 更新时间
     */
    public GoldPrice(String bank, String product, BigDecimal price, String updateTime) {
        this.bank = bank;
        this.product = product;
        this.price = price;
        this.updateTime = updateTime;
    }

    // region Getter/Setter
    public String getBank() {
        return bank;
    }

    public void setBank(String bank) {
        this.bank = bank;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }
    // endregion
}