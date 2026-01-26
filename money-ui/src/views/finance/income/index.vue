<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
<!--      <el-form-item label="关联用户" prop="userId">-->
<!--        <el-input-->
<!--          v-model="queryParams.userId"-->
<!--          placeholder="请输入关联用户"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="收入来源" prop="source">-->
<!--        <el-select v-model="queryParams.source" placeholder="请选择收入来源" clearable>-->
<!--          <el-option-->
<!--            v-for="dict in dict.type.salary_income"-->
<!--            :key="dict.value"-->
<!--            :label="dict.label"-->
<!--            :value="dict.value"-->
<!--          />-->
<!--        </el-select>-->
<!--      </el-form-item>-->
      <el-form-item label="收入日期" prop="incomeDate">
        <el-date-picker clearable
                        v-model="queryParams.incomeDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择收入日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="记录时间" prop="createdAt">
        <el-date-picker clearable
                        v-model="queryParams.createdAt"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择记录时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <div ref="chart" style="width: 100%; height: 350px; margin-bottom: 20px;"></div>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['finance:income:add']"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['finance:income:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="incomeList" @selection-change="handleSelectionChange" :stripe="true" :border="false">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="收入ID" align="center" prop="incomeId" width="80" />
      <el-table-column label="用户姓名" align="center">
        <template slot-scope="scope">
          <el-tag size="mini" type="info">{{ getUserName(scope.row.userId) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" align="right" prop="amount">
        <template slot-scope="scope">
           <span style="color: #67C23A; font-weight: bold;">+{{ scope.row.amount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收入来源" align="center" prop="source">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.salary_income" :value="scope.row.source"/>
        </template>
      </el-table-column>
      <el-table-column label="收入日期" align="center" prop="incomeDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.incomeDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <!--      <el-table-column label="收入分类" align="center" prop="category"/>-->
      <el-table-column label="记录时间" align="center" prop="createdAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createdAt, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['finance:income:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:income:remove']"
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

    <!-- 添加或修改收入对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="选择用户" prop="userId">
          <el-select
            v-model="form.userId"
            placeholder="请选择用户"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="user in users"
              :key="user.userId"
              :label="user.nickName"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input v-model="form.amount" placeholder="请输入金额"/>
        </el-form-item>
        <el-form-item label="收入来源" prop="source">
          <el-select v-model="form.source" placeholder="请选择收入来源">
            <el-option
              v-for="dict in dict.type.salary_income"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="收入日期" prop="incomeDate">
          <el-date-picker clearable
                          v-model="form.incomeDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择收入日期">
          </el-date-picker>
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
import {listIncome, getIncome, delIncome, addIncome, updateIncome, getIncomeStats} from "@/api/finance/income";
import {listUser} from "@/api/stock/dropdown_component";
import * as echarts from 'echarts';

export default {
  name: "Income",
  dicts: ['salary_income'],
  data() {
    return {
      chartInstance: null,
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
      // 收入表格数据
      incomeList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        amount: null,
        source: null,
        incomeDate: null,
        category: null,
        createdAt: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        userId: [
          {required: true, message: "关联用户不能为空", trigger: "blur"}
        ],
        amount: [
          {required: true, message: "金额不能为空", trigger: "blur"}
        ],
        incomeDate: [
          {required: true, message: "收入日期不能为空", trigger: "blur"}
        ],
      }
    };
  },
  async created() {
    this.getUserList();
    this.getList(); // 再加载表格数据
  },
  mounted() {
    this.initChart();
    this.getChartData();
    window.addEventListener('resize', this.resizeChart);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeChart);
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
  },
  methods: {
    initChart() {
      this.chartInstance = echarts.init(this.$refs.chart);
      this.chartInstance.setOption({
        title: {
          text: '月度收入趋势',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          formatter: '{b}: {c}元'
        },
        xAxis: {
          type: 'category',
          data: []
        },
        yAxis: {
          type: 'value',
          name: '金额 (元)'
        },
        series: [{
          data: [],
          type: 'line',
          smooth: true,
          areaStyle: {},
          itemStyle: {
            color: '#67C23A'
          }
        }]
      });
    },
    resizeChart() {
      if (this.chartInstance) {
        this.chartInstance.resize();
      }
    },
    getChartData() {
      getIncomeStats(this.addDateRange(this.queryParams, this.dateRange)).then(response => {
        const data = response.data || [];
        // Extract months and amounts, ensure sorted by month
        const months = data.map(item => item.month);
        const amounts = data.map(item => item.totalAmount);

        // Update chart
        if (this.chartInstance) {
          this.chartInstance.setOption({
            xAxis: {
              data: months
            },
            series: [{
              data: amounts
            }]
          });
        }
      });
    },
    // 修改后的 getUserList 方法，兼容返回 {code, msg, rows, total} 格式
    async getUserList() {
      try {
        this.isLoading = true;
        const response = await listUser({pageSize: this.pageSize || 1000});
        // 如果返回结果在 data 属性中，则取 data，否则直接使用 response
        const data = response.data || response;
        if (data.code === 200) {
          this.users = data.rows || [];
          console.info("获取用户列表成功:", this.users);
        } else {
          console.error("获取用户列表失败, 返回码:", data.code, data.msg);
          this.error = data.msg || "获取用户列表失败，请稍后再试";
        }
      } catch (error) {
        console.error("获取用户列表失败:", error);
        this.error = "获取用户列表失败，请稍后再试";
      } finally {
        this.isLoading = false;
      }
    },
    getUserName(userId) {
      if (!userId) {
        throw new Error("用户ID不能为空");
      }
      if (!this.users || this.users.length === 0) {
        throw new Error("用户列表为空");
      }
      const targetId = String(userId);
      const user = this.users.find(u => String(u.userId) === targetId);
      if (!user) {
        console.warn(`未找到匹配的用户，用户ID: ${userId}`);
        return "未知用户";
      }
      return user.nickName || "匿名用户";
    },
    /** 查询收入列表 */
    getList() {
      this.loading = true;
      listIncome(this.queryParams).then(response => {
        this.incomeList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
      this.getChartData();
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        incomeId: null,
        userId: null,
        amount: null,
        source: null,
        incomeDate: null,
        category: null,
        createdAt: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.incomeId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加收入";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const incomeId = row.incomeId || this.ids
      getIncome(incomeId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改收入";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.incomeId != null) {
            updateIncome(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addIncome(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const incomeIds = row.incomeId || this.ids;
      this.$modal.confirm('是否确认删除收入编号为"' + incomeIds + '"的数据项？').then(function () {
        return delIncome(incomeIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {
      });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/income/export', {
        ...this.queryParams
      }, `income_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>

<style lang="scss" scoped>
</style>
