<template>
  <div class="app-container">
    <!-- Charts Section -->
    <el-row :gutter="20" class="mb20">
      <el-col :span="12" :xs="24">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-pie-chart"></i> 付款人支出分布</span>
          </div>
          <div id="payerRingChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12" :xs="24">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-data-line"></i> 总支出统计</span>
          </div>
          <div id="totalLiquidChart" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Search Form -->
    <el-card shadow="never" class="mb20">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
        <el-form-item label="婚礼名称" prop="weddingName">
          <el-input
            v-model="queryParams.weddingName"
            placeholder="请输入婚礼名称"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="付款人" prop="payer">
          <el-input
            v-model="queryParams.payer"
            placeholder="请输入付款人"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="婚礼日期" prop="weddingDate">
          <el-date-picker clearable
                          v-model="queryParams.weddingDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择婚礼日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="举办城市" prop="weddingCity">
          <el-input
            v-model="queryParams.weddingCity"
            placeholder="请输入婚礼举办城市"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="支出分类" prop="expenseCategory">
          <el-input
            v-model="queryParams.expenseCategory"
            placeholder="请输入支出分类"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="收款方" prop="payee">
          <el-input
            v-model="queryParams.payee"
            placeholder="请输入收款方"
            clearable
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Action Buttons -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['finance:weddingExpense:add']"
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
          v-hasPermi="['finance:weddingExpense:edit']"
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
          v-hasPermi="['finance:weddingExpense:remove']"
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
          v-hasPermi="['finance:weddingExpense:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- Data Table -->
    <el-table v-loading="loading" :data="weddingExpenseList" @selection-change="handleSelectionChange" border stripe>
      <el-table-column type="selection" width="55" align="center"/>
      <!-- <el-table-column label="主键ID" align="center" prop="id"/> -->
      <el-table-column label="婚礼名称" align="center" prop="weddingName"/>
      <el-table-column label="付款人" align="center" prop="payer">
        <template slot-scope="scope">
          <el-tag size="small" v-if="scope.row.payer">{{ scope.row.payer }}</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="婚礼日期" align="center" prop="weddingDate" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.weddingDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="城市" align="center" prop="weddingCity"/>
      <el-table-column label="支出分类" align="center" prop="expenseCategory"/>
      <el-table-column label="具体项目" align="center" prop="expenseItem"/>
      <el-table-column label="金额 (元)" align="center" prop="amount">
        <template slot-scope="scope">
          <span style="font-weight: bold; color: #F56C6C;">{{ scope.row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="支付日期" align="center" prop="paymentDate" width="120">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.paymentDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收款方" align="center" prop="payee"/>
      <el-table-column label="备注" align="center" prop="notes" show-overflow-tooltip/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['finance:weddingExpense:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:weddingExpense:remove']"
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

    <!-- Add/Edit Dialog -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="婚礼名称" prop="weddingName">
              <el-input v-model="form.weddingName" placeholder="请输入婚礼名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="婚礼日期" prop="weddingDate">
              <el-date-picker clearable
                              v-model="form.weddingDate"
                              type="date"
                              value-format="yyyy-MM-dd"
                              placeholder="请选择婚礼日期"
                              style="width: 100%">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
             <el-form-item label="付款人" prop="payer">
              <el-input v-model="form.payer" placeholder="请输入付款人"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="城市" prop="weddingCity">
              <el-input v-model="form.weddingCity" placeholder="请输入婚礼举办城市"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="支出分类" prop="expenseCategory">
              <el-input v-model="form.expenseCategory" placeholder="例如：酒席、婚纱"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="具体项目" prop="expenseItem">
              <el-input v-model="form.expenseItem" placeholder="例如：定金、尾款"/>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="支出金额" prop="amount">
              <el-input v-model="form.amount" placeholder="请输入支出金额" type="number"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="支付日期" prop="paymentDate">
              <el-date-picker clearable
                              v-model="form.paymentDate"
                              type="date"
                              value-format="yyyy-MM-dd"
                              placeholder="请选择支付日期"
                              style="width: 100%">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="收款方" prop="payee">
          <el-input v-model="form.payee" placeholder="请输入收款方"/>
        </el-form-item>
        <el-form-item label="备注说明" prop="notes">
          <el-input v-model="form.notes" type="textarea" placeholder="请输入内容"/>
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
import * as echarts from 'echarts';
import 'echarts-liquidfill';
import {
  listWeddingExpense,
  getWeddingExpense,
  delWeddingExpense,
  addWeddingExpense,
  updateWeddingExpense
} from "@/api/finance/weddingExpense"
import {listUser} from "@/api/stock/dropdown_component";

export default {
  name: "WeddingExpense",
  data() {
    return {
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
      // 婚礼支出记录表格数据
      weddingExpenseList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        weddingName: null,
        weddingDate: null,
        weddingCity: null,
        expenseCategory: null,
        expenseItem: null,
        amount: null,
        paymentDate: null,
        payee: null,
        payer: null,
        notes: null,
        updatedAt: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        expenseCategory: [
          {required: true, message: "支出分类不能为空", trigger: "blur"}
        ],
        amount: [
          {required: true, message: "支出金额不能为空", trigger: "blur"}
        ],
        weddingDate: [
           {required: true, message: "婚礼日期不能为空", trigger: "blur"}
        ]
      },
      // Charts
      charts: {
        payerRing: null,
        totalLiquid: null
      }
    }
  },
  async created() {
    await this.initUserList();
    this.getList();
  },
  mounted() {
    window.addEventListener('resize', this.resizeCharts);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    this.disposeCharts();
  },
  methods: {
    /**
     * 初始化用户列表数据
     */
    async initUserList() {
      try {
        const response = await listUser({pageSize: 1000});
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
    /** 查询婚礼支出记录列表 */
    getList() {
      this.loading = true
      listWeddingExpense(this.queryParams).then(response => {
        this.weddingExpenseList = response.rows
        this.total = response.total
        this.loading = false
        // Refresh charts with all data
        this.loadCharts();
      })
    },
    /** 加载图表数据 */
    loadCharts() {
      // Create a separate query for all data (no pagination)
      const chartParams = { ...this.queryParams, pageNum: 1, pageSize: 10000 };
      listWeddingExpense(chartParams).then(response => {
        const allData = response.rows || [];
        this.renderPayerRingChart(allData);
        this.renderTotalLiquidChart(allData);
      });
    },
    /** 渲染付款人环形图 */
    renderPayerRingChart(data) {
      const dom = document.getElementById('payerRingChart');
      if (!dom) return;
      if (this.charts.payerRing) this.charts.payerRing.dispose();
      this.charts.payerRing = echarts.init(dom);

      // Aggregation
      const payerMap = {};
      let totalAmount = 0;
      data.forEach(item => {
        const payer = item.payer || '未知';
        const amount = Number(item.amount) || 0;
        payerMap[payer] = (payerMap[payer] || 0) + amount;
        totalAmount += amount;
      });

      const seriesData = Object.keys(payerMap).map(key => ({
        name: key,
        value: payerMap[key]
      }));

      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c} ({d}%)'
        },
        legend: {
          top: '5%',
          left: 'center'
        },
        series: [
          {
            name: '付款人支出',
            type: 'pie',
            radius: ['40%', '70%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderRadius: 10,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: '20',
                fontWeight: 'bold'
              }
            },
            labelLine: {
              show: false
            },
            data: seriesData
          }
        ]
      };
      this.charts.payerRing.setOption(option);
    },
    /** 渲染总支出水滴图 */
    renderTotalLiquidChart(data) {
      const dom = document.getElementById('totalLiquidChart');
      if (!dom) return;
      if (this.charts.totalLiquid) this.charts.totalLiquid.dispose();
      this.charts.totalLiquid = echarts.init(dom);

      const totalAmount = data.reduce((sum, item) => sum + (Number(item.amount) || 0), 0);

      // Liquid fill typically expects a value between 0 and 1.
      // Since we just want to show the total amount, we can fake a "fullness" or just show it.
      // We will set it to 0.6 (60%) for visual effect and display the total amount as text.

      const option = {
        series: [{
          type: 'liquidFill',
          data: [0.6, 0.55, 0.5],
          radius: '80%',
          color: ['#409EFF', '#66b1ff', '#8cc5ff'],
          backgroundStyle: {
             color: '#f0f2f5'
          },
          label: {
            formatter: function() {
              return '总支出\n' + totalAmount.toLocaleString() + '元';
            },
            fontSize: 28,
            color: '#409EFF',
            insideColor: '#fff'
          },
          outline: {
            show: true,
            borderDistance: 0,
            itemStyle: {
              borderWidth: 2,
              borderColor: '#409EFF',
            }
          }
        }]
      };

      this.charts.totalLiquid.setOption(option);
    },
    resizeCharts() {
      if (this.charts.payerRing) this.charts.payerRing.resize();
      if (this.charts.totalLiquid) this.charts.totalLiquid.resize();
    },
    disposeCharts() {
      if (this.charts.payerRing) this.charts.payerRing.dispose();
      if (this.charts.totalLiquid) this.charts.totalLiquid.dispose();
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
        userId: this.queryParams.userId, // Keep userId
        weddingName: null,
        weddingDate: null,
        weddingCity: null,
        expenseCategory: null,
        expenseItem: null,
        amount: null,
        paymentDate: null,
        payee: null,
        payer: null,
        notes: null,
        createdAt: null,
        updatedAt: null,
        createdBy: null
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
      // restore userId if needed, but resetForm might clear it if it's in the form.
      // queryForm matches props. We should verify if userId is in queryForm?
      // No, queryParams.userId is not bound to a form field in template, so resetForm("queryForm") won't touch it.
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
      this.title = "添加婚礼支出记录"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getWeddingExpense(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改婚礼支出记录"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // Ensure userId is set
          if (!this.form.userId) {
             this.form.userId = this.queryParams.userId;
          }
          if (this.form.id != null) {
            updateWeddingExpense(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addWeddingExpense(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除婚礼支出记录编号为"' + ids + '"的数据项？').then(function () {
        return delWeddingExpense(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/weddingExpense/export', {
        ...this.queryParams
      }, `weddingExpense_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.mb20 {
  margin-bottom: 20px;
}
.chart-card {
  margin-bottom: 20px;
}
</style>
