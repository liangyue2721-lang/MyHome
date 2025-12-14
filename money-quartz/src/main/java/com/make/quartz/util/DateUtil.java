package com.make.quartz.util;

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
        // 1. 元旦：1月1日放假1天，不调休:cite[1]:cite[6]
        HOLIDAYS.add(LocalDate.of(2025, 1, 1));

        // 2. 春节：1月28日-2月4日放假，共8天
        LocalDate springFestivalStart = LocalDate.of(2025, 1, 28);
        LocalDate springFestivalEnd = LocalDate.of(2025, 2, 4);
        addDateRange(HOLIDAYS, springFestivalStart, springFestivalEnd);

        // 3. 清明节：4月4日-6日放假，共3天:cite[3]:cite[7]
        LocalDate qingmingStart = LocalDate.of(2025, 4, 4);
        LocalDate qingmingEnd = LocalDate.of(2025, 4, 6);
        addDateRange(HOLIDAYS, qingmingStart, qingmingEnd);

        // 4. 劳动节：5月1日-5日放假，共5天
        LocalDate laborDayStart = LocalDate.of(2025, 5, 1);
        LocalDate laborDayEnd = LocalDate.of(2025, 5, 5);
        addDateRange(HOLIDAYS, laborDayStart, laborDayEnd);

        // 5. 端午节：5月31日-6月2日放假，共3天:cite[6]:cite[8]
        LocalDate dragonBoatStart = LocalDate.of(2025, 5, 31);
        LocalDate dragonBoatEnd = LocalDate.of(2025, 6, 2);
        addDateRange(HOLIDAYS, dragonBoatStart, dragonBoatEnd);

        // 6. 国庆节+中秋节：10月1日-8日放假，共8天
        LocalDate nationalDayStart = LocalDate.of(2025, 10, 1);
        LocalDate nationalDayEnd = LocalDate.of(2025, 10, 8);
        addDateRange(HOLIDAYS, nationalDayStart, nationalDayEnd);

        // 调休补班日期（周末需要上班的日期）
        // 春节调休：1月26日（周日）、2月8日（周六）上班:cite[2]:cite[4]
        EXTRA_WORK_DAYS.add(LocalDate.of(2025, 1, 26));
        EXTRA_WORK_DAYS.add(LocalDate.of(2025, 2, 8));

        // 劳动节调休：4月27日（周日）上班:cite[9]
        EXTRA_WORK_DAYS.add(LocalDate.of(2025, 4, 27));

        // 国庆调休：9月28日（周日）、10月11日（周六）上班:cite[5]:cite[10]
        EXTRA_WORK_DAYS.add(LocalDate.of(2025, 9, 28));
        EXTRA_WORK_DAYS.add(LocalDate.of(2025, 10, 11));
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
