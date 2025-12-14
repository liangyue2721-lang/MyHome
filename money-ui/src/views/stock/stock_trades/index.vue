<template>
  <div class="app-container">
    <el-card shadow="hover" class="chart-card-container mb-4">
      <div slot="header" class="clearfix">
        <span class="section-title"><i class="el-icon-s-data"></i> 资金分布概览</span>
      </div>
      <div class="liquid-chart-wrapper">
        <div class="chart-item">
          <div ref="liquidPrincipal" class="liquid-chart-box"></div>
        </div>
        <div class="chart-item">
          <div ref="liquidProfit" class="liquid-chart-box"></div>
        </div>
        <div class="chart-item">
          <div ref="liquidLoss" class="liquid-chart-box"></div>
        </div>
        <div class="chart-item">
          <div ref="liquidTotal" class="liquid-chart-box"></div>
        </div>
        <div class="chart-item">
          <div ref="liquidTotalProfit" class="liquid-chart-box"></div>
        </div>
      </div>
    </el-card>

    <el-card shadow="never" class="mb-4">
      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        v-show="showSearch"
        label-width="70px"
        class="search-form"
      >
        <el-form-item label="股票代码" prop="stockCode">
          <el-input
            v-model="queryParams.stockCode"
            placeholder="请输入股票代码"
            clearable
            prefix-icon="el-icon-search"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="股票名称" prop="stockName">
          <el-input
            v-model="queryParams.stockName"
            placeholder="请输入股票名称"
            clearable
            prefix-icon="el-icon-tickets"
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
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                     v-hasPermi="['stock:stock_trades:add']">新增交易
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
                     v-hasPermi="['stock:stock_trades:edit']">修改
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
                     v-hasPermi="['stock:stock_trades:export']">导出报表
          </el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>
    </el-card>

    <el-card shadow="never" class="table-card">
      <el-table
        v-loading="loading"
        :data="tradesList"
        @selection-change="handleSelectionChange"
        stripe
        border
        :header-cell-style="{background:'#f5f7fa', color:'#606266', fontWeight:'bold'}"
      >
        <el-table-column type="selection" width="50" align="center"/>
        <el-table-column label="股票代码" align="center" prop="stockCode" width="100">
          <template slot-scope="scope">
            <el-tag size="mini" type="info">{{ scope.row.stockCode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="股票名称" align="center" prop="stockName" min-width="100">
          <template slot-scope="scope">
            <span class="stock-name">{{ scope.row.stockName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="持仓数量" align="center" prop="initialShares" width="100"/>
        <el-table-column label="买入价" align="center" prop="buyPrice" width="100">
          <template slot-scope="scope">
            <span>{{ scope.row.buyPrice }}</span>
          </template>
        </el-table-column>
        <el-table-column label="卖出价" align="center" prop="sellPrice" width="100">
          <template slot-scope="scope">
            <span v-if="scope.row.sellPrice">{{ scope.row.sellPrice }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column label="涨幅" align="center" width="100">
          <template slot-scope="scope">
            <div v-if="scope.row.buyPrice && scope.row.sellPrice">
               <span :class="getProfitClass(scope.row.buyPrice, scope.row.sellPrice)">
                 {{ calculateIncreasePercentage(scope.row.buyPrice, scope.row.sellPrice) }}
               </span>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="总成本" align="center" prop="totalCost" width="120">
          <template slot-scope="scope">
            <b>{{ scope.row.totalCost }}</b>
          </template>
        </el-table-column>
        <el-table-column label="净利润" align="center" prop="netProfit" width="120">
          <template slot-scope="scope">
            <span :style="{ color: parseFloat(scope.row.netProfit) >= 0 ? '#F56C6C' : '#67C23A', fontWeight: 'bold' }">
              {{ scope.row.netProfit }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="目标价位" align="center" prop="sellTargetPrice" width="100"/>
        <el-table-column label="目标利润" align="center" prop="targetNetProfit" width="120"/>
        <el-table-column label="状态" align="center" prop="isSell" width="100">
          <template slot-scope="scope">
            <dict-tag :options="dict.type.position_status" :value="scope.row.isSell"/>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="100">
          <template #default="{ row }">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(row)"
                       v-hasPermi="['stock:stock_trades:edit']">修改
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />
    </el-card>

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body destroy-on-close
               :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px" class="dialog-form">
        <el-row>
          <el-col :span="12">
            <el-form-item label="股票代码" prop="stockCode">
              <el-input v-model="form.stockCode" placeholder="请输入股票代码"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="股票名称" prop="stockName">
              <el-input v-model="form.stockName" placeholder="请输入股票名称"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="买入数量" prop="initialShares">
              <el-input v-model="form.initialShares" placeholder="股数" type="number"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="买入单价" prop="buyPrice">
              <el-input v-model="form.buyPrice" placeholder="单价" type="number">
                <template slot="append">元</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="卖出单价" prop="sellPrice">
              <el-input v-model="form.sellPrice" placeholder="单价" type="number">
                <template slot="append">元</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="涨幅比值">
              <div class="static-value-box">
                {{
                  (form.buyPrice && form.sellPrice) ? calculateIncreasePercentage(form.buyPrice, form.sellPrice) : '-'
                }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="center">目标与收益</el-divider>

        <el-row>
          <el-col :span="12">
            <el-form-item label="净利润" prop="netProfit">
              <el-input v-model="form.netProfit" placeholder="自动计算或手动输入"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="目标卖出价" prop="sellTargetPrice">
              <el-input v-model="form.sellTargetPrice" placeholder="目标价格"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="持仓状态" prop="isSell">
          <el-radio-group v-model="form.isSell">
            <el-radio-button
              v-for="dict in dict.type.position_status"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{ dict.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="API接口" prop="stockApi">
          <el-input type="textarea" v-model="form.stockApi" placeholder="请输入外部查询API接口链接"
                    :autosize="{ minRows: 2, maxRows: 4 }"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancel">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
// 引入后端 API 方法
import {
  listStock_trades,
  getStock_trades,
  delStock_trades,
  addStock_trades,
  updateStock_trades
} from '@/api/stock/stock_trades';
import {listUser} from "@/api/stock/dropdown_component";

// 引入 ECharts
import * as echarts from 'echarts/core';
import {GridComponent, TooltipComponent, TitleComponent} from 'echarts/components';
import {CanvasRenderer} from 'echarts/renderers';
import 'echarts-liquidfill';

echarts.use([GridComponent, TooltipComponent, TitleComponent, CanvasRenderer]);

export default {
  name: 'TradesLiquid3D',
  dicts: ['position_status', 'synchronization_status'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      tradesList: [],
      title: '',
      open: false,

      // 图表实例
      liquidChartPrincipal: null,
      liquidChartProfit: null,
      liquidChartLoss: null,
      liquidChartTotal: null,
      liquidChartTotalProfit: null,

      queryParams: {
        pageNum: 1,
        pageSize: 10,
        stockCode: null,
        stockName: null,
        buyPrice: null,
        sellPrice: null,
        stockApi: null,
        userId: null,
      },
      form: {
        userId: null
      },
      rules: {
        stockCode: [{required: true, message: '股票代码不能为空', trigger: 'blur'}],
        stockName: [{required: true, message: '股票名称不能为空', trigger: 'blur'}],
        initialShares: [{required: true, message: '数量不能为空', trigger: 'blur'}],
        buyPrice: [{required: true, message: '买入价格不能为空', trigger: 'blur'}]
      }
    };
  },

  async created() {
    await this.initUserList();
    this.getList();
  },

  mounted() {
    this.initLiquidCharts();
  },

  // 销毁时清理图表实例，防止内存泄漏
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    ['Principal', 'Profit', 'Loss', 'Total', 'TotalProfit'].forEach(key => {
      if (this[`liquidChart${key}`]) {
        this[`liquidChart${key}`].dispose();
      }
    });
  },

  methods: {
    calculateIncreasePercentage(buyPrice, sellPrice) {
      const buy = parseFloat(buyPrice);
      const sell = parseFloat(sellPrice);
      if (isNaN(buy) || isNaN(sell) || buy === 0) return '-';
      const increase = ((sell - buy) / buy) * 100;
      return (increase > 0 ? '+' : '') + increase.toFixed(2) + '%';
    },

    // 辅助样式方法：判断涨跌颜色
    getProfitClass(buyPrice, sellPrice) {
      const buy = parseFloat(buyPrice);
      const sell = parseFloat(sellPrice);
      if (isNaN(buy) || isNaN(sell) || buy === 0) return '';
      return sell >= buy ? 'text-danger' : 'text-success';
    },

    async initUserList() {
      try {
        const response = await listUser({pageSize: 100}); // 稍微调大size以获取更多用户
        const payload = response.data || response;
        const rawUsers = Array.isArray(payload.rows) ? payload.rows : (Array.isArray(payload) ? payload : []);

        const userList = rawUsers.map(u => ({
          id: u.userId,
          name: u.userName || u.nickName || `用户${u.userId}`
        }));

        if (userList.length) {
          const savedUsername = this.$cookies.get('username');
          const matchedUser = userList.find(u => u.name === savedUsername);
          if (matchedUser) {
            this.queryParams.userId = matchedUser.id;
            this.form.userId = matchedUser.id;
          }
        }
      } catch (err) {
        console.error('用户列表加载失败:', err);
      }
    },

    getList() {
      this.loading = true;
      listStock_trades(this.queryParams).then(({rows, total}) => {
        this.tradesList = rows;
        this.total = total;
        this.loading = false;
        this.$nextTick(this.updateLiquidCharts);
      });
    },

    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },

    handleAdd() {
      this.reset();
      this.open = true;
      this.title = '新增股票交易';
      this.form.userId = this.queryParams.userId;
    },

    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids[0];
      getStock_trades(id).then(({data}) => {
        this.form = data;
        this.open = true;
        this.title = '修改交易记录';
      });
    },

    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return;
        const action = this.form.id ? updateStock_trades : addStock_trades;
        action(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功');
          this.open = false;
          this.getList();
        });
      });
    },

    cancel() {
      this.open = false;
      this.reset();
    },

    reset() {
      this.form = {
        id: null,
        stockCode: null,
        stockName: null,
        buyPrice: null,
        sellPrice: null,
        sellTargetPrice: null,
        isSell: 0,
        stockApi: null,
        userId: this.queryParams.userId
      };
      this.$refs.form && this.$refs.form.resetFields();
    },

    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },

    resetQuery() {
      this.$refs.queryForm.resetFields();
      this.handleQuery();
    },

    handleExport() {
      this.download('stock/stock_trades/export', this.queryParams, `股票交易报表_${new Date().getTime()}.xlsx`);
    },

    calculateFunds() {
      let principal = 0, profit = 0, loss = 0, totalProfit = 0;
      this.tradesList.forEach(item => {
        const net = Number(item.netProfit) || 0;
        totalProfit += net;

        if (item.isSell === 0) {
          principal += Number(item.totalCost) || 0;
          if (net > 0) profit += net;
          else loss += Math.abs(net);
        }
      });
      return {principal, profit, loss, totalProfit};
    },

    initLiquidCharts() {
      ['Principal', 'Profit', 'Loss', 'Total', 'TotalProfit'].forEach(key => {
        const dom = this.$refs[`liquid${key}`];
        if (dom) {
          this[`liquidChart${key}`] = echarts.init(dom);
        }
      });
      window.addEventListener('resize', this.resizeCharts);
    },

    resizeCharts() {
      ['Principal', 'Profit', 'Loss', 'Total', 'TotalProfit'].forEach(key =>
        this[`liquidChart${key}`] && this[`liquidChart${key}`].resize()
      );
    },

    updateLiquidCharts() {
      const {principal, profit, loss, totalProfit} = this.calculateFunds();
      const total = principal + profit - loss;
      const maxVal = Math.max(principal, profit, loss, total, 1) * 1.2; // 稍微调大分母，防止过满

      const configs = [
        {key: 'Principal', label: '持仓本金', value: principal, color: ['#67C23A', '#95D475']},
        {key: 'Profit', label: '持仓盈利', value: profit, color: ['#F56C6C', '#FAB6B6']},
        {key: 'Loss', label: '持仓亏损', value: loss, color: ['#909399', '#B1B3B8']}, // 灰色示警
        {key: 'Total', label: '持仓利本和', value: total, color: ['#409EFF', '#79bbff']},
        {key: 'TotalProfit', label: '总利润', value: totalProfit, color: ['#A140FF', '#C48AFF']}
      ];

      configs.forEach(({key, label, value, color}) => {
        if (!this[`liquidChart${key}`]) return;

        let displayValue = value;
        // 简单的数据格式化，超过万显示W
        let valStr = value.toLocaleString();

        this[`liquidChart${key}`].setOption({
          title: {
            text: `${label}\n¥${valStr}`,
            left: 'center',
            top: '40%',
            textStyle: {
              fontSize: 15,
              color: '#303133',
              fontFamily: 'Helvetica Neue',
              fontWeight: 600,
              lineHeight: 24
            }
          },
          series: [{
            type: 'liquidFill',
            radius: '85%',
            data: [value / maxVal, (value / maxVal) * 0.9], // 双波浪效果
            center: ['50%', '50%'],
            backgroundStyle: {
              color: '#fff',
              borderColor: '#EBEEF5',
              borderWidth: 1,
              shadowBlur: 10,
              shadowColor: 'rgba(0, 0, 0, 0.05)'
            },
            outline: {
              show: true,
              borderDistance: 0,
              itemStyle: {
                borderWidth: 2,
                borderColor: color[0],
              }
            },
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {offset: 0, color: color[1]},
                {offset: 1, color: color[0]}
              ]),
              shadowBlur: 0
            },
            label: {show: false}
          }]
        }, true);
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.app-container {
  padding: 20px;
  background-color: #f6f8f9; /* 浅灰色背景，提升卡片对比度 */
  min-height: 100vh;
}

/* 卡片通用样式 */
.el-card {
  border: none;
  border-radius: 8px;
}

.chart-card-container {
  background: linear-gradient(to bottom, #ffffff, #fafafa);
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

/* 液体图相关 */
.liquid-chart-wrapper {
  display: flex;
  justify-content: space-around;
  flex-wrap: wrap;
  gap: 20px;
}

.chart-item {
  flex: 1;
  min-width: 180px;
  max-width: 250px;
}

.liquid-chart-box {
  width: 100%;
  height: 240px; /* 稍微减小高度，显得更精致 */
}

/* 表格相关 */
.table-card {
  .stock-name {
    font-weight: 500;
    color: #409EFF;
  }

  .text-danger {
    color: #F56C6C; /* 红涨 */
    font-weight: bold;
  }

  .text-success {
    color: #67C23A; /* 绿跌 */
    font-weight: bold;
  }

  .text-muted {
    color: #909399;
  }
}

/* 搜索表单微调 */
.search-form .el-form-item {
  margin-bottom: 12px;
}

/* 弹窗样式 */
.dialog-form {
  padding-right: 20px;
}

.static-value-box {
  background-color: #f5f7fa;
  border-radius: 4px;
  padding: 0 15px;
  height: 32px;
  line-height: 32px;
  color: #606266;
  border: 1px solid #dcdfe6;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .liquid-chart-wrapper {
    justify-content: center;
  }

  .chart-item {
    min-width: 45%; /* 移动端一行显示两个 */
    margin-bottom: 10px;
  }

  .liquid-chart-box {
    height: 180px;
  }
}

/* 实用工具类 */
.mb-4 {
  margin-bottom: 16px;
}

.mb8 {
  margin-bottom: 8px;
}
</style>
