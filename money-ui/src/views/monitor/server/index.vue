<template>
  <div class="app-container">
    <el-row :gutter="10">
      <!-- CPU信息 -->
      <el-col :span="12" class="card-box">
        <el-card>
          <div slot="header"><span><i class="el-icon-cpu"></i> CPU</span></div>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table cellspacing="0" style="width: 100%;">
              <thead>
                <tr>
                  <th class="el-table__cell is-leaf"><div class="cell">属性</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">值</div></th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">核心数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.cpu">{{ server.cpu.cpuNum }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">用户使用率</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.cpu">{{ server.cpu.used }}%</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">系统使用率</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.cpu">{{ server.cpu.sys }}%</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">当前空闲率</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.cpu">{{ server.cpu.free }}%</div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <!-- 内存信息 -->
      <el-col :span="12" class="card-box">
        <el-card>
          <div slot="header"><span><i class="el-icon-tickets"></i> 内存</span></div>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table cellspacing="0" style="width: 100%;">
              <thead>
                <tr>
                  <th class="el-table__cell is-leaf"><div class="cell">属性</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">内存</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">JVM</div></th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">总内存</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.mem">{{ server.mem.total }}M</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.jvm">{{ server.jvm.total }}M</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">已用内存</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.mem">{{ server.mem.used}}M</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.jvm">{{ server.jvm.used}}M</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">剩余内存</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.mem">{{ server.mem.free }}M</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.jvm">{{ server.jvm.free }}M</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">使用率</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.mem">{{ server.mem.usage }}%</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.jvm">{{ server.jvm.usage }}%</div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <!-- 内存使用率图表 -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span>内存使用率</span>
          </div>
          <div class="echarts-container">
            <div ref="memoryChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>

      <!-- CPU使用率图表 -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span>CPU使用率</span>
          </div>
          <div class="echarts-container">
            <div ref="cpuChart" class="chart"></div>
          </div>
        </el-card>
      </el-col>

      <!-- 磁盘信息 -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header"><span><i class="el-icon-monitor"></i> 服务器信息</span></div>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table cellspacing="0" style="width: 100%;">
              <thead>
                <tr>
                  <th class="el-table__cell is-leaf"><div class="cell">属性</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">值</div></th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">服务器名称</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.sys">{{ server.sys.computerName }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">操作系统</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.sys">{{ server.sys.osName }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">服务器IP</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.sys">{{ server.sys.computerIp }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">系统架构</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.sys">{{ server.sys.osArch }}</div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <!-- 磁盘信息 -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span><i class="el-icon-box"></i> 磁盘状态</span>
          </div>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table cellspacing="0" style="width: 100%;">
              <thead>
                <tr>
                  <th class="el-table__cell is-leaf"><div class="cell">盘符路径</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">文件系统</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">盘符类型</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">总大小</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">可用大小</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">已用大小</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">使用率</div></th>
                </tr>
              </thead>
              <tbody v-if="server.sysFiles && server.sysFiles.length > 0">
                <tr v-for="(sysFile, index) in server.sysFiles" :key="index">
                  <td class="el-table__cell is-leaf"><div class="cell">{{ sysFile.dirName }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ sysFile.sysTypeName }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ sysFile.typeName }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ sysFile.total }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ sysFile.free }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ sysFile.used }}</div></td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">
                      <el-progress :percentage="sysFile.usage" :color="sysFile.usage > 80 ? 'red' : sysFile.usage > 60 ? '#E6A23C' : '#67C23A'"></el-progress>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <!-- 线程池信息 -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span><i class="el-icon-help"></i> 线程池信息</span>
          </div>
          <div class="el-table el-table--enable-row-hover el-table--medium" v-if="server.threadPoolInfo && server.threadPoolInfo.threadPools">
            <div v-for="(pool, index) in server.threadPoolInfo.threadPools" :key="index">
              <h4>{{ pool.name }}</h4>
              <table cellspacing="0" style="width: 100%;">
                <tbody>
                  <tr>
                    <td class="el-table__cell is-leaf"><div class="cell">核心线程数</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">{{ pool.corePoolSize }}</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">最大线程数</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">{{ pool.maximumPoolSize }}</div></td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf"><div class="cell">当前活跃线程数</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">{{ pool.activeCount }}</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">当前池大小</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">{{ pool.poolSize }}</div></td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf"><div class="cell">已完成任务数</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">{{ pool.completedTaskCount }}</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">总任务数</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">{{ pool.taskCount }}</div></td>
                  </tr>
                  <tr>
                    <td class="el-table__cell is-leaf"><div class="cell">队列大小</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">{{ pool.queueSize }}</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">队列剩余容量</div></td>
                    <td class="el-table__cell is-leaf"><div class="cell">{{ pool.queueRemainingCapacity }}</div></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 集群线程池信息 -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span>集群线程池信息</span>
            <el-switch
              v-model="useRedis"
              active-text="Redis模式"
              inactive-text="内存模式"
              @change="handleModeChange"
              style="float: right; margin-top: -5px;"
            ></el-switch>
            <el-button
              style="float: right; margin-top: -5px; margin-right: 10px;"
              size="mini"
              @click="getClusterInfo"
            >
              刷新
            </el-button>
          </div>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table cellspacing="0" style="width: 100%;">
              <thead>
                <tr>
                  <th class="el-table__cell is-leaf"><div class="cell">节点标识</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">线程池名称</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">核心线程数</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">最大线程数</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">当前活跃线程数</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">当前池大小</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">已完成任务数</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">队列大小</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">队列剩余容量</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">总任务数</div></th>
                </tr>
              </thead>
              <tbody v-if="formattedClusterThreadPoolInfo.length > 0">
                <tr v-for="(info, index) in formattedClusterThreadPoolInfo" :key="index">
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.nodeId }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.name }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.corePoolSize }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.maximumPoolSize }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.activeCount }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.poolSize }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.completedTaskCount }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.queueSize }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.queueRemainingCapacity }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.taskCount }}</div></td>
                </tr>
              </tbody>
              <tbody v-else>
                <tr>
                  <td colspan="10" class="el-table__cell is-leaf" style="text-align: center;">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <!-- 集群节点CPU和内存使用图表 -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span>集群节点资源使用情况</span>
          </div>
          <el-tabs v-model="activeChartTab">
            <el-tab-pane label="CPU使用率" name="cpu">
              <div ref="clusterCpuChart" class="chart cluster-chart-container" style="height: 400px;"></div>
            </el-tab-pane>
            <el-tab-pane label="内存使用率" name="memory">
              <div ref="clusterMemoryChart" class="chart cluster-chart-container" style="height: 400px;"></div>
            </el-tab-pane>
            <el-tab-pane label="活跃线程数" name="threads">
              <div ref="clusterThreadChart" class="chart cluster-chart-container" style="height: 400px;"></div>
            </el-tab-pane>
            <el-tab-pane label="网络流量" name="network">
              <div ref="clusterNetworkChart" class="chart cluster-chart-container" style="height: 400px;"></div>
            </el-tab-pane>
          </el-tabs>
        </el-card>
      </el-col>

      <!-- 集群服务器信息 -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span>集群服务器信息</span>
            <el-button
              style="float: right; margin-top: -5px;"
              size="mini"
              @click="getClusterInfo"
            >
              刷新
            </el-button>
          </div>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table cellspacing="0" style="width: 100%;">
              <thead>
                <tr>
                  <th class="el-table__cell is-leaf"><div class="cell">节点标识</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">CPU核心数</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">CPU使用率</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">内存总量</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">内存使用率</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">JVM使用率</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">操作系统</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">磁盘使用率</div></th>
                </tr>
              </thead>
              <tbody v-if="clusterServerInfo">
                <tr v-for="(info, nodeId) in clusterServerInfo" :key="nodeId">
                  <td class="el-table__cell is-leaf"><div class="cell">{{ nodeId }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">{{ info.cpu && info.cpu.cpuNum || '-' }}</div></td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">{{ info.cpu && info.cpu.used ? info.cpu.used + '%' : '-' }}</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">{{ info.mem && info.mem.total ? (info.mem.total / 1024).toFixed(2) + 'G' : '-' }}</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">{{ info.mem && info.mem.usage ? info.mem.usage + '%' : '-' }}</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">{{ info.jvm && info.jvm.usage ? info.jvm.usage + '%' : '-' }}</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell">{{ info.sys && info.sys.osName || '-' }}</div>
                  </td>
                  <td class="el-table__cell is-leaf">
                    <div class="cell" v-if="info.sysFiles && info.sysFiles.length > 0">
                      <!-- 仅显示磁盘使用率最高的分区 -->
                      <div>
                        {{ getHighestDiskUsage(info.sysFiles) }}
                      </div>
                    </div>
                    <div class="cell" v-else>-</div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getServer, getNetworkTraffic, getClusterThreadPool, getClusterThreadPoolRedis, getAggregatedThreadPoolRedis, getClusterServerRedis } from "@/api/monitor/server";
