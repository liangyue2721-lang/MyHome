<template>
  <div class="app-container home">
    <!-- Wealth Stage Bar (Horizontal Visualization) -->
    <el-row class="wealth-stage-row" v-if="wealthStage.current">
      <el-col :span="24">
        <el-card shadow="never" class="wealth-stage-card">
          <!-- Summary Header -->
          <div class="stage-summary-header">
            <div class="current-assets">
              <span class="label">当前年度资产</span>
              <span class="amount">¥ {{ wealthStage.totalAssets.toLocaleString() }}</span>
            </div>

            <div class="stage-gap" v-if="wealthStage.next">
              <span class="gap-label">距离 <span class="next-name">{{ wealthStage.next.name }}</span> 还需</span>
              <span class="gap-amount">¥ {{ wealthStage.gap.toLocaleString() }}</span>
              <el-progress
                :percentage="wealthStage.progress"
                :show-text="false"
                :stroke-width="6"
                color="#67C23A"
                class="mini-progress"
              ></el-progress>
            </div>
            <div class="stage-gap success" v-else>
              <i class="el-icon-medal"></i> 已登峰造极
            </div>
          </div>

          <!-- Horizontal Stages -->
          <div class="stages-container">
            <div
              v-for="(stage, index) in allStages"
              :key="index"
              class="stage-item"
              :class="{
                'is-completed': wealthStage.totalAssets >= stage.max,
                'is-current': wealthStage.totalAssets >= stage.min && wealthStage.totalAssets < stage.max,
                'is-future': wealthStage.totalAssets < stage.min
              }"
            >
              <div class="stage-bar"></div>
              <div
                class="stage-dot"
                :style="wealthStage.totalAssets >= stage.min && wealthStage.totalAssets < stage.max ? { borderColor: stage.customColor, backgroundColor: stage.customColor } : {}"
              >
                <!-- Completed: Show Icon (Inherits Green from CSS) -->
                <i :class="stage.icon" v-if="wealthStage.totalAssets >= stage.max" style="font-weight: bold;"></i>
                <!-- Current: Show Icon (White on Custom Background) -->
                <i :class="stage.icon" v-else-if="wealthStage.totalAssets >= stage.min && wealthStage.totalAssets < stage.max" style="color: #fff; font-size: 16px;"></i>
                <!-- Future: Show Icon (Gray) -->
                <i :class="stage.icon" v-else style="color: #C0C4CC; font-size: 14px;"></i>
              </div>
              <div class="stage-content">
                <div class="stage-name" :style="wealthStage.totalAssets >= stage.min && wealthStage.totalAssets < stage.max ? { color: stage.customColor, fontWeight: 'bold' } : {}">{{ stage.name }}</div>
                <div class="stage-range">{{ formatMoney(stage.min) }}</div>
              </div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Task Status Panel (New) -->
    <el-row :gutter="20" class="task-status-row">
      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>待执行任务数</span>
          </div>
          <div class="card-body">
            <span class="count-text pending">{{ taskStats.pending }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>已执行任务数</span>
          </div>
          <div class="card-body">
            <span class="count-text completed">{{ taskStats.completed }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>执行中任务</span>
          </div>
          <div class="card-body">
            <span class="count-text executing">{{ taskStats.executing }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>执行中占比</span>
          </div>
          <div class="card-body chart-container">
            <el-progress
              type="circle"
              class="progress-ring"
              :percentage="executingPercentage"
              :width="80"
              :color="customColors"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Original Charts -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>📈 利润趋势分析</span>
            <el-tag size="small" effect="plain">历史数据</el-tag>
          </div>
          <div id="profitLineChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>💳 近一年还贷对比</span>
          </div>
          <div id="generateMonthlyLoanRepaymentBarChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>💰 月度收支对比</span>
          </div>
          <div id="monthlyIncomeExpenseBarChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import request from '@/utils/request'
import * as echarts from 'echarts';
import 'echarts-liquidfill';
import {
  getTotalAmountChart,
  getMonthlyIncomeBarChart,
  getTotalRepaymentPieChart,
  getWechatAlipayData,
  getYearIncomeExpenseRatio,
  getProfitLineData,
  renderLoanRepaymentComparisonChart
} from "@/api/finance/pieChart";
import { getAnnualSummary } from "@/api/finance/annual_deposit_summary";
import {listUser} from "@/api/stock/dropdown_component";
import Cookies from 'js-cookie';

const WEALTH_STAGES = [
  { name: '负债阶段', min: -Infinity, max: 0, desc: '需优化债务结构', icon: 'el-icon-bottom-right', customColor: '#F56C6C' },
  { name: '生存艰难', min: 0, max: 27000, desc: '维持基本生存', icon: 'el-icon-sunny', customColor: '#67C23A' },
  { name: '贫穷阶段', min: 27000, max: 60000, desc: '积累原始资本', icon: 'el-icon-coin', customColor: '#909399' },
  { name: '低收入阶段', min: 60000, max: 150000, desc: '提升主动收入', icon: 'el-icon-suitcase', customColor: '#E6A23C' },
  { name: '中下产阶段', min: 150000, max: 300000, desc: '建立安全缓冲', icon: 'el-icon-umbrella', customColor: '#409EFF' },
  { name: '中产阶段', min: 300000, max: 500000, desc: '资产稳步增长', icon: 'el-icon-top-right', customColor: '#67C23A' },
  { name: '中上产阶段', min: 500000, max: 1000000, desc: '多元化投资布局', icon: 'el-icon-pie-chart', customColor: '#1890FF' },
  { name: '富人阶段', min: 1000000, max: 8000000, desc: '实现财务自由', icon: 'el-icon-trophy', customColor: '#722ED1' },
  { name: '富豪阶段', min: 8000000, max: 20000000, desc: '资产传承规划', icon: 'el-icon-school', customColor: '#C71585' },
  { name: '大富豪阶段', min: 20000000, max: Infinity, desc: '社会影响力构建', icon: 'el-icon-s-cooperation', customColor: '#FFD700' }
];

export default {
  name: 'Index', // Changed from 'Charts' to 'Index' to match usage
  data() {
    return {
      // Wealth Stage Data
      wealthStage: {
        current: null,
        next: null,
        totalAssets: 0,
        progress: 0,
        gap: 0,
        loading: false
      },
      allStages: WEALTH_STAGES,
      // Task Stats Data
      taskStats: {
        pending: 0,
        completed: 0,
        executing: 0
      },
      customColors: [
        { color: '#f56c6c', percentage: 20 },
        { color: '#e6a23c', percentage: 40 },
        { color: '#5cb87a', percentage: 60 },
        { color: '#1989fa', percentage: 80 },
        { color: '#6f7ad3', percentage: 100 }
      ],

      // Chart Data
      colors: [
        '#5470c6', '#91cc75', '#fac858', '#ee6666', '#73c0de', '#3ba272', '#fc8452', '#9a60b4', '#ea7ccc'
      ],
      pageSize: 1000,
      userLoading: false,
      userList: [],
      selectedUserId: null,
      charts: {
        transactionType: null,
        monthlyConsumption: null,
        monthlyIncomeExpense: null,
        generateMonthlyLoanRepayment: null,
        totalRepayment: null,
        expenseLiquid: null,
        incomeLiquid: null,
        profitLine: null,
      },
    };
  },
  computed: {
    executingPercentage() {
      const total = this.taskStats.pending + this.taskStats.completed + this.taskStats.executing;
      if (total === 0) return 0;
      return Math.round((this.taskStats.executing / total) * 100);
    }
  },
  mounted() {
    // Load Wealth Stage
    this.fetchWealthStage();

    // Load Task Stats
    this.fetchTaskStats();

    // Load Charts
    this.initUserList().then(() => {
      this.loadAllCharts(this.selectedUserId);
      window.addEventListener('resize', this.resizeCharts);
    });
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    this.disposeCharts();
  },
  methods: {
    // --- Wealth Stage Method ---
    fetchWealthStage() {
      this.wealthStage.loading = true;
      getAnnualSummary().then(response => {
        const payload = response.data || response;
        if (payload) {
          const totalAssets = Number(payload.totalDeposit) || 0;
          this.wealthStage.totalAssets = totalAssets;

          // Find Current Stage
          let stageIndex = WEALTH_STAGES.findIndex(s => totalAssets >= s.min && totalAssets < s.max);
          // Handle edge case for max value (Infinity)
          if (stageIndex === -1) {
             if (totalAssets >= WEALTH_STAGES[WEALTH_STAGES.length - 1].min) {
                 stageIndex = WEALTH_STAGES.length - 1;
             } else {
                 stageIndex = 0; // Fallback
             }
          }

          this.wealthStage.current = WEALTH_STAGES[stageIndex];

          // Calculate Progress & Gap
          if (stageIndex < WEALTH_STAGES.length - 1) {
            this.wealthStage.next = WEALTH_STAGES[stageIndex + 1];
            const currentMin = this.wealthStage.current.min === -Infinity ? 0 : this.wealthStage.current.min;
            const currentMax = this.wealthStage.current.max;

            // Avoid division by zero
            const range = currentMax - currentMin;
            const effectiveAssets = totalAssets < currentMin ? currentMin : totalAssets; // Handle negative assets in negative stage logic if needed, simplified here

            this.wealthStage.gap = currentMax - totalAssets;
            this.wealthStage.progress = range > 0
                ? Math.min(100, Math.max(0, ((totalAssets - currentMin) / range) * 100))
                : 100;
          } else {
            // Top Stage
            this.wealthStage.next = null;
            this.wealthStage.gap = 0;
            this.wealthStage.progress = 100;
          }
        }
      }).catch(err => {
        console.error("Failed to fetch wealth stage", err);
      }).finally(() => {
        this.wealthStage.loading = false;
      });
    },

    // --- Task Stats Method ---
    fetchTaskStats() {
      request({
        url: '/quartz/runtime/overview',
        method: 'get'
      }).then(response => {
        // Handle various response formats:
        // Backend returns: AjaxResult.success(data), where data = { taskStats: {...}, executingPercentage: ... }
        // request interceptor usually returns 'response.data' directly if success
        const payload = response.data || response;

        if (payload) {
             if (payload.taskStats) {
                 this.taskStats = payload.taskStats;
             }
             // executingPercentage is calculated in computed property based on taskStats,
             // but if backend provides it, we could use it too.
             // The computed property 'executingPercentage' logic below is:
             // Math.round((this.taskStats.executing / total) * 100);
             // Backend also returns 'executingPercentage', but computed is safer for reactivity if taskStats updates.
        }
      }).catch(error => {
        console.error("Failed to fetch task stats", error);
        // Reset to 0 on error
        this.taskStats = { pending: 0, completed: 0, executing: 0 };
      });
    },

    // --- Chart Methods (Preserved) ---
    async initUserList() {
      this.userLoading = true;
      try {
        const response = await listUser({pageSize: this.pageSize});
        const payload = response.data || response;
        const rawUsers = Array.isArray(payload.rows) ? payload.rows : Array.isArray(payload) ? payload : [];
        this.userList = rawUsers.map(u => ({
          id: u.userId,
          name: u.userName || u.nickName || `用户${u.userId}`
        }));
        if (this.userList.length) {
          const savedUsername = Cookies.get('username');
          const matchedUser = this.userList.find(u => u.name === savedUsername);
          this.selectedUserId = matchedUser ? matchedUser.id : this.userList[0].id;
        } else {
          this.selectedUserId = null;
        }
      } catch (err) {
        console.error('用户列表加载失败:', err);
      } finally {
        this.userLoading = false;
      }
    },
    handleUserChange() {
      this.disposeCharts();
      this.loadAllCharts();
    },
    disposeCharts() {
      Object.values(this.charts).forEach(chart => chart && chart.dispose());
    },
    formatMoney(val) {
      if (val === -Infinity) return '< 0';
      if (val === Infinity) return '> 2000w';
      if (val >= 10000) return (val / 10000).toFixed(0) + '万';
      return val;
    },
    resizeCharts() {
      Object.values(this.charts).forEach(chart => chart && chart.resize());
    },
    initChart(key, domId) {
      const dom = document.getElementById(domId);
      if (!dom) return null;
      if (this.charts[key]) this.charts[key].dispose();
      this.charts[key] = echarts.init(dom);
      return this.charts[key];
    },

    loadAllCharts(selectedUserId) {
      this.loadPieChart('transactionType', 'clientPieChart', () => getWechatAlipayData(selectedUserId), '交易类型', '个');
      this.loadBarChart('monthlyConsumption', 'monthlyConsumptionColumnChart', () => getTotalAmountChart(selectedUserId), '每月支出', '元');
      this.loadMixedChart('monthlyIncomeExpense', 'monthlyIncomeExpenseBarChart', () => getMonthlyIncomeBarChart(selectedUserId), '每月收支', '元', ['收入', '支出', '结余']);
      this.loadMixedChart('generateMonthlyLoanRepayment', 'generateMonthlyLoanRepaymentBarChart', () => renderLoanRepaymentComparisonChart(selectedUserId), '还贷本息', '元', ['贷款偿还']);
      this.loadHeartProgressChart('totalRepayment', 'totalRepaymentPieChart', () => getTotalRepaymentPieChart(selectedUserId));
      this.loadLiquidChart('expenseLiquid', 'expenseLiquidChart', () => getYearIncomeExpenseRatio(selectedUserId), '支出');
      this.loadLiquidChart('incomeLiquid', 'incomeLiquidChart', () => getYearIncomeExpenseRatio(selectedUserId), '结余');
      this.loadLineChart('profitLine', 'profitLineChart', () => getProfitLineData(selectedUserId));
    },

    loadPieChart(key, domId, apiFn, title, unit) {
      apiFn({userId: this.selectedUserId}).then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;

        const seriesData = data.map((i) => ({
          name: i.category,
          value: i.amount,
        }));

        chart.setOption({
          title: {show: false},
          tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            textStyle: {color: '#333'},
            formatter: (params) => {
              return `
                <div style="font-size:14px; font-weight:bold; margin-bottom:5px;">${params.name}</div>
                <div style="display:flex; justify-content:space-between; min-width:120px;">
                  <span>金额:</span>
                  <span style="font-weight:bold; color:${params.color}">${params.value} ${unit}</span>
                </div>
                <div style="display:flex; justify-content:space-between; margin-top:3px;">
                  <span>占比:</span>
                  <span>${params.percent}%</span>
                </div>
              `;
            }
          },
          legend: {
            type: 'scroll',
            orient: 'horizontal',
            bottom: 0,
            itemWidth: 10, itemHeight: 10
          },
          series: [{
            name: title,
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '45%'],
            itemStyle: {
              borderRadius: 8,
              borderColor: '#fff',
              borderWidth: 2
            },
            emphasis: {
              scale: true,
              scaleSize: 10,
              label: {
                show: true,
                fontSize: 18,
                fontWeight: 'bold',
                color: '#333',
                formatter: `{b}\n{c} ${unit}`
              }
            },
            label: {show: false, position: 'center'},
            data: seriesData
          }]
        });
      }).catch(e => console.error(e));
    },

    loadBarChart(key, domId, apiFn, title, unit) {
      apiFn({userId: this.selectedUserId}).then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;

        const getGradient = (start, end) => new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
          offset: 0,
          color: start
        }, {offset: 1, color: end}]);

        chart.setOption({
          grid: {top: 40, left: '3%', right: '4%', bottom: '10%', containLabel: true},
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255,255,255,0.95)',
            formatter: (params) => {
              let html = `<div style="margin-bottom:5px;font-weight:bold;border-bottom:1px solid #eee;padding-bottom:5px;">${params[0].axisValue}</div>`;
              params.forEach(item => {
                const color = item.color.colorStops ? item.color.colorStops[0].color : item.color;
                html += `
                  <div style="display:flex; align-items:center; justify-content:space-between; margin-top:5px;">
                    <span style="display:flex; align-items:center;">
                      <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${color};margin-right:5px;"></span>
                      ${item.seriesName}
                    </span>
                    <span style="font-weight:bold; margin-left:15px;">${item.value} ${unit}</span>
                  </div>`;
              });
              return html;
            }
          },
          xAxis: {
            type: 'category',
            data: data.map(i => i.transactionTime),
            axisTick: {show: false},
            axisLine: {lineStyle: {color: '#ccc'}}
          },
          yAxis: {type: 'value', splitLine: {lineStyle: {type: 'dashed', color: '#eee'}}},
          series: [{
            type: 'bar',
            name: title,
            data: data.map(i => i.amount),
            barWidth: '50%',
            itemStyle: {
              borderRadius: [4, 4, 0, 0],
              color: getGradient('#3AA1FF', '#36D1DC')
            },
            emphasis: {
              focus: 'series',
              label: {
                show: true,
                position: 'top',
                formatter: `{c} ${unit}`,
                fontWeight: 'bold',
                color: '#3AA1FF'
              }
            }
          }]
        });
      }).catch(e => console.error(e));
    },

    loadMixedChart(key, domId, apiFn, title, unit, legendData = []) {
      apiFn({userId: this.selectedUserId}).then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;

        const xData = data.map(i => i.transactionTime);
        const series = [];

        const createSeries = (name, colorStart, colorEnd) => {
          let amountKey = '';
          if (name === '收入') amountKey = 'supportInAmount';
          else if (name === '支出' || name === '贷款偿还') amountKey = 'supportOutAmount';
          else if (name === '结余') amountKey = 'balanceAmount';

          series.push({
            name: name,
            type: 'bar',
            data: data.map(i => i[amountKey]),
            barWidth: '30%',
            itemStyle: {
              borderRadius: [4, 4, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {offset: 0, color: colorStart},
                {offset: 1, color: colorEnd}
              ])
            },
            emphasis: {
              focus: 'series',
              label: {
                show: true,
                position: 'top',
                formatter: `{c}`,
                color: colorStart,
                fontWeight: 'bold'
              }
            }
          });

          series.push({
            name: `${name}趋势`,
            type: 'line',
            data: data.map(i => i[amountKey]),
            smooth: true,
            symbol: 'none',
            lineStyle: {width: 3, color: colorStart},
            tooltip: {show: false}
          });
        };

        legendData.forEach(name => {
          if (name === '收入') createSeries('收入', '#67C23A', '#95D475');
          else if (name === '支出') createSeries('支出', '#F56C6C', '#FAB6B6');
          else if (name === '贷款偿还') createSeries('贷款偿还', '#E6A23C', '#F3D19E');
          else if (name === '结余') createSeries('结余', '#409EFF', '#79BBFF');
        });

        const legendNames = legendData;

        chart.setOption({
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255,255,255,0.95)',
            axisPointer: {type: 'shadow'},
            formatter: (params) => {
              let html = `<div style="font-weight:bold;margin-bottom:5px;">📅 ${params[0].axisValue}</div>`;
              params.filter(p => p.seriesType === 'bar').forEach(item => {
                let color = item.color;
                if (typeof color === 'object' && color.colorStops) color = color.colorStops[0].color;

                html += `
                  <div style="display:flex; justify-content:space-between; margin:3px 0;">
                    <span style="margin-right:15px;">
                      <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${color};margin-right:5px;"></span>
                      ${item.seriesName}
                    </span>
                    <span style="font-weight:bold;">${item.value} ${unit}</span>
                  </div>`;
              });
              return html;
            }
          },
          legend: {data: legendNames, top: 0},
          grid: {top: 40, left: '3%', right: '4%', bottom: 40, containLabel: true},
          xAxis: {type: 'category', data: xData, axisLine: {lineStyle: {color: '#ddd'}}},
          yAxis: {type: 'value', name: unit, splitLine: {lineStyle: {type: 'dashed', color: '#f0f0f0'}}},
          dataZoom: [{
            type: 'slider',
            height: 15,
            bottom: 5,
            borderColor: 'transparent',
            backgroundColor: '#f5f7fa',
            handleStyle: {color: '#409EFF'}
          }],
          series
        });
      }).catch(e => console.error(e));
    },

    loadLineChart(key, domId, apiFn) {
      apiFn({userId: this.selectedUserId}).then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;

        const xData = data.map(item => item.recordDate);
        const yData = data.map(item => item.profit);

        chart.setOption({
          backgroundColor: '#fff',
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255,255,255,0.95)',
            padding: 12,
            axisPointer: {type: 'cross', label: {backgroundColor: '#6a7985'}},
            formatter: (params) => {
              const p = params[0];
              return `
                <div style="font-weight:bold; margin-bottom:5px;">📅 ${p.axisValue}</div>
                <div style="display:flex; justify-content:space-between; align-items:center;">
                  <span>${p.marker} 利润</span>
                  <span style="font-weight:bold; color:#409EFF; margin-left:15px; font-size:16px;">${p.value} 元</span>
                </div>
              `;
            }
          },
          grid: {left: '3%', right: '4%', bottom: '3%', containLabel: true},
          xAxis: {type: 'category', boundaryGap: false, data: xData, axisLine: {lineStyle: {color: '#ccc'}}},
          yAxis: {type: 'value', splitLine: {lineStyle: {color: '#f0f0f0'}}},
          series: [{
            name: '利润',
            type: 'line',
            smooth: true,
            symbol: 'circle',
            symbolSize: 8,
            itemStyle: {color: '#409EFF', borderColor: '#fff', borderWidth: 2},
            lineStyle: {width: 3, color: '#409EFF'},
            areaStyle: {
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                {offset: 0, color: 'rgba(64, 158, 255, 0.4)'},
                {offset: 1, color: 'rgba(64, 158, 255, 0.05)'}
              ])
            },
            data: yData,
            markPoint: {
              data: [
                {type: 'max', name: '最高', label: {formatter: '{c}'}},
                {type: 'min', name: '最低', label: {formatter: '{c}'}}
              ]
            }
          }]
        });
      }).catch(e => console.error(e));
    },

    loadLiquidChart(key, domId, apiFn, categoryLabel) {
      apiFn({userId: this.selectedUserId}).then(raw => {
        if (!raw || !Array.isArray(raw)) return;

        const chart = this.initChart(key, domId);
        if (!chart) return;

        const keywords = [categoryLabel];
        if (categoryLabel === '结余') keywords.push('結余');

        const item = raw.find(i => keywords.some(k => i.category && i.category.includes(k)));

        const amount = item ? Number(item.amount) : 0;
        const total = raw.reduce((sum, i) => sum + Number(i.amount), 0);

        const ratio = total > 0 ? amount / total : 0;

        const color = categoryLabel.includes('支出')
          ? ['#F56C6C', 'rgba(245, 108, 108, 0.6)']
          : ['#67C23A', 'rgba(103, 194, 58, 0.6)'];

        chart.setOption({
          series: [{
            type: 'liquidFill',
            radius: '85%',
            center: ['50%', '50%'],
            data: [ratio, ratio > 0.1 ? ratio - 0.05 : ratio],
            color: color,
            backgroundStyle: {color: '#fff', borderWidth: 1, borderColor: '#e0e0e0'},
            label: {
              formatter: () => {
                return `${(ratio * 100).toFixed(1)}%\n${categoryLabel}`;
              },
              fontSize: 22,
              color: color[0],
              insideColor: '#fff',
              fontWeight: 'bold'
            },
            outline: {
              show: true,
              borderDistance: 4,
              itemStyle: {borderWidth: 2, borderColor: color[0]}
            }
          }],
          tooltip: {
            show: true,
            formatter: () => `${categoryLabel}: <b>${amount} 元</b><br/>总流动: ${total} 元`
          }
        });
      }).catch(e => console.error("水滴图加载失败:", e));
    },

    loadHeartProgressChart(key, domId, apiFn) {
      apiFn({userId: this.selectedUserId}).then(rawList => {
        if (!rawList || !Array.isArray(rawList)) {
          console.warn(`${key} API 返回数据为空或格式错误`, rawList);
          return;
        }

        const chart = this.initChart(key, domId);
        if (!chart) return;

        const findVal = (keywords) => {
          const item = rawList.find(i => {
            const cat = i.category || "";
            return keywords.some(k => cat.includes(k));
          });
          return item ? Number(item.amount) : 0;
        };

        const principalPaid = findVal(['已償還本金', '已偿还本金']);
        const principalUnpaid = findVal(['未還本金', '未还本金']);
        const interestPaid = findVal(['已償還利息', '已偿还利息']);
        const interestUnpaid = findVal(['未還利息', '未还利息']);

        const pTotal = principalPaid + principalUnpaid;
        const iTotal = interestPaid + interestUnpaid;

        const pPercent = pTotal > 0 ? +((principalPaid / pTotal) * 100).toFixed(1) : 0;
        const iPercent = iTotal > 0 ? +((interestPaid / iTotal) * 100).toFixed(1) : 0;

        const bgColor = '#FFE6EB';

        chart.setOption({
          grid: {left: '5%', right: '15%', top: '10%', bottom: '5%', containLabel: true},
          tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(255,255,255,0.98)',
            formatter: (params) => {
              const isInterest = params.dataIndex === 0;
              const type = isInterest ? '利息' : '本金';
              const paid = isInterest ? interestPaid : principalPaid;
              const unpaid = isInterest ? interestUnpaid : principalUnpaid;
              const percent = isInterest ? iPercent : pPercent;

              if (params.seriesName === '已偿还') {
                return `<div style="font-weight:bold">${type} - 已偿还</div>
                        <div>金额：${paid.toLocaleString()} 元</div>
                        <div>进度：${percent}%</div>`;
              } else {
                return `<div style="font-weight:bold">${type} - 未偿还</div>
                        <div>金额：${unpaid.toLocaleString()} 元</div>
                        <div>剩余：${(100 - percent).toFixed(1)}%</div>`;
              }
            }
          },
          xAxis: {max: 100, show: false},
          yAxis: {
            data: ['利息', '本金'],
            axisLine: {show: false},
            axisTick: {show: false},
            axisLabel: {fontWeight: 'bold', color: '#666', fontSize: 14}
          },
          series: [
            {
              name: '已偿还',
              type: 'bar',
              stack: 'total',
              data: [iPercent, pPercent],
              barWidth: 30,
              itemStyle: {
                borderRadius: [15, 0, 0, 15],
                color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{offset: 0, color: '#FF4D6D'}, {
                  offset: 1,
                  color: '#FF8FA3'
                }])
              },
              label: {
                show: true,
                position: 'inside',
                color: '#fff',
                fontWeight: 'bold',
                formatter: (p) => {
                  return p.value > 10 ? `${p.value}%` : '';
                }
              }
            },
            {
              name: '未偿还',
              type: 'bar',
              stack: 'total',
              data: [100 - iPercent, 100 - pPercent],
              barWidth: 30,
              itemStyle: {color: bgColor, borderRadius: [0, 15, 15, 0]},
              label: {show: false}
            },
            {
              type: 'scatter',
              data: [[iPercent, 0], [pPercent, 1]],
              symbolSize: 1,
              label: {
                show: true,
                position: 'right',
                offset: [-5, -2],
                formatter: '❤️',
                fontSize: 24,
                color: '#FF1E4D'
              },
              z: 10
            }
          ]
        });
      }).catch(e => console.error("贷款图表加载失败:", e));
    },
  }
};
</script>

