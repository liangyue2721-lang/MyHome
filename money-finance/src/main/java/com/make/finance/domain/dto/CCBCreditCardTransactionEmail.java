package com.make.finance.domain.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 中国建设银行信用卡交易记录实体类
 * <p>用于解析和存储建行信用卡交易明细数据，支持从表格文本格式解析交易记录。</p>
 *
 * @author Ruoyi
 * @version 1.1
 * @since 2023-09-01
 */
@Data
public class CCBCreditCardTransactionEmail {
    /**
     * 交易发生日期（格式：yyyy-MM-dd）
     * <p>示例："2023-08-15"</p>
     */
    private String tradeDate;

    /**
     * 交易入账日期（格式：yyyy-MM-dd）
     * <p>可能与交易日存在差异，示例："2023-08-16"</p>
     */
    private String postDate;

    /**
     * 信用卡尾号后四位
     * <p>用于标识交易卡号，示例："4396"</p>
     */
    private String cardLast4;

    /**
     * 交易描述信息
     * <p>包含商户名称、交易类型等详细信息，示例："财付通-微信零钱充值"</p>
     */
    private String description;

    /**
     * 交易原始金额（含币种）
     * <p>格式："币种/金额"，示例："CNY/1500.00"</p>
     */
    private String transAmount;

    /**
     * 结算金额（含币种）
     * <p>跨境交易时可能与交易金额不同，示例："USD/200.00"</p>
     */
    private String settleAmount;

    // 预编译正则表达式提升解析性能
    private static final Pattern PIPE_SPLIT = Pattern.compile("\\|");
    private static final Pattern ESCAPED_PIPE = Pattern.compile("\\\\([|])");

    /**
     * 解析中国建设银行信用卡交易明细表格文本，提取交易记录。
     *
     * <p>本方法用于处理以管道符("|")分隔的表格文本数据，支持中英文表头，自动识别数据行并转换为交易对象列表。
     * 输入文本可能包含转义的管道符("\|")，方法会预先处理此类转义字符。
     *
     * <p><b>表格格式要求:</b>
     * <ul>
     *   <li>表格头需包含"交易日"或"T-Date"列，以及"结算币/金额"或"Sett.Curr/Amt"列</li>
     *   <li>数据行从表头下方第二行开始，直到遇到"*** 结束"标记或文件末尾</li>
     *   <li>列顺序应为: [交易日, 记账日, 卡号末四位, 交易描述, 交易金额币种, 交易金额, 结算币种, 结算金额]</li>
     *   <li>交易描述字段可能包含未转义的管道符("|")</li>
     * </ul>
     *
     * <p><b>示例输入:</b><pre>
     * | 交易日      | 记账日     | 卡号末四位 | 交易描述           | 交易币种 | 金额    | 结算币种 | 结算金额 |
     * |------------|------------|------------|--------------------|----------|---------|----------|---------|
     * | 2023-03-01 | 2023-03-02 | 1234       | 支付宝消费|网购      | CNY      | 100.00  | CNY      | 100.00  |
     * *** 结束</pre>
     *
     * @param tableText 表格文本内容，支持包含换行符(\n或\r\n)的原始表格数据
     * @return 解析后的信用卡交易记录列表，当无法定位表头或没有数据时返回空列表
     *
     * @see CCBCreditCardTransactionEmail 交易明细实体类结构
     *
     * <p><b>处理逻辑:</b>
     * <ol>
     *   <li>还原转义管道符: 将文本中的"\\|"转换为"|"</li>
     *   <li>按行拆分并清理空行</li>
     *   <li>识别包含指定中英文列名的表头行</li>
     *   <li>从表头下两行开始解析数据，直到结束标记</li>
     *   <li>拆分管道符列时保留交易描述中的原生管道符</li>
     *   <li>自动处理币种/金额格式化(清理非法字符，组合为"CNY/100.00"格式)</li>
     * </ol>
     *
     * <p><b>注意事项:</b>
     * <ul>
     *   <li>若交易描述包含管道符，该字段不会被错误拆分(最多拆分为8列)</li>
     *   <li>金额字段支持负数表示退款(如"-50.00")</li>
     *   <li>结算币种/金额为可选列，若不存在则留空</li>
     *   <li>自动过滤不完整的数据行(列数小于6则跳过)</li>
     * </ul>
     */
    public static List<CCBCreditCardTransactionEmail> parseTransactions(String tableText) {
        // 1. 还原转义的管道符 "\|" → "|"
        String cleaned = ESCAPED_PIPE.matcher(tableText).replaceAll("$1");

        // 2. 拆行并去空
        List<String> lines = Arrays.stream(cleaned.split("\\r?\\n"))
                .map(String::trim)
                .filter(l -> !l.isEmpty())
                .collect(Collectors.toList());

        // 3. 定位表头：支持“交易日…结算币/金额”及其英文化
        int headerIdx = -1;
        for (int i = 0; i < lines.size(); i++) {
            String l = lines.get(i);
            if (l.matches("^\\|\\s*(交易日|T-Date).*?(结算币/金额|Sett\\.Curr/Amt)\\s*\\|$")) {
                headerIdx = i;
                break;
            }
        }
        if (headerIdx < 0 || headerIdx + 2 >= lines.size()) {
            return Collections.emptyList();
        }

        // 4. 解析数据行：从表头下两行开始，到 "*** 结束" 或文件尾
        List<CCBCreditCardTransactionEmail> result = new ArrayList<>();
        for (int i = headerIdx + 2; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.contains("*** 结束")) break;
            if (!line.startsWith("|")) continue;

            // 5. 拆分字段，最多 8 段，保留描述中可能的额外“|”
            String[] parts = PIPE_SPLIT.split(line, 8);
            List<String> cols = Arrays.stream(parts)
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            if (cols.size() < 6) {
                continue;
            }

            // 6. 填充实体
            CCBCreditCardTransactionEmail tx = new CCBCreditCardTransactionEmail();
            tx.tradeDate = cols.get(0);
            tx.postDate = cols.get(1);
            tx.cardLast4 = cols.get(2);
            tx.description = cols.get(3);
            tx.transAmount = formatAmount(cols.get(4), cols.get(5));
            // 如果有第7/8列，则当作结算字段
            String ccy = cols.size() > 6 ? cols.get(6) : "";
            String val = cols.size() > 7 ? cols.get(7) : "";
            tx.settleAmount = formatAmount(ccy, val);

            result.add(tx);
        }

        return result;
    }

    /**
     * 格式化币种/金额组合字段
     *
     * <p>清理输入中的非法字符并组合为标准化格式：
     * <ul>
     *   <li>币种: 移除非字母字符(保留如"CNY"、"USD"等)</li>
     *   <li>金额: 移除非数字、小数点、负号字符(如"￥100.00" → "100.00")</li>
     *   <li>组合格式: 当币种和金额均存在时返回"CNY/100.00"，仅金额存在时返回"100.00"</li>
     * </ul>
     *
     * @param currency 原始币种字符串(可能包含特殊符号)
     * @param value 原始金额字符串(可能包含货币符号)
     * @return 标准化格式的币种/金额组合，或空字符串
     */
    private static String formatAmount(String currency, String value) {
        if (currency == null) currency = "";
        if (value == null) value = "";
        currency = currency.replaceAll("[^A-Za-z]", "");
        value = value.replaceAll("[^0-9.\\-]", "");
        if (currency.isEmpty() && value.isEmpty()) {
            return "";
        }
        if (value.isEmpty()) {
            return currency;
        }
        return currency.isEmpty() ? value : (currency + "/" + value);
    }
}