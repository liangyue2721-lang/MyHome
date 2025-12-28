<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" @tab-click="handleTabClick">
      <!-- 股票维度 (Original Content) -->
      <el-tab-pane label="股票维度" name="stock">
        <!-- 统计图表卡片 -->
        <el-card shadow="hover" class="mb8" style="margin-bottom: 20px;">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-pie-chart"></i> 任务执行统计 {{ queryParams.stockCode ? `(${queryParams.stockCode})` : '(全局)' }}</span>
          </div>
          <div id="stats-chart" style="width: 100%; height: 250px;"></div>
        </el-card>

        <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
          <el-form-item label="股票代码" prop="stockCode">
            <el-input
              v-model="queryParams.stockCode"
              placeholder="请输入股票代码"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="股票名称" prop="stockName">
            <el-input
              v-model="queryParams.stockName"
              placeholder="请输入股票名称"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="执行结果" prop="executeResult">
            <el-input
              v-model="queryParams.executeResult"
              placeholder="请输入执行结果"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="执行节点IP" prop="nodeIp">
            <el-input
              v-model="queryParams.nodeIp"
              placeholder="请输入执行节点IP"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
          <el-form-item label="Trace ID" prop="traceId">
            <el-input
              v-model="queryParams.traceId"
              placeholder="请输入Trace ID"
              clearable
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
              type="warning"
              plain
              icon="el-icon-download"
              size="mini"
              @click="handleExport"
              v-hasPermi="['quartz:refresh_execute_record:export']"
            >导出
            </el-button>
          </el-col>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
        </el-row>

        <el-table v-loading="loading" :data="refresh_execute_recordList" @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55" align="center"/>
          <el-table-column label="主键ID" align="center" prop="id"/>
          <el-table-column label="股票代码" align="center" prop="stockCode"/>
          <el-table-column label="股票名称" align="center" prop="stockName"/>
          <el-table-column label="任务状态" align="center" prop="status"/>
          <el-table-column label="执行结果" align="center" prop="executeResult"/>
          <el-table-column label="执行节点IP" align="center" prop="nodeIp"/>
          <el-table-column label="任务执行时间" align="center" prop="executeTime" width="180">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.executeTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
            </template>
          </el-table-column>
          <el-table-column label="Trace ID" align="center" prop="traceId"/>
        </el-table>

        <pagination
          v-show="total>0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="getList"
        />
      </el-tab-pane>

      <!-- 节点维度 (New Content) -->
      <el-tab-pane label="节点维度" name="node">
        <el-card shadow="hover" class="mb8">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-s-data"></i> 节点执行统计 (全局)</span>
          </div>
          <div id="node-stats-chart" style="width: 100%; height: 250px;"></div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 添加或修改刷新任务执行记录对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="股票代码" prop="stockCode">
          <el-input v-model="form.stockCode" placeholder="请输入股票代码"/>
        </el-form-item>
        <el-form-item label="股票名称" prop="stockName">
          <el-input v-model="form.stockName" placeholder="请输入股票名称"/>
        </el-form-item>
        <el-form-item label="执行结果" prop="executeResult">
          <el-input v-model="form.executeResult" placeholder="请输入执行结果"/>
        </el-form-item>
        <el-form-item label="执行节点IP" prop="nodeIp">
          <el-input v-model="form.nodeIp" placeholder="请输入执行节点IP"/>
        </el-form-item>
        <el-form-item label="任务执行时间" prop="executeTime">
          <el-date-picker clearable
                          v-model="form.executeTime"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择任务执行时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="Trace ID" prop="traceId">
          <el-input v-model="form.traceId" placeholder="请输入Trace ID"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listRefresh_execute_record,
  getRefreshExecuteStats,
  getRefreshExecuteStatsByNodeIp,
  getRefresh_execute_record,
  delRefresh_execute_record,
  addRefresh_execute_record,
  updateRefresh_execute_record
} from "@/api/quartz/refresh_execute_record"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
import * as echarts from 'echarts'

export default {
  name: "Refresh_execute_record",
  data() {
    return {
      activeTab: 'stock',
      // 统计图表实例
      chartInstance: null,
      nodeChartInstance: null,
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 刷新任务执行记录表格数据
      refresh_execute_recordList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        stockCode: null,
        stockName: null,
        status: null,
        executeResult: null,
        nodeIp: null,
        executeTime: null,
        traceId: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        stockCode: [
          {required: true, message: "股票代码不能为空", trigger: "blur"}
        ],
        stockName: [
          {required: true, message: "股票名称不能为空", trigger: "blur"}
        ],
        status: [
          {required: true, message: "任务状态不能为空", trigger: "change"}
        ],
        nodeIp: [
          {required: true, message: "执行节点IP不能为空", trigger: "blur"}
        ],
        executeTime: [
          {required: true, message: "任务执行时间不能为空", trigger: "blur"}
        ],
        traceId: [
          {required: true, message: "Trace ID不能为空", trigger: "blur"}
        ],
        createTime: [
          {required: true, message: "创建时间不能为空", trigger: "blur"}
        ],
        updateTime: [
          {required: true, message: "更新时间不能为空", trigger: "blur"}
        ]
      }
    }
  },
  async created() {
    // 获取用户列表并设置 userId
    await this.initUserList();
    // 加载数据
    this.getList();
  },
  mounted() {
    this.initChart();
    this.getStatsData();
    window.addEventListener('resize', this.resizeAllCharts);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeAllCharts);
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
    if (this.nodeChartInstance) {
      this.nodeChartInstance.dispose();
    }
  },
  methods: {
    handleTabClick(tab) {
      this.$nextTick(() => {
        if (tab.name === 'stock') {
          this.resizeChart();
        } else if (tab.name === 'node') {
          if (!this.nodeChartInstance) {
            this.initNodeChart();
            this.getNodeStatsData();
          } else {
            this.resizeNodeChart();
          }
        }
      });
    },
    resizeAllCharts() {
      this.resizeChart();
      this.resizeNodeChart();
    },

    // --- Stock Chart Logic ---
    initChart() {
      const chartDom = document.getElementById('stats-chart');
      if (chartDom) {
        this.chartInstance = echarts.init(chartDom);
      }
    },
    resizeChart() {
      if (this.chartInstance) {
        this.chartInstance.resize();
      }
    },
    getStatsData() {
      if (!this.chartInstance) return;

      this.chartInstance.showLoading();
      const params = {};
      if (this.queryParams.stockCode) {
        params.stockCode = this.queryParams.stockCode;
      }

      getRefreshExecuteStats(params).then(response => {
        this.chartInstance.hideLoading();
        const data = response.data || [];
        const isSingleMode = !!params.stockCode;

        if (isSingleMode) {
          // Single Stock Mode
          this.renderSingleChart(data);
        } else {
          // Grid Mode (List of stocks)
          this.renderGridChart(data);
        }
      }).catch(error => {
        console.error("Failed to load chart stats", error);
        this.chartInstance.hideLoading();
      });
    },

    renderSingleChart(data) {
      // Logic for single chart (same as before)
      let successCount = 0;
      let failedCount = 0;
      data.forEach(item => {
        if (item.status === 'SUCCESS') successCount = Number(item.count);
        if (item.status === 'FAILED') failedCount = Number(item.count);
      });

      const chartData = [
        { value: successCount, name: '成功', itemStyle: { color: '#67C23A' } },
        { value: failedCount, name: '失败', itemStyle: { color: '#F56C6C' } }
      ];
      if (successCount === 0 && failedCount === 0) {
        chartData.push({ value: 0, name: '无数据', itemStyle: { color: '#909399' } });
      }

      // Reset height for single chart
      const chartDom = document.getElementById('stats-chart');
      if (chartDom) {
        chartDom.style.height = '250px';
        this.chartInstance.resize();
      }

      const option = {
        title: {
          text: this.queryParams.stockCode,
          left: 'center',
          top: 'center',
          textStyle: { fontSize: 16 }
        },
        tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
        series: [{
          name: '执行结果',
          type: 'pie',
          radius: ['50%', '70%'],
          center: ['50%', '50%'],
          itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          data: chartData
        }]
      };
      // Clear previous settings (like multiple titles)
      this.chartInstance.clear();
      this.chartInstance.setOption(option);
    },

    renderGridChart(data) {
      // Group data by stockCode
      const stockStats = {};
      data.forEach(item => {
        // Robust check for stock code key (Map return from backend might vary in case)
        const code = item.stockCode || item.stock_code || item.STOCK_CODE || 'Unknown';
        if (!stockStats[code]) stockStats[code] = { success: 0, failed: 0 };
        if (item.status === 'SUCCESS') stockStats[code].success = Number(item.count);
        if (item.status === 'FAILED') stockStats[code].failed = Number(item.count);
      });

      const stockCodes = Object.keys(stockStats).sort(); // Sort alphabetically
      const totalStocks = stockCodes.length;

      // 10 columns per row
      const cols = 10;
      const rowHeight = 150; // px per row
      const rows = Math.ceil(totalStocks / cols);
      const totalHeight = Math.max(250, rows * rowHeight);

      // Resize container
      const chartDom = document.getElementById('stats-chart');
      if (chartDom) {
        chartDom.style.height = totalHeight + 'px';
        this.chartInstance.resize();
      }

      const seriesList = [];
      const titleList = [];

      stockCodes.forEach((code, index) => {
        const stats = stockStats[code];
        const colIndex = index % cols;
        const rowIndex = Math.floor(index / cols);

        // Center X: 5%, 15%, 25% ... 95%
        const centerX = (colIndex * 10 + 5) + '%';
        // Center Y: row top + half height
        const centerY = (rowIndex * rowHeight + rowHeight / 2);

        const chartData = [
          { value: stats.success, name: '成功', itemStyle: { color: '#67C23A' } },
          { value: stats.failed, name: '失败', itemStyle: { color: '#F56C6C' } }
        ];

        seriesList.push({
          type: 'pie',
          radius: [30, 45], // Fixed px size for uniform look
          center: [centerX, centerY],
          itemStyle: { borderRadius: 3, borderColor: '#fff', borderWidth: 1 },
          label: { show: false },
          hoverAnimation: false, // Reduce performance cost
          data: chartData
        });

        titleList.push({
          text: code,
          left: centerX,
          top: centerY, // Center text in pie
          textAlign: 'center',
          textVerticalAlign: 'middle',
          textStyle: { fontSize: 10, fontWeight: 'normal' }
        });
      });

      const option = {
        tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' }, // {a} is series name (Stock Code?) No, series name is undefined here.
        // ECharts doesn't bind title to tooltip easily.
        // Let's rely on hover. Or we can set series name to stock code.
        title: titleList,
        series: seriesList.map((s, i) => ({ ...s, name: stockCodes[i] }))
      };

      this.chartInstance.clear();
      this.chartInstance.setOption(option);
    },

    // --- Node Chart Logic ---
    initNodeChart() {
      const chartDom = document.getElementById('node-stats-chart');
      if (chartDom) {
        this.nodeChartInstance = echarts.init(chartDom);
      }
    },
    resizeNodeChart() {
      if (this.nodeChartInstance) {
        this.nodeChartInstance.resize();
      }
    },
    getNodeStatsData() {
      if (!this.nodeChartInstance) return;

      this.nodeChartInstance.showLoading();
      getRefreshExecuteStatsByNodeIp().then(response => {
        this.nodeChartInstance.hideLoading();
        const data = response.data || [];
        this.renderNodeGridChart(data);
      }).catch(error => {
        console.error("Failed to load node chart stats", error);
        this.nodeChartInstance.hideLoading();
      });
    },
    renderNodeGridChart(data) {
      const nodeStats = {};
      data.forEach(item => {
        const ip = item.nodeIp || 'Unknown';
        if (!nodeStats[ip]) nodeStats[ip] = { success: 0, failed: 0 };
        if (item.status === 'SUCCESS') nodeStats[ip].success = Number(item.count);
        if (item.status === 'FAILED') nodeStats[ip].failed = Number(item.count);
      });

      const nodeIps = Object.keys(nodeStats).sort();
      const totalNodes = nodeIps.length;

      // 5 columns per row for Nodes (assuming fewer nodes than stocks, make them bigger)
      const cols = 5;
      const rowHeight = 200;
      const rows = Math.ceil(totalNodes / cols);
      const totalHeight = Math.max(250, rows * rowHeight);

      const chartDom = document.getElementById('node-stats-chart');
      if (chartDom) {
        chartDom.style.height = totalHeight + 'px';
        this.nodeChartInstance.resize();
      }

      const seriesList = [];
      const titleList = [];

      nodeIps.forEach((ip, index) => {
        const stats = nodeStats[ip];
        const colIndex = index % cols;
        const rowIndex = Math.floor(index / cols);

        // Center X: 10%, 30%, 50%, 70%, 90%
        const centerX = (colIndex * 20 + 10) + '%';
        const centerY = (rowIndex * rowHeight + rowHeight / 2);

        const chartData = [
          { value: stats.success, name: '成功', itemStyle: { color: '#67C23A' } },
          { value: stats.failed, name: '失败', itemStyle: { color: '#F56C6C' } }
        ];

        seriesList.push({
          type: 'pie',
          radius: [40, 60], // Bigger than stock pie
          center: [centerX, centerY],
          itemStyle: { borderRadius: 5, borderColor: '#fff', borderWidth: 2 },
          label: { show: false },
          hoverAnimation: false,
          data: chartData
        });

        titleList.push({
          text: ip,
          left: centerX,
          top: centerY,
          textAlign: 'center',
          textVerticalAlign: 'middle',
          textStyle: { fontSize: 12, fontWeight: 'bold' }
        });
      });

      const option = {
        tooltip: { trigger: 'item', formatter: '{a} <br/>{b}: {c} ({d}%)' },
        title: titleList,
        series: seriesList.map((s, i) => ({ ...s, name: nodeIps[i] }))
      };

      this.nodeChartInstance.clear();
      this.nodeChartInstance.setOption(option);
    },

    /**
     * 初始化用户列表数据
     * @returns {Promise<void>} 异步操作完成Promise
     */
    async initUserList() {
      try {
        // 调用后端接口获取用户列表，传入分页参数
        const response = await listUser({pageSize: this.pageSize});
        // 兼容接口返回格式，优先取 response.data，再取 response 本身
        const payload = response.data || response;
        // 根据返回数据格式判断用户列表位置，支持两种结构
        const rawUsers = Array.isArray(payload.rows)
          ? payload.rows
          : Array.isArray(payload)
            ? payload
            : [];

        // 格式化用户列表，只保留用户ID和名称字段
        const userList = rawUsers.map(u => ({
          id: u.userId,
          // 优先使用昵称，没昵称用用户名，最后用默认“用户+ID”
          name: u.userName || u.nickName || `用户${u.userId}`
        }));

        console.log('用户列表加载完成，列表数据:', userList);

        if (userList.length) {
          // 从 cookie 中获取保存的用户名，假设使用 vue-cookies 插件
          const savedUsername = this.$cookies.get('username');
          console.log('从cookie获取的用户名:', savedUsername);

          // 查找与 cookie 中用户名匹配的用户
          const matchedUser = userList.find(u => u.name === savedUsername);
          if (matchedUser) {
            this.queryParams.userId = matchedUser.id; // 匹配成功，选中对应用户
            this.form.userId = matchedUser.id; // 匹配成功，选中对应用户
            console.log('选中cookie中的用户:', matchedUser);
          }
        } else {
          this.queryParams.userId = null; // 没有用户列表，清空选中状态
          this.$message.info('暂无用户数据');
          console.log('用户列表为空');
        }
      } catch (err) {
        console.error('用户列表加载失败:', err);
        this.$message.error('用户列表加载失败，请稍后重试');
      } finally {
      }
    },
    /** 查询刷新任务执行记录列表 */
    getList() {
      this.loading = true
      listRefresh_execute_record(this.queryParams).then(response => {
        this.refresh_execute_recordList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        stockCode: null,
        stockName: null,
        status: null,
        executeResult: null,
        nodeIp: null,
        executeTime: null,
        traceId: null,
        createTime: null,
        updateTime: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
      this.getStatsData() // Refresh chart
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
      // getStatsData is called inside handleQuery
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加刷新任务执行记录"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getRefresh_execute_record(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改刷新任务执行记录"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateRefresh_execute_record(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addRefresh_execute_record(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除刷新任务执行记录编号为"' + ids + '"的数据项？').then(function () {
        return delRefresh_execute_record(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('quartz/refresh_execute_record/export', {
        ...this.queryParams
      }, `refresh_execute_record_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