<style lang="scss" scoped>

.home {
  .page-header {
    margin-bottom: 20px;
    border: none;
    .header-content {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .page-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: $text-primary;
    }
  }

  .el-row + .el-row {
    margin-top: 20px;
  }

  /* Wealth Stage Bar Styles */
  .wealth-stage-card {
    border-radius: 8px;
    background: #fff;

    .stage-summary-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 30px;
      padding-bottom: 15px;
      border-bottom: 1px solid #EBEEF5;

      .current-assets {
        .label {
          font-size: 14px;
          color: #909399;
          margin-right: 10px;
        }
        .amount {
          font-size: 28px;
          font-weight: bold;
          color: #303133;
        }
      }

      .stage-gap {
        display: flex;
        align-items: center;
        background: #fdf6ec;
        padding: 8px 15px;
        border-radius: 20px;
        color: #E6A23C;
        font-size: 13px;

        .gap-amount {
          font-weight: bold;
          margin: 0 10px;
        }

        .mini-progress {
          width: 60px;
        }

        &.success {
          background: #f0f9eb;
          color: #67C23A;
        }
      }
    }

    .stages-container {
      display: flex;
      justify-content: space-between;
      position: relative;
      padding: 0 10px;

      // Connecting line behind dots
      &::before {
        content: '';
        position: absolute;
        top: 15px; /* Aligns with middle of dot (30px height) */
        left: 20px;
        right: 20px;
        height: 2px;
        background: #EBEEF5;
        z-index: 0;
      }

      .stage-item {
        position: relative;
        z-index: 1;
        display: flex;
        flex-direction: column;
        align-items: center;
        flex: 1;
        text-align: center;

        .stage-bar {
          display: none; // Handled by container ::before
        }

        .stage-dot {
          width: 30px;
          height: 30px;
          border-radius: 50%;
          background: #fff;
          border: 2px solid #DCDFE6;
          display: flex;
          justify-content: center;
          align-items: center;
          margin-bottom: 10px;
          color: #909399;
          font-size: 12px;
          transition: all 0.3s;
        }

        .stage-content {
          .stage-name {
            font-size: 12px;
            color: #909399;
            margin-bottom: 4px;
            font-weight: 500;
          }
          .stage-range {
            font-size: 11px;
            color: #C0C4CC;
          }
        }

        /* States */
        &.is-completed {
          .stage-dot {
            background: #E1F3D8;
            border-color: #67C23A;
            color: #67C23A;
          }
          .stage-name { color: #606266; }
        }

        &.is-current {
          .stage-dot {
            background: #409EFF;
            border-color: #409EFF;
            color: #fff;
            box-shadow: 0 0 0 4px rgba(64, 158, 255, 0.2);
            transform: scale(1.1);
          }
          .stage-name {
            color: #409EFF;
            font-weight: bold;
          }
          .stage-range { color: #606266; }
        }

        &.is-future {
             /* Default styles apply */
        }
      }
    }
  }

  /* Status Card Styles */
  .status-card {
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

  .chart-card {
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    &:hover {
      transform: translateY(-5px);
    }
    &:last-child {
        margin-bottom: 0;
    }
  }

  ::v-deep .el-card__header {
    border-bottom: 1px solid #e8eaec;
    padding: 16px 20px;
  }

  .chart-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    span {
      font-size: 16px;
      font-weight: 600;
      color: $text-primary;
    }
  }

  .chart-box {
    width: 100%;
    height: 360px;
  }

  .chart-box-small {
    width: 100%;
    height: 260px;
  }

  @media (max-width: 768px) {
    .header-content {
      flex-direction: column;
      align-items: flex-start;
    }
    .page-title {
      margin-bottom: 10px;
    }
    .chart-box {
      height: 300px;
    }
    .chart-box-small {
      height: 220px;
    }
  }
}
</style>
