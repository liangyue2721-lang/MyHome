<template>
  <div class="app-container">
    <el-row :gutter="10">
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span><i class="el-icon-monitor"></i> 线程池状态</span>
          </div>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table cellspacing="0" style="width: 100%;">
              <tbody>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">核心线程数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ poolInfo.corePoolSize }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">最大线程数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ poolInfo.maximumPoolSize }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">当前活跃线程数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ poolInfo.activeCount }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">当前池大小</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ poolInfo.poolSize }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">队列大小</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ poolInfo.queueSize }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">队列剩余容量</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ poolInfo.queueRemainingCapacity }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">已完成任务数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ poolInfo.completedTaskCount }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">总任务数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ poolInfo.taskCount }}</div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span><i class="el-icon-data-line"></i> 线程池使用率</span>
          </div>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <div ref="threadPoolChart" style="height: 420px" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getThreadPool } from "@/api/monitor/threadPool"
import * as echarts from "echarts"

export default {
  name: "ThreadPool",
  data() {
    return {
      // 线程池信息
      poolInfo: {},
      // 图表实例
      threadPoolChart: null
    }
  },
  created() {
    this.getList()
    this.openLoading()
  },
  methods: {
    /** 查询线程池信息 */
    getList() {
      getThreadPool().then(response => {
        this.poolInfo = response.data
        this.$modal.closeLoading()
        this.initChart()
      })
    },
    // 打开加载层
    openLoading() {
      this.$modal.loading("正在加载线程池监控数据，请稍候！")
    },
    // 初始化图表
    initChart() {
      this.$nextTick(() => {
        this.threadPoolChart = echarts.init(this.$refs.threadPoolChart, "macarons")
        
        const activeRate = this.poolInfo.corePoolSize ? 
          Math.round((this.poolInfo.activeCount / this.poolInfo.corePoolSize) * 100) : 0
        const queueRate = this.poolInfo.queueCapacity ? 
          Math.round((this.poolInfo.queueSize / (this.poolInfo.queueSize + this.poolInfo.queueRemainingCapacity)) * 100) : 0
          
        this.threadPoolChart.setOption({
          tooltip: {
            trigger: "item",
            formatter: "{a} <br/>{b} : {c} ({d}%)"
          },
          series: [
            {
              name: "线程池使用情况",
              type: "pie",
              radius: [15, 95],
              center: ["50%", "50%"],
              roseType: "radius",
              data: [
                { value: this.poolInfo.activeCount, name: `活跃线程数 (${activeRate}%)` },
                { value: this.poolInfo.poolSize - this.poolInfo.activeCount, name: "空闲线程数" },
                { value: this.poolInfo.queueSize, name: `队列任务数 (${queueRate}%)` },
                { value: this.poolInfo.completedTaskCount, name: "已完成任务数" }
              ],
              animationEasing: "cubicInOut",
              animationDuration: 1000
            }
          ]
        })
        
        window.addEventListener("resize", () => {
          this.threadPoolChart.resize()
        })
      })
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.card-box {
  margin-bottom: 20px;
}

.cell {
  padding: 8px;
  font-size: 14px;
}
</style>