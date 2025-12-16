<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb-4">
      <el-col :span="24">
        <el-card shadow="hover" class="timeline-card">
          <div slot="header" class="clearfix">
            <span class="card-title"><i class="el-icon-collection-tag"></i> 投资里程碑 (Investment Journey)</span>
          </div>
          <div class="timeline-wrapper">
            <div class="custom-timeline">
              <div v-for="(item, index) in timelineList" :key="item.id" class="timeline-node"
                   :class="getStatusClass(item.isCompleted)">
                <div class="node-header">
                  <span class="year-badge">{{ item.year }}</span>
                  <div class="connector" v-if="index < timelineList.length - 1"></div>
                </div>
                <div class="node-card" @click="handleUpdate(item)">
                  <div class="card-status">
                    <i :class="getStatusIcon(item.isCompleted)"></i>
                    {{ getStatusText(item.isCompleted) }}
                  </div>
                  <div class="card-body">
                    <div class="data-row">
                      <span class="label">本金</span>
                      <span class="value">¥{{ formatNumber(item.startPrincipal) }}</span>
                    </div>
                    <div class="data-row">
                      <span class="label">总值</span>
                      <span class="value highlight">¥{{ formatNumber(item.actualEndValue) }}</span>
                    </div>
                    <div class="data-row">
                      <span class="label">收益率</span>
                      <span class="value" :class="Number(item.actualGrowthRate) >= 0 ? 'text-up' : 'text-down'">
                               {{ item.actualGrowthRate }}%
                            </span>
                    </div>
                  </div>
                </div>
              </div>
              <div v-if="timelineList.length === 0" class="empty-timeline">
                暂无年度数据，请点击下方新增
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="search-card mb-4">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="70px">
        <el-form-item label="年份" prop="year">
          <el-input
            v-model="queryParams.year"
            placeholder="例如 2025"
            clearable
            style="width: 200px"
            @keyup.enter.native="handleQuery"
          />
        </el-form-item>
        <el-form-item label="进度状态" prop="isCompleted">
          <el-select v-model="queryParams.isCompleted" placeholder="请选择状态" clearable style="width: 200px">
            <el-option
              v-for="dict in dict.type.sys_progress"
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
        <span class="card-title">详细数据列表</span>
        <div class="right-actions">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['finance:yearly_investment_summary:add']"
          >新增年度计划
          </el-button>
          <el-button
            type="warning"
            plain
            icon="el-icon-download"
            size="mini"
            @click="handleExport"
            v-hasPermi="['finance:yearly_investment_summary:export']"
          >导出Excel
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="yearly_investment_summaryList"
        @selection-change="handleSelectionChange"
        border
        stripe
        style="width: 100%"
        :header-cell-style="{background:'#f8f8f9', color:'#515a6e', textAlign: 'center'}"
        :cell-style="{textAlign: 'center'}"
      >
        <el-table-column type="selection" width="50" align="center"/>

        <el-table-column label="年份" align="center" prop="year" min-width="80" sortable>
          <template slot-scope="scope">
            <span style="font-weight: bold; color: #409EFF;">{{ scope.row.year }}</span>
          </template>
        </el-table-column>

        <el-table-column label="本金概况" align="center">
          <el-table-column label="年初本金" align="center" prop="startPrincipal" min-width="120">
            <template slot-scope="scope">¥{{ formatMoney(scope.row.startPrincipal) }}</template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="预期目标" align="center">
          <el-table-column label="期望增值" align="center" min-width="90">
            <template slot-scope="scope">{{ scope.row.expectedGrowthRate }}%</template>
          </el-table-column>
          <el-table-column label="预期总值" align="center" min-width="120">
            <template slot-scope="scope">¥{{ formatMoney(scope.row.expectedEndValue) }}</template>
          </el-table-column>
          <el-table-column label="预期收益" align="center" min-width="110">
            <template slot-scope="scope">
              <span style="color: #909399;">¥{{
                  formatMoney(scope.row.expectedEndValue - scope.row.startPrincipal)
                }}</span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="实际达成" align="center">
          <el-table-column label="实际增值" align="center" min-width="90">
            <template slot-scope="scope">
                     <span :class="Number(scope.row.actualGrowthRate) >= 0 ? 'text-danger' : 'text-success'">
                         {{ scope.row.actualGrowthRate }}%
                     </span>
            </template>
          </el-table-column>
          <el-table-column label="实际总值" align="center" min-width="120">
            <template slot-scope="scope">
              <span style="font-weight: bold;">¥{{ formatMoney(scope.row.actualEndValue) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="实际收益" align="center" min-width="110">
            <template slot-scope="scope">
                    <span
                      :class="(scope.row.actualEndValue - scope.row.startPrincipal) >= 0 ? 'text-danger' : 'text-success'">
                        ¥{{ formatMoney(scope.row.actualEndValue - scope.row.startPrincipal) }}
                    </span>
            </template>
          </el-table-column>
        </el-table-column>

        <el-table-column label="状态" align="center" prop="isCompleted" min-width="90">
          <template slot-scope="scope">
            <el-tag :type="getIsCompletedTagType(scope.row.isCompleted)" effect="dark" size="small">
              <dict-tag :options="dict.type.sys_progress" :value="scope.row.isCompleted"/>
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="备注" align="center" prop="remark" min-width="150" show-overflow-tooltip/>

        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" fixed="right" width="120">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['finance:yearly_investment_summary:edit']"
            >修改
            </el-button>
            <el-button
              size="mini"
              type="text"
              class="text-delete"
              icon="el-icon-delete"
              @click="handleDelete(scope.row)"
              v-hasPermi="['finance:yearly_investment_summary:remove']"
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

    <el-dialog :title="title" :visible.sync="open" width="680px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="120px" class="custom-form">
        <el-row>
          <el-col :span="12">
            <el-form-item label="年份" prop="year">
              <el-input v-model="form.year" placeholder="例如 2025"/>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="进度状态" prop="isCompleted">
              <el-select v-model="form.isCompleted" placeholder="请选择" style="width: 100%">
                <el-option
                  v-for="dict in dict.type.sys_progress"
                  :key="dict.value"
                  :label="dict.label"
                  :value="dict.value"
                />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <h4 class="form-header">本金与预期</h4>
        <el-row>
          <el-col :span="12">
            <el-form-item label="年初本金" prop="startPrincipal">
              <el-input v-model="form.startPrincipal" placeholder="0.00">
                <template slot="append">元</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="期望增值率" prop="expectedGrowthRate">
              <el-input v-model="form.expectedGrowthRate" placeholder="0.00">
                <template slot="append">%</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年末预期总值" prop="expectedEndValue">
              <el-input v-model="form.expectedEndValue" placeholder="0.00">
                <template slot="append">元</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <h4 class="form-header">实际达成情况</h4>
        <el-row>
          <el-col :span="12">
            <el-form-item label="实际增值率" prop="actualGrowthRate">
              <el-input v-model="form.actualGrowthRate" placeholder="0.00">
                <template slot="append">%</template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="年末实际总值" prop="actualEndValue">
              <el-input v-model="form.actualEndValue" placeholder="0.00">
                <template slot="append">元</template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="备注信息" prop="remark">
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
import {
  listYearly_investment_summary,
  getYearly_investment_summary,
  delYearly_investment_summary,
  addYearly_investment_summary,
  updateYearly_investment_summary
} from "@/api/finance/yearly_investment_summary"
import {listUser} from "@/api/stock/dropdown_component";

export default {
  name: "YearlyInvestmentSummary",
  dicts: ['sys_progress'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      yearly_investment_summaryList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        year: null,
        isCompleted: null,
        userId: null,
      },
      form: {
        userId: null
      },
      rules: {
        year: [
          {required: true, message: "年份不能为空", trigger: "blur"}
        ],
        startPrincipal: [
          {required: true, message: "年初本金不能为空", trigger: "blur"}
        ],
        expectedEndValue: [
          {required: true, message: "年末预期总值不能为空", trigger: "blur"}
        ],
      }
    }
  },
  computed: {
    // 自动排序时间轴数据
    timelineList() {
      if (!this.yearly_investment_summaryList) return [];
      return [...this.yearly_investment_summaryList].sort((a, b) => a.year - b.year);
    }
  },
  async created() {
    await this.initUserList();
    this.getList();
  },
  methods: {
    // 格式化数字，保留2位小数，不带货币符号
    formatNumber(num) {
      if (num === null || num === undefined) return '--';
      return Number(num).toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    // 格式化金额，用于表格
    formatMoney(val) {
      if (val === null || val === undefined) return '0.00';
      return Number(val).toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    // 里程碑状态样式
    getStatusClass(status) {
      if (status === 'Y') return 'status-completed'; // 已完成 - 绿色
      if (status === 'A') return 'status-active';    // 进行中 - 蓝色
      return 'status-pending';                       // 未完成/其他 - 灰色
    },
    // 里程碑图标
    getStatusIcon(status) {
      if (status === 'Y') return 'el-icon-circle-check';
      if (status === 'A') return 'el-icon-loading';
      return 'el-icon-time';
    },
    // 里程碑文本转换（假设字典不可用时的兜底）
    getStatusText(status) {
      if (status === 'Y') return '已达成';
      if (status === 'A') return '进行中';
      return '计划中';
    },
    // 表格Tag颜色
    getIsCompletedTagType(status) {
      if (status === 'Y') return 'success';
      if (status === 'A') return ''; // primary
      return 'info';
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
      listYearly_investment_summary(this.queryParams).then(response => {
        this.yearly_investment_summaryList = response.rows
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
        year: null,
        startPrincipal: null,
        expectedGrowthRate: null,
        expectedEndValue: null,
        actualGrowthRate: null,
        actualEndValue: null,
        isCompleted: null,
        remark: null,
        userId: this.form.userId // 保留当前用户
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
      this.title = "添加年度投资汇总"
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getYearly_investment_summary(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改年度投资汇总"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateYearly_investment_summary(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addYearly_investment_summary(this.form).then(response => {
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
        return delYearly_investment_summary(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    handleExport() {
      this.download('finance/yearly_investment_summary/export', {
        ...this.queryParams
      }, `yearly_investment_summary_${new Date().getTime()}.xlsx`)
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

/* 卡片通用样式 */
.timeline-card, .search-card, .table-card {
  border-radius: 8px;
  border: none;
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

/* 弹窗表单样式 */
.form-header {
  border-left: 4px solid #409EFF;
  padding-left: 10px;
  margin-bottom: 20px;
  margin-top: 10px;
  font-size: 15px;
  color: #303133;
  background-color: #f5f7fa;
  padding: 8px 10px;
  border-radius: 0 4px 4px 0;
}

.text-danger {
  color: #F56C6C;
}

.text-success {
  color: #67C23A;
}

.text-delete {
  color: #F56C6C;

  &:hover {
    color: #f78989;
  }
}

/* === 自定义时间轴样式 === */
.timeline-wrapper {
  overflow-x: auto;
  padding: 10px 0 20px 0; /* 底部留白给阴影 */
  &::-webkit-scrollbar {
    height: 8px;
  }

  &::-webkit-scrollbar-thumb {
    background: #dcdfe6;
    border-radius: 4px;
  }
}

.custom-timeline {
  display: flex;
  align-items: flex-start;
  padding: 0 10px;
}

.timeline-node {
  display: flex;
  flex-direction: column;
  align-items: center;
  position: relative;
  margin-right: 20px;
  flex-shrink: 0;
  width: 200px; /* 卡片宽度 */

  &:last-child {
    margin-right: 0;

    .connector {
      display: none;
    }
  }

  /* 状态颜色定义 */
  &.status-completed {
    --theme-color: #67C23A;
    --bg-color: #e1f3d8;
  }

  &.status-active {
    --theme-color: #409EFF;
    --bg-color: #ecf5ff;
  }

  &.status-pending {
    --theme-color: #909399;
    --bg-color: #f4f4f5;
  }
}

.node-header {
  display: flex;
  align-items: center;
  width: 100%;
  margin-bottom: 12px;
  position: relative;
  justify-content: center;
}

.year-badge {
  background-color: var(--theme-color);
  color: #fff;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 14px;
  font-weight: bold;
  z-index: 2;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.connector {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%; /* 连接到下一个节点 */
  height: 2px;
  background-color: #e4e7ed;
  z-index: 1;
  width: calc(100% + 20px); /* 补偿 margin-right */
}

.node-card {
  width: 100%;
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
  cursor: pointer;
  overflow: hidden;

  &:hover {
    transform: translateY(-5px);
    box-shadow: 0 8px 16px 0 rgba(0, 0, 0, 0.1);
    border-color: var(--theme-color);
  }
}

.card-status {
  background-color: var(--bg-color);
  color: var(--theme-color);
  padding: 8px;
  font-size: 13px;
  font-weight: bold;
  text-align: center;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);

  i {
    margin-right: 4px;
  }
}

.card-body {
  padding: 12px;
}

.data-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  font-size: 13px;

  &:last-child {
    margin-bottom: 0;
  }

  .label {
    color: #909399;
  }

  .value {
    font-weight: 500;
    color: #303133;
  }

  .highlight {
    font-weight: bold;
    font-size: 14px;
    color: #303133;
  }

  .text-up {
    color: #F56C6C;
  }

  .text-down {
    color: #67C23A;
  }
}

.empty-timeline {
  padding: 20px;
  color: #909399;
  text-align: center;
  width: 100%;
}
</style>
