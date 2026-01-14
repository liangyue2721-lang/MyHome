package com.make.stock.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * @author 84522
 */
public class DateUtil {

    /**
     * 将字符串日期转换为Date对象。
     *
     * @param dateString 表示日期的字符串，格式为 "yyyy-MM-dd"
     * @return 转换后的Date对象
     */
    public static Date convertStringToDate(String dateString) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            date = sdf.parse(dateString);
        } catch (ParseException e) {
            System.err.println("日期格式转换出错: " + e.getMessage());
        }
        return date;
    }

    // 节假日集合（示例数据，需根据实际情况维护）
    private static final Set<LocalDate> HOLIDAYS = new HashSet<>();
    // 补班工作日集合（周末需要工作的日期）
    private static final Set<LocalDate> EXTRA_WORK_DAYS = new HashSet<>();

    static {
        // 法定节假日（需排除调休的周末）
        // 1. 元旦：1月1日至3日放假调休，共3天[citation:1][citation:2]
        LocalDate newYearStart = LocalDate.of(2026, 1, 1);
        LocalDate newYearEnd = LocalDate.of(2026, 1, 3);
        addDateRange(HOLIDAYS, newYearStart, newYearEnd);

        // 2. 春节：2月15日至23日放假调休，共9天[citation:1][citation:2]
        LocalDate springFestivalStart = LocalDate.of(2026, 2, 15);
        LocalDate springFestivalEnd = LocalDate.of(2026, 2, 23);
        addDateRange(HOLIDAYS, springFestivalStart, springFestivalEnd);

        // 3. 清明节：4月4日至6日放假，共3天[citation:1][citation:2]
        LocalDate qingmingStart = LocalDate.of(2026, 4, 4);
        LocalDate qingmingEnd = LocalDate.of(2026, 4, 6);
        addDateRange(HOLIDAYS, qingmingStart, qingmingEnd);

        // 4. 劳动节：5月1日至5日放假调休，共5天[citation:1][citation:2]
        LocalDate laborDayStart = LocalDate.of(2026, 5, 1);
        LocalDate laborDayEnd = LocalDate.of(2026, 5, 5);
        addDateRange(HOLIDAYS, laborDayStart, laborDayEnd);

        // 5. 端午节：6月19日至21日放假，共3天[citation:1][citation:2]
        LocalDate dragonBoatStart = LocalDate.of(2026, 6, 19);
        LocalDate dragonBoatEnd = LocalDate.of(2026, 6, 21);
        addDateRange(HOLIDAYS, dragonBoatStart, dragonBoatEnd);

        // 6. 中秋节：9月25日至27日放假，共3天[citation:1][citation:2]
        LocalDate midAutumnStart = LocalDate.of(2026, 9, 25);
        LocalDate midAutumnEnd = LocalDate.of(2026, 9, 27);
        addDateRange(HOLIDAYS, midAutumnStart, midAutumnEnd);

        // 7. 国庆节：10月1日至7日放假调休，共7天[citation:1][citation:2]
        LocalDate nationalDayStart = LocalDate.of(2026, 10, 1);
        LocalDate nationalDayEnd = LocalDate.of(2026, 10, 7);
        addDateRange(HOLIDAYS, nationalDayStart, nationalDayEnd);

        // 调休补班日期（周末需要上班的日期）
        // 元旦调休：1月4日（周日）上班[citation:1][citation:2]
        EXTRA_WORK_DAYS.add(LocalDate.of(2026, 1, 4));

        // 春节调休：2月14日（周六）、2月28日（周六）上班[citation:1][citation:2]
        EXTRA_WORK_DAYS.add(LocalDate.of(2026, 2, 14));
        EXTRA_WORK_DAYS.add(LocalDate.of(2026, 2, 28));

        // 劳动节调休：5月9日（周六）上班[citation:1][citation:2]
        EXTRA_WORK_DAYS.add(LocalDate.of(2026, 5, 9));

        // 国庆节调休：9月20日（周日）、10月10日（周六）上班[citation:1][citation:2]
        EXTRA_WORK_DAYS.add(LocalDate.of(2026, 9, 20));
        EXTRA_WORK_DAYS.add(LocalDate.of(2026, 10, 10));
    }

    private static void addDateRange(Set<LocalDate> set, LocalDate start, LocalDate end) {
        for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
            set.add(date);
        }
    }

    /**
     * 判断当前时间是否在交易时间内（工作日+非节假日+9:30~16:00）
     *
     * @return true-在交易时间内；false-不在交易时间
     */
    public static boolean isCurrentTimeInRange() {
        // 使用中国时区
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);
        LocalTime now = LocalTime.now(zone);

        // 1. 判断是否为有效工作日（排除周末和节假日）
        if (!isValidWorkday(today)) {
            return false;
        }

        // 2. 判断时间范围
        LocalTime start = LocalTime.of(9, 30);
        LocalTime end = LocalTime.of(15, 02);
        return !now.isBefore(start) && !now.isAfter(end);
    }

    /**
     * 判断当前时间是否在节假日
     *
     * @return true-在交易时间内；false-不在交易时间
     */
    public static boolean isValidWorkday() {
        // 使用中国时区
        ZoneId zone = ZoneId.of("Asia/Shanghai");
        LocalDate today = LocalDate.now(zone);

        // 1. 判断是否为有效工作日（排除周末和节假日）
        return isValidWorkday(today);
    }

    /**
     * 判断是否为有效工作日
     */
    private static boolean isValidWorkday(LocalDate date) {
        // 先检查补班日期（周末但需要工作）
        if (EXTRA_WORK_DAYS.contains(date)) {
            return true;
        }

        // 检查是否为周末
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false;
        }

        // 检查是否为节假日
        return !HOLIDAYS.contains(date);
    }

    // 以下方法供外部维护节假日数据用
    public static void addHoliday(LocalDate date) {
        HOLIDAYS.add(date);
    }

    public static void removeHoliday(LocalDate date) {
        HOLIDAYS.remove(date);
    }

    public static void addExtraWorkDay(LocalDate date) {
        EXTRA_WORK_DAYS.add(date);
    }

    public static void removeExtraWorkDay(LocalDate date) {
        EXTRA_WORK_DAYS.remove(date);
    }
}
