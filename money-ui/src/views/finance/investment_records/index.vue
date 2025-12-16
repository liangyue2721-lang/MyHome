<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb-4">
      <el-col :span="24">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="clearfix">
            <span class="card-title"><i class="el-icon-s-marketing"></i> 资金流向分布 (当前页)</span>
          </div>
          <div class="chart-wrapper">
            <MarketTreemap :data="chartData"/>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="search-card mb-4">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="70px">
        <el-form-item label="成交日期" prop="tradeDate">
          <el-date-picker
            clearable
            v-model="queryParams.tradeDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="选择日期"
            style="width: 200px"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item label="成交类型" prop="tradeType">
          <el-select v-model="queryParams.tradeType" placeholder="请选择类型" clearable style="width: 200px">
            <el-option
              v-for="dict in dict.type.position_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="table-card">
      <div slot="header" class="clearfix table-header-actions">
        <span class="card-title">交易记录列表</span>
        <div class="right-actions">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['finance:investment_records:add']"
          >新增记录
          </el-button>
          <el-button
            type="success"
            plain
            icon="el-icon-edit"
            size="mini"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['finance:investment_records:edit']"
          >修改
          </el-button>
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="mini"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['finance:investment_records:remove']"
          >删除
          </el-button>
          <el-button
            type="warning"
            plain
            icon="el-icon-download"
            size="mini"
            @click="handleExport"
            v-hasPermi="['finance:investment_records:export']"
          >导出
          </el-button>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="investment_recordsList"
        @selection-change="handleSelectionChange"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{background:'#f8f8f9', color:'#515a6e', textAlign: 'center'}"
        :cell-style="{textAlign: 'center'}"
      >
        <el-table-column type="selection" width="55" align="center"/>

        <el-table-column label="成交日期" align="center" prop="tradeDate" min-width="110">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.tradeDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="投资标的" align="center" prop="investType" min-width="140" show-overflow-tooltip/>

        <el-table-column label="成交类型" align="center" prop="tradeType" min-width="90">
          <template slot-scope="scope">
            <el-tag :type="scope.row.tradeType == '0' ? 'danger' : 'success'" effect="light">
              <dict-tag :options="dict.type.position_status" :value="scope.row.tradeType"/>
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="成交详情" align="center">
          <el-table-column label="成交价" align="center" prop="tradePrice" min-width="110">
            <template slot-scope="scope">¥{{ Number(scope.row.tradePrice).toFixed(3) }}</template>
          </el-table-column>
          <el-table-column label="成交量" align="center" prop="tradeVolume" min-width="120">
            <template slot-scope="scope">{{ Number(scope.row.tradeVolume) }} 股/份</template>
          </el-table-column>
          <el-table-column label="成交总额" align="center" prop="tradeAmount" min-width="130">
            <template slot-scope="scope">
                    <span :class="scope.row.tradeType == '0' ? 'text-primary-bold' : 'text-success-bold'">
                        ¥{{ formatMoney(scope.row.tradeAmount) }}
                    </span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip min-width="150"/>

        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="120">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)">修改</el-button>
            <el-button size="mini" type="text" class="text-danger" icon="el-icon-delete"
                       @click="handleDelete(scope.row)">删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <pagination
          v-show="total>0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </el-card>

    <el-dialog :title="title" :visible.sync="open" width="650px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="24">
            <el-form-item label="投资标的" prop="investType">
              <el-input v-model="form.investType" placeholder="请输入股票名称/代码、基金名称等"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成交日期" prop="tradeDate">
              <el-date-picker clearable
                              style="width: 100%"
                              v-model="form.tradeDate"
                              type="date"
                              value-format="yyyy-MM-dd"
                              placeholder="请选择日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成交类型" prop="tradeType">
              <el-radio-group v-model="form.tradeType">
                <el-radio
                  v-for="dict in dict.type.position_status"
                  :key="dict.value"
                  :label="dict.value"
                >{{ dict.label }}
                </el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>

        <el-divider content-position="left">交易明细</el-divider>

        <el-row>
          <el-col :span="12">
            <el-form-item label="成交单价" prop="tradePrice">
              <el-input-number style="width: 100%" v-model="form.tradePrice" placeholder="单价" :precision="3"
                               :step="0.01"
                               @change="calculateTradeAmount" controls-position="right"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="成交数量" prop="tradeVolume">
              <el-input-number style="width: 100%" v-model="form.tradeVolume" placeholder="股/份" :min="0" :step="100"
                               @change="calculateTradeAmount" controls-position="right"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="成交总金额" prop="tradeAmount">
              <el-input-number style="width: 100%" v-model="form.tradeAmount" placeholder="系统自动计算，也可手动修改"
                               :precision="2" :step="100" controls-position="right"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注信息" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注"/>
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
  listInvestment_records,
  getInvestment_records,
  delInvestment_records,
  addInvestment_records,
  updateInvestment_records
} from "@/api/finance/investment_records"
import {listUser} from "@/api/stock/dropdown_component";
import MarketTreemap from '@/components/MarketTreemap.vue'

