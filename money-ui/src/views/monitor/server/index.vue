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
                  <td class="el-table__cell is-leaf"><div class="cell">核心线程数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.threadPoolInfo">{{ server.threadPoolInfo.corePoolSize }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">最大线程数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.threadPoolInfo">{{ server.threadPoolInfo.maximumPoolSize }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">当前活跃线程数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.threadPoolInfo">{{ server.threadPoolInfo.activeCount }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">当前池大小</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.threadPoolInfo">{{ server.threadPoolInfo.poolSize }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">已完成任务数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.threadPoolInfo">{{ server.threadPoolInfo.completedTaskCount }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">队列中等待执行的任务数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.threadPoolInfo">{{ server.threadPoolInfo.queueSize }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">队列剩余容量</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.threadPoolInfo">{{ server.threadPoolInfo.queueRemainingCapacity }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">总任务数</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.threadPoolInfo">{{ server.threadPoolInfo.taskCount }}</div></td>
                </tr>
              </tbody>
            </table>
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
              @click="getClusterThreadPoolInfo"
            >
              刷新
            </el-button>
          </div>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table cellspacing="0" style="width: 100%;">
              <thead>
                <tr>
                  <th class="el-table__cell is-leaf"><div class="cell">节点标识</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">核心线程数</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">最大线程数</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">当前活跃线程数</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">当前池大小</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">已完成任务数</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">队列中等待执行的任务数</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">队列剩余容量</div></th>
                  <th class="el-table__cell is-leaf"><div class="cell">总任务数</div></th>
                </tr>
              </thead>
              <tbody v-if="clusterThreadPoolInfo">
                <tr v-for="(info, nodeId) in clusterThreadPoolInfo" :key="nodeId">
                  <td class="el-table__cell is-leaf"><div class="cell">{{ nodeId }}</div></td>
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
              @click="getClusterServerInfo"
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
                    <div class="cell">{{ info.mem && info.mem.total ? info.mem.total.toFixed(2) + 'G' : '-' }}</div>
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
import { getServer, getNetworkTraffic, getClusterThreadPoolRedis, getAggregatedThreadPoolRedis, getClusterServerRedis } from "@/api/monitor/server";
import * as echarts from "echarts";

