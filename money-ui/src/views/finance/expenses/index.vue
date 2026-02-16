<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="项目名称" prop="itemName">
        <el-input
          v-model="queryParams.itemName"
          placeholder="请输入项目名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="出资人" prop="payer">
        <el-select v-model="queryParams.payer" placeholder="请选择出资人" clearable filterable @change="handleQuery">
          <el-option
            v-for="item in userOptions"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="20" style="margin-bottom: 20px;">
      <el-col :span="12">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span style="font-weight: bold; font-size: 16px;">出资方归属比例</span>
          </div>
          <div ref="pieChart" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span style="font-weight: bold; font-size: 16px;">总金额概览</span>
          </div>
          <div ref="liquidChart" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['finance:expenses:add']"
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
          v-hasPermi="['finance:expenses:edit']"
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
          v-hasPermi="['finance:expenses:remove']"
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
          v-hasPermi="['finance:expenses:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="expensesList" @selection-change="handleSelectionChange" stripe border>
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="项目名称" align="center" prop="itemName"/>
      <el-table-column label="金额" align="center" prop="amount"/>
      <el-table-column label="消费日期" align="center" prop="expenseDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.expenseDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="所属阶段" align="center" prop="stage">
        <template slot-scope="scope">
          <span v-if="scope.row.stage == '0'">订婚</span>
          <span v-else-if="scope.row.stage == '1'">婚礼</span>
          <span v-else-if="scope.row.stage == '2'">婚后</span>
          <span v-else>{{ scope.row.stage }}</span>
        </template>
      </el-table-column>
      <el-table-column label="出资人" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" type="info">{{ getUserName(scope.row.payer) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="出资方归属" align="center" prop="payerType">
        <template slot-scope="scope">
          <span v-if="scope.row.payerType == '0'">小家</span>
          <span v-else-if="scope.row.payerType == '1'">男方父母</span>
          <span v-else-if="scope.row.payerType == '2'">女方父母</span>
          <span v-else>{{ scope.row.payerType }}</span>
        </template>
      </el-table-column>
      <el-table-column label="分类" align="center" prop="category"/>
      <el-table-column label="备注" align="center" prop="remark"/>
      <el-table-column label="更新时间" align="center" prop="updatedAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updatedAt, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['finance:expenses:edit']"
          >修改
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

    <!-- 添加或修改婚礼订婚支出流水对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="项目名称" prop="itemName">
          <el-input v-model="form.itemName" placeholder="请输入项目名称：如 订婚宴、三金、婚庆尾款"/>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input v-model="form.amount" placeholder="请输入金额：精确到分"/>
        </el-form-item>
        <el-form-item label="消费日期" prop="expenseDate">
          <el-date-picker clearable
                          v-model="form.expenseDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择消费日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="所属阶段" prop="stage">
          <el-select v-model="form.stage" placeholder="请选择所属阶段">
            <el-option label="订婚" value="0"></el-option>
            <el-option label="婚礼" value="1"></el-option>
            <el-option label="婚后" value="2"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="出资方归属" prop="payerType">
          <el-select v-model="form.payerType" placeholder="请选择出资方归属">
            <el-option label="小家" value="0"></el-option>
            <el-option label="男方父母" value="1"></el-option>
            <el-option label="女方父母" value="2"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-input v-model="form.category" placeholder="请输入分类"/>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" placeholder="请输入备注"/>
        </el-form-item>
        <el-form-item label="出资方" prop="payer">
          <el-select v-model="form.payer" placeholder="出资方" filterable>
            <el-option
              v-for="item in userOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
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
  listExpenses,
  getExpenses,
  delExpenses,
  addExpenses,
  updateExpenses,
  getExpensesStats
} from "@/api/finance/expenses"
import {listUser} from "@/api/stock/dropdown_component";
import * as echarts from 'echarts';
import 'echarts-liquidfill';

