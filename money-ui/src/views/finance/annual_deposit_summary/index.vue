<template>
  <div class="app-container">
    <!-- 顶部图表区 -->
    <el-row :gutter="20" class="mb-4">
      <el-col :span="24">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="clearfix">
            <span class="card-title"><i class="el-icon-s-data"></i> 资金积累趋势 (Annual Deposit Trend)</span>
          </div>
          <div id="lineChart" ref="lineChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索筛选区 -->
    <el-card shadow="never" class="search-card mb-4">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="70px">
        <el-form-item label="统计年份" prop="year">
          <el-input
            v-model="queryParams.year"
            placeholder="例如 2025"
            clearable
            style="width: 200px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格区 -->
    <el-card shadow="hover" class="table-card">
      <div slot="header" class="clearfix table-header-actions">
        <span class="card-title">年度存款明细列表</span>
        <div class="right-actions">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
          >新增记录
          </el-button>
          <el-button
            type="success"
            plain
            icon="el-icon-edit"
            size="mini"
            :disabled="single"
            @click="handleUpdate"
          >修改
          </el-button>
          <el-button
            type="danger"
            plain
            icon="el-icon-delete"
            size="mini"
            :disabled="multiple"
            @click="handleDelete"
          >删除
          </el-button>
          <el-button
            type="warning"
            plain
            icon="el-icon-download"
            size="mini"
            @click="handleExport"
          >导出Excel
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="annualList"
        @selection-change="handleSelectionChange"
        border
        stripe
        :header-cell-style="{background:'#f8f8f9',color:'#515a6e'}"
      >
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="统计年份" align="center" prop="year" sortable width="120">
          <template slot-scope="scope">
            <el-tag effect="dark" size="small" type="primary">{{ scope.row.year }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="年度存款总额" align="center" prop="totalDeposit">
          <template slot-scope="scope">
            <span class="money-text">¥{{ formatMoney(scope.row.totalDeposit) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注信息" align="center" prop="remark" show-overflow-tooltip/>

        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="150">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
            >修改
            </el-button>
            <el-button
              size="mini"
              type="text"
              class="text-delete"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
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

    <!-- 添加/编辑对话框 -->
    <el-dialog :title="title" :visible.sync="dialogVisible" width="500px" append-to-body :close-on-click-modal="false">
      <el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">
        <el-form-item label="统计年份" prop="year">
          <el-input v-model="formData.year" placeholder="请输入年份 (如 2025)"/>
        </el-form-item>
        <el-form-item label="存款总额" prop="totalDeposit">
          <el-input v-model="formData.totalDeposit" placeholder="0.00">
            <template slot="append">元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="备注信息" prop="remark">
          <el-input v-model="formData.remark" type="textarea" :rows="3" placeholder="请输入备注内容"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts'
import {
  listAnnual_deposit_summary,
  getAnnual_deposit_summary,
  addAnnual_deposit_summary,
  updateAnnual_deposit_summary,
  delAnnual_deposit_summary
} from '@/api/finance/annual_deposit_summary'
import {listUser} from "@/api/stock/dropdown_component";

export default {
  name: 'AnnualDepositSummary',
  data() {
    return {
      loading: false,
      annualList: [],
      total: 0,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      dialogVisible: false,
      title: '',
      formData: {
        id: null,
        year: null,
        totalDeposit: null,
        remark: ''
      },
      rules: {
        year: [{required: true, message: '统计年份不能为空', trigger: 'blur'}],
        totalDeposit: [{required: true, message: '年度存款总额不能为空', trigger: 'blur'}]
      },
      queryParams: {
        pageNum: 1,
        pageSize: 100,
        year: null,
        userId: null,
      },
      chart: null
    }
  },
  async created() {
    await this.initUserList();
    this.getList();
  },
  mounted() {
    window.addEventListener('resize', this.resizeChart)
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeChart)
    if (this.chart) {
      this.chart.dispose();
    }
  },
  methods: {
    formatMoney(val) {
      if (val === null || val === undefined) return '0.00';
      return Number(val).toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
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
            this.queryParams.userId = matchedUser.id;
          }
        }
      } catch (err) {
        console.error('用户列表加载失败:', err);
      }
    },
    // 列表与图表
    getList() {
      this.loading = true
      listAnnual_deposit_summary(this.queryParams).then(res => {
        this.annualList = res.rows || []
        this.total = res.total || 0
        this.loading = false
        this.$nextTick(() => {
          this.renderChart()
        })
      }).catch(() => {
        this.loading = false
      })
    },
    renderChart() {
      if (!this.$refs.lineChart) return; // 防止DOM未加载
      if (!this.annualList.length) {
        this.chart && this.chart.clear()
        return
      }
      const sorted = [...this.annualList].sort((a, b) => a.year - b.year)
      const xData = sorted.map(i => i.year)
      const yData = sorted.map(i => i.totalDeposit)

      if (this.chart) this.chart.dispose()
      this.chart = echarts.init(this.$refs.lineChart)

      this.chart.setOption({
        tooltip: {
          trigger: 'axis',
          backgroundColor: 'rgba(255, 255, 255, 0.95)',
          borderColor: '#dedede',
          textStyle: {color: '#333'}
        },
        grid: {left: '3%', right: '4%', bottom: '3%', containLabel: true},
        xAxis: {
          type: 'category',
          boundaryGap: false,
          data: xData,
          axisLine: {lineStyle: {color: '#909399'}}
        },
        yAxis: {
          type: 'value',
          name: '金额 (元)',
          splitLine: {lineStyle: {type: 'dashed', color: '#f0f0f0'}}
        },
        series: [{
          name: '年度存款',
          type: 'line',
          data: yData,
          smooth: true,
          symbol: 'circle',
          symbolSize: 8,
          itemStyle: {
            color: '#409EFF',
            borderColor: '#fff',
            borderWidth: 2
          },
          lineStyle: {
            width: 3,
            shadowColor: 'rgba(64,158,255,0.3)',
            shadowBlur: 10
          },
          areaStyle: {
            opacity: 0.2,
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              {offset: 0, color: 'rgba(64,158,255,0.8)'},
              {offset: 1, color: 'rgba(64,158,255,0.1)'}
            ])
          }
        }]
      })
    },
    resizeChart() {
      this.chart && this.chart.resize()
    },

    // 搜索
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList()
    },
    resetQuery() {
      this.$refs.queryForm.resetFields();
      this.handleQuery()
    },

    // 选择
    handleSelectionChange(selection) {
      this.ids = selection.map(i => i.id)
      this.single = selection.length !== 1
      this.multiple = selection.length === 0
    },

    // 新增/编辑
    handleAdd() {
      this.resetFormData()
      this.title = '添加年度存款记录'
      this.dialogVisible = true
    },
    handleUpdate(row) {
      const id = row?.id || this.ids[0]
      if (!id) return;
      getAnnual_deposit_summary(id).then(res => {
        this.formData = {...res.data}
        this.title = '修改年度存款记录'
        this.dialogVisible = true
      })
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        const apiFn = this.formData.id ? updateAnnual_deposit_summary : addAnnual_deposit_summary
        apiFn(this.formData).then(() => {
          this.$message.success(this.formData.id ? '修改成功' : '新增成功')
          this.dialogVisible = false
          this.getList()
        })
      })
    },
    resetFormData() {
      this.formData = {id: null, year: null, totalDeposit: null, remark: ''}
      this.$nextTick(() => this.$refs.formRef && this.$refs.formRef.clearValidate())
    },

    // 删除
    handleDelete(row) {
      const ids = row?.id || this.ids.join(',')
      if (!ids) return;
      this.$confirm(`确认删除编号 ${ids} 的记录?`, "警告", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        delAnnual_deposit_summary(ids).then(() => {
          this.$message.success('删除成功')
          this.getList()
        })
      }).catch(() => {
      });
    },

    // 导出
    handleExport() {
      this.download('finance/annual_deposit_summary/export', {...this.queryParams}, `annual_deposit_summary_${Date.now()}.xlsx`)
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
  .chart-box {
    width: 100%;
    height: 350px;
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

.money-text {
  font-weight: bold;
  color: #303133;
  font-size: 14px;
}

.text-delete {
  color: #F56C6C;

  &:hover {
    color: #f78989;
  }
}
</style>
