<template>
  <div class="app-container">
    <!-- 查询表单 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用戶" prop="userId">
        <el-select v-model="queryParams.userId" placeholder="請選擇用戶" filterable style="width: 100%">
          <el-option v-for="user in users" :key="user.userId" :label="user.nickName" :value="user.userId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="繳費類型" prop="paymentType">
        <el-select v-model="queryParams.paymentType" placeholder="請選擇繳費類型" clearable>
          <el-option v-for="dict in dict.type.utility_payments_bill_type" :key="dict.value" :label="dict.label"
                     :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="繳費狀態" prop="paymentStatus">
        <el-select v-model="queryParams.paymentStatus" placeholder="請選擇繳費狀態" clearable>
          <el-option v-for="dict in dict.type.utility_payments_bill_status" :key="dict.value" :label="dict.label"
                     :value="dict.value"/>
        </el-select>
      </el-form-item>
      <el-form-item label="繳費日期" prop="paymentDate">
        <el-date-picker clearable v-model="queryParams.paymentDate" type="date" value-format="yyyy-MM-dd"
                        placeholder="請選擇繳費日期"></el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                   v-hasPermi="['finance:payments:add']">新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
                   v-hasPermi="['finance:payments:export']">導出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span>費用佔比 (Top 1)</span>
          </div>
          <div ref="user1Chart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span>費用佔比 (Top 2)</span>
          </div>
          <div ref="user2Chart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="24">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span>本年度各類型繳費統計</span>
          </div>
          <div ref="barChart" class="chart-box" style="height: 350px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="paymentsList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="用戶姓名" align="center">
        <template slot-scope="scope">
          {{ getUserName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column label="費用" align="center" prop="fee"/>
      <el-table-column label="繳費類型" align="center" prop="paymentType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.utility_payments_bill_type" :value="scope.row.paymentType"/>
        </template>
      </el-table-column>
      <el-table-column label="繳費狀態" align="center" prop="paymentStatus">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.utility_payments_bill_status" :value="scope.row.paymentStatus"/>
        </template>
      </el-table-column>
      <el-table-column label="繳費日期" align="center" prop="paymentDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.paymentDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="最後更新時間" align="center" prop="updatedAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updatedAt, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                     v-hasPermi="['finance:payments:edit']">修改
          </el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)"
                     v-hasPermi="['finance:payments:remove']">刪除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
                @pagination="getList"/>

    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="選擇用戶" prop="userId">
          <el-select v-model="form.userId" placeholder="請選擇用戶" filterable style="width: 100%">
            <el-option v-for="user in users" :key="user.userId" :label="user.nickName" :value="user.userId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="費用" prop="fee">
          <el-input v-model="form.fee" placeholder="請輸入費用"/>
        </el-form-item>
        <el-form-item label="繳費類型" prop="paymentType">
          <el-select v-model="form.paymentType" placeholder="請選擇繳費類型">
            <el-option v-for="dict in dict.type.utility_payments_bill_type" :key="dict.value" :label="dict.label"
                       :value="parseInt(dict.value)"/>
          </el-select>
        </el-form-item>
        <el-form-item label="繳費狀態" prop="paymentStatus">
          <el-select v-model="form.paymentStatus" placeholder="請選擇繳費狀態">
            <el-option v-for="dict in dict.type.utility_payments_bill_status" :key="dict.value" :label="dict.label"
                       :value="parseInt(dict.value)"/>
          </el-select>
        </el-form-item>
        <el-form-item label="繳費日期" prop="paymentDate">
          <el-date-picker clearable v-model="form.paymentDate" type="date" value-format="yyyy-MM-dd"
                          placeholder="請選擇繳費日期"></el-date-picker>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">確 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import 'echarts-liquidfill';
import {
  queryPayments,
  listPayments,
  getPayments,
  delPayments,
  addPayments,
  updatePayments
} from "@/api/finance/payments";
import {listUser} from "@/api/stock/dropdown_component";

export default {
  name: "Payments",
  dicts: ['utility_payments_bill_status', 'utility_payments_bill_type'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      paymentsList: [],
      title: "",
      open: false,
      users: [],
      // ECharts 实例
      user1Chart: null,
      user2Chart: null,
      barChartInstance: null, // 新增：柱状图实例

      userId: undefined,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        fee: null,
        paymentType: null,
        paymentStatus: null,
        paymentDate: null,
        createdAt: null,
        updatedAt: null
      },
      form: {
        userId: this.userId
      },
      rules: {
        userId: [{required: true, message: "用户不能为空", trigger: "change"}],
        fee: [{required: true, message: "费用不能为空", trigger: "blur"}],
        paymentType: [{required: true, message: "缴费类型不能为空", trigger: "change"}],
        paymentStatus: [{required: true, message: "缴费状态不能为空", trigger: "change"}],
      }
    };
  },
  async created() {
    // 初始化用户列表数据并设置 userId
    console.log("Payments component created - 初始化用户列表数据");
    await this.initUserList();
    this.getUserList();
    this.getList();
  },
  mounted() {
    console.log("Payments component mounted - 初始化图表");
    this.$nextTick(() => {
      this.initCharts();
      this.updateLiquidCharts();   // 打开页面立即渲染图表
    });
    window.addEventListener('resize', this.resizeCharts);
  },
  beforeDestroy() {
    console.log("Payments component beforeDestroy - 清理资源");
    window.removeEventListener('resize', this.resizeCharts);
    [this.user1Chart, this.user2Chart, this.barChartInstance].forEach(chart => chart?.dispose());
  },
  methods: {
    /**
     * 初始化用户列表数据
     */
    async initUserList() {
      try {
        console.log("开始获取用户列表数据");
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
            this.userId = matchedUser.id;
          }
        } else {
          this.queryParams.userId = null;
        }
        console.log("用户列表初始化完成，共获取到", userList.length, "个用户");
      } catch (err) {
        console.error('用户列表加载失败:', err);
        this.$message.error('用户列表加载失败，请稍后重试');
      }
    },
    // 初始化用户下拉列表
    async getUserList() {
      try {
        console.log("获取用户下拉列表数据");
        const response = await listUser({pageSize: this.pageSize || 1000});
        const data = response.data || response;
        if (data.code === 200) {
          this.users = data.rows || [];
          console.log("用户下拉列表获取成功，共", this.users.length, "个用户");
        }
      } catch (error) {
        console.error("获取用户列表失败:", error);
      }
    },
    // 根据 userId 获取用户姓名
    getUserName(userId) {
      if (!userId) return "未知用户";
      if (!this.users || this.users.length === 0) return "加载中...";
      const targetId = String(userId);
      const user = this.users.find(u => String(u.userId) === targetId);
      return user ? (user.nickName || "匿名用户") : "未知用户";
    },
    // 查询费用缴纳记录列表
    getList() {
      console.log("开始查询费用缴纳记录，查询参数:", this.queryParams);
      this.loading = true;
      listPayments(this.queryParams).then(response => {
        this.paymentsList = response.rows;
        this.total = response.total;
        this.loading = false;
        console.log("费用缴纳记录查询完成，共获取到", this.paymentsList.length, "条记录");
        this.updateLiquidCharts(); // 列表更新时，同时更新图表数据
      }).catch(error => {
        console.error("费用缴纳记录查询失败:", error);
        this.loading = false;
        this.$message.error("查询失败，请稍后重试");
      });
    },
    // 初始化所有图表实例
    initCharts() {
      console.log("初始化图表实例");
      this.user1Chart = echarts.init(this.$refs.user1Chart);
      this.user2Chart = echarts.init(this.$refs.user2Chart);
      this.barChartInstance = echarts.init(this.$refs.barChart); // 初始化柱状图
    },
    // 图表自适应
    resizeCharts() {
      [this.user1Chart, this.user2Chart, this.barChartInstance].forEach(chart => chart?.resize());
    },

    /**
     * 核心图表更新逻辑
     * 拉取全量数据，并分发给 水滴图 和 柱状图
     */
    async updateLiquidCharts() {
      console.log("开始更新图表数据");

      // 获取当前本地日期 (YYYY-MM-DD)
      const d = new Date();
      const todayStr = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
      // 构造查询全量的参数
      const allParams = {
        pageNum: 1,
        pageSize: 999999,
        userId: null,
        fee: null,
        paymentType: null,
        paymentStatus: null,
        paymentDate: todayStr, // 设置为当前日期
        createdAt: null,
        updatedAt: null
      };

      let rows = [];
      try {
        const resp = await queryPayments(allParams);
        rows = resp.rows || [];
        console.log("获取全量支付记录成功，共", rows.length, "条记录");
      } catch (e) {
        console.error("拉取全量支付记录失败", e);
        return;
      }

      // 1. 更新水滴图 (原有逻辑)
      this.updateWaterCharts(rows);

      // 2. 更新本年度柱状图 (新增逻辑)
      this.updateYearlyBarChart(rows);
    },

    // 水滴图逻辑拆分
    updateWaterCharts(rows) {
      console.log("更新水滴图数据");
      const userTotals = rows.reduce((acc, item) => {
        const id = item.userId;
        const fee = parseFloat(item.fee) || 0;
        acc[id] = (acc[id] || 0) + fee;
        return acc;
      }, {});
      const totalAll = Object.values(userTotals).reduce((sum, v) => sum + v, 0);

      // 只展示前两个用户的「水滴」
      const topUsers = Object.keys(userTotals).slice(0, 2);

      const makeOption = (name, value, color) => ({
        series: [{
          type: 'liquidFill',
          data: [totalAll ? value / totalAll : 0],
          radius: '80%',
          label: {
            formatter: () => `${name}\n¥${value.toFixed(2)}\n${totalAll ? ((value / totalAll) * 100).toFixed(1) + '%' : '0%'}`,
            fontSize: 16,
            color: '#333'
          },
          itemStyle: {color, opacity: 0.8},
          backgroundStyle: {color: '#f8f9fa'},
          outline: {show: false}
        }]
      });

      // 如果不足两个用户，需处理空指针
      if (topUsers.length > 0) {
        this.user1Chart.setOption(makeOption(this.getUserName(topUsers[0]), userTotals[topUsers[0]], '#4ECDC4'));
      }
      if (topUsers.length > 1) {
        this.user2Chart.setOption(makeOption(this.getUserName(topUsers[1]), userTotals[topUsers[1]], '#FF6B6B'));
      }
    },

    // 新增：更新本年度类型统计柱状图
    updateYearlyBarChart(rows) {
      console.log("更新年度类型统计柱状图");
      if (!rows || rows.length === 0) {
        this.barChartInstance.clear();
        return;
      }

      // 1. 获取当前年份
      const currentYear = new Date().getFullYear().toString();

      // 2. 筛选本年度数据
      const thisYearData = rows.filter(item => {
        // 假设 paymentDate 是 "YYYY-MM-DD" 格式
        const dateStr = item.paymentDate;
        if (!dateStr) return false;
        return dateStr.startsWith(currentYear);
      });

      // 3. 按类型分组统计金额
      // 初始化 map，确保所有类型都在X轴显示，即使金额为0
      const typeMap = {};
      this.dict.type.utility_payments_bill_type.forEach(d => {
        typeMap[d.value] = 0;
      });

      thisYearData.forEach(item => {
        const type = item.paymentType;
        const fee = parseFloat(item.fee) || 0;
        // 如果该类型在字典中存在（或后端返回了新类型），则累加
        // 转为 string 比较，防止类型不一致 (int vs string)
        const matchedKey = Object.keys(typeMap).find(k => String(k) === String(type));
        if (matchedKey) {
          typeMap[matchedKey] += fee;
        }
      });

      // 4. 准备 ECharts 数据
      const xData = []; // 类型名称
      const yData = []; // 金额

      this.dict.type.utility_payments_bill_type.forEach(d => {
        xData.push(d.label); // X轴: 类型标签 (水费、电费等)
        yData.push(typeMap[d.value].toFixed(2)); // Y轴: 总金额
      });

      // 5. 渲染图表
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: {type: 'shadow'}
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: [
          {
            type: 'category',
            data: xData,
            axisTick: {alignWithLabel: true},
            axisLabel: {interval: 0} // 强制显示所有标签
          }
        ],
        yAxis: [
          {
            type: 'value',
            name: '金额 (元)'
          }
        ],
        series: [
          {
            name: '缴费金额',
            type: 'bar',
            barWidth: '40%',
            data: yData,
            itemStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {offset: 0, color: '#83bff6'},
                {offset: 0.5, color: '#188df0'},
                {offset: 1, color: '#188df0'}
              ])
            },
            label: {
              show: true,
              position: 'top'
            }
          }
        ]
      };

      this.barChartInstance.setOption(option);
    },

    // 搜索按钮操作
    handleQuery() {
      console.log("执行搜索操作");
      this.queryParams.pageNum = 1;
      this.getList();
    },
    // 重置查询条件
    resetQuery() {
      console.log("重置查询条件");
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      console.log("选择数据变更，选中", selection.length, "条记录");
      this.ids = selection.map(item => item.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    // 新增操作
    handleAdd() {
      console.log("打开新增费用缴纳记录对话框");
      this.reset();
      this.open = true;
      this.title = "添加费用缴纳记录";
    },
    // 修改操作
    handleUpdate(row) {
      console.log("打开修改费用缴纳记录对话框，记录ID:", row.id);
      this.reset();
      const id = row.id || this.ids;
      getPayments(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改费用缴纳记录";
        console.log("获取待修改记录详情成功");
      }).catch(error => {
        console.error("获取待修改记录详情失败:", error);
        this.$message.error("获取记录详情失败");
      });
    },
    // 提交表单
    submitForm() {
      console.log("提交表单，当前表单数据:", this.form);
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updatePayments(this.form).then(response => {
              console.log("费用缴纳记录修改成功");
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            }).catch(error => {
              console.error("费用缴纳记录修改失败:", error);
              this.$modal.msgError("修改失败");
            });
          } else {
            addPayments(this.form).then(response => {
              console.log("费用缴纳记录新增成功");
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            }).catch(error => {
              console.error("费用缴纳记录新增失败:", error);
              this.$modal.msgError("新增失败");
            });
          }
        } else {
          console.log("表单验证失败");
        }
      });
    },
    // 取消按钮操作
    cancel() {
      console.log("取消操作，关闭对话框");
      this.open = false;
      this.reset();
    },
    // 重置表单
    reset() {
      console.log("重置表单");
      this.form = {
        id: null,
        userId: this.queryParams.userId,
        fee: null,
        paymentType: null,
        paymentStatus: null,
        paymentDate: null,
        createdAt: null,
        updatedAt: null
      };
      this.resetForm("form");
    },
    // 删除操作
    handleDelete(row) {
      console.log("执行删除操作，记录ID:", row.id);
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除费用缴纳记录编号为"' + ids + '"的数据项？').then(() => {
        return delPayments(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
        console.log("费用缴纳记录删除成功");
      }).catch((error) => {
        console.error("费用缴纳记录删除失败:", error);
      });
    },
    // 导出操作
    handleExport() {
      console.log("执行导出操作，导出参数:", this.queryParams);
      this.download('finance/payments/export', {...this.queryParams}, `payments_${new Date().getTime()}.xlsx`);
    }
  }
};
</script>

<style scoped>
.chart-row {
  margin-bottom: 20px;
}

.chart-box {
  width: 100%;
  height: 300px;
}
</style>
