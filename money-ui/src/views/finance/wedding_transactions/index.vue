<template>
  <div class="app-container">
    <!-- 搜索条件 + 操作按钮 -->
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      :inline="true"
      label-width="100px"
      class="search-form"
    >
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="交易日期" prop="transactionDate">
            <el-date-picker
              v-model="queryParams.transactionDate"
              type="date"
              value-format="yyyy-MM-dd"
              placeholder="请选择交易日期"
              style="width:100%"
              clearable
            />
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="交易类型" prop="transactionType">
            <el-select
              v-model="queryParams.transactionType"
              placeholder="请选择交易类型"
              style="width:100%"
              clearable
            >
              <el-option
                v-for="d in dict.type.type_of_expenditure"
                :key="d.value"
                :label="d.label"
                :value="d.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="交易类别" prop="category">
            <el-select
              v-model="queryParams.category"
              placeholder="请选择交易类别"
              style="width:100%"
              clearable
            >
              <el-option
                v-for="d in dict.type.presents"
                :key="d.value"
                :label="d.label"
                :value="d.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="具体项目" prop="itemName">
            <el-input
              v-model="queryParams.itemName"
              placeholder="请输入项目名称"
              style="width:100%"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-form-item label="支付方式" prop="paymentMethod">
            <el-select
              v-model="queryParams.paymentMethod"
              placeholder="请选择支付方式"
              style="width:100%"
              clearable
            >
              <el-option
                v-for="d in dict.type.payment_method"
                :key="d.value"
                :label="d.label"
                :value="d.value"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="6">
          <el-form-item label="参与人" prop="participant">
            <el-input
              v-model="queryParams.participant"
              placeholder="请输入参与人"
              style="width:100%"
              clearable
              @keyup.enter.native="handleQuery"
            />
          </el-form-item>
        </el-col>
        <el-col :span="12" class="text-right">
          <el-button type="primary" size="mini" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button size="mini" icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-col>
      </el-row>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['finance:wedding_transactions:add']"
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
          v-hasPermi="['finance:wedding_transactions:edit']"
        >修改
        </el-button>
      </el-col>
    </el-row>

    <!-- 支出 vs 收入 水滴图 -->
    <el-row :gutter="20" class="chart-row">
      <el-col :span="12">
        <el-card shadow="hover">
          <div ref="expenseChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div ref="incomeChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="wedding_transactionsList"
      @selection-change="handleSelectionChange"
      style="width:100%; margin-top:20px;"
    >
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column prop="transactionDate" label="交易日期" width="140"/>
      <el-table-column label="交易类型" align="center" prop="transactionType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.type_of_expenditure" :value="scope.row.transactionType"/>
        </template>
      </el-table-column>
      <el-table-column label="交易类别" align="center" prop="category">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.presents" :value="scope.row.category"/>
        </template>
      </el-table-column>
      <el-table-column label="具体项目名称" align="center" prop="itemName">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.marriage_expenses" :value="scope.row.itemName"/>
        </template>
      </el-table-column>
      <el-table-column label="交易金额" align="center" prop="amount"/>
      <el-table-column label="支付方式" align="center" prop="paymentMethod">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.payment_method" :value="scope.row.paymentMethod"/>
        </template>
      </el-table-column>
      <el-table-column prop="participant" label="参与人" width="100"/>
      <el-table-column prop="notes" label="备注"/>
      <el-table-column prop="updatedAt" label="更新时间" width="140"/>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
      style="margin-top:20px;"
    />

    <!-- 新增/修改对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px" class="dialog-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="交易日期" prop="transactionDate">
              <el-date-picker
                v-model="form.transactionDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="请选择交易日期"
                style="width:100%"
                clearable
              />
            </el-form-item>
            <el-form-item label="交易类型" prop="transactionType">
              <el-select v-model="form.transactionType" placeholder="请选择交易类型">
                <el-option
                  v-for="dict in dict.type.type_of_expenditure"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="交易类别" prop="category">
              <el-select v-model="form.category" placeholder="请选择交易类别">
                <el-option
                  v-for="dict in dict.type.presents"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="具体项目名称" prop="itemName">
              <el-select v-model="form.itemName" placeholder="请选择具体项目名称">
                <el-option
                  v-for="dict in dict.type.marriage_expenses"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="交易金额" prop="amount">
              <el-input v-model="form.amount" placeholder="请输入交易金额"/>
            </el-form-item>
            <el-form-item label="支付方式，如现金、银行卡等" prop="paymentMethod">
              <el-select v-model="form.paymentMethod" placeholder="请选择支付方式，如现金、银行卡等">
                <el-option
                  v-for="dict in dict.type.payment_method"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                ></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="参与人" prop="participant">
              <el-input v-model="form.participant" placeholder="请输入参与人" style="width:100%"/>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注" prop="notes">
              <el-input type="textarea" v-model="form.notes" placeholder="请输入备注" style="width:100%"/>
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="cancel">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import 'echarts-liquidfill';
import {
  listWeddingTransactions,
  addWeddingTransactions,
  updateWeddingTransactions,
  delWeddingTransactions,
} from '@/api/finance/wedding_transactions';
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
export default {
  name: 'Wedding_transactions',
  dicts: ['marriage_expenses', 'presents', 'payment_method', 'type_of_expenditure'],
  data() {
    return {
      loading: false,
      wedding_transactionsList: [],
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        transactionDate: null,
        transactionType: null,
        category: null,
        itemName: '',
        paymentMethod: null,
        participant: '',
        userId: null,        // 直接在 form 中使用 userId
      },
      form: {
        userId: null         // 直接在 form 中使用 userId
      },
      rules: {
        transactionDate: [{required: true, message: '交易日期不能为空', trigger: 'change'}],
        transactionType: [{required: true, message: '交易类型不能为空', trigger: 'change'}],
        category: [{required: true, message: '交易类别不能为空', trigger: 'change'}],
        itemName: [{required: true, message: '具体项目不能为空', trigger: 'blur'}],
        amount: [{required: true, message: '金额不能为空', trigger: 'blur'}],
      },
      open: false,
      title: '',
      ids: [],
      single: true,
      multiple: true,
      expenseChart: null,
      incomeChart: null,
    };
  },
  async created() {
    // 获取用户列表并设置 userId
    await this.initUserList();
    // 加载数据
    this.getList();
  },
  mounted() {
    this.expenseChart = echarts.init(this.$refs.expenseChart);
    this.incomeChart = echarts.init(this.$refs.incomeChart);
  },
  updated() {
    this.renderCharts();
  },
  methods: {
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
    async getList() {
      this.loading = true;
      const res = await listWeddingTransactions(this.queryParams);
      this.wedding_transactionsList = res.rows;
      this.total = res.total;
      this.loading = false;
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.$refs.queryForm.resetFields();
      this.handleQuery();
    },
    handleSelectionChange(sel) {
      this.ids = sel.map(i => i.id);
      this.single = sel.length !== 1;
      this.multiple = sel.length === 0;
    },
    handleAdd() {
      this.form = {};
      this.open = true;
      this.title = '添加明细';
    },
    handleUpdate() {
      const id = this.ids[0];
      const row = this.wedding_transactionsList.find(i => i.id === id);
      this.form = {...row};
      this.open = true;
      this.title = '修改明细';
    },
    cancel() {
      this.open = false;
    },
    async submitForm() {
      this.$refs.form.validate(async valid => {
        if (!valid) return;
        if (this.form.id) {
          await updateWeddingTransactions(this.form);
        } else {
          await addWeddingTransactions(this.form);
        }
        this.open = false;
        this.getList();
      });
    },
    handleDelete() {
      delWeddingTransactions(this.ids).then(() => this.getList());
    },
    handleExport() {
      // 导出逻辑
    },
    renderCharts() {
      const totalExpense = this.wedding_transactionsList
        .filter(i => i.transactionType === '1')
        .reduce((s, i) => s + Number(i.amount), 0);
      const totalIncome = this.wedding_transactionsList
        .filter(i => i.transactionType === '2')
        .reduce((s, i) => s + Number(i.amount), 0);

      const expRatio = totalExpense / Math.max(totalIncome + totalExpense, 1);
      const incRatio = totalIncome / Math.max(totalIncome + totalExpense, 1);

      const option = (name, val, color) => ({
        series: [
          {
            type: 'liquidFill',
            radius: '80%',
            data: [val],
            center: ['50%', '50%'],
            backgroundStyle: {color: '#f5f5f5'},
            outline: {show: false},
            itemStyle: {color, opacity: 0.7},
            label: {
              formatter: () => `${name}\n${(val * 100).toFixed(1)}%`,
              fontSize: 18,
              color: '#000',
            },
          },
        ],
      });

      this.expenseChart.setOption(option('支出', expRatio, '#FF6B6B'));
      this.incomeChart.setOption(option('收入', incRatio, '#4ECDC4'));
    },
  },
};
</script>

<style scoped>
@import "~@/assets/styles/global.scss";
.app-container {
  padding: 20px;
}

.search-form .el-form-item {
  margin-bottom: 10px;
}

.chart-row {
  margin-top: 20px;
}

.chart-box {
  width: 100%;
  height: 300px;
}

.mb8 {
  margin-bottom: 8px;
}

.dialog-form .el-form-item {
  margin-bottom: 16px;
}

.dialog-footer {
  text-align: right;
}
</style>
