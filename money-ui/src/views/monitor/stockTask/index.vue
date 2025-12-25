<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="股票代码" prop="stockCode">
        <el-input
          v-model="queryParams.stockCode"
          placeholder="请输入股票代码"
          clearable
          size="small"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-refresh"
          size="mini"
          @click="getList"
        >刷新</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="taskList" :stripe="true" :border="false">
      <el-table-column label="股票代码" align="center" prop="stockCode" />
      <el-table-column label="股票名称" align="center" prop="stockName" />
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <el-tag :type="statusType(scope.row.status)">{{ scope.row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="占用节点 (IP)" align="center" prop="occupiedByNode" />
      <el-table-column label="占用时间" align="center" prop="occupiedTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.occupiedTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="Trace ID" align="center" prop="traceId" :show-overflow-tooltip="true" />
      <el-table-column label="执行结果" align="center" prop="lastResult" :show-overflow-tooltip="true" />
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />
  </div>
</template>

<script>
import { listStockTask } from "@/api/monitor/stockTask";

export default {
  name: "StockTaskMonitor",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 表格数据
      taskList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        stockCode: null
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询列表 */
    getList() {
      this.loading = true;
      listStockTask(this.queryParams).then(response => {
        this.taskList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    statusType(status) {
      if (status === 'SUCCESS') return 'success';
      if (status === 'FAILED') return 'danger';
      if (status === 'RUNNING') return 'primary';
      if (status === 'OCCUPIED') return 'warning';
      if (status === 'SKIPPED') return 'info';
      return '';
    }
  }
};
</script>