export default {
  name: "Server",
  data() {
    return {
      // 服务器信息
      server: [],
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
  created() {
    this.getList();
    this.openLoading();

    // 立即获取一次并开始定时刷新
    this.getNetworkTrafficInfo();
    this.getClusterInfo();

    // 每10秒刷新一次数据
    this.timer = setInterval(() => {
      this.getList();
    }, 10000);

    // 每5秒刷新一次网络流量数据
    this.networkTrafficTimer = setInterval(() => {
      this.getNetworkTrafficInfo();
    }, 5000);

    // 每10秒刷新一次集群信息
    this.clusterInfoTimer = setInterval(() => {
      this.getClusterInfo();
    }, 10000);
  },
  mounted() {
    // 初始化图表
    this.initCharts();
  },
  activated() {
    // 当组件被激活时重新初始化图表（针对keep-alive）
    this.$nextTick(() => {
      this.initCharts();
    });
  },
  beforeDestroy() {
    // 清除定时器
    if (this.timer) {
      clearInterval(this.timer);
    }
    if (this.networkTrafficTimer) {
      clearInterval(this.networkTrafficTimer);
    }
    if (this.clusterInfoTimer) {
      clearInterval(this.clusterInfoTimer);
    }
    // 销毁图表实例
    if (this.clusterCpuChart) {
      this.clusterCpuChart.dispose();
    }
    if (this.clusterMemoryChart) {
      this.clusterMemoryChart.dispose();
    }
    if (this.clusterThreadChart) {
      this.clusterThreadChart.dispose();
    }
    if (this.clusterNetworkChart) {
      this.clusterNetworkChart.dispose();
    }
    // 销毁单节点图表实例
    if (this.cpuChart) {
      this.cpuChart.dispose();
    }
    if (this.memoryChart) {
      this.memoryChart.dispose();
    }
  },
  watch: {
    // 监听服务器信息变化，更新单节点图表
    server: {
      handler() {
        this.$nextTick(() => {
          this.updateCharts();
        });
      },
      deep: true
    },
    // 监听网络流量信息变化，更新图表
    networkTraffic: {
      handler() {
        this.$nextTick(() => {
          this.updateClusterCharts();
        });
      },
      deep: true
    },
    // 监听集群信息变化，更新集群图表
    clusterServerInfo: {
      handler() {
        this.$nextTick(() => {
          this.updateClusterCharts();
        });
      },
      deep: true
    },
    clusterThreadPoolInfo: {
      handler() {
        this.$nextTick(() => {
          this.updateClusterCharts();
        });
      },
      deep: true
    },
    // 监听活动标签变化，更新相应图表
    activeChartTab() {
      this.$nextTick(() => {
        // 确保在DOM更新后再调整图表尺寸并更新数据
        this.handleChartResize();
        this.updateClusterCharts();
      });
    }
  },
  methods: {
    /** 查询服务器信息 */
    getList() {
      getServer().then(response => {
        this.server = response.data;
        this.$modal.closeLoading();
        // 更新单节点图表
        this.updateSingleNodeCharts();
      });
    },

    /** 打开加载提示 */
    openLoading() {
      this.$modal.loading("正在加载服务监控数据，请稍候！");
    },
    // 处理图表尺寸调整
    handleChartResize() {
      if (this.clusterCpuChart) {
        this.clusterCpuChart.resize();
      }
      if (this.clusterMemoryChart) {
        this.clusterMemoryChart.resize();
      }
      if (this.clusterThreadChart) {
        this.clusterThreadChart.resize();
      }
      if (this.clusterNetworkChart) {
        this.clusterNetworkChart.resize();
      }
      if (this.cpuChart) {
        this.cpuChart.resize();
      }
      if (this.memoryChart) {
        this.memoryChart.resize();
      }
    },
    /** 获取网络流量信息 */
    getNetworkTrafficInfo() {
      getNetworkTraffic().then(response => {
        this.networkTraffic = response.data;
      }).catch(err => {
        console.error("获取网络流量信息失败:", err);
      });
    },

    /** 获取集群信息 */
    getClusterInfo() {
      // 获取集群线程池信息
      getClusterThreadPoolRedis().then(response => {
        this.clusterThreadPoolInfo = response.data;
        console.log("获取Redis模式集群线程池信息，节点数量:", Object.keys(response.data).length);
      }).catch(err => {
        console.error("获取集群线程池信息失败:", err);
      });

      // 获取集群服务器信息
      getClusterServerRedis().then(response => {
        this.clusterServerInfo = response.data;
        console.log("获取集群服务器信息，节点数量:", Object.keys(response.data).length);
      }).catch(err => {
        console.error("获取集群服务器信息失败:", err);
      });
    },
    /** 处理模式切换 */
    handleModeChange() {
      this.getClusterThreadPoolInfo();
    },
    /** 获取磁盘使用率最高的分区 */
    getHighestDiskUsage(sysFiles) {
      if (!sysFiles || sysFiles.length === 0) return '-';

      // 找到使用率最高的磁盘分区
      let highestUsageFile = sysFiles[0];
      for (let i = 1; i < sysFiles.length; i++) {
        if (sysFiles[i].usage > highestUsageFile.usage) {
          highestUsageFile = sysFiles[i];
        }
      }

      // 返回格式化后的信息
      return `${highestUsageFile.dirName}: ${highestUsageFile.usage}%`;
    },
    // 初始化图表
    initCharts() {
      this.$nextTick(() => {
        // 初始化集群CPU使用率图表
        if (this.$refs.clusterCpuChart) {
          this.clusterCpuChart = echarts.init(this.$refs.clusterCpuChart);
        }

        // 初始化集群内存使用率图表
        if (this.$refs.clusterMemoryChart) {
          this.clusterMemoryChart = echarts.init(this.$refs.clusterMemoryChart);
        }

        // 初始化集群线程数图表
        if (this.$refs.clusterThreadChart) {
          this.clusterThreadChart = echarts.init(this.$refs.clusterThreadChart);
        }

        // 初始化集群网络流量图表
        if (this.$refs.clusterNetworkChart) {
          this.clusterNetworkChart = echarts.init(this.$refs.clusterNetworkChart);
        }

        // 初始化单节点CPU使用率图表
        if (this.$refs.cpuChart) {
          this.cpuChart = echarts.init(this.$refs.cpuChart);
        }

        // 初始化单节点内存使用率图表
        if (this.$refs.memoryChart) {
          this.memoryChart = echarts.init(this.$refs.memoryChart);
        }

        // 首次更新图表
        this.updateCharts();
        // 只有在数据准备好后再更新集群图表
        if (this.clusterServerInfo && this.clusterThreadPoolInfo) {
          this.updateClusterCharts();
        }
      });
    },

    // 更新单节点相关图表
    updateSingleNodeCharts() {
      this.$nextTick(() => {
        this.updateCpuChart();
        this.updateMemoryChart();
      });
    },
    // 更新所有图表
    updateCharts() {
      this.updateSingleNodeCharts();
      this.updateClusterCharts();
    },
    // 更新CPU图表
    updateCpuChart() {
      if (!this.cpuChart || !this.server.cpu) return;

      const option = {
        title: {
          text: 'CPU使用率',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: (params) => {
            return `${params[0].name}: ${params[0].value}%`;
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['用户使用率', '系统使用率', '当前空闲率']
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: '{value} %'
          }
        },
        series: [{
          name: '使用率',
          type: 'bar',
          barWidth: '60%',
          data: [
            parseFloat(this.server.cpu.used),
            parseFloat(this.server.cpu.sys),
            parseFloat(this.server.cpu.free)
          ],
          itemStyle: {
            color: (params) => {
              const index = params.dataIndex;
              if (index === 0) {
                return '#409eff'; // 用户使用率 - 蓝色
              } else if (index === 1) {
                return '#67c23a'; // 系统使用率 - 绿色
              } else {
                return '#909399'; // 空闲率 - 灰色
              }
            }
          }
        }]
      };

      this.cpuChart.setOption(option, true);
    },
    // 更新内存图表
    updateMemoryChart() {
      if (!this.memoryChart || !this.server.mem || !this.server.jvm) return;

      const option = {
        title: {
          text: '内存使用率',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: (params) => {
            return `${params[0].name}: ${params[0].value}%`;
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: ['物理内存', 'JVM内存']
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: '{value} %'
          }
        },
        series: [{
          name: '使用率',
          type: 'bar',
          barWidth: '60%',
          data: [
            parseFloat(this.server.mem.usage),
            parseFloat(this.server.jvm.usage)
          ],
          itemStyle: {
            color: (params) => {
              const index = params.dataIndex;
              const value = params.value;
              if (index === 0) {
                // 物理内存使用不同颜色表示使用率级别
                if (value > 80) {
                  return '#f56c6c'; // 红色
                } else if (value > 60) {
                  return '#e6a23c'; // 橙色
                } else {
                  return '#67c23a'; // 绿色
                }
              } else {
                // JVM内存使用蓝色
                return '#409eff';
              }
            }
          }
        }]
      };

      this.memoryChart.setOption(option, true);
    },
    // 更新集群图表
    updateClusterCharts() {
      // 如果没有集群信息，不更新图表
      if (!this.clusterServerInfo || !this.clusterThreadPoolInfo) {
        return;
      }

      // 根据当前激活的tab更新对应的图表
      switch (this.activeChartTab) {
        case 'cpu':
          this.updateClusterCpuChart();
          break;
        case 'memory':
          this.updateClusterMemoryChart();
          break;
        case 'threads':
          this.updateClusterThreadChart();
          break;
        case 'network':
          this.updateClusterNetworkChart();
          break;
        default:
          // 默认更新所有图表
          this.updateClusterCpuChart();
          this.updateClusterMemoryChart();
          this.updateClusterThreadChart();
          this.updateClusterNetworkChart();
      }
    },
    // 更新集群CPU图表
    updateClusterCpuChart() {
      if (!this.clusterCpuChart || !this.clusterServerInfo) return;

      // 提取节点的实际IP地址作为显示名称
      const nodeNames = Object.keys(this.clusterServerInfo).map(nodeId => {
        // 节点ID格式为 IP:UUID，我们只需要IP部分
        return nodeId.split(':')[0] || nodeId;
      });
      
      const originalNodeIds = Object.keys(this.clusterServerInfo);
      const cpuUsages = originalNodeIds.map(nodeId => {
        const nodeInfo = this.clusterServerInfo[nodeId];
        return nodeInfo.cpu ? parseFloat(nodeInfo.cpu.used) : 0;
      });

      const option = {
        title: {
          text: '集群节点CPU使用率',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: (params) => {
            return `${params[0].name}<br/>${params[0].seriesName}: ${params[0].value}%`;
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: nodeNames
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: '{value} %'
          }
        },
        series: [{
          name: 'CPU使用率',
          type: 'bar',
          barWidth: '60%',
          data: cpuUsages,
          itemStyle: {
            color: (params) => {
              const value = params.value;
              if (value > 80) {
                return '#f56c6c'; // 红色
              } else if (value > 60) {
                return '#e6a23c'; // 橙色
              } else {
                return '#67c23a'; // 绿色
              }
            }
          }
        }]
      };

      this.clusterCpuChart.setOption(option, true);
    },
    // 更新集群内存图表
    updateClusterMemoryChart() {
      if (!this.clusterMemoryChart || !this.clusterServerInfo) return;

      // 提取节点的实际IP地址作为显示名称
      const nodeNames = Object.keys(this.clusterServerInfo).map(nodeId => {
        // 节点ID格式为 IP:UUID，我们只需要IP部分
        return nodeId.split(':')[0] || nodeId;
      });
      
      const originalNodeIds = Object.keys(this.clusterServerInfo);
      const memoryUsages = originalNodeIds.map(nodeId => {
        const nodeInfo = this.clusterServerInfo[nodeId];
        return nodeInfo.mem ? parseFloat(nodeInfo.mem.usage) : 0;
      });

      const option = {
        title: {
          text: '集群节点内存使用率',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: (params) => {
            return `${params[0].name}<br/>${params[0].seriesName}: ${params[0].value}%`;
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: nodeNames
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: '{value} %'
          }
        },
        series: [{
          name: '内存使用率',
          type: 'bar',
          barWidth: '60%',
          data: memoryUsages,
          itemStyle: {
            color: (params) => {
              const value = params.value;
              if (value > 80) {
                return '#f56c6c'; // 红色
              } else if (value > 60) {
                return '#e6a23c'; // 橙色
              } else {
                return '#67c23a'; // 绿色
              }
            }
          }
        }]
      };

      this.clusterMemoryChart.setOption(option, true);
    },
    // 更新集群线程图表
    updateClusterThreadChart() {
      if (!this.clusterThreadChart || !this.clusterThreadPoolInfo || this.activeChartTab !== 'threads') return;

      // 提取节点的实际IP地址作为显示名称
      const nodeNames = Object.keys(this.clusterThreadPoolInfo).map(nodeId => {
        // 节点ID格式为 IP:UUID，我们只需要IP部分
        return nodeId.split(':')[0] || nodeId;
      });
      
      const originalNodeIds = Object.keys(this.clusterThreadPoolInfo);
      const activeThreads = originalNodeIds.map(nodeId => {
        const nodeInfo = this.clusterThreadPoolInfo[nodeId];
        return nodeInfo.activeCount || 0;
      });

      const option = {
        title: {
          text: '集群节点活跃线程数',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: (params) => {
            return `${params[0].name}<br/>${params[0].seriesName}: ${params[0].value}`;
          }
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: nodeNames
        },
        yAxis: {
          type: 'value'
        },
        series: [{
          name: '活跃线程数',
          type: 'bar',
          barWidth: '60%',
          data: activeThreads,
          itemStyle: {
            color: '#409eff'
          }
        }]
      };

      this.clusterThreadChart.setOption(option, true);
    },
    
    // 更新集群网络流量图表
    updateClusterNetworkChart() {
      if (!this.clusterNetworkChart || this.activeChartTab !== 'network') return;

      // 获取所有节点的网络流量数据
      const originalNodeIds = Object.keys(this.clusterServerInfo);
      
      // 提取节点的实际IP地址作为显示名称
      const nodeNames = originalNodeIds.map(nodeId => {
        // 节点ID格式为 IP:UUID，我们只需要IP部分
        return nodeId.split(':')[0] || nodeId;
      });
      
      const networkData = [];
      
      // 遍历所有节点，收集网络流量信息
      originalNodeIds.forEach(nodeId => {
        const nodeInfo = this.clusterServerInfo[nodeId];
        if (nodeInfo.networkTraffic && nodeInfo.networkTraffic.interfaces) {
          // 计算该节点的总接收和发送字节数
          let totalBytesRecv = 0;
          let totalBytesSent = 0;
          
          nodeInfo.networkTraffic.interfaces.forEach(iface => {
            totalBytesRecv += iface.bytesRecv || 0;
            totalBytesSent += iface.bytesSent || 0;
          });
          
          networkData.push({
            name: nodeId.split(':')[0] || nodeId,
            bytesRecv: totalBytesRecv,
            bytesSent: totalBytesSent
          });
        }
      });

      const option = {
        title: {
          text: '集群节点网络流量',
          left: 'center'
        },
        tooltip: {
          trigger: 'axis',
          axisPointer: {
            type: 'shadow'
          },
          formatter: (params) => {
            // 显示所有系列的数据，而不仅仅是第一个系列
            let tooltipContent = params[0].name;
            params.forEach(param => {
              const seriesName = param.seriesName;
              const value = param.value;
              const formattedValue = this.formatBytes(value);
              tooltipContent += `<br/>${seriesName}: ${formattedValue}`;
            });
            return tooltipContent;
          }
        },
        legend: {
          data: ['接收流量', '发送流量'],
          top: '10%'
        },
        grid: {
          left: '3%',
          right: '4%',
          bottom: '3%',
          containLabel: true
        },
        xAxis: {
          type: 'category',
          data: nodeNames
        },
        yAxis: {
          type: 'value',
          axisLabel: {
            formatter: (value) => {
              return this.formatBytes(value);
            }
          }
        },
        series: [
          {
            name: '接收流量',
            type: 'bar',
            barGap: 0,
            data: networkData.map(item => item.bytesRecv),
            itemStyle: {
              color: '#5470c6'
            }
          },
          {
            name: '发送流量',
            type: 'bar',
            data: networkData.map(item => item.bytesSent),
            itemStyle: {
              color: '#91cc75'
            }
          }
        ]
      };

      this.clusterNetworkChart.setOption(option, true);
    },
    
    // 格式化字节大小
    formatBytes(bytes) {
      if (bytes === 0 || bytes === null || bytes === undefined) return '0 B';
      const k = 1024;
      const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
      const i = Math.floor(Math.log(bytes) / Math.log(k));
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
    }
  }
};
</script>

<style scoped>
.app-container {
  padding: 20px;
}

.card-box {
  padding-right: 15px;
  padding-left: 15px;
  margin-bottom: 10px;
}

/* 表格样式 */
.el-table {
  border: 1px solid #ebeef5;
}

.el-table table {
  width: 100%;
  border-collapse: collapse;
}

.el-table th,
.el-table td {
  border-bottom: 1px solid #ebeef5;
  border-right: 1px solid #ebeef5;
}

.el-table th:last-child,
.el-table td:last-child {
  border-right: 0;
}

.el-table .cell {
  padding-left: 10px;
  padding-right: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.el-table th>.cell {
  font-weight: bold;
  color: #909399;
}

.text-danger {
  color: #f56c6c;
}

/* 图表样式 */
.chart {
  width: 100%;
  height: 400px;
}

/* 添加针对集群图表的特殊样式 */
.cluster-chart-container {
  width: 100%;
  height: 400px;
  min-width: 0; /* 防止flex布局中的宽度问题 */
}

/* 确保tabs中的图表容器有正确的尺寸 */
.el-tabs__content .chart {
  width: 100%;
  height: 400px;
  min-width: 0;
}
</style>
