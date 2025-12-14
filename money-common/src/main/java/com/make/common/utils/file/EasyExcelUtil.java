package com.make.common.utils.file;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.read.builder.ExcelReaderBuilder;

import java.util.ArrayList;
import java.util.List;


/**
 * Excel文件读取工具类，使用阿里的EasyExcel库实现。
 * 提供支持 Object 和 泛型类型的读取方法。
 *
 * @author 84522
 */
public class EasyExcelUtil {

    /**
     * 读取指定Excel文件中的数据，并将数据存储在列表中返回。
     * 适用于不关心对象类型的场景，返回 List<Object>
     *
     * @param filePath Excel文件路径
     * @return 从Excel文件中读取的数据列表（Object 类型）
     */
    public static List<Object> readExcel(String filePath) {
        List<Object> dataList = new ArrayList<>();
        ExcelReaderBuilder excelReaderBuilder = EasyExcel.read(filePath, new ExcelDataListener<>(dataList));
        excelReaderBuilder.sheet().doRead();
        return dataList;
    }

    /**
     * 读取指定Excel文件中的数据，并将数据映射为指定类型的对象列表返回。
     *
     * @param filePath Excel文件路径
     * @param clazz    数据模型对应的类（必须含有 @ExcelProperty 注解）
     * @param <T>      返回的对象类型
     * @return 从Excel文件中读取的指定类型对象列表
     */
    public static <T> List<T> readExcel(String filePath, Class<T> clazz) {
        List<T> dataList = new ArrayList<>();
        EasyExcel.read(filePath, clazz, new ExcelDataListener<>(dataList))
                .sheet()
                .doRead();
        return dataList;
    }
}