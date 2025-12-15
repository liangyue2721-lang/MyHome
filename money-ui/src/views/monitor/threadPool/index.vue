<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="border-card" @tab-click="handleTabClick">
      <el-tab-pane v-for="(pool, index) in poolList" :key="index" :label="pool.name" :name="String(index)">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-card class="box-card" shadow="hover">
              <div slot="header" class="clearfix">
                <span><i class="el-icon-monitor"></i> {{ pool.name }}</span>
              </div>
              <div class="el-table el-table--enable-row-hover el-table--medium">
                <table cellspacing="0" class="custom-table">
                  <tbody>
                    <tr>
                      <td class="cell-label">核心线程数</td>
                      <td class="cell-value">{{ pool.corePoolSize }}</td>
                      <td class="cell-label">最大线程数</td>
                      <td class="cell-value">{{ pool.maximumPoolSize }}</td>
                    </tr>
                    <tr>
                      <td class="cell-label">当前活跃线程数</td>
                      <td class="cell-value">{{ pool.activeCount }}</td>
                      <td class="cell-label">当前池大小</td>
                      <td class="cell-value">{{ pool.poolSize }}</td>
                    </tr>
                    <tr>
                      <td class="cell-label">队列大小</td>
                      <td class="cell-value">{{ pool.queueSize }}</td>
                      <td class="cell-label">队列剩余容量</td>
                      <td class="cell-value">{{ pool.queueRemainingCapacity }}</td>
                    </tr>
                    <tr>
                      <td class="cell-label">已完成任务数</td>
                      <td class="cell-value">{{ pool.completedTaskCount }}</td>
                      <td class="cell-label">总任务数</td>
                      <td class="cell-value">{{ pool.taskCount }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </el-card>
          </el-col>

          <el-col :span="12">
            <el-card class="box-card" shadow="hover">
              <div slot="header" class="clearfix">
                <span><i class="el-icon-data-line"></i> {{ pool.name }}使用率</span>
              </div>
              <div class="chart-container">
                <div :ref="'threadPoolChart' + index" style="height: 320px" />
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script>
import { getThreadPool } from "@/api/monitor/threadPool"
import * as echarts from "echarts"

export default {
  name: "ThreadPool",
  data() {
    return {
      poolList: [],
      activeTab: "0",
      chartInstances: []
    }
  },
  created() {
    this.getList()
    this.openLoading()
  },
  methods: {
    getList() {
      getThreadPool().then(response => {
        this.poolList = response.data
        this.$modal.closeLoading()
        this.initCharts()
      })
    },
    openLoading() {
      this.$modal.loading("正在加载线程池监控数据，请稍候！")
    },
    initCharts() {
      this.$nextTick(() => {
        this.poolList.forEach((pool, index) => {
          const chartRef = this.$refs['threadPoolChart' + index][0]
          const chart = echarts.init(chartRef, "macarons")
          this.renderChart(chart, pool)
          this.chartInstances.push(chart)
        })
        window.addEventListener("resize", this.resizeCharts)
      })
    },
    renderChart(chart, poolInfo) {
      const activeRate = poolInfo.corePoolSize ? Math.round((poolInfo.activeCount / poolInfo.corePoolSize) * 100) : 0
      const queueRate = poolInfo.queueCapacity ? Math.round((poolInfo.queueSize / poolInfo.queueCapacity) * 100) : 0
      chart.setOption({
        tooltip: {
          trigger: "item",
          formatter: "{a} <br/>{b} : {c} ({d}%)"
        },
        series: [{
          name: "线程池使用情况",
          type: "pie",
          radius: [15, 95],
          center: ["50%", "50%"],
          roseType: "radius",
          data: [
            { value: poolInfo.activeCount, name: `活跃线程数 (${activeRate}%)` },
            { value: poolInfo.poolSize - poolInfo.activeCount, name: "空闲线程数" },
            { value: poolInfo.queueSize, name: `队列任务数 (${queueRate}%)` },
            { value: poolInfo.completedTaskCount, name: "已完成任务数" }
          ],
          animationEasing: "cubicInOut",
          animationDuration: 1000
        }]
      })
    },
    handleTabClick() {
      this.$nextTick(() => {
        this.resizeCharts()
      })
    },
    resizeCharts() {
      this.chartInstances.forEach(chart => {
        chart.resize()
      })
    }
  },
  beforeDestroy() {
    window.removeEventListener("resize", this.resizeCharts)
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
  background-color: #f5f7fa;
}
.box-card {
  margin-bottom: 20px;
  border-radius: 8px;
}
.clearfix:before,
.clearfix:after {
  display: table;
  content: "";
}
.clearfix:after {
  clear: both
}
.custom-table {
  width: 100%;
  border-collapse: collapse;
}
.cell-label {
  padding: 12px;
  font-size: 14px;
  font-weight: bold;
  background-color: #f8f8f9;
  border: 1px solid #e8eaec;
  text-align: left;
}
.cell-value {
  padding: 12px;
  font-size: 14px;
  border: 1px solid #e8eaec;
  text-align: left;
}
.chart-container {
  padding-top: 20px;
}
</style>