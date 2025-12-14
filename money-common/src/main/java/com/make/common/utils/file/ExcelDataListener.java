package com.make.common.utils.file;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;

import java.util.List;

/**
 * 通用 Excel 数据读取监听器，支持泛型类型。
 *
 * @param <T> Excel 行数据的目标对象类型
 */
public class ExcelDataListener<T> implements ReadListener<T> {

    private final List<T> dataList;

    /**
     * 构造函数，初始化数据列表容器。
     *
     * @param dataList 用于保存读取数据的列表
     */
    public ExcelDataListener(List<T> dataList) {
        this.dataList = dataList;
    }

    /**
     * 每读取一行数据时的处理逻辑。
     *
     * @param data    当前行数据
     * @param context 分析上下文
     */
    @Override
    public void invoke(T data, AnalysisContext context) {
        dataList.add(data);
        System.out.println("Read data: " + data);
    }

    /**
     * 所有数据读取完成后的处理逻辑。
     *
     * @param context 分析上下文
     */
    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        System.out.println("All data read finished.");
    }

    /**
     * 读取过程中异常处理逻辑。
     *
     * @param exception 异常信息
     * @param context   分析上下文
     * @throws Exception 抛出异常
     */
    @Override
    public void onException(Exception exception, AnalysisContext context) throws Exception {
        System.err.println("Read error: " + exception.getMessage());
        ReadListener.super.onException(exception, context);
    }
}
