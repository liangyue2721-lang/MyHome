<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb-4">
      <el-col :span="24">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="clearfix">
            <span class="card-title"><i class="el-icon-data-line"></i> 薪资趋势分析 (近12个月)</span>
          </div>
          <div ref="salaryChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="search-card mb-4">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="70px">
        <el-form-item label="选择用户" prop="userId">
          <el-select v-model="queryParams.userId" placeholder="请输入姓名搜索" filterable clearable
                     style="width: 200px">
            <el-option v-for="user in users" :key="user.userId" :label="user.nickName" :value="user.userId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="工资月份" prop="createdAt">
          <el-date-picker
            clearable
            v-model="queryParams.createdAt"
            type="month"
            value-format="yyyy-MM"
            placeholder="选择月份"
            style="width: 200px"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="hover" class="table-card">
      <div slot="header" class="clearfix table-header-actions">
        <span class="card-title">薪资明细列表</span>
        <div class="right-actions">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['finance:salary_record:add']"
          >新增记录
          </el-button>
          <el-button
            type="warning"
            plain
            icon="el-icon-download"
            size="mini"
            @click="handleExport"
            v-hasPermi="['finance:salary_record:export']"
          >导出Excel
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="salary_recordList"
        @selection-change="handleSelectionChange"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{background:'#f8f8f9', color:'#515a6e', textAlign: 'center'}"
        :cell-style="{textAlign: 'center'}"
      >
        <el-table-column type="selection" width="55" align="center"/>

        <el-table-column label="用户姓名" align="center" fixed width="100">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ getUserName(row.userId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="工资月份" prop="createdAt" align="center" min-width="100">
          <template #default="{ row }">
            <i class="el-icon-date"></i> {{ parseTime(row.createdAt, '{y}-{m}') }}
          </template>
        </el-table-column>
        <el-table-column label="发放日期" prop="issueDate" align="center" min-width="110">
          <template #default="{ row }">
            {{ parseTime(row.issueDate, '{y}-{m}-{d}') }}
          </template>
        </el-table-column>

        <el-table-column label="收入明细" align="center">
          <el-table-column label="基本工资" align="center" prop="baseSalary" min-width="100"
                           :formatter="formatTwoDecimal"/>
          <el-table-column label="岗位工资" align="center" prop="positionSalary" min-width="100"
                           :formatter="formatTwoDecimal"/>
          <el-table-column label="绩效工资" align="center" prop="performanceSalary" min-width="100"
                           :formatter="formatTwoDecimal"/>
          <el-table-column label="各类津贴" align="center" min-width="100">
            <template #default="{ row }">
              <el-popover trigger="hover" placement="top">
                <p>津贴: {{ row.allowance }}</p>
                <p>电脑补助: {{ row.computerSubsidy }}</p>
                <div slot="reference" class="name-wrapper">
                  <span class="link-type">{{
                      (Number(row.allowance || 0) + Number(row.computerSubsidy || 0)).toFixed(2)
                    }}</span>
                </div>
              </el-popover>
            </template>
          </el-table-column>
          <el-table-column label="应付薪酬" align="center" prop="payableSalary" min-width="120">
            <template #default="{ row }">
              <span class="text-primary-bold">¥{{ formatTwoDecimal(null, null, row.payableSalary) }}</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="扣款明细" align="center">
          <el-table-column label="社保/公积金" align="center" min-width="120">
            <template #default="{ row }">
              <div>社:{{ row.socialSecurity }}</div>
              <div>公:{{ row.housingFund }}</div>
            </template>
          </el-table-column>
          <el-table-column label="个税" align="center" prop="incomeTax" min-width="90" :formatter="formatTwoDecimal"/>
          <el-table-column label="缺勤扣款" align="center" prop="absenceDeduction" min-width="90"
                           :formatter="formatTwoDecimal"/>
          <el-table-column label="扣款合计" align="center" prop="totalDeduction" min-width="100">
            <template #default="{ row }">
              <span class="text-danger">¥{{ formatTwoDecimal(null, null, row.totalDeduction) }}</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip min-width="150"/>

        <el-table-column label="实发金额" align="center" prop="netSalary" min-width="120">
          <template #default="{ row }">
            <span class="text-success-bold">¥{{ formatTwoDecimal(null, null, row.netSalary) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" fixed="right" width="160" class-name="small-padding fixed-width">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['finance:salary_record:edit']"
            >修改
            </el-button>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-document-copy"
              @click="handleCopy(scope.row)"
              v-hasPermi="['finance:salary_record:copy']"
            >复制
            </el-button>
            <el-button
              size="mini"
              type="text"
              class="text-danger"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['finance:salary_record:remove']"
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

    <el-dialog :title="title" :visible.sync="open" width="800px" append-to-body top="5vh" :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" class="custom-form">
        <h4 class="form-header">基础信息</h4>
        <el-row>
<!--          <el-col :span="12">-->
<!--            <el-form-item label="选择用户" prop="userId">-->
<!--              <el-select v-model="form.userId" placeholder="请选择用户" filterable style="width: 100%">-->
<!--                <el-option v-for="user in users" :key="user.userId" :label="user.nickName" :value="user.userId"/>-->
<!--              </el-select>-->
<!--            </el-form-item>-->
<!--          </el-col>-->
          <el-col :span="12">
            <el-form-item label="工资代发单位" prop="salaryPayOrg">
              <el-input v-model="form.salaryPayOrg" placeholder="请输入单位名称"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="工资月份" prop="createdAt">
              <el-date-picker style="width: 100%" clearable v-model="form.createdAt" type="date"
                              value-format="yyyy-MM-dd" placeholder="归属月份"></el-date-picker>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="发放日期" prop="issueDate">
              <el-date-picker style="width: 100%" clearable v-model="form.issueDate" type="date"
                              value-format="yyyy-MM-dd" placeholder="实际发放日"></el-date-picker>
            </el-form-item>
          </el-col>
        </el-row>

        <h4 class="form-header">收入项目 (单位:元)</h4>
        <el-row>
          <el-col :span="8">
            <el-form-item label="基本工资" prop="baseSalary">
              <el-input v-model="form.baseSalary" placeholder="0.00"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="岗位工资" prop="positionSalary">
              <el-input v-model="form.positionSalary" placeholder="0.00"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="绩效工资" prop="performanceSalary">
              <el-input v-model="form.performanceSalary" placeholder="0.00"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="一般津贴" prop="allowance">
              <el-input v-model="form.allowance" placeholder="0.00"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="电脑补助" prop="computerSubsidy">
              <el-input v-model="form.computerSubsidy" placeholder="0.00"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="通讯/交通补" prop="transportSubsidy">
              <el-tooltip content="此处可填写通讯或交通补贴总和" placement="top">
                <el-input v-model="form.transportSubsidy" placeholder="0.00"/>
              </el-tooltip>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="调整工资" prop="adjustSalary">
              <el-input v-model="form.adjustSalary" placeholder="补发/扣减"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="应付薪酬" prop="payableSalary">
              <el-input v-model="form.payableSalary" placeholder="自动计算项"/>
            </el-form-item>
          </el-col>
        </el-row>

        <h4 class="form-header">扣款项目 (单位:元)</h4>
        <el-row>
          <el-col :span="8">
            <el-form-item label="社保个人" prop="socialSecurity">
              <el-input v-model="form.socialSecurity" placeholder="0.00"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="公积金个人" prop="housingFund">
              <el-input v-model="form.housingFund" placeholder="0.00"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="个税" prop="incomeTax">
              <el-input v-model="form.incomeTax" placeholder="0.00"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="缺勤扣款" prop="absenceDeduction">
              <el-input v-model="form.absenceDeduction" placeholder="0.00"/>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="扣款合计" prop="totalDeduction">
              <el-input v-model="form.totalDeduction" placeholder="0.00"/>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注" prop="remark" style="margin-top: 10px;">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注信息"/>
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
  listSalary_record,
  copySalary_record,
  getSalary_record,
  delSalary_record,
  addSalary_record,
  updateSalary_record
} from '@/api/finance/salary_record'
import dayjs from 'dayjs'
import {listUser} from "@/api/stock/dropdown_component";

export default {
  name: 'Salary_record',
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      salary_recordList: [],
      chartData: [],
      chartInstance: null,
      users: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        createdAt: null,
        issueDate: null
      },
      form: {},
      rules: {
        userId: [{required: true, message: "用户不能为空", trigger: "change"}],
        issueDate: [{required: true, message: "发放日期不能为空", trigger: "blur"}],
        createdAt: [{required: true, message: "工资日期不能为空", trigger: "blur"}]
      }
    }
  },
  async created() {
    await this.getUserList();
    await this.getList();
  },
  mounted() {
    window.addEventListener('resize', this.handleResize);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
  },
  methods: {
    handleResize() {
      if (this.chartInstance) {
        this.chartInstance.resize();
      }
    },
    /** 获取列表并更新图表 */
    getList() {
      this.loading = true
      listSalary_record(this.queryParams).then(response => {
        this.salary_recordList = response.rows
        this.total = response.total
        this.loading = false
        this.computeChartData()
      })
    },
    formatTwoDecimal(row, column, cellValue) {
      if (cellValue === null || cellValue === undefined) return '0.00';
      return Number(cellValue).toFixed(2);
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
    /** 在前端根据 salary_recordList 聚合最近12个月数据 */
    computeChartData() {
      const now = dayjs()
      const months = []
      for (let i = 11; i >= 0; i--) {
        months.push(now.subtract(i, 'month').format('YYYY-MM'))
      }
      const map = {}
      months.forEach(m => {
        map[m] = {payable: 0, net: 0}
      })
      this.salary_recordList.forEach(item => {
        const dateStr = item.createdAt || item.issueDate;
        const m = dayjs(dateStr).format('YYYY-MM')
        if (map[m]) {
          map[m].payable += Number(item.payableSalary) || 0
          map[m].net += Number(item.netSalary) || 0
        }
      })
      this.chartData = months.map(m => ({month: m, ...map[m]}))
      this.renderLineChart(
        this.$refs.salaryChart,
        '',
        this.chartData,
        [
          {name: '应付薪酬', key: 'payable'},
          {name: '实发金额', key: 'net'}
        ]
      )
    },
    renderLineChart(container, title, data, series) {
      this.$nextTick(() => {
        if (!this.chartInstance) {
          this.chartInstance = echarts.init(container)
        }
        const option = {
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255, 255, 255, 0.9)',
            borderColor: '#ccc',
            borderWidth: 1,
            textStyle: {color: '#333'}
          },
          grid: {top: 40, right: 30, bottom: 20, left: 50, containLabel: true},
          legend: {
            data: series.map(s => s.name),
            bottom: 0
          },
          xAxis: {
            type: 'category',
            data: data.map(i => i.month),
            axisLine: {lineStyle: {color: '#ccc'}},
            axisLabel: {color: '#666'}
          },
          yAxis: {
            type: 'value',
            name: '金额（元）',
            splitLine: {lineStyle: {type: 'dashed', color: '#eee'}}
          },
          series: series.map((s, index) => ({
            name: s.name,
            type: 'line',
            data: data.map(i => i[s.key]),
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            itemStyle: {
              color: index === 0 ? '#409EFF' : '#67C23A'
            },
            areaStyle: {
              opacity: 0.1,
              color: index === 0 ? '#409EFF' : '#67C23A'
            }
          }))
        }
        this.chartInstance.setOption(option)
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields()
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.resetFormData()
      this.open = true
      this.title = '新增工资记录'
    },
    handleUpdate(row) {
      this.resetFormData()
      const id = row.id || this.ids
      getSalary_record(id).then(res => {
        this.form = res.data
        this.open = true
        this.title = '修改工资记录'
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const fn = this.form.id ? updateSalary_record : addSalary_record
        fn(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm(`是否确认删除编号为"${ids}"的记录？`)
        .then(() => delSalary_record(ids))
        .then(() => {
          this.$modal.msgSuccess('删除成功')
          this.getList()
        })
    },
    handleExport() {
      this.download(
        'finance/salary_record/export',
        {...this.queryParams},
        `salary_record_${Date.now()}.xlsx`
      )
    },
    handleCopy(row) {
      const id = row.id || this.ids
      if (!id || (Array.isArray(this.ids) && this.ids.length !== 1 && !row.id)) {
        this.$message.warning('请选择一条要复制的记录');
        return;
      }
      const targetId = Array.isArray(id) ? id[0] : id;

      this.$modal.confirm(`是否确认复制当前记录？`)
        .then(() => copySalary_record(targetId))
        .then(res => {
          this.$modal.msgSuccess('复制成功');
          this.getList();
        })
        .catch((error) => {
          if (error !== 'cancel') {
            console.error('复制失败:', error);
          }
        });
    },
    resetFormData() {
      this.form = {
        id: null,
        userId: null,
        baseSalary: null,
        positionSalary: null,
        performanceSalary: null,
        allowance: null,
        computerSubsidy: null,
        transportSubsidy: null,
        adjustSalary: null,
        payableSalary: null,
        socialSecurity: null,
        housingFund: null,
        incomeTax: null,
        absenceDeduction: null,
        totalDeduction: null,
        salaryPayOrg: null,
        remark: null,
        issueDate: null,
        createdAt: null
      }
      this.$refs.form && this.$refs.form.resetFields()
    },
    cancel() {
      this.open = false
      this.resetFormData()
    }
  }
}
</script>

<style scoped lang="scss">
.app-container {
  padding: 20px;
  background-color: #f0f2f5;
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

.chart-card {
  .chart-box {
    width: 100%;
    height: 320px;
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

/* 表格头部按钮区 */
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

/* 文本样式 */
.text-primary-bold {
  color: #409EFF;
  font-weight: 600;
}

.text-success-bold {
  color: #67C23A;
  font-weight: 600;
  font-size: 1.1em;
}

.text-danger {
  color: #F56C6C;
}

/* 弹窗表单样式 */
.form-header {
  border-left: 4px solid #409EFF;
  padding-left: 10px;
  margin-bottom: 20px;
  margin-top: 0;
  font-size: 15px;
  color: #303133;
  background-color: #f5f7fa;
  padding: 8px 10px;
  border-radius: 0 4px 4px 0;
}

.link-type {
  color: #409EFF;
  cursor: pointer;
  text-decoration: underline;
}
</style>
