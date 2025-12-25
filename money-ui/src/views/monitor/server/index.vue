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
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.cpu">{{ server.cpu.cpuNum || '--' }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">用户使用率</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.cpu">{{ server.cpu.used || '--' }}%</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">系统使用率</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.cpu">{{ server.cpu.sys || '--' }}%</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">当前空闲率</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.cpu">{{ server.cpu.free || '--' }}%</div></td>
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
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.mem">{{ server.mem.total || '--' }}M</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.jvm">{{ server.jvm.total || '--' }}M</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">已用内存</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.mem">{{ server.mem.used || '--' }}M</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.jvm">{{ server.jvm.used || '--' }}M</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">剩余内存</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.mem">{{ server.mem.free || '--' }}M</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.jvm">{{ server.jvm.free || '--' }}M</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">使用率</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.mem">{{ server.mem.usage || '--' }}%</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.jvm">{{ server.jvm.usage || '--' }}%</div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <!-- CPU使用率图表 -->
      <el-col :span="12" class="card-box">
        <el-card>
          <div slot="header">
            <span>CPU使用率</span>
          </div>
          <div class="echarts-container">
            <div ref="cpuChart" style="height: 300px;"></div>
          </div>
        </el-card>
      </el-col>

      <!-- 内存使用率图表 -->
      <el-col :span="12" class="card-box">
        <el-card>
          <div slot="header">
            <span>内存使用率</span>
          </div>
          <div class="echarts-container">
            <div ref="memoryChart" style="height: 300px;"></div>
          </div>
        </el-card>
      </el-col>

      <!-- 服务器信息 -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header"><span><i class="el-icon-monitor"></i> 服务器信息</span></div>
          <div class="el-table el-table--enable-row-hover el-table--medium">
            <table cellspacing="0" style="width: 100%;">
              <tbody>
                <tr>
                  <td class="el-table__cell is-leaf" style="width: 25%"><div class="cell">服务器名称</div></td>
                  <td class="el-table__cell is-leaf" style="width: 25%"><div class="cell" v-if="server.sys">{{ server.sys.computerName || '--' }}</div></td>
                  <td class="el-table__cell is-leaf" style="width: 25%"><div class="cell">操作系统</div></td>
                  <td class="el-table__cell is-leaf" style="width: 25%"><div class="cell" v-if="server.sys">{{ server.sys.osName || '--' }}</div></td>
                </tr>
                <tr>
                  <td class="el-table__cell is-leaf"><div class="cell">服务器IP</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.sys">{{ server.sys.computerIp || '--' }}</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell">系统架构</div></td>
                  <td class="el-table__cell is-leaf"><div class="cell" v-if="server.sys">{{ server.sys.osArch || '--' }}</div></td>
                </tr>
              </tbody>
            </table>
          </div>
        </el-card>
      </el-col>

      <!-- 磁盘状态 -->
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
                      <el-progress :percentage="parseFloat(sysFile.usage)" :color="sysFile.usage > 80 ? 'red' : sysFile.usage > 60 ? '#E6A23C' : '#67C23A'"></el-progress>
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
                    <td class="el-table__cell is-leaf" style="width: 15%"><div class="cell">核心线程数</div></td>
                    <td class="el-table__cell is-leaf" style="width: 35%"><div class="cell">{{ pool.corePoolSize }}</div></td>
                    <td class="el-table__cell is-leaf" style="width: 15%"><div class="cell">最大线程数</div></td>
                    <td class="el-table__cell is-leaf" style="width: 35%"><div class="cell">{{ pool.maximumPoolSize }}</div></td>
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
            <el-table :data="formattedClusterThreadPoolInfo" style="width: 100%">
              <el-table-column prop="nodeId" label="节点标识" />
              <el-table-column prop="name" label="线程池名称" />
              <el-table-column prop="corePoolSize" label="核心线程数" />
              <el-table-column prop="maximumPoolSize" label="最大线程数" />
              <el-table-column prop="activeCount" label="当前活跃线程数" />
              <el-table-column prop="poolSize" label="当前池大小" />
              <el-table-column prop="completedTaskCount" label="已完成任务数" />
              <el-table-column prop="queueSize" label="队列大小" />
              <el-table-column prop="queueRemainingCapacity" label="队列剩余容量" />
              <el-table-column prop="taskCount" label="总任务数" />
            </el-table>
          </div>
        </el-card>
      </el-col>

      <!-- 集群节点资源使用情况 -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span>集群节点资源使用情况</span>
          </div>
          <el-row :gutter="20">
            <el-col :span="12">
               <div style="text-align: center; margin-bottom: 10px; font-weight: bold;">CPU使用率</div>
               <div ref="clusterCpuChart" style="height: 300px;"></div>
            </el-col>
            <el-col :span="12">
               <div style="text-align: center; margin-bottom: 10px; font-weight: bold;">内存使用率</div>
               <div ref="clusterMemoryChart" style="height: 300px;"></div>
            </el-col>
            <el-col :span="12" style="margin-top: 20px;">
               <div style="text-align: center; margin-bottom: 10px; font-weight: bold;">活跃线程数</div>
               <div ref="clusterThreadChart" style="height: 300px;"></div>
            </el-col>
             <el-col :span="12" style="margin-top: 20px;">
               <div style="text-align: center; margin-bottom: 10px; font-weight: bold;">网络流量</div>
               <div ref="clusterNetworkChart" style="height: 300px;"></div>
            </el-col>
          </el-row>
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
            <el-table :data="clusterServerInfoList" style="width: 100%">
              <el-table-column prop="nodeId" label="节点标识" />
              <el-table-column label="CPU核心数">
                <template slot-scope="scope">{{ scope.row.cpu && scope.row.cpu.cpuNum || '-' }}</template>
              </el-table-column>
              <el-table-column label="CPU使用率">
                <template slot-scope="scope">
                   <div class="cell">{{ scope.row.cpu && scope.row.cpu.used ? scope.row.cpu.used + '%' : '-' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="内存总量">
                 <template slot-scope="scope">
                   <div class="cell">{{ scope.row.mem && scope.row.mem.total ? (scope.row.mem.total / 1024).toFixed(2) + 'G' : '-' }}</div>
                </template>
              </el-table-column>
              <el-table-column label="内存使用率">
                 <template slot-scope="scope">
                   <div class="cell">{{ scope.row.mem && scope.row.mem.usage ? scope.row.mem.usage + '%' : '-' }}</div>
                 </template>
              </el-table-column>
               <el-table-column label="JVM使用率">
                 <template slot-scope="scope">
                   <div class="cell">{{ scope.row.jvm && scope.row.jvm.usage ? scope.row.jvm.usage + '%' : '-' }}</div>
                 </template>
              </el-table-column>
              <el-table-column label="操作系统">
                 <template slot-scope="scope">
                   <div class="cell">{{ scope.row.sys && scope.row.sys.osName || '-' }}</div>
                 </template>
              </el-table-column>
              <el-table-column label="磁盘使用率">
                <template slot-scope="scope">
                  <div class="cell" v-if="scope.row.sysFiles && scope.row.sysFiles.length > 0">
                    <div>{{ getHighestDiskUsage(scope.row.sysFiles) }}</div>
                  </div>
                  <div class="cell" v-else>-</div>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-card>
      </el-col>

      <!-- Queue Monitor Section -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span><i class="el-icon-s-order"></i> 任务队列实时监控</span>
            <el-button
              style="float: right; margin-top: -5px;"
              size="mini"
              @click="getQueueInfo"
            >
              刷新
            </el-button>
          </div>
          <el-table :data="queueData" style="width: 100%" v-loading="queueLoading" stripe border height="400" empty-text="暂无任务运行数据">
             <el-table-column prop="jobId" label="任务ID" width="100" show-overflow-tooltip></el-table-column>
             <el-table-column prop="jobName" label="任务名称" width="200" show-overflow-tooltip></el-table-column>
             <el-table-column prop="jobGroup" label="任务分组" width="120" show-overflow-tooltip></el-table-column>
             <el-table-column prop="executionId" label="执行实例ID" width="280" show-overflow-tooltip></el-table-column>
             <el-table-column prop="displayStatus" label="任务状态" width="140" align="center">
                <template slot-scope="scope">
                   <el-tag v-if="scope.row.displayStatus === 'NOT_ENQUEUED'" type="danger">NOT_ENQUEUED</el-tag>
                   <el-tag v-else-if="scope.row.displayStatus === 'RUNNING'" type="primary">RUNNING</el-tag>
                   <el-tag v-else-if="scope.row.displayStatus === 'WAITING'" type="warning">WAITING</el-tag>
                   <el-tag v-else-if="scope.row.status === 'FAILED'" type="danger">FAILED</el-tag>
                   <el-tag v-else type="info">{{ scope.row.displayStatus || scope.row.status }}</el-tag>
                </template>
             </el-table-column>
             <el-table-column prop="nodeId" label="执行节点" width="140" align="center"></el-table-column>
             <el-table-column label="计划执行时间" width="160" align="center">
               <template slot-scope="scope">
                  {{ formatDate(scope.row.scheduledTime) }}
               </template>
             </el-table-column>
             <el-table-column label="入队时间" width="160" align="center">
               <template slot-scope="scope">
                  {{ formatDate(scope.row.enqueueTime) }}
               </template>
             </el-table-column>
             <el-table-column label="开始时间" width="160" align="center">
               <template slot-scope="scope">
                  {{ formatDate(scope.row.startTime) }}
               </template>
             </el-table-column>
             <el-table-column label="重试次数" width="100" align="center">
                <template slot-scope="scope">
                   {{ scope.row.retryCount }} / {{ scope.row.maxRetry }}
                </template>
             </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- Stock Task Monitor Section -->
      <el-col :span="24" class="card-box">
        <el-card>
          <div slot="header">
            <span><i class="el-icon-s-data"></i> 股票刷新任务监控</span>
          </div>
          <stock-task-table />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import StockTaskTable from "@/views/monitor/stockTask/StockTaskTable";
import { getServer, getClusterThreadPool, getClusterThreadPoolRedis, getClusterServerRedis } from "@/api/monitor/server";
import request from '@/utils/request';
import * as echarts from "echarts";

// API function for queue details
function getQueueDetails() {
  return request({
    url: '/quartz/runtime/detail',
    method: 'get'
  })
}

export default {
  name: "Server",
  components: {
    StockTaskTable
  },
  data() {
    return {
      // Queue Data
      queueData: [],
      queueLoading: false,

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
      // 集群线程池信息
      clusterThreadPoolInfo: {},
      // 集群服务器信息
      clusterServerInfo: {},
      // 定时器
      timer: null,
      clusterInfoTimer: null,
    queueTimer: null,
      // 是否使用Redis模式
      useRedis: true,
      // 图表实例
      cpuChart: null,
      memoryChart: null,
      clusterCpuChart: null,
      clusterMemoryChart: null,
      clusterThreadChart: null,
      clusterNetworkChart: null,
      // 图表历史数据
      cpuHistory: [],
      memoryHistory: [],
      clusterChartHistory: {}
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
    },
    clusterServerInfoList() {
      const list = [];
      for (const nodeId in this.clusterServerInfo) {
        list.push({
          nodeId,
          ...this.clusterServerInfo[nodeId]
        });
      }
      return list;
    }
  },
  created() {
    this.openLoading();
    this.getList();
    this.getClusterInfo();
    this.getQueueInfo();
    this.timer = setInterval(this.getList, 5000);
    this.clusterInfoTimer = setInterval(this.getClusterInfo, 5000);
    this.queueTimer = setInterval(this.getQueueInfo, 5000);
  },
  mounted() {
    this.initCharts();
  },
  beforeDestroy() {
    clearInterval(this.timer);
    clearInterval(this.clusterInfoTimer);
    if (this.queueTimer) clearInterval(this.queueTimer);
    if (this.cpuChart) this.cpuChart.dispose();
    if (this.memoryChart) this.memoryChart.dispose();
    if (this.clusterCpuChart) this.clusterCpuChart.dispose();
    if (this.clusterMemoryChart) this.clusterMemoryChart.dispose();
    if (this.clusterThreadChart) this.clusterThreadChart.dispose();
    if (this.clusterNetworkChart) this.clusterNetworkChart.dispose();
  },
  methods: {
    getList() {
      getServer().then(response => {
        this.server = response.data || {};
        this.$modal.closeLoading();

        const now = new Date().toLocaleTimeString().substring(3); // HH:mm:ss -> mm:ss
        if (this.server.cpu) {
            this.cpuHistory.push({ time: now, usage: parseFloat(this.server.cpu.used) });
            if (this.cpuHistory.length > 20) this.cpuHistory.shift();
        }
        if (this.server.mem) {
            this.memoryHistory.push({ time: now, usage: parseFloat(this.server.mem.usage) });
            if (this.memoryHistory.length > 20) this.memoryHistory.shift();
        }
        this.updateSingleNodeCharts();
      }).catch(error => {
        this.$modal.closeLoading();
        if (error.response) {
            if (error.response.status === 404) {
                 this.$modal.msgError("监控服务未注册或接口不存在");
            } else if (error.response.status === 500) {
                 this.$modal.msgError("监控服务异常，请联系管理员");
            }
        }
        this.server = { cpu: {}, mem: {}, jvm: {}, sys: {}, sysFiles: [], threadPoolInfo: { threadPools: [] } };
      });
    },
    getClusterInfo() {
      const apiCall = this.useRedis ? getClusterThreadPoolRedis : getClusterThreadPool;
      Promise.all([apiCall(), getClusterServerRedis()]).then(([threadPoolResponse, serverResponse]) => {
        this.clusterThreadPoolInfo = threadPoolResponse.data;
        this.clusterServerInfo = serverResponse.data;
        this.updateClusterCharts();
      }).catch(() => {
          // Silent catch for periodic refresh, or log to console
          console.error("Cluster info refresh failed");
      });
    },
    handleModeChange() {
      this.clusterChartHistory = {}; // Reset history on mode change
      this.getClusterInfo();
    },
    openLoading() {
      this.$modal.loading("正在加载服务监控数据，请稍候！");
    },
    initCharts() {
      // Single Node Charts
      this.cpuChart = echarts.init(this.$refs.cpuChart);
      this.memoryChart = echarts.init(this.$refs.memoryChart);

      const commonOption = {
        tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255, 255, 255, 0.9)',
            borderColor: '#ccc',
            borderWidth: 1,
            textStyle: { color: '#333' }
        },
        xAxis: {
            type: 'category',
            data: [],
            axisLine: { lineStyle: { color: '#E4E7ED' } },
            axisLabel: { color: '#606266' }
        },
        yAxis: {
            type: 'value',
            min: 0,
            max: 100,
            axisLabel: { formatter: '{value} %', color: '#606266' },
            splitLine: { lineStyle: { type: 'dashed', color: '#E4E7ED' } }
        },
        series: [{
            data: [],
            type: 'line',
            smooth: true,
            symbol: 'none',
            lineStyle: { width: 3 },
            areaStyle: { opacity: 0.2 }
        }],
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true }
      };

      // CPU Chart specific
      const cpuOption = JSON.parse(JSON.stringify(commonOption));
      cpuOption.series[0].itemStyle = { color: '#409EFF' };
      cpuOption.series[0].areaStyle.color = new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
          offset: 0, color: 'rgba(64, 158, 255, 0.5)'
      }, {
          offset: 1, color: 'rgba(64, 158, 255, 0.05)'
      }]);
      this.cpuChart.setOption(cpuOption);

      // Memory Chart specific
      const memOption = JSON.parse(JSON.stringify(commonOption));
      memOption.series[0].itemStyle = { color: '#67C23A' };
      memOption.series[0].areaStyle.color = new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
          offset: 0, color: 'rgba(103, 194, 58, 0.5)'
      }, {
          offset: 1, color: 'rgba(103, 194, 58, 0.05)'
      }]);
      this.memoryChart.setOption(memOption);

      // Cluster Charts
      this.clusterCpuChart = echarts.init(this.$refs.clusterCpuChart);
      this.clusterMemoryChart = echarts.init(this.$refs.clusterMemoryChart);
      this.clusterThreadChart = echarts.init(this.$refs.clusterThreadChart);
      this.clusterNetworkChart = echarts.init(this.$refs.clusterNetworkChart);

      const clusterCommonOption = {
          tooltip: { trigger: 'axis' },
          legend: { data: [], top: 'bottom' },
          xAxis: { type: 'category', boundaryGap: false, data: [] },
          yAxis: { type: 'value' },
          series: [],
          grid: { left: '3%', right: '4%', bottom: '10%', containLabel: true }
      };
      this.clusterCpuChart.setOption(JSON.parse(JSON.stringify(clusterCommonOption)));
      this.clusterMemoryChart.setOption(JSON.parse(JSON.stringify(clusterCommonOption)));
      this.clusterThreadChart.setOption(JSON.parse(JSON.stringify(clusterCommonOption)));
      this.clusterNetworkChart.setOption(JSON.parse(JSON.stringify(clusterCommonOption)));
    },
    updateSingleNodeCharts() {
      if (!this.cpuChart || !this.memoryChart) return;

      const times = this.cpuHistory.map(h => h.time);

      this.cpuChart.setOption({
        xAxis: { data: times },
        series: [{ data: this.cpuHistory.map(h => h.usage) }]
      });

      this.memoryChart.setOption({
        xAxis: { data: times },
        series: [{ data: this.memoryHistory.map(h => h.usage) }]
      });
    },
    updateClusterCharts() {
      if (!this.clusterServerInfo || Object.keys(this.clusterServerInfo).length === 0) return;

      const nodeIds = Object.keys(this.clusterServerInfo);
      const latestTime = new Date().toLocaleTimeString().substring(3);

      nodeIds.forEach(id => {
          if (!this.clusterChartHistory[id]) {
               this.$set(this.clusterChartHistory, id, {
                  time: [], cpu: [], memory: [], threads: [], network: []
              });
          }
          const history = this.clusterChartHistory[id];
          const server = this.clusterServerInfo[id];

          history.time.push(latestTime);
          history.cpu.push(server.cpu ? server.cpu.used : 0);
          history.memory.push(server.mem ? parseFloat(server.mem.usage) : 0);

          let totalActiveThreads = 0;
          if (this.clusterThreadPoolInfo && this.clusterThreadPoolInfo[id]) {
              totalActiveThreads = this.clusterThreadPoolInfo[id].reduce((sum, pool) => sum + pool.activeCount, 0);
          }
          history.threads.push(totalActiveThreads);

          // Calculate total network traffic (in + out)
          let totalNetworkTraffic = 0;
          if (server.networkTraffic) {
              // networkTraffic is a List of interfaces in JSON
              const interfaces = server.networkTraffic;
              if (Array.isArray(interfaces)) {
                  totalNetworkTraffic = interfaces.reduce((sum, iface) => sum + (iface.receiveRate || 0) + (iface.sendRate || 0), 0);
              }
          }
          history.network.push(totalNetworkTraffic);

          if (history.time.length > 20) {
              history.time.shift();
              history.cpu.shift();
              history.memory.shift();
              history.threads.shift();
              history.network.shift();
          }
      });

      const times = this.clusterChartHistory[nodeIds[0]] ? this.clusterChartHistory[nodeIds[0]].time : [];

      this.clusterCpuChart.setOption({
          legend: { data: nodeIds },
          xAxis: { data: times },
          yAxis: { axisLabel: { formatter: '{value} %' } },
          series: nodeIds.map(id => ({ name: id, type: 'line', data: this.clusterChartHistory[id].cpu }))
      });

      this.clusterMemoryChart.setOption({
          legend: { data: nodeIds },
          xAxis: { data: times },
          yAxis: { axisLabel: { formatter: '{value} %' } },
          series: nodeIds.map(id => ({ name: id, type: 'line', data: this.clusterChartHistory[id].memory }))
      });

      this.clusterThreadChart.setOption({
          legend: { data: nodeIds },
          xAxis: { data: times },
          yAxis: { name: 'Active Threads' },
          series: nodeIds.map(id => ({ name: id, type: 'line', data: this.clusterChartHistory[id].threads }))
      });

      // Update Network Chart
      this.clusterNetworkChart.setOption({
          title: { text: '' }, // Clear "Not Implemented" text
          legend: { data: nodeIds },
          tooltip: {
            trigger: 'axis',
            formatter: function(params) {
                let result = params[0].name + '<br/>';
                params.forEach(param => {
                    let value = param.value;
                    let unit = 'B/s';
                    if (value > 1024 * 1024) {
                        value = (value / (1024 * 1024)).toFixed(2);
                        unit = 'MB/s';
                    } else if (value > 1024) {
                        value = (value / 1024).toFixed(2);
                        unit = 'KB/s';
                    }
                    result += param.marker + param.seriesName + ': ' + value + ' ' + unit + '<br/>';
                });
                return result;
            }
          },
          xAxis: { data: times },
          yAxis: {
            name: 'Traffic Rate',
            axisLabel: {
                formatter: function (value) {
                    if (value > 1024 * 1024) return (value / (1024 * 1024)).toFixed(1) + ' MB/s';
                    if (value > 1024) return (value / 1024).toFixed(1) + ' KB/s';
                    return value + ' B/s';
                }
            }
          },
          series: nodeIds.map(id => ({ name: id, type: 'line', data: this.clusterChartHistory[id].network }))
      });
    },
    getHighestDiskUsage(sysFiles) {
      if (!sysFiles || sysFiles.length === 0) return '-';
      let highestUsageFile = sysFiles.reduce((max, file) => parseFloat(file.usage) > parseFloat(max.usage) ? file : max, sysFiles[0]);
      return `${highestUsageFile.dirName}: ${highestUsageFile.usage}%`;
    },
    getQueueInfo() {
      // Avoid spinner flicker on refresh
      // this.queueLoading = true;
      getQueueDetails().then(response => {
        this.queueData = response.data || response || [];
        // this.queueLoading = false;
      }).catch(() => {
        this.queueData = []; // Clear data on error
        // this.queueLoading = false;
      });
    },
    formatDate(timestamp) {
        if (!timestamp) return '-';
        return new Date(timestamp).toLocaleString();
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
