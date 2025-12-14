package com.make.stock.domain.dto;


import com.make.stock.domain.StockKline;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 单任务执行完后的中间结果
 * 存放本次任务需要保存/更新的所有记录
 * 不直接落库
 */
@Data
public class ProcessResult {

    /**
     * 本任务需要新增的记录
     */
    public List<StockKline> insertList = new ArrayList<>();

    /**
     * 本任务需要更新的记录
     */
    public List<StockKline> updateList = new ArrayList<>();

    /**
     * 是否处理成功
     */
    public boolean success = true;

    /**
     * 股票代码（仅用于日志）
     */
    public String stockCode;

    /**
     * 失败条数（记录解析失败、日期无效等）
     */
    public int failedCount = 0;
}
