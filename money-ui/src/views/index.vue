<template>
  <div class="app-container home">
    <!-- Page Header & User Selection -->
    <el-card shadow="never" class="page-header">
      <div class="header-content">
        <h2 class="page-title">儀表板</h2>
        <div class="header-controls">
          <el-select v-model="selectedUserId" placeholder="選擇用戶" @change="handleUserChange" filterable clearable>
            <el-option
              v-for="user in userList"
              :key="user.id"
              :label="user.name"
              :value="user.id"
            />
          </el-select>
        </div>
      </div>
    </el-card>

    <!-- Task Status Panel (New) -->
    <el-row :gutter="20" class="task-status-row">
      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>待執行任務數</span>
          </div>
          <div class="card-body">
            <span class="count-text pending">{{ taskStats.pending }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>已執行任務數</span>
          </div>
          <div class="card-body">
            <span class="count-text completed">{{ taskStats.completed }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>執行中任務</span>
          </div>
          <div class="card-body">
            <span class="count-text executing">{{ taskStats.executing }}</span>
          </div>
        </el-card>
      </el-col>

      <el-col :span="6">
        <el-card shadow="hover" class="status-card">
          <div class="card-header">
            <span>執行中占比</span>
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
            <span>📈 利潤趨勢分析</span>
            <el-tag size="small" effect="plain">歷史數據</el-tag>
          </div>
          <div id="profitLineChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>💳 近一年還貸對比</span>
          </div>
          <div id="generateMonthlyLoanRepaymentBarChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>💰 月度收支對比</span>
          </div>
          <div id="monthlyIncomeExpenseBarChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>📊 交易類型分布 (微信/支付寶)</span>
          </div>
          <div id="clientPieChart" class="chart-box"></div>
        </el-card>

        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>💸 每月支出總額趨勢</span>
          </div>
          <div id="monthlyConsumptionColumnChart" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>🏦 貸款償還進度 (本金+利息)</span>
          </div>
          <div id="totalRepaymentPieChart" class="chart-box"></div>
        </el-card>

        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>💧 年度收支比例</span>
          </div>
          <el-row :gutter="10">
            <el-col :span="12">
              <div id="expenseLiquidChart" class="chart-box-small"></div>
            </el-col>
            <el-col :span="12">
              <div id="incomeLiquidChart" class="chart-box-small"></div>
            </el-col>
          </el-row>
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
import {listUser} from "@/api/stock/dropdown_component";
import Cookies from 'js-cookie';

export default {
  name: 'Index', // Changed from 'Charts' to 'Index' to match usage
  data() {
    return {
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
    // --- Task Stats Method ---
    fetchTaskStats() {
      request({
        url: '/monitor/job/status-summary',
        method: 'get'
      }).then(response => {
        // Handle various response formats
        const data = response.data || response;
        if (data) {
          // If data is wrapped in 'data' field again (AjaxResult structure)
          this.taskStats = data.data || data;
        }
      }).catch(error => {
        console.error("Failed to fetch task stats", error);
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
          name: u.userName || u.nickName || `用戶${u.userId}`
        }));
        if (this.userList.length) {
          const savedUsername = Cookies.get('username');
          const matchedUser = this.userList.find(u => u.name === savedUsername);
          this.selectedUserId = matchedUser ? matchedUser.id : this.userList[0].id;
        } else {
          this.selectedUserId = null;
        }
      } catch (err) {
        console.error('用戶列表加載失敗:', err);
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
      this.loadPieChart('transactionType', 'clientPieChart', () => getWechatAlipayData(selectedUserId), '交易類型', '個');
      this.loadBarChart('monthlyConsumption', 'monthlyConsumptionColumnChart', () => getTotalAmountChart(selectedUserId), '每月支出', '元');
      this.loadMixedChart('monthlyIncomeExpense', 'monthlyIncomeExpenseBarChart', () => getMonthlyIncomeBarChart(selectedUserId), '每月收支', '元', ['收入', '支出', '結余']);
      this.loadMixedChart('generateMonthlyLoanRepayment', 'generateMonthlyLoanRepaymentBarChart', () => renderLoanRepaymentComparisonChart(selectedUserId), '還貸本息', '元', ['貸款償還']);
      this.loadHeartProgressChart('totalRepayment', 'totalRepaymentPieChart', () => getTotalRepaymentPieChart(selectedUserId));
      this.loadLiquidChart('expenseLiquid', 'expenseLiquidChart', () => getYearIncomeExpenseRatio(selectedUserId), '支出');
      this.loadLiquidChart('incomeLiquid', 'incomeLiquidChart', () => getYearIncomeExpenseRatio(selectedUserId), '結余');
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
                  <span>金額:</span>
                  <span style="font-weight:bold; color:${params.color}">${params.value} ${unit}</span>
                </div>
                <div style="display:flex; justify-content:space-between; margin-top:3px;">
                  <span>佔比:</span>
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
          else if (name === '支出' || name === '貸款償還') amountKey = 'supportOutAmount';
          else if (name === '結余') amountKey = 'balanceAmount';

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
            name: `${name}趨勢`,
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
          else if (name === '貸款償還') createSeries('貸款償還', '#E6A23C', '#F3D19E');
          else if (name === '結余') createSeries('結余', '#409EFF', '#79BBFF');
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
                  <span>${p.marker} 利潤</span>
                  <span style="font-weight:bold; color:#409EFF; margin-left:15px; font-size:16px;">${p.value} 元</span>
                </div>
              `;
            }
          },
          grid: {left: '3%', right: '4%', bottom: '3%', containLabel: true},
          xAxis: {type: 'category', boundaryGap: false, data: xData, axisLine: {lineStyle: {color: '#ccc'}}},
          yAxis: {type: 'value', splitLine: {lineStyle: {color: '#f0f0f0'}}},
          series: [{
            name: '利潤',
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
        if (categoryLabel === '結余') keywords.push('结余');

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
            formatter: () => `${categoryLabel}: <b>${amount} 元</b><br/>總流動: ${total} 元`
          }
        });
      }).catch(e => console.error("水滴圖加載失敗:", e));
    },

    loadHeartProgressChart(key, domId, apiFn) {
      apiFn({userId: this.selectedUserId}).then(rawList => {
        if (!rawList || !Array.isArray(rawList)) {
          console.warn(`${key} API 返回數據為空或格式錯誤`, rawList);
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

              if (params.seriesName === '已償還') {
                return `<div style="font-weight:bold">${type} - 已償還</div>
                        <div>金額：${paid.toLocaleString()} 元</div>
                        <div>進度：${percent}%</div>`;
              } else {
                return `<div style="font-weight:bold">${type} - 未償還</div>
                        <div>金額：${unpaid.toLocaleString()} 元</div>
                        <div>剩餘：${(100 - percent).toFixed(1)}%</div>`;
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
              name: '已償還',
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
              name: '未償還',
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
      }).catch(e => console.error("貸款圖表加載失敗:", e));
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
