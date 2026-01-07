<template>
  <div class="app-container">
    <el-card shadow="never" class="search-wrapper mb-20">
      <el-form :model="queryParams" ref="queryForm" :inline="true" size="small">
        <el-form-item label="日期" prop="date">
          <el-date-picker
            v-model="queryParams.date"
            type="date"
            placeholder="请选择日期"
            value-format="yyyy-MM-dd"
            style="width: 160px"
          />
        </el-form-item>
        <el-form-item label="股票代码" prop="code">
          <el-input
            v-model="queryParams.code"
            placeholder="请输入股票代码"
            clearable
            style="width: 140px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="股票名称" prop="name">
          <el-input
            v-model="queryParams.name"
            placeholder="请输入股票名称"
            clearable
            style="width: 140px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="20" class="mb-20">
      <el-col :span="12">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span style="font-weight: bold; font-size: 16px;">📈 本周涨幅榜</span>
          </div>
          <el-table :data="weeklyGainList" size="mini" :border="false" :show-header="true">
            <el-table-column type="index" label="排名" width="50" align="center"/>
            <el-table-column prop="stockName" label="名称" align="center" show-overflow-tooltip/>
            <el-table-column prop="stockCode" label="代码" align="center"/>
            <el-table-column label="最新价" align="center">
              <template slot-scope="scope">
                <span class="price-font">{{ scope.row.currentValue }}</span>
              </template>
            </el-table-column>
            <el-table-column label="周涨幅" align="center">
              <template slot-scope="scope">
                   <span :class="getWeeklyValueColor(scope.row.currentValue, scope.row.prevValue)">
                      {{ calculateWeeklyRate(scope.row.currentValue, scope.row.prevValue) }}
                   </span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <div slot="header" class="clearfix">
            <span style="font-weight: bold; font-size: 16px;">📉 本周跌幅榜</span>
          </div>
          <el-table :data="weeklyLossList" size="mini" :border="false" :show-header="true">
            <el-table-column type="index" label="排名" width="50" align="center"/>
            <el-table-column prop="stockName" label="名称" align="center" show-overflow-tooltip/>
            <el-table-column prop="stockCode" label="代码" align="center"/>
            <el-table-column label="最新价" align="center">
              <template slot-scope="scope">
                <span class="price-font">{{ scope.row.currentValue }}</span>
              </template>
            </el-table-column>
            <el-table-column label="周跌幅" align="center">
              <template slot-scope="scope">
                   <span :class="getWeeklyValueColor(scope.row.currentValue, scope.row.prevValue)">
                      {{ calculateWeeklyRate(scope.row.currentValue, scope.row.prevValue) }}
                   </span>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <div class="content-header mb-10">
      <el-row :gutter="10">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd">新增</el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate">
            修改
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete">
            删除
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport">导出</el-button>
        </el-col>
      </el-row>
    </div>

    <el-table
      v-loading="loading"
      :data="stockList"
      @selection-change="handleSelectionChange"
      border
      stripe
      size="mini"
      style="width: 100%"
      :header-cell-style="{background:'#f5f7fa', color:'#303133', fontWeight:'bold', textAlign:'center'}"
    >
      <el-table-column type="selection" width="45" align="center" fixed="left"/>

      <el-table-column label="代码" prop="code" width="80" align="center" fixed="left">
        <template slot-scope="scope">
          <span class="link-type" @click="handleUpdate(scope.row)">{{ scope.row.code }}</span>
        </template>
      </el-table-column>

      <el-table-column label="名称" prop="name" min-width="90" align="center" fixed="left" show-overflow-tooltip/>

      <el-table-column label="板块" align="center" min-width="80">
        <template slot-scope="scope">
          <el-tag size="mini" type="info" effect="plain">{{ getChineseName(scope.row.code) }}</el-tag>
        </template>
      </el-table-column>

      <el-table-column label="最新价" min-width="80" align="center">
        <template slot-scope="scope">
          <span class="price-font">{{ scope.row.newPrice }}</span>
        </template>
      </el-table-column>

      <el-table-column label="涨跌幅" min-width="80" align="center">
        <template slot-scope="scope">
          <div :class="getValueColor(scope.row.changeRate)">
            {{ formatRate(scope.row.changeRate) }}
          </div>
        </template>
      </el-table-column>

      <el-table-column label="昨收" min-width="70" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.previousClose || '-' }}</span>
        </template>
      </el-table-column>

      <el-table-column label="今日高/低" min-width="140" align="center">
        <template slot-scope="scope">
          <div class="range-row">
            <span class="range-label">高:</span>
            <span class="range-value">{{ scope.row.highPrice || '-' }}</span>
          </div>
          <div class="range-row">
            <span class="range-label">低:</span>
            <span class="range-value">{{ scope.row.lowPrice || '-' }}</span>
            <span class="range-rate text-down" v-if="scope.row.highPrice && scope.row.lowPrice">
              ({{ getTodayLowRate(scope.row) }})
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="周内高/低" min-width="150" align="center">
        <template slot-scope="scope">
          <div class="range-row">
            <span class="range-label">高:</span>
            <span class="range-value">{{ scope.row.weekHigh || '-' }}</span>
            <span class="range-rate" :class="getCompareColor(scope.row.newPrice, scope.row.weekHigh)">
               {{ calculateRate(scope.row.newPrice, scope.row.weekHigh) }}
            </span>
          </div>
          <div class="range-row">
            <span class="range-label">低:</span>
            <span class="range-value">{{ scope.row.weekLow || '-' }}</span>
            <span class="range-rate" :class="getCompareColor(scope.row.newPrice, scope.row.weekLow)">
               {{ calculateRate(scope.row.newPrice, scope.row.weekLow) }}
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="年内高/低" min-width="150" align="center">
        <template slot-scope="scope">
          <div class="range-row">
            <span class="range-label">高:</span>
            <span class="range-value">{{ scope.row.yearHigh || '-' }}</span>
            <span class="range-rate" :class="getCompareColor(scope.row.newPrice, scope.row.yearHigh)">
              {{ calculateRate(scope.row.newPrice, scope.row.yearHigh) }}
            </span>
          </div>
          <div class="range-row">
            <span class="range-label">低:</span>
            <span class="range-value">{{ scope.row.yearLow || '-' }}</span>
            <span class="range-rate" :class="getCompareColor(scope.row.newPrice, scope.row.yearLow)">
              {{ calculateRate(scope.row.newPrice, scope.row.yearLow) }}
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="去年高/低" min-width="150" align="center">
        <template slot-scope="scope">
          <div class="range-row">
            <span class="range-label">高:</span>
            <span class="range-value">{{ scope.row.compareYearHigh || '-' }}</span>
            <span class="range-rate" :class="getCompareColor(scope.row.newPrice, scope.row.compareYearHigh)">
              {{ calculateRate(scope.row.newPrice, scope.row.compareYearHigh) }}
            </span>
          </div>
          <div class="range-row">
            <span class="range-label">低:</span>
            <span class="range-value">{{ scope.row.compareYearLow || '-' }}</span>
            <span class="range-rate" :class="getCompareColor(scope.row.newPrice, scope.row.compareYearLow)">
              {{ calculateRate(scope.row.newPrice, scope.row.compareYearLow) }}
            </span>
          </div>
        </template>
      </el-table-column>

      <el-table-column label="阈值设定" min-width="120" align="center">
        <template slot-scope="scope">
          <div class="range-row" style="justify-content: center">
            {{ scope.row.thresholdPrice || '-' }}
          </div>
          <div class="range-row" style="justify-content: center">
               <span class="range-rate" :class="getCompareColor(scope.row.newPrice, scope.row.thresholdPrice)">
                  {{ calculateRate(scope.row.newPrice, scope.row.thresholdPrice) }}
               </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="通知次数" min-width="80" align="center">
        <template slot-scope="scope">
          <span class="price-font">{{ scope.row.num }}</span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" prop="updatedAt" min-width="140" align="center" show-overflow-tooltip/>

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="100">
        <template slot-scope="scope">
          <div style="display: flex; flex-direction: column; align-items: center;">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" class="text-delete" style="margin-left: 0;"
                       @click="handleDelete(scope.row)">
              删除
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
        background
        v-show="total>0"
        :current-page.sync="queryParams.pageNum"
        :page-size.sync="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        @size-change="getList"
        @current-change="getList"
      />
    </div>

    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="股票代码" prop="code">
          <el-input v-model="form.code" placeholder="请输入股票代码" :disabled="form.id !== undefined"/>
        </el-form-item>
        <el-form-item label="股票名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入股票名称"/>
        </el-form-item>
        <el-form-item label="阈值价格" prop="thresholdPrice">
          <el-input-number v-model="form.thresholdPrice" :precision="2" :step="0.1" :min="0" style="width: 100%"/>
        </el-form-item>
        <el-form-item label="通知次数" prop="num">
          <el-input-number v-model="form.num" :precision="0" :step="1" :min="0" :max="3" style="width: 100%"/>
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
import {
  listWatch_stock,
  getWatch_stock,
  delWatch_stock,
  addWatch_stock,
  updateWatch_stock
} from "@/api/stock/watch_stock";
import { getRankingStats } from "@/api/stock/kline";