export default {
  name: "Expenses",
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
      // 婚礼订婚支出流水表格数据
      expensesList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        itemName: null,
        amount: null,
        expenseDate: null,
        stage: null,
        payerType: null,
        category: null,
        userId: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        itemName: [
          {required: true, message: "项目名称不能为空", trigger: "blur"}
        ],
        amount: [
          {required: true, message: "金额不能为空", trigger: "blur"}
        ],
        expenseDate: [
          {required: true, message: "消费日期不能为空", trigger: "blur"}
        ],
        stage: [
          {required: true, message: "所属阶段不能为空", trigger: "blur"}
        ],
        payerType: [
          {required: true, message: "出资方归属不能为空", trigger: "change"}
        ],
        category: [
          {required: true, message: "分类不能为空", trigger: "blur"}
        ],
        userId: [
          {required: true, message: "关联用户不能为空", trigger: "blur"}
        ],
      },
      userOptions: [],
      pieChartInstance: null,
      liquidChartInstance: null
    }
  },
  async created() {
    await this.initUserList();
    this.getList();
  },
  mounted() {
    this.initCharts();
    window.addEventListener('resize', this.resizeCharts);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    if (this.pieChartInstance) this.pieChartInstance.dispose();
    if (this.liquidChartInstance) this.liquidChartInstance.dispose();
  },
  methods: {
    /**
     * 初始化用户列表数据
     */
    async initUserList() {
      try {
        const response = await listUser({pageSize: 1000});
        const payload = response.data || response;
        const rawUsers = Array.isArray(payload.rows)
          ? payload.rows
          : Array.isArray(payload)
            ? payload
            : [];

        this.userOptions = rawUsers.map(u => ({
          id: u.userId,
          name: u.nickName || u.userName || `用户${u.userId}`
        }));

        if (this.userOptions.length) {
          const savedUsername = this.$cookies.get('username');
          const matchedUser = this.userOptions.find(u => u.name === savedUsername);
          if (matchedUser) {
            // Note: We do NOT auto-select for queryParams here to allow viewing all data by default,
            // or we could if that's the desired behavior. The original code did.
            // But usually filters are optional. Let's keep it optional for list view.
            // However, the original code did: this.queryParams.userId = matchedUser.id;
            // I will set it only if it was null? Or just not set it to allow seeing all.
            // Let's stick to the behavior: auto-select "me" if found.
            if (!this.queryParams.userId) {
              this.queryParams.userId = matchedUser.id;
            }
          }
        }
      } catch (err) {
        console.error('用户列表加载失败:', err);
      }
    },
    getUserName(userId) {
      if (!userId) return '-';
      const user = this.userOptions.find(u => u.id === userId);
      return user ? user.name : `用户${userId}`;
    },
    /** 查询婚礼订婚支出流水列表 */
    getList() {
      this.loading = true
      listExpenses(this.queryParams).then(response => {
        this.expensesList = response.rows
        this.total = response.total
        this.loading = false
        this.getStats();
      })
    },
    getStats() {
      getExpensesStats(this.queryParams).then(response => {
        const data = response.data || [];
        this.updateCharts(data);
      });
    },
    initCharts() {
      this.pieChartInstance = echarts.init(this.$refs.pieChart);
      this.liquidChartInstance = echarts.init(this.$refs.liquidChart);

      // Initial empty state or loading state
      this.pieChartInstance.setOption({
        title: {
          text: '暂无数据',
          left: 'center',
          top: 'center',
          textStyle: {color: '#909399'}
        },
        series: []
      });

      this.liquidChartInstance.setOption({
        title: {
          text: '¥0.00',
          left: 'center',
          top: 'center',
          textStyle: {fontSize: 24, color: '#C23531'}
        },
        series: [{
          type: 'liquidFill',
          data: [0.5],
          radius: '80%',
          label: {show: false} // Hide label inside, use title
        }]
      });
    },
    resizeCharts() {
      if (this.pieChartInstance) this.pieChartInstance.resize();
      if (this.liquidChartInstance) this.liquidChartInstance.resize();
    },
    updateCharts(data) {
      const payerTypeMap = {
        '0': '小家',
        '1': '男方父母',
        '2': '女方父母'
      };

      // Calculate total amount
      let totalAmount = 0;
      const pieData = [];

      data.forEach(item => {
        const amount = parseFloat(item.totalAmount);
        totalAmount += amount;
        pieData.push({
          value: amount,
          name: payerTypeMap[item.payerType] || item.payerType
        });
      });

      // Update Pie Chart
      if (pieData.length > 0) {
        this.pieChartInstance.setOption({
          title: {show: false}, // Hide "No Data"
          tooltip: {
            trigger: 'item',
            formatter: '{b}: {c} ({d}%)'
          },
          legend: {
            orient: 'vertical',
            left: 'left'
          },
          series: [
            {
              name: '出资方',
              type: 'pie',
              radius: ['40%', '70%'], // Donut chart looks better
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
              data: pieData
            }
          ]
        });
      } else {
        this.pieChartInstance.setOption({
          title: {
            text: '暂无数据',
            left: 'center',
            top: 'center',
            textStyle: {color: '#909399'}
          },
          series: []
        }, true); // Merge = true, but clearing series requires care.
        // Actually setOption with empty series clears it if not merge?
        // Let's explicitly clear series.
        this.pieChartInstance.clear();
        this.pieChartInstance.setOption({
          title: {
            text: '暂无数据',
            left: 'center',
            top: 'center',
            textStyle: {color: '#909399'}
          }
        });
      }

      // Update Liquid Fill Chart (Spherical) for Total Amount
      const formattedTotal = totalAmount.toLocaleString('zh-CN', {style: 'currency', currency: 'CNY'});

      this.liquidChartInstance.setOption({
        title: {
          show: false
        },
        series: [{
          type: 'liquidFill',
          data: [0.6, 0.5, 0.4], // Water level
          radius: '80%',
          backgroundStyle: {
            borderWidth: 2,
            borderColor: '#156ACF',
            color: '#E3F7FF'
          },
          outline: {
            show: false
          },
          label: {
            show: true,
            formatter: function () {
              return formattedTotal;
            },
            fontSize: 28,
            color: '#C23531',
            insideColor: '#fff'
          }
        }]
      });
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
        itemName: null,
        amount: null,
        expenseDate: null,
        stage: null,
        payerType: null,
        category: null,
        remark: null,
        userId: null,
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
      this.title = "添加婚礼订婚支出流水"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getExpenses(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改婚礼订婚支出流水"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateExpenses(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addExpenses(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除婚礼订婚支出流水编号为"' + ids + '"的数据项？').then(function () {
        return delExpenses(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/expenses/export', {
        ...this.queryParams
      }, `expenses_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}

.clearfix:after {
  clear: both
}
</style>
