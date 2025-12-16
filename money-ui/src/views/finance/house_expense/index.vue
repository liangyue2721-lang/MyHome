<template>
  <div class="app-container">
    <el-row :gutter="20" class="dashboard-row">
      <el-col :span="8" :xs="24">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="clearfix">
            <span class="card-title"><i class="el-icon-data-line"></i> 总支出概览</span>
          </div>
          <div ref="sumChart" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :span="16" :xs="24">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="clearfix">
            <span class="card-title"><i class="el-icon-pie-chart"></i> 支出分布分析</span>
          </div>
          <div ref="chart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="search-card">
      <el-form
        :model="queryParams"
        ref="queryForm"
        size="small"
        :inline="true"
        v-show="showSearch"
        label-width="70px"
      >
        <el-form-item label="用户名" prop="userId">
          <el-input
            v-model="queryParams.userId"
            placeholder="请输入用户名"
            clearable
            style="width: 200px;"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="支出类型" prop="expenseType">
          <el-select v-model="queryParams.expenseType" placeholder="请选择类型" clearable style="width: 200px">
            <el-option
              v-for="dict in dict.type.house_expense_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item style="margin-left: 10px;">
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="table-card">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['finance:house_expense:add']"
          >新增支出
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="success"
            plain
            icon="el-icon-edit"
            size="mini"
            :disabled="single"
            @click="handleUpdate()"
            v-hasPermi="['finance:house_expense:edit']"
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
            @click="handleDelete()"
            v-hasPermi="['finance:house_expense:remove']"
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
            v-hasPermi="['finance:house_expense:export']"
          >导出
          </el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
      </el-row>

      <el-table
        v-loading="loading"
        :data="houseExpenseList"
        @selection-change="handleSelectionChange"
        border
        stripe
        :header-cell-style="{background:'#f8f8f9',color:'#515a6e'}"
      >
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="用户姓名" align="center" min-width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ getUserName(row.userId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="房产名称" prop="houseName" align="center" min-width="120"
                         :show-overflow-tooltip="true"/>
        <el-table-column label="支出类型" prop="expenseType" align="center">
          <template #default="{ row }">
            <dict-tag :options="dict.type.house_expense_type" :value="row.expenseType"/>
          </template>
        </el-table-column>
        <el-table-column label="支出金额" prop="amount" align="center" min-width="100">
          <template #default="{ row }">
            <span style="color: #F56C6C; font-weight: bold;">¥ {{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="发生日期" prop="paymentDate" align="center" width="120">
          <template #default="{ row }">
            <i class="el-icon-time"></i> {{ parseTime(row.paymentDate, '{y}-{m}-{d}') }}
          </template>
        </el-table-column>
        <el-table-column label="收款方" prop="payee" align="center" :show-overflow-tooltip="true"/>
        <el-table-column label="房产地址" prop="location" align="center" :show-overflow-tooltip="true"/>
        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
          <template #default="{ row }">
            <el-button
              type="text"
              size="mini"
              icon="el-icon-edit"
              @click="handleUpdate(row)"
              v-hasPermi="['finance:house_expense:edit']"
            >修改
            </el-button>
            <el-button
              type="text"
              size="mini"
              class="text-danger"
              icon="el-icon-delete"
              style="color: #F56C6C;"
              @click="handleDelete(row)"
              v-hasPermi="['finance:house_expense:remove']"
            >删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <pagination
          v-show="total > 0"
          :total="total"
          :page.sync="queryParams.pageNum"
          :limit.sync="queryParams.pageSize"
          @pagination="getList"
        />
      </div>
    </el-card>

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="用户" prop="userId">
              <el-select v-model="form.userId" placeholder="请选择用户" filterable style="width: 100%">
                <el-option
                  v-for="u in users"
                  :key="u.userId"
                  :label="u.nickName"
                  :value="u.userId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支出类型" prop="expenseType">
              <el-select v-model="form.expenseType" placeholder="请选择类型" style="width: 100%">
                <el-option
                  v-for="d in dict.type.house_expense_type"
                  :key="d.value"
                  :label="d.label"
                  :value="d.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="支出金额" prop="amount">
              <el-input v-model="form.amount" placeholder="请输入金额">
                <template slot="append">元</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发生日期" prop="paymentDate">
              <el-date-picker
                style="width: 100%"
                clearable
                v-model="form.paymentDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="选择日期"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="房产名称" prop="houseName">
          <el-input v-model="form.houseName" placeholder="例如：XX小区3栋201"/>
        </el-form-item>
        <el-form-item label="房产地址" prop="location">
          <el-input v-model="form.location" placeholder="详细地址"/>
        </el-form-item>

        <el-row>
          <el-col :span="12">
            <el-form-item label="房屋面积" prop="area">
              <el-input v-model="form.area" placeholder="平米"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="房屋总价" prop="totalPrice">
              <el-input v-model="form.totalPrice" placeholder="万元"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="收款方" prop="payee">
          <el-input v-model="form.payee" placeholder="请输入收款方"/>
        </el-form-item>
        <el-form-item label="备注" prop="notes">
          <el-input
            v-model="form.notes"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
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
  listHouseExpense,
  getHouseExpense,
  delHouseExpense,
  addHouseExpense,
  updateHouseExpense,
} from "@/api/finance/house_expense";
import {listUser} from "@/api/stock/dropdown_component";
import * as echarts from "echarts";
import "echarts-liquidfill";

export default {
  name: "HouseExpense",
  dicts: ["house_expense_type"],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      houseExpenseList: [],
      users: [],
      sumChartInstance: null,
      chartInstance: null,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        houseName: null,
        location: null,
        area: null,
        totalPrice: null,
        expenseType: null,
        amount: null,
        paymentDate: null,
        payee: null,
        stage: null,
        notes: null,
        createdAt: null,
        updatedAt: null,
        createdName: null
      },
      open: false,
      title: "",
      form: {
        createdName: null
      },
      rules: {},
    };
  },
  async created() {
    await this.initUserList();
    await this.getUserList();
    await this.getList();
  },
  mounted() {
    // 稍微延迟初始化图表，确保DOM已渲染
    this.$nextTick(() => {
      this.sumChartInstance = echarts.init(this.$refs.sumChart);
      this.chartInstance = echarts.init(this.$refs.chart);
      window.addEventListener("resize", this.handleResize);
    })
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.handleResize);
    if (this.sumChartInstance) this.sumChartInstance.dispose();
    if (this.chartInstance) this.chartInstance.dispose();
  },
  methods: {
    handleResize() {
      if (this.sumChartInstance) this.sumChartInstance.resize();
      if (this.chartInstance) this.chartInstance.resize();
    },
    async initUserList() {
      try {
        const response = await listUser({pageSize: this.pageSize});
        const payload = response.data || response;
        const rawUsers = Array.isArray(payload.rows)
          ? payload.rows
          : Array.isArray(payload)
            ? payload
            : [];

        const userList = rawUsers.map(u => ({
          id: u.userId,
          name: u.userName || u.nickName || `用户${u.userId}`
        }));

        if (userList.length) {
          const savedUsername = this.$cookies.get('username');
          const matchedUser = userList.find(u => u.name === savedUsername);
          if (matchedUser) {
            this.queryParams.createdName = matchedUser.id;
            this.form.createdName = matchedUser.id;
          }
        }
      } catch (err) {
        console.error('用户列表加载失败:', err);
      }
    },
    renderSumChart() {
      if (!this.sumChartInstance) return;
      this.sumChartInstance.clear();
      const total = this.houseExpenseList.reduce(
        (s, item) => s + parseFloat(item.amount || 0),
        0
      );
      // 防止分母为0
      const ratio = total > 0 ? 0.6 : 0;

      this.sumChartInstance.setOption({
        series: [
          {
            type: "liquidFill",
            radius: "90%",
            center: ["50%", "50%"],
            data: [ratio, ratio - 0.1], // 双波浪效果
            backgroundStyle: {color: "#fff"},
            outline: {
              show: true,
              borderDistance: 4,
              itemStyle: {
                borderWidth: 4,
                borderColor: "#409EFF",
                shadowBlur: 10,
                shadowColor: "rgba(64, 158, 255, 0.4)"
              },
            },
            color: ['#409EFF', 'rgba(64, 158, 255, 0.6)'],
            label: {
              show: true,
              position: ["50%", "45%"],
              formatter: () => `¥${total.toLocaleString()}`, // 千分位展示
              fontSize: 28,
              color: "#303133",
              fontWeight: 'normal',
              insideColor: '#fff'
            },
          },
        ],
      });
    },
    renderUserChart() {
      if (!this.chartInstance) return;
      this.chartInstance.clear();
      const agg = this.houseExpenseList.reduce((m, item) => {
        const name = this.getUserName(item.userId);
        m[name] = (m[name] || 0) + Number(item.amount || 0);
        return m;
      }, {});
      const data = Object.entries(agg).map(([name, value]) => ({
        name,
        value: Number(value.toFixed(2)),
      }));

      if (data.length === 0) {
        this.chartInstance.setOption({
          title: {
            text: "暂无支出数据",
            left: "center",
            top: "middle",
            textStyle: {color: "#909399", fontSize: 14},
          },
        });
        return;
      }

      this.chartInstance.setOption({
        tooltip: {trigger: "item", formatter: "{b}: ¥{c} ({d}%)"},
        legend: {
          orient: "vertical",
          left: "left",
          top: "middle",
          itemWidth: 10,
          itemHeight: 10,
          textStyle: {fontSize: 12}
        },
        series: [
          {
            name: "支出占比",
            type: "pie",
            radius: ["45%", "75%"],
            center: ["60%", "50%"], // 稍微右移，给Legend留空间
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 5,
              borderColor: "#fff",
              borderWidth: 2,
            },
            label: {
              show: false,
              position: "center",
            },
            emphasis: {
              label: {
                show: true,
                fontSize: "16",
                fontWeight: "bold",
                formatter: "{b}\n{d}%"
              },
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: "rgba(0, 0, 0, 0.5)",
              }
            },
            labelLine: {show: false},
            data,
          },
        ],
      });
    },
    async getUserList() {
      try {
        const res = await listUser({pageSize: 1000});
        const p = res.data || res;
        this.users = p.rows || p;
      } catch (e) {
        console.error("获取用户列表失败", e);
      }
    },
    getUserName(id) {
      const u = this.users.find((u) => u.userId === id);
      return u ? u.nickName : "未知用户";
    },
    async getList() {
      this.loading = true;
      try {
        const res = await listHouseExpense(this.queryParams);
        this.houseExpenseList = res.rows || [];
        this.total = res.total || 0;
      } catch (e) {
        console.error("获取支出记录失败", e);
      } finally {
        this.loading = false;
        this.$nextTick(() => {
          this.renderSumChart();
          this.renderUserChart();
        });
      }
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.$refs.queryForm.resetFields();
      this.queryParams.expenseType = null; // 手动重置select
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map((i) => i.id);
      this.single = selection.length !== 1;
      this.multiple = selection.length === 0;
    },
    handleAdd() {
      this.form = {};
      this.open = true;
      this.title = "添加买房支出记录";
    },
    handleUpdate(row) {
      const id = row?.id || this.ids[0];
      getHouseExpense(id).then((res) => {
        this.form = res.data;
        this.open = true;
        this.title = "修改买房支出记录";
      });
    },
    submitForm() {
      this.$refs.form.validate((valid) => {
        if (!valid) return;
        const fn = this.form.id ? updateHouseExpense : addHouseExpense;
        fn(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id ? "修改成功" : "新增成功");
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
      this.form = {createdName: null};
    },
    handleDelete(row) {
      const ids = row?.id ? [row.id] : this.ids;
      this.$modal
        .confirm(`确认删除编号 ${ids.join(",")} 的记录？`)
        .then(() => delHouseExpense(ids))
        .then(() => {
          this.$modal.msgSuccess("删除成功");
          this.getList();
        })
        .catch(() => {
        });
    },
    handleExport() {
      this.download(
        "finance/houseExpense/export",
        {...this.queryParams},
        `houseExpense_${Date.now()}.xlsx`
      );
    },
  },
};
</script>

<style scoped lang="scss">
@import "~@/assets/styles/global.scss";
.app-container {
  padding: 20px;
  background-color: #f0f2f5; // 浅灰色背景，衬托卡片
  min-height: 100vh;
}

.dashboard-row {
  margin-bottom: 20px;
}

.chart-card {
  margin-bottom: 10px; // 移动端间距
  border-radius: 8px;

  .card-title {
    font-weight: bold;
    font-size: 16px;
    color: #303133;
    display: flex;
    align-items: center;

    i {
      margin-right: 6px;
      color: #409EFF;
    }
  }

  .chart-box {
    height: 300px;
    width: 100%;
  }
}

.search-card {
  margin-bottom: 20px;
  border-radius: 8px;

  ::v-deep .el-card__body {
    padding-bottom: 2px; // 减少底部空白
  }
}

.table-card {
  border-radius: 8px;

  .mb8 {
    margin-bottom: 15px;
  }
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 15px;
}

// 调整表格内的按钮样式
.text-danger {
  color: #F56C6C;

  &:hover {
    color: #f78989;
  }
}
</style>