export default {
  name: "StockList",
  data() {
    return {
      weeklyGainList: [],
      weeklyLossList: [],
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      total: 0,
      stockList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        date: null,
        code: null,
        name: null
      },
      form: {},
      rules: {
        code: [{required: true, message: "股票代码不能为空", trigger: "blur"}],
        name: [{required: true, message: "股票名称不能为空", trigger: "blur"}],
        thresholdPrice: [{required: true, message: "阈值价格不能为空", trigger: "blur"}],
        num: [{required: true, message: "通知次数不能为空", trigger: "blur"}]
      }
    };
  },
  created() {
    this.getList();
    this.loadRankings();
  },
  methods: {
    loadRankings() {
      getRankingStats('WEEKLY_GAIN').then(res => {
        this.weeklyGainList = res.data;
      });
      getRankingStats('WEEKLY_LOSS').then(res => {
        this.weeklyLossList = res.data;
      });
    },
    calculateWeeklyRate(current, prev) {
      if (!prev || prev == 0) return '-';
      const rate = ((current - prev) / prev) * 100;
      return (rate > 0 ? '+' : '') + rate.toFixed(2) + '%';
    },
    getWeeklyValueColor(current, prev) {
      if (!prev || prev == 0) return '';
      const rate = current - prev;
      return rate >= 0 ? 'text-up' : 'text-down';
    },
    getList() {
      this.loading = true;
      listWatch_stock(this.queryParams).then(response => {
        this.stockList = response.rows;
        this.total = response.total;
        this.loading = false;
      }).catch(() => {
        this.loading = false;
      });
    },

    reset() {
      this.form = {
        id: undefined,
        code: undefined,
        name: undefined,
        thresholdPrice: undefined,
      };
      this.resetForm("form");
    },

    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },

    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },

    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },

    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加股票监控";
    },

    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids;
      getWatch_stock(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改股票监控";
      });
    },

    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          const action = this.form.id ? updateWatch_stock : addWatch_stock;
          action(this.form).then(() => {
            this.$message.success(this.form.id ? "修改成功" : "新增成功");
            this.open = false;
            this.getList();
          });
        }
      });
    },

    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$confirm('是否确认删除股票ID为"' + ids + '"的数据项？', "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        return delWatch_stock(ids);
      }).then(() => {
        this.getList();
        this.$message.success("删除成功");
      });
    },

    handleExport() {
      this.download('stock/watch_stock/export', this.queryParams, `watch_stock_${new Date().getTime()}.xlsx`);
    },

    cancel() {
      this.open = false;
      this.reset();
    },

    resetForm(refName) {
      if (this.$refs[refName]) {
        this.$refs[refName].resetFields();
      }
    },

    // ==========================================
    // ============  业务逻辑展示方法  ============
    // ==========================================

    getChineseName(ticker) {
      if (!ticker) return '-';
      ticker = String(ticker);
      if (ticker.startsWith('300') || ticker.startsWith('301')) return '创业板';
      if (ticker.startsWith('688')) return '科创板';
      if (ticker.startsWith('200')) return '深市B股';
      if (ticker.startsWith('900')) return '沪市B股';
      if (ticker.startsWith('002')) return '中小板';
      if (['600', '601', '603', '605'].some(p => ticker.startsWith(p))) return '沪市主板';
      if (['000', '001', '003'].some(p => ticker.startsWith(p))) return '深市主板';
      if (ticker.startsWith('8') || ticker.startsWith('920')) return '北交所';
      return '其他';
    },

    formatRate(rate) {
      const num = parseFloat(rate);
      if (isNaN(num)) return '-';
      return num > 0 ? `+${num.toFixed(2)}%` : `${num.toFixed(2)}%`;
    },

    calculateRate(current, base) {
      const curVal = parseFloat(current);
      const baseVal = parseFloat(base);
      if (isNaN(curVal) || isNaN(baseVal) || baseVal === 0) return '';
      const rate = ((curVal - baseVal) / baseVal) * 100;
      return `(${this.formatRate(rate)})`;
    },

    getTodayLowRate(row) {
      const high = parseFloat(row.highPrice);
      const low = parseFloat(row.lowPrice);
      if (isNaN(high) || isNaN(low) || high === 0) return '-';
      const rate = ((low - high) / high) * 100;
      return this.formatRate(rate);
    },

    getValueColor(value) {
      const val = parseFloat(value);
      if (isNaN(val) || val === 0) return '';
      return val > 0 ? 'text-up' : 'text-down';
    },

    getCompareColor(current, base) {
      const curVal = parseFloat(current);
      const baseVal = parseFloat(base);
      if (isNaN(curVal) || isNaN(baseVal)) return '';
      return curVal >= baseVal ? 'text-up' : 'text-down';
    }
  }
};
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
  background-color: #fff;
  min-height: 100vh;
}

