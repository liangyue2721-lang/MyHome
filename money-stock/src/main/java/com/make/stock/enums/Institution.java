package com.make.stock.enums;

import lombok.Getter;

@Getter
public enum Institution {
    ICBC("工商银行", "ICBC"),
    BOC("中国银行", "BOC"),
    CCB("建设银行", "CCB"),
    ABC("农业银行", "ABC"),
    CMBC("民生银行", "CMBC"),
    PAB("平安银行", "PAB"),
    PSBC("邮政银行", "PSBC"),
    CMB("招商银行", "CMB"),
    BCM("交通银行", "BCM"),
    CITIC("中信银行", "CITIC"),
    CIB("兴业银行", "CIB"),
    CGOLD("中国黄金", "CGOLD"),
    CAIBAIBAI("菜百首饰", "CAIBAIBAI"),
    CHOWTAIFOOK("周大福", "CHOWTAIFOOK"),
    LAOFENGXIANG("老凤祥", "LAOFENGXIANG"),
    LAOMIAO("老庙黄金", "LAOMIAO"),
    CHOWSANGSANG("周生生", "CHOWSANGSANG");

    /**
     * -- GETTER --
     *  获取机构的中文名称
     *
     * @return 中文名称
     */
    private final String chineseName;
    /**
     * -- GETTER --
     *  获取机构的英文简写
     *
     * @return 英文简写
     */
    private final String abbreviation;

    Institution(String chineseName, String abbreviation) {
        this.chineseName = chineseName;
        this.abbreviation = abbreviation;
    }

    /**
     * 根据英文简写查找对应的枚举
     *
     * @param abbr 英文简写
     * @return Optional 包含对应枚举或为空
     */
    public static java.util.Optional<Institution> fromAbbreviation(String abbr) {
        if (abbr == null) {
            return java.util.Optional.empty();
        }
        for (Institution inst : Institution.values()) {
            if (inst.abbreviation.equalsIgnoreCase(abbr)) {
                return java.util.Optional.of(inst);
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * 根据中文名称查找对应的枚举
     *
     * @param name 中文名称
     * @return Optional 包含对应枚举或为空
     */
    public static java.util.Optional<Institution> fromChineseName(String name) {
        if (name == null) {
            return java.util.Optional.empty();
        }
        for (Institution inst : Institution.values()) {
            if (inst.chineseName.equals(name)) {
                return java.util.Optional.of(inst);
            }
        }
        return java.util.Optional.empty();
    }

    /**
     * 根据英文简写获取对应的中文名称
     * @param abbr 英文简写
     * @return 中文名称，若未找到则返回 null
     */
    public static String getChineseNameByAbbreviation(String abbr) {
        return fromAbbreviation(abbr)
                .map(Institution::getChineseName)
                .orElse(null);
    }
}