export default {
  name: "Investment_records",
  dicts: ['position_status', 'synchronization_status'],
  components: {
    MarketTreemap
  },
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      investment_recordsList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        tradeDate: null,
        tradePrice: null,
        tradeVolume: null,
        tradeAmount: null,
        tradeType: null,
        investType: null,
        remark: null,
        userId: null,
      },
      form: {
        userId: null
      },
      rules: {
        tradeDate: [
          {required: true, message: "成交日期不能为空", trigger: "blur"}
        ],
        tradeType: [
          {required: true, message: "成交类型不能为空", trigger: "change"}
        ],
        tradePrice: [
          {required: true, message: "成交价位不能为空", trigger: "blur"}
        ],
        tradeVolume: [
          {required: true, message: "成交量不能为空", trigger: "blur"}
        ],
        tradeAmount: [
          {required: true, message: "成交金额不能为空", trigger: "blur"}
        ],
      }
    };
  },
  computed: {
    chartData() {
      const list = this.investment_recordsList || [];
      if (!Array.isArray(list) || list.length === 0) return [];

      const grouped = {};

      list.forEach(row => {
        const investType = row.investType || '未知标的';
        // 假设字典 position_status: 0=买入, 1=卖出
        const isBuy = String(row.tradeType) === '0';

        const price = Number(row.tradePrice) || 0;
        // 使用绝对值确保逻辑正确
        const volume = Math.abs(Number(row.tradeVolume) || 0);
        let amount = Math.abs(Number(row.tradeAmount) || 0);

        // 数据修正：如果总额丢失，用 单价*数量 回填
        if (amount === 0 && price > 0 && volume > 0) {
          amount = price * volume;
        }

        if (!grouped[investType]) {
          grouped[investType] = {
            name: investType,
            buyAmount: 0,
            sellAmount: 0,
            buyVolume: 0,
            sellVolume: 0,
            buyCount: 0,
            sellCount: 0
          };
        }

        const g = grouped[investType];
        if (isBuy) {
          g.buyAmount += amount;
          g.buyVolume += volume;
          g.buyCount += 1;
        } else {
          g.sellAmount += amount;
          g.sellVolume += volume;
          g.sellCount += 1;
        }
      });

      return Object.values(grouped)
        .map(g => {
          const {buyAmount, sellAmount, buyVolume, sellVolume} = g;

          if (buyAmount === 0 && sellAmount === 0) return null;

          let profit = 0;
          let profitRate = 0;

          // 估算逻辑：仅当有买入和卖出行为时计算
          // 注意：这是基于当前页数据的估算，不代表历史总收益
          if (sellAmount > 0 && buyAmount > 0 && buyVolume > 0) {
            // 平均买入成本 = 总买入金额 / 总买入数量
            const avgBuyPrice = buyAmount / buyVolume;
            // 已卖出部分的成本 = 平均买入成本 * 卖出数量
            const costOfSold = avgBuyPrice * sellVolume;
            // 利润 = 卖出总得 - 卖出成本
            profit = sellAmount - costOfSold;
            // 收益率 (基于卖出成本)
            profitRate = costOfSold > 0 ? (profit / costOfSold) * 100 : 0;
          }

          // Treemap 面积大小：交易活跃度 (买+卖总额) 或者 净流入流出
          // 这里使用净值绝对值作为面积大小，更直观显示对资金池的影响
          const netFlow = buyAmount - sellAmount;
          const value = Math.abs(netFlow);

          return {
            name: g.name,
            value: value > 0 ? value : (buyAmount + sellAmount), // 面积
            netFlow,           // 用于颜色判断 (正=净买入/红, 负=净卖出/绿)
            percent: profitRate,
            profit,
            buyAmount,
            sellAmount
          };
        })
        .filter(Boolean);
    }
  },
  async created() {
    await this.initUserList();
    this.getList();
  },
  methods: {
    formatMoney(val) {
      if (!val) return '0.00';
      return Number(val).toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    async initUserList() {
      try {
        const response = await listUser({pageSize: this.pageSize});
        const payload = response.data || response;
        const rawUsers = Array.isArray(payload.rows) ? payload.rows : Array.isArray(payload) ? payload : [];

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
      this.loading = true
      listInvestment_records(this.queryParams).then(response => {
        this.investment_recordsList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: null,
        tradeDate: new Date(), // 默认当天
        tradePrice: undefined,
        tradeVolume: undefined,
        tradeAmount: undefined,
        tradeType: '0', // 默认买入
        investType: null,
        remark: null,
        userId: this.form.userId || null
      }
      this.resetForm("form")
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "新增交易记录"
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getInvestment_records(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改交易记录"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateInvestment_records(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addInvestment_records(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('确认删除？').then(function () {
        return delInvestment_records(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    handleExport() {
      this.download('finance/investment_records/export', {
        ...this.queryParams
      }, `investment_records_${new Date().getTime()}.xlsx`)
    },
    /** 自动计算成交金额 = 单价 * 数量 */
    calculateTradeAmount() {
      const price = parseFloat(this.form.tradePrice);
      const volume = parseFloat(this.form.tradeVolume);

      if (!isNaN(price) && !isNaN(volume)) {
        // 保留两位小数
        this.form.tradeAmount = parseFloat((price * volume).toFixed(2));
      }
    }
  }
}
</script>

<style scoped lang="scss">
@import "~@/assets/styles/global.scss";
.app-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: 100vh;
}

.mb-4 {
  margin-bottom: 20px;
}

.chart-card, .search-card, .table-card {
  border-radius: 8px;
  border: none;
}

.chart-card {
  .chart-wrapper {
    height: 350px;
    width: 100%;
  }
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
  display: flex;
  align-items: center;

  i {
    margin-right: 8px;
    color: #409EFF;
  }
}

.table-header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 文本颜色 */
.text-primary-bold {
  color: #F56C6C; /* 买入/流出通常用红色或醒目颜色 */
  font-weight: 600;
}

.text-success-bold {
  color: #67C23A; /* 卖出/回笼通常用绿色 */
  font-weight: 600;
}

.text-danger {
  color: #F56C6C;

  &:hover {
    color: #f78989;
  }
}
</style>