.search-wrapper {
  margin-bottom: 20px;
  border: 1px solid #ebeef5;
  background-color: #fff;
}

.mb-20 {
  margin-bottom: 20px;
}

.mb-10 {
  margin-bottom: 10px;
}

.link-type {
  color: #409EFF;
  cursor: pointer;
  font-weight: bold;

  &:hover {
    text-decoration: underline;
  }
}

.text-delete {
  color: #F56C6C;

  &:hover {
    color: #f78989;
  }
}

.pagination-container {
  margin-top: 20px;
  text-align: right;
  padding-bottom: 20px;
}

/* --- 自定义表格内容样式 --- */

.price-font {
  font-weight: 600;
  font-size: 14px;
}

.text-up {
  color: #F56C6C; /* 红色 */
  font-weight: bold;
}

.text-down {
  color: #67C23A; /* 绿色 */
  font-weight: bold;
}

/* 高低组合样式 (核心样式) */
.range-row {
  display: flex;
  align-items: center;
  justify-content: flex-start; /* 左对齐，配合min-width自适应看起来更整齐 */
  line-height: 1.5;
  font-size: 13px;
  padding-left: 5px;
}

/* "高/低" 标签 */
.range-label {
  color: #909399; /* 淡灰色 */
  margin-right: 4px;
  font-size: 12px;
  min-width: 20px; /* 保证对齐 */
}

/* 具体数值 */
.range-value {
  color: #606266;
  margin-right: 6px;
  white-space: nowrap; /* 防止数值换行 */
}

/* 百分比 */
.range-rate {
  font-size: 12px;
  transform: scale(0.95);
  white-space: nowrap; /* 防止百分比换行 */
}
</style>
