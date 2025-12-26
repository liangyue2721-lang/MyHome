<template>
  <div class="app-container dashboard-container">
    <!-- Row 1: Key Metrics Cards -->
    <el-row :gutter="20" class="mb-20">
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background-color: #409EFF">
            <i class="el-icon-monitor"></i>
          </div>
          <div class="metric-info">
            <div class="metric-title">Redis状态</div>
            <div class="metric-value" v-if="cache.info">{{ cache.info.redis_mode == "standalone" ? "单机" : "集群" }}</div>
            <div class="metric-sub">版本: {{ cache.info ? cache.info.redis_version : '-' }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background-color: #67C23A">
            <i class="el-icon-time"></i>
          </div>
          <div class="metric-info">
            <div class="metric-title">运行天数</div>
            <div class="metric-value" v-if="cache.info">{{ cache.info.uptime_in_days }}</div>
            <div class="metric-sub">天</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background-color: #E6A23C">
            <i class="el-icon-key"></i>
          </div>
          <div class="metric-info">
            <div class="metric-title">Key数量</div>
            <div class="metric-value" v-if="cache.dbSize">{{ cache.dbSize }}</div>
            <div class="metric-sub">个</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover" class="metric-card">
          <div class="metric-icon" style="background-color: #F56C6C">
            <i class="el-icon-cpu"></i>
          </div>
          <div class="metric-info">
            <div class="metric-title">内存占用</div>
            <div class="metric-value" v-if="cache.info">{{ cache.info.used_memory_human }}</div>
            <div class="metric-sub">配置: {{ cache.info ? cache.info.maxmemory_human : '-' }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Row 2: Detailed Info Tables -->
    <el-row :gutter="20" class="mb-20">
      <el-col :span="12">
        <el-card shadow="hover" class="info-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-info"></i> 系统信息</span>
          </div>
          <div class="detail-table">
            <div class="detail-row">
              <span class="detail-label">Redis 版本</span>
              <span class="detail-value" v-if="cache.info">{{ cache.info.redis_version }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">运行模式</span>
              <span class="detail-value" v-if="cache.info">{{ cache.info.redis_mode == "standalone" ? "单机" : "集群" }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">端口</span>
              <span class="detail-value" v-if="cache.info">{{ cache.info.tcp_port }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">客户端连接数</span>
              <span class="detail-value" v-if="cache.info">{{ cache.info.connected_clients }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">运行时间</span>
              <span class="detail-value" v-if="cache.info">{{ cache.info.uptime_in_days }} 天</span>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover" class="info-card">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-s-data"></i> 性能与状态</span>
          </div>
          <div class="detail-table">
            <div class="detail-row">
              <span class="detail-label">网络入口流量</span>
              <span class="detail-value" v-if="cache.info">{{ cache.info.instantaneous_input_kbps }} kps</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">网络出口流量</span>
              <span class="detail-value" v-if="cache.info">{{ cache.info.instantaneous_output_kbps }} kps</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">使用 CPU</span>
              <span class="detail-value" v-if="cache.info">{{ parseFloat(cache.info.used_cpu_user_children).toFixed(2) }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">AOF 开启</span>
              <span class="detail-value" v-if="cache.info">{{ cache.info.aof_enabled == "0" ? "否" : "是" }}</span>
            </div>
            <div class="detail-row">
              <span class="detail-label">RDB 状态</span>
              <span class="detail-value" v-if="cache.info">{{ cache.info.rdb_last_bgsave_status }}</span>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Row 3: Charts -->
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-pie-chart"></i> 命令统计</span>
          </div>
          <div class="chart-wrapper">
            <div ref="commandstats" style="height: 350px" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="hover">
          <div slot="header" class="clearfix">
            <span><i class="el-icon-odometer"></i> 内存使用率</span>
          </div>
          <div class="chart-wrapper">
            <div ref="usedmemory" style="height: 350px" />
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getCache } from "@/api/monitor/cache"
import * as echarts from "echarts"

export default {
  name: "Cache",
  data() {
    return {
      // 统计命令信息
      commandstats: null,
      // 使用内存
      usedmemory: null,
      // cache信息
      cache: []
    }
  },
  created() {
    this.getList()
    this.openLoading()
  },
  methods: {
    /** 查缓存询信息 */
    getList() {
      getCache().then((response) => {
        this.cache = response.data
        this.$modal.closeLoading()

        this.commandstats = echarts.init(this.$refs.commandstats, "macarons")
        this.commandstats.setOption({
          tooltip: {
            trigger: "item",
            formatter: "{a} <br/>{b} : {c} ({d}%)",
          },
          series: [
            {
              name: "命令",
              type: "pie",
              roseType: "radius",
              radius: [15, 95],
              center: ["50%", "38%"],
              data: response.data.commandStats,
              animationEasing: "cubicInOut",
              animationDuration: 1000,
            }
          ]
        })
        this.usedmemory = echarts.init(this.$refs.usedmemory, "macarons")
        this.usedmemory.setOption({
          tooltip: {
            formatter: "{b} <br/>{a} : " + this.cache.info.used_memory_human,
          },
          series: [
            {
              name: "峰值",
              type: "gauge",
              min: 0,
              max: 1000,
              detail: {
                formatter: this.cache.info.used_memory_human,
              },
              data: [
                {
                  value: parseFloat(this.cache.info.used_memory_human),
                  name: "内存消耗",
                }
              ]
            }
          ]
        })
        window.addEventListener("resize", () => {
          this.commandstats.resize()
          this.usedmemory.resize()
        })
      })
    },
    // 打开加载层
    openLoading() {
      this.$modal.loading("正在加载缓存监控数据，请稍候！")
    }
  }
}
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 84px);
}

.mb-20 {
  margin-bottom: 20px;
}

.metric-card {
  display: flex;
  align-items: center;
  height: 100px;
}

.metric-card ::v-deep .el-card__body {
  display: flex;
  align-items: center;
  padding: 20px;
  width: 100%;
}

.metric-icon {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 20px;
}

.metric-icon i {
  font-size: 30px;
  color: #fff;
}

.metric-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.metric-title {
  font-size: 14px;
  color: #909399;
  margin-bottom: 5px;
}

.metric-value {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
}

.metric-sub {
  font-size: 12px;
  color: #C0C4CC;
  margin-top: 5px;
}

.detail-table {
  padding: 10px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #EBEEF5;
}

.detail-row:last-child {
  border-bottom: none;
}

.detail-label {
  color: #606266;
}

.detail-value {
  color: #303133;
  font-weight: 500;
}
</style>
