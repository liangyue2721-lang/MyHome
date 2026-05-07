<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb8" v-if="chartDataLoaded">
      <el-col :span="6">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span>3日排行 Top10 (次数)</span>
          </div>
          <div ref="chart3Day" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span>5日排行 Top10 (次数)</span>
          </div>
          <div ref="chart5Day" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span>季度排行 Top10 (次数)</span>
          </div>
          <div ref="chartQuarter" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span>年度排行 Top10 (次数)</span>
          </div>
          <div ref="chartYear" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="交易日期" prop="tradeDate">
        <el-date-picker clearable
                        v-model="queryParams.tradeDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择交易日期 (按天分区)">
        </el-date-picker>
      </el-form-item>
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
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['stock:stockFlow:add']"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['stock:stockFlow:edit']"
        >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['stock:stockFlow:remove']"
        >删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['stock:stockFlow:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

        <div class="mb8" style="margin-bottom: 20px; margin-top: 10px;">
      <el-radio-group v-model="queryParams.rankDays" @change="handleQuery">
        <el-radio :label="1">今日排行</el-radio>
        <el-radio :label="3">3日排行</el-radio>
        <el-radio :label="5">5日排行</el-radio>
        <el-radio :label="10">10日排行</el-radio>
      </el-radio-group>
    </div>

    <el-table v-loading="loading" :data="stockFlowList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="序号" type="index" width="50" align="center" />
      <el-table-column label="代码" align="center" prop="stockCode">
        <template slot-scope="scope">
          <span style="color: #409EFF; cursor: pointer;">{{ scope.row.stockCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="名称" align="center" prop="stockName">
        <template slot-scope="scope">
          <span style="color: #409EFF; cursor: pointer;">{{ scope.row.stockName }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最新价" align="center" prop="latestPrice">
        <template slot-scope="scope">
          <span :class="getColorClass(scope.row.changePercent)">{{ formatPrice(scope.row.latestPrice) }}</span>
        </template>
      </el-table-column>
      <el-table-column :label="rankPrefix + '涨跌幅'" align="center" prop="changePercent">
        <template slot-scope="scope">
          <span :class="getColorClass(scope.row.changePercent)">{{ formatRatio(scope.row.changePercent) }}</span>
        </template>
      </el-table-column>

      <el-table-column :label="rankPrefix + '主力净流入'" align="center">
        <el-table-column label="净额" align="center" prop="mainNetInflow">
          <template slot-scope="scope">
            <span :class="getColorClass(scope.row.mainNetInflow)">{{ formatAmount(scope.row.mainNetInflow) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="净占比" align="center" prop="mainInflowRatio">
          <template slot-scope="scope">
            <span :class="getColorClass(scope.row.mainInflowRatio)">{{ formatRatio(scope.row.mainInflowRatio) }}</span>
          </template>
        </el-table-column>
      </el-table-column>

      <el-table-column :label="rankPrefix + '超大单净流入'" align="center">
        <el-table-column label="净额" align="center" prop="superLargeInflow">
          <template slot-scope="scope">
            <span :class="getColorClass(scope.row.superLargeInflow)">{{ formatAmount(scope.row.superLargeInflow) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="净占比" align="center" prop="superLargeRatio">
          <template slot-scope="scope">
            <span :class="getColorClass(scope.row.superLargeRatio)">{{ formatRatio(scope.row.superLargeRatio) }}</span>
          </template>
        </el-table-column>
      </el-table-column>

      <el-table-column :label="rankPrefix + '大单净流入'" align="center">
        <el-table-column label="净额" align="center" prop="largeInflow">
          <template slot-scope="scope">
            <span :class="getColorClass(scope.row.largeInflow)">{{ formatAmount(scope.row.largeInflow) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="净占比" align="center" prop="largeRatio">
          <template slot-scope="scope">
            <span :class="getColorClass(scope.row.largeRatio)">{{ formatRatio(scope.row.largeRatio) }}</span>
          </template>
        </el-table-column>
      </el-table-column>

      <el-table-column :label="rankPrefix + '中单净流入'" align="center">
        <el-table-column label="净额" align="center" prop="mediumInflow">
          <template slot-scope="scope">
            <span :class="getColorClass(scope.row.mediumInflow)">{{ formatAmount(scope.row.mediumInflow) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="净占比" align="center" prop="mediumRatio">
          <template slot-scope="scope">
            <span :class="getColorClass(scope.row.mediumRatio)">{{ formatRatio(scope.row.mediumRatio) }}</span>
          </template>
        </el-table-column>
      </el-table-column>

      <el-table-column :label="rankPrefix + '小单净流入'" align="center">
        <el-table-column label="净额" align="center" prop="smallInflow">
          <template slot-scope="scope">
            <span :class="getColorClass(scope.row.smallInflow)">{{ formatAmount(scope.row.smallInflow) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="净占比" align="center" prop="smallRatio">
          <template slot-scope="scope">
            <span :class="getColorClass(scope.row.smallRatio)">{{ formatRatio(scope.row.smallRatio) }}</span>
          </template>
        </el-table-column>
      </el-table-column>

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:stockFlow:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['stock:stockFlow:remove']"
          >删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改主力资金流向对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="交易日期 (按天分区或查询过滤使用)" prop="tradeDate">
          <el-date-picker clearable
                          v-model="form.tradeDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择交易日期 (按天分区或查询过滤使用)">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="股票代码" prop="stockCode">
          <el-input v-model="form.stockCode" placeholder="请输入股票代码"/>
        </el-form-item>
        <el-form-item label="股票名称" prop="stockName">
          <el-input v-model="form.stockName" placeholder="请输入股票名称"/>
        </el-form-item>
        <el-form-item label="最新价 (元)" prop="latestPrice">
          <el-input v-model="form.latestPrice" placeholder="请输入最新价 (元)"/>
        </el-form-item>
        <el-form-item label="涨跌幅 (%)" prop="changePercent">
          <el-input v-model="form.changePercent" placeholder="请输入涨跌幅 (%)"/>
        </el-form-item>
        <el-form-item label="当天成交总额 (元)" prop="totalAmount">
          <el-input v-model="form.totalAmount" placeholder="请输入当天成交总额 (元)"/>
        </el-form-item>
        <el-form-item label="主力净流入净额 (元)" prop="mainNetInflow">
          <el-input v-model="form.mainNetInflow" placeholder="请输入主力净流入净额 (元)"/>
        </el-form-item>
        <el-form-item label="主力净流入占比 (%)" prop="mainInflowRatio">
          <el-input v-model="form.mainInflowRatio" placeholder="请输入主力净流入占比 (%)"/>
        </el-form-item>
        <el-form-item label="超大单净流入额 (元)" prop="superLargeInflow">
          <el-input v-model="form.superLargeInflow" placeholder="请输入超大单净流入额 (元)"/>
        </el-form-item>
        <el-form-item label="超大单净流入占比 (%)" prop="superLargeRatio">
          <el-input v-model="form.superLargeRatio" placeholder="请输入超大单净流入占比 (%)"/>
        </el-form-item>
        <el-form-item label="大单净流入额 (元)" prop="largeInflow">
          <el-input v-model="form.largeInflow" placeholder="请输入大单净流入额 (元)"/>
        </el-form-item>
        <el-form-item label="大单净流入占比 (%)" prop="largeRatio">
          <el-input v-model="form.largeRatio" placeholder="请输入大单净流入占比 (%)"/>
        </el-form-item>
        <el-form-item label="中单净流入额 (元)" prop="mediumInflow">
          <el-input v-model="form.mediumInflow" placeholder="请输入中单净流入额 (元)"/>
        </el-form-item>
        <el-form-item label="中单净流入占比 (%)" prop="mediumRatio">
          <el-input v-model="form.mediumRatio" placeholder="请输入中单净流入占比 (%)"/>
        </el-form-item>
        <el-form-item label="小单净流入额 (元)" prop="smallInflow">
          <el-input v-model="form.smallInflow" placeholder="请输入小单净流入额 (元)"/>
        </el-form-item>
        <el-form-item label="小单净流入占比 (%)" prop="smallRatio">
          <el-input v-model="form.smallRatio" placeholder="请输入小单净流入占比 (%)"/>
        </el-form-item>
        <el-form-item label="源数据更新时间戳 (Unix秒)" prop="dataTimestamp">
          <el-input v-model="form.dataTimestamp" placeholder="请输入源数据更新时间戳 (Unix秒)"/>
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
import {listStockFlow, getStockFlow, delStockFlow, addStockFlow, updateStockFlow, getChartData} from "@/api/stock/stockFlow"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
import * as echarts from 'echarts';

export default {
  name: "StockFlow",
  computed: {
    rankPrefix() {
      return this.queryParams.rankDays === 1 ? '今日' : this.queryParams.rankDays + '日';
    }
  },

  data() {
    return {
      chartDataLoaded: false,
      charts: {},
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
      // 主力资金流向表格数据
      stockFlowList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        rankDays: 1,
        pageNum: 1,
        pageSize: 10,
        tradeDate: null,
        stockCode: null,
        stockName: null,
        marketType: null,
        latestPrice: null,
        changePercent: null,
        totalAmount: null,
        mainNetInflow: null,
        mainInflowRatio: null,
        superLargeInflow: null,
        superLargeRatio: null,
        largeInflow: null,
        largeRatio: null,
        mediumInflow: null,
        mediumRatio: null,
        smallInflow: null,
        smallRatio: null,
        dataTimestamp: null,
        createdAt: null,
        updatedAt: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        tradeDate: [
          {required: true, message: "交易日期 (按天分区或查询过滤使用)不能为空", trigger: "blur"}
        ],
        stockCode: [
          {required: true, message: "股票代码不能为空", trigger: "blur"}
        ],
        stockName: [
          {required: true, message: "股票名称不能为空", trigger: "blur"}
        ],
        marketType: [
          {required: true, message: "市场类型: 0-深交所, 1-上交所不能为空", trigger: "change"}
        ],
        createdAt: [
          {required: true, message: "记录创建时间不能为空", trigger: "blur"}
        ],
        updatedAt: [
          {required: true, message: "记录更新时间不能为空", trigger: "blur"}
        ]
      }
    }
  },
  async created() {
    // 获取用户列表并设置 userId
    await this.initUserList();
    // 加载数据
    this.getList();
    this.loadChartData();
  },
  mounted() {
    window.addEventListener('resize', this.resizeCharts);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    Object.values(this.charts).forEach(chart => chart.dispose());
  },
  methods: {
    loadChartData() {
      getChartData().then(response => {
        if (response.code === 200) {
          this.chartDataLoaded = true;
          this.$nextTick(() => {
            this.initChart('chart3Day', response.data.day3 || []);
            this.initChart('chart5Day', response.data.day5 || []);
            this.initChart('chartQuarter', response.data.quarter || []);
            this.initChart('chartYear', response.data.year || []);
          });
        }
      });
    },
    initChart(refName, dataList) {
      if (!this.$refs[refName]) return;

      const chart = echarts.init(this.$refs[refName]);
      this.charts[refName] = chart;

      const xData = dataList.map(item => item.stockName);
      const yData = dataList.map(item => item.count);

      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: xData,
          axisLabel: {
            interval: 0,
            rotate: 45
          }
        },
        yAxis: {
          type: 'value',
          minInterval: 1
        },
        series: [
          {
            name: '上榜次数',
            type: 'bar',
            barWidth: '60%',
            data: yData,
            itemStyle: {
              color: '#409EFF'
            }
          }
        ]
      };

      chart.setOption(option);
    },
    resizeCharts() {
      Object.values(this.charts).forEach(chart => {
        if (chart) chart.resize();
      });
    },
    formatAmount(amount) {
      if (amount === null || amount === undefined) return '-';
      const num = Number(amount);
      if (isNaN(num)) return amount;
      if (Math.abs(num) >= 100000000) {
        return (num / 100000000).toFixed(2) + '亿';
      } else if (Math.abs(num) >= 10000) {
        return (num / 10000).toFixed(2) + '万';
      } else {
        return num.toFixed(2);
      }
    },
    formatRatio(ratio) {
      if (ratio === null || ratio === undefined) return '-';
      const num = Number(ratio);
      if (isNaN(num)) return ratio;
      return num.toFixed(2) + '%';
    },
    formatPrice(price) {
      if (price === null || price === undefined) return '-';
      const num = Number(price);
      if (isNaN(num)) return price;
      return num.toFixed(2);
    },
    getColorClass(val) {
      if (val === null || val === undefined) return '';
      const num = Number(val);
      if (isNaN(num) || num === 0) return '';
      return num > 0 ? 'text-red' : 'text-green';
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
    /** 查询主力资金流向列表 */
    getList() {
      this.loading = true
      listStockFlow(this.queryParams).then(response => {
        this.stockFlowList = response.rows
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
        tradeDate: null,
        stockCode: null,
        stockName: null,
        marketType: null,
        latestPrice: null,
        changePercent: null,
        totalAmount: null,
        mainNetInflow: null,
        mainInflowRatio: null,
        superLargeInflow: null,
        superLargeRatio: null,
        largeInflow: null,
        largeRatio: null,
        mediumInflow: null,
        mediumRatio: null,
        smallInflow: null,
        smallRatio: null,
        dataTimestamp: null,
        createdAt: null,
        updatedAt: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
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
      this.title = "添加主力资金流向"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getStockFlow(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改主力资金流向"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateStockFlow(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addStockFlow(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除主力资金流向编号为"' + ids + '"的数据项？').then(function () {
        return delStockFlow(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/stockFlow/export', {
        ...this.queryParams
      }, `stockFlow_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.text-red {
  color: #f56c6c;
}
.text-green {
  color: #3f9000;
}
</style>