import * as echarts from "echarts";

export default {
  name: "Server",
  data() {
    return {
      // 服务器信息
      server: {
        cpu: {},
        mem: {},
        jvm: {},
        sys: {},
        sysFiles: [],
        threadPoolInfo: {
          threadPools: []
        }
      },
      // 网络流量信息
      networkTraffic: {
        interfaces: []
      },
      // 集群线程池信息
      clusterThreadPoolInfo: {},
      // 聚合统计信息
      aggregatedInfo: {},
      // 集群服务器信息
      clusterServerInfo: {},
      // 定时器
      timer: null,
      networkTrafficTimer: null,
      clusterInfoTimer: null,
      // 是否使用Redis模式
      useRedis: true,
      // 图表实例
      clusterCpuChart: null,
      clusterMemoryChart: null,
      clusterThreadChart: null,
      clusterNetworkChart: null,
      // 单节点图表实例
      cpuChart: null,
      memoryChart: null,
      // 当前激活的图表标签
      activeChartTab: 'cpu'
    };
  },
  computed: {
    formattedClusterThreadPoolInfo() {
      const formatted = [];
      for (const nodeId in this.clusterThreadPoolInfo) {
        if (this.clusterThreadPoolInfo[nodeId]) {
          this.clusterThreadPoolInfo[nodeId].forEach(pool => {
            formatted.push({
              nodeId: nodeId,
              ...pool
            });
          });
        }
      }
      return formatted;
    }
  },
  created() {
    this.getList();
    this.openLoading();
    this.getClusterInfo();
    this.timer = setInterval(this.getList, 10000);
    this.clusterInfoTimer = setInterval(this.getClusterInfo, 10000);
  },
  mounted() {
    this.initCharts();
  },
  beforeDestroy() {
    clearInterval(this.timer);
    clearInterval(this.clusterInfoTimer);
  },
  methods: {
    getList() {
      getServer().then(response => {
        this.server = response.data;
        this.$modal.closeLoading();
        this.updateSingleNodeCharts();
      });
    },
    getClusterInfo() {
      const apiCall = this.useRedis ? getClusterThreadPoolRedis : getClusterThreadPool;
      apiCall().then(response => {
        this.clusterThreadPoolInfo = response.data;
      });
      getClusterServerRedis().then(response => {
        this.clusterServerInfo = response.data;
      });
    },
    handleModeChange() {
      this.getClusterInfo();
    },
    openLoading() {
      this.$modal.loading("正在加载服务监控数据，请稍候！");
    },
    initCharts() {
      // Chart initialization logic
    },
    updateSingleNodeCharts() {
      // Chart update logic
    },
    getHighestDiskUsage(sysFiles) {
      if (!sysFiles || sysFiles.length === 0) return '-';
      let highestUsageFile = sysFiles.reduce((max, file) => file.usage > max.usage ? file : max, sysFiles[0]);
      return `${highestUsageFile.dirName}: ${highestUsageFile.usage}%`;
    }
  }
};
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.card-box {
  margin-bottom: 20px;
}
</style>
