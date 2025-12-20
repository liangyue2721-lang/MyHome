<template>
  <div class="app-container home">
    <!-- 顶部状态卡片 (新增) -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>待执行任务数 (Pending)</span>
          </div>
          <div class="card-body">
            <span class="count-text pending">{{ taskStats.pending }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>已执行任务数 (Completed)</span>
          </div>
          <div class="card-body">
            <span class="count-text completed">{{ taskStats.completed }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>执行中任务 (Executing)</span>
          </div>
          <div class="card-body">
            <span class="count-text executing">{{ taskStats.executing }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>执行中占比 (In Progress)</span>
          </div>
          <div class="card-body chart-container">
            <el-progress
              type="circle"
              :percentage="executingPercentage"
              :width="80"
              :color="customColors"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 原有仪表盘内容 (保留) -->
    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :sm="24" :lg="24">
        <!-- 欢迎卡片 -->
        <el-card class="box-card">
          <div slot="header" class="clearfix">
            <span>欢迎回来</span>
          </div>
          <div class="text item">
            {{ currentTime }}
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 示例图表 (Profit Trend) -->
    <el-row :gutter="20" style="margin-top: 20px;">
        <el-col :span="12">
            <el-card>
                <div slot="header">
                    <span>收益趋势</span>
                </div>
                <!-- Mock Chart Area -->
                <div style="height: 300px; display: flex; align-items: center; justify-content: center; background: #f0f2f5;">
                    <span style="color: #909399;">Profit Trend Chart Placeholder</span>
                </div>
            </el-card>
        </el-col>
        <el-col :span="12">
            <el-card>
                <div slot="header">
                    <span>还款计划</span>
                </div>
                <!-- Mock Chart Area -->
                <div style="height: 300px; display: flex; align-items: center; justify-content: center; background: #f0f2f5;">
                    <span style="color: #909399;">Loan Repayment Chart Placeholder</span>
                </div>
            </el-card>
        </el-col>
    </el-row>

  </div>
</template>

<script>
import request from '@/utils/request'
import { parseTime } from "@/utils/ruoyi";

export default {
  name: "Index",
  data() {
    return {
      taskStats: {
        pending: 0,
        completed: 0,
        executing: 0
      },
      currentTime: parseTime(new Date()),
      timer: null,
      customColors: [
        { color: '#f56c6c', percentage: 20 },
        { color: '#e6a23c', percentage: 40 },
        { color: '#5cb87a', percentage: 60 },
        { color: '#1989fa', percentage: 80 },
        { color: '#6f7ad3', percentage: 100 }
      ]
    };
  },
  computed: {
    executingPercentage() {
      const total = this.taskStats.pending + this.taskStats.completed + this.taskStats.executing;
      if (total === 0) return 0;
      return Math.round((this.taskStats.executing / total) * 100);
    }
  },
  created() {
    this.fetchTaskStats();
    this.timer = setInterval(() => {
        this.currentTime = parseTime(new Date());
    }, 1000);
  },
  beforeDestroy() {
    if (this.timer) {
        clearInterval(this.timer);
    }
  },
  methods: {
    fetchTaskStats() {
      request({
        url: '/monitor/job/status-summary',
        method: 'get'
      }).then(response => {
        if (response.data) {
          this.taskStats = response.data;
        }
      }).catch(error => {
        console.error("Failed to fetch task stats", error);
      });
    }
  },
};
</script>

<style scoped lang="scss">
.home {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: 100vh;
}

.status-card {
  margin-bottom: 20px;
  .card-header {
    font-size: 16px;
    font-weight: bold;
    color: #333;
    padding-bottom: 10px;
    border-bottom: 1px solid #EBEEF5;
  }
  .card-body {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100px;

    .count-text {
      font-size: 32px;
      font-weight: bold;

      &.pending {
        color: #E6A23C;
      }
      &.completed {
        color: #67C23A;
      }
      &.executing {
        color: #409EFF;
      }
    }

    &.chart-container {
      padding-top: 10px;
    }
  }
}
</style>
