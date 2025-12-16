<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb-4">
      <el-col :span="24">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="clearfix">
            <span class="card-title"><i class="el-icon-data-line"></i> 还贷趋势分析</span>
          </div>
          <div id="lineChart" ref="lineChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="search-card mb-4">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="70px">
        <el-form-item label="期数" prop="installments">
          <el-input
            v-model="queryParams.installments"
            placeholder="请输入期数"
            clearable
            style="width: 200px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="还款日期" prop="repaymentDate">
          <el-date-picker clearable
                          v-model="queryParams.repaymentDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="选择日期"
                          style="width: 200px">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="是否结清" prop="isSettled">
          <el-select v-model="queryParams.isSettled" placeholder="请选择状态" clearable style="width: 200px">
            <el-option
              v-for="dict in dict.type.repayment_status"
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
        <span class="card-title">还款明细列表</span>
        <div class="right-actions">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['finance:loan_repayments:add']"
          >新增
          </el-button>
          <el-button
            type="success"
            plain
            icon="el-icon-edit"
            size="mini"
            :disabled="single"
            @click="handleUpdate"
            v-hasPermi="['finance:loan_repayments:edit']"
          >修改
          </el-button>
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="mini"
            :disabled="multiple"
            @click="handleDelete"
            v-hasPermi="['finance:loan_repayments:remove']"
          >删除
          </el-button>
          <el-button
            type="warning"
            plain
            icon="el-icon-download"
            size="mini"
            @click="handleExport"
            v-hasPermi="['finance:loan_repayments:export']"
          >导出
          </el-button>
          <el-upload
            class="upload-demo inline-block"
            ref="upload"
            action=""
            :show-file-list="false"
            :before-upload="beforeUpload"
            :on-change="handleFileChange"
            accept=".csv, .xlsx"
            :auto-upload="false"
          >
            <el-button
              type="info"
              plain
              icon="el-icon-upload2"
              size="mini"
              class="ml-10"
              v-hasPermi="['finance:loan_repayments:import']"
            >导入更新
            </el-button>
          </el-upload>
          <right-toolbar :showSearch.sync="showSearch" @queryTable="getList" class="ml-10"></right-toolbar>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="loan_repaymentsList"
        @selection-change="handleSelectionChange"
        border
        stripe
        :header-cell-style="{background:'#f8f8f9',color:'#515a6e'}"
      >
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="期数" align="center" prop="installments" width="80">
          <template slot-scope="scope">
            <el-tag size="mini" effect="plain">第 {{ scope.row.installments }} 期</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="还款日期" align="center" prop="repaymentDate" width="120">
          <template slot-scope="scope">
            <span>{{ parseTime(scope.row.repaymentDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="贷款本金余额" align="right" prop="totalAmount" min-width="120">
          <template slot-scope="scope">{{ formatMoney(scope.row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="应还本金" align="right" prop="principal" min-width="110">
          <template slot-scope="scope">{{ formatMoney(scope.row.principal) }}</template>
        </el-table-column>
        <el-table-column label="应还利息" align="right" prop="interest" min-width="110">
          <template slot-scope="scope">{{ formatMoney(scope.row.interest) }}</template>
        </el-table-column>
        <el-table-column label="本息合计" align="right" prop="totalPrincipalAndInterest" min-width="120">
          <template slot-scope="scope">
            <span style="color: #F56C6C; font-weight: bold;">{{
                formatMoney(scope.row.totalPrincipalAndInterest)
              }}</span>
          </template>
        </el-table-column>

        <el-table-column label="还款年龄" align="center" prop="repaymentAge" width="100"/>
        <el-table-column label="利率(LPR-50)" align="center" prop="floatingInterestRate" width="120">
          <template slot-scope="scope">{{
              scope.row.floatingInterestRate ? scope.row.floatingInterestRate + '%' : '-'
            }}
          </template>
        </el-table-column>

        <el-table-column label="状态" align="center" prop="isSettled" width="100">
          <template slot-scope="scope">
            <dict-tag :options="dict.type.repayment_status" :value="scope.row.isSettled"/>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="120">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['finance:loan_repayments:edit']"
            >修改
            </el-button>
            <el-button
              size="mini"
              type="text"
              class="text-danger"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['finance:loan_repayments:remove']"
            >删除
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

    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="期数" prop="installments">
              <el-input-number v-model="form.installments" :min="1" controls-position="right" style="width: 100%"
                               placeholder="请输入期数"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="还款日期" prop="repaymentDate">
              <el-date-picker clearable
                              style="width: 100%"
                              v-model="form.repaymentDate"
                              type="date"
                              value-format="yyyy-MM-dd"
                              placeholder="请选择还款日期">
              </el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="贷款余额" prop="totalAmount">
              <el-input v-model="form.totalAmount" placeholder="0.00">
                <template slot="append">元</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="本息合计" prop="totalPrincipalAndInterest">
              <el-input v-model="form.totalPrincipalAndInterest" placeholder="0.00">
                <template slot="append">元</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="应还本金" prop="principal">
              <el-input v-model="form.principal" placeholder="0.00">
                <template slot="append">元</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="应还利息" prop="interest">
              <el-input v-model="form.interest" placeholder="0.00">
                <template slot="append">元</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row>
          <el-col :span="12">
            <el-form-item label="浮动利率" prop="floatingInterestRate">
              <el-input v-model="form.floatingInterestRate" placeholder="LPR-50">
                <template slot="append">%</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="还款年龄" prop="repaymentAge">
              <el-input v-model="form.repaymentAge" placeholder="岁"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="状态" prop="isSettled">
          <el-radio-group v-model="form.isSettled">
            <el-radio
              v-for="dict in dict.type.repayment_status"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{ dict.label }}
            </el-radio>
          </el-radio-group>
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
import * as echarts from 'echarts'
import {
  listLoanRepayments,
  getLoanRepayments,
  delLoanRepayments,
  addLoanRepayments,
  importRepayments,
  getLoanRepaymentsLineChart,
  updateLoanRepayments
} from "@/api/finance/loan_repayments";
import {listUser} from "@/api/stock/dropdown_component";

export default {
  name: "Loan_repayments",
  dicts: ['repayment_status'],
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
      // 贷款剩余计算表格数据
      loan_repaymentsList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        totalAmount: null,
        repaymentAge: null,
        installments: null,
        repaymentDate: null,
        isSettled: null,
        userId: null,
      },
      // 表单参数
      form: {
        userId: null
      },
      // 表单校验
      rules: {},
      selectedFile: null,
      isUploading: false,
      chart: null
    };
  },
  async created() {
    await this.initUserList();
    this.getList();
  },
  mounted() {
    window.addEventListener('resize', this.resizeChart);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeChart);
    if (this.chart) {
      this.chart.dispose();
    }
  },
  methods: {
    // 格式化金额，添加千分位
    formatMoney(value) {
      if (value === null || value === undefined) return '0.00';
      return '¥ ' + Number(value).toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2});
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
      this.loading = true;
      listLoanRepayments(this.queryParams).then(response => {
        this.loan_repaymentsList = response.rows;
        this.total = response.total;
        this.loading = false;
        this.$nextTick(() => {
          this.renderChart();
        })
      });
    },
    renderChart() {
      if (!this.$refs.lineChart) return;
      getLoanRepaymentsLineChart(this.queryParams.userId).then(response => {
        const data = response || [];
        const sorted = [...data].sort((a, b) => new Date(a.repaymentDate) - new Date(b.repaymentDate));
        const xData = sorted.map(i => i.repaymentDate);
        const yData = sorted.map(i => i.totalPrincipalAndInterest);

        if (this.chart) {
          this.chart.dispose();
        }
        this.chart = echarts.init(this.$refs.lineChart);
        this.chart.setOption({
          color: ['#409EFF'],
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255, 255, 255, 0.9)',
            textStyle: {color: '#333'}
          },
          grid: {left: '3%', right: '4%', bottom: '3%', containLabel: true},
          xAxis: {
            type: 'category',
            data: xData,
            axisLine: {lineStyle: {color: '#ccc'}},
            axisLabel: {color: '#666'}
          },
          yAxis: {
            type: 'value',
            name: '还款金额(元)',
            splitLine: {lineStyle: {type: 'dashed', color: '#eee'}}
          },
          series: [{
            name: '本息合计',
            type: 'line',
            data: yData,
            smooth: true,
            symbol: 'emptyCircle',
            symbolSize: 6,
            areaStyle: {
              opacity: 0.1,
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {offset: 0, color: '#409EFF'},
                {offset: 1, color: '#fff'}
              ])
            }
          }]
        });
      });
    },
    resizeChart() {
      this.chart && this.chart.resize();
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        id: null,
        totalAmount: null,
        repaymentAge: null,
        installments: null,
        repaymentDate: null,
        principal: null,
        interest: null,
        totalPrincipalAndInterest: null,
        isSettled: null,
        floatingInterestRate: null,
        userId: this.form.userId // 保留当前用户ID
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
      this.title = "添加还款记录";
    },
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids;
      getLoanRepayments(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改还款记录";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateLoanRepayments(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addLoanRepayments(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除数据项？').then(function () {
        return delLoanRepayments(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {
      });
    },
    handleExport() {
      this.download('finance/loan_repayments/export', {
        ...this.queryParams
      }, `loan_repayments_${new Date().getTime()}.xlsx`);
    },
    handleFileChange(file, fileList) {
      this.selectedFile = file.raw;
      if (this.selectedFile) {
        this.$modal.confirm(`是否确认导入文件 "${this.selectedFile.name}" ?`).then(() => {
          this.confirmImport();
        }).catch(() => {
          this.resetUpload();
        });
      }
    },
    beforeUpload(file) {
      const isCsv = file.type === 'text/csv' || file.type === 'application/vnd.ms-excel';
      const isXlsx = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
      if (!isCsv && !isXlsx) {
        this.$message.error('上传文件只能是 .csv 或 .xlsx 格式!');
      }
      return isCsv || isXlsx; // 注意：element upload 手动上传模式下 before-upload 可能会被跳过，主要靠 on-change
    },
    confirmImport() {
      if (!this.selectedFile) return;
      this.isUploading = true;
      importRepayments(this.selectedFile)
        .then(response => {
          this.$message.success('文件上传成功');
          this.getList();
        })
        .catch(error => {
          this.$message.error(`文件上传失败: ${error.response?.data?.message || error.message}`);
        })
        .finally(() => {
          this.isUploading = false;
          this.resetUpload();
        });
    },
    resetUpload() {
      this.selectedFile = null;
      this.$refs.upload.clearFiles();
    }
  }
};
</script>

<style scoped lang="scss">
@import "@/assets/styles/global.scss";
.app-container {
  padding: 20px;
  background-color: #f0f2f5; /* 浅灰背景，突显卡片 */
  min-height: 100vh;
}

.mb-4 {
  margin-bottom: 20px;
}

/* 卡片通用样式 */
.chart-card, .search-card, .table-card {
  border-radius: 8px;
  border: none;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

/* 图表高度 */
.chart-box {
  width: 100%;
  height: 350px;
}

/* 表格头部按钮区 */
.table-header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .right-actions {
    display: flex;
    align-items: center;
  }
}

.inline-block {
  display: inline-block;
}

.ml-10 {
  margin-left: 10px;
}

.text-danger {
  color: #F56C6C;

  &:hover {
    color: #f78989;
  }
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>
