<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>📈 利润趋势分析</span>
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

    <el-row :gutter="20">
      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>📊 交易类型分布 (微信/支付宝)</span>
          </div>
          <div id="clientPieChart" class="chart-box"></div>
        </el-card>

        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>💸 每月支出总额趋势</span>
          </div>
          <div id="monthlyConsumptionColumnChart" class="chart-box"></div>
        </el-card>
      </el-col>

      <el-col :xs="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>🏦 贷款偿还进度 (本金+利息)</span>
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
      <el-col :span="24">
        <h3 style="margin-top: 20px; border-bottom: 1px solid #eee; padding-bottom: 10px;">关注股票资金博弈</h3>
      </el-col>
      <el-col v-for="flow in stockFundFlows" :key="flow.stockCode" :xs="24" :sm="12" :md="8" :lg="6">
        <fund-flow-pie-chart :flow-data="flow"></fund-flow-pie-chart>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import 'echarts-liquidfill';
// 引入 API
import {
  getTotalAmountChart,                // 每月支出总额趋势
  getMonthlyIncomeBarChart,           // 月度收支对比
  getTotalRepaymentPieChart,          // 贷款偿还进度
  getWechatAlipayData,                // 交易类型分布
  getYearIncomeExpenseRatio,          // 年度收支比例 (水滴图)
  getProfitLineData,                  // 利润趋势分析
  renderLoanRepaymentComparisonChart  // 近一年还贷对比
} from "@/api/finance/pieChart";

import {listWatch_stock} from "@/api/stock/watch_stock";
import {getLatestFundFlowByCodes} from "@/api/stock/stockFlow";
import FundFlowPieChart from "./FundFlowPieChart.vue";

// 定义宋体字体栈
const FONT_FAMILY = '"SimSun", "Songti SC", "STSong", serif';

export default {
  name: 'Charts',
  components: {
    FundFlowPieChart
  },
  data() {
    return {
      stockFundFlows: [],
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
  mounted() {
    this.$nextTick(() => {
      this.loadAllCharts();
      this.loadStockFundFlows();
      window.addEventListener('resize', this.resizeCharts);
    });
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    this.disposeCharts();
  },
  methods: {
    loadStockFundFlows() {
      listWatch_stock({}).then(res => {
        if (res.rows && res.rows.length > 0) {
          const codes = res.rows.map(item => item.stockCode);
          getLatestFundFlowByCodes(codes).then(flowRes => {
            if (flowRes.data) {
              this.stockFundFlows = flowRes.data;
            }
          });
        }
      });
    },

    // --- 基础图表管理 ---
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

    // --- 加载所有图表 ---
    loadAllCharts() {
      this.loadPieChart('transactionType', 'clientPieChart', getWechatAlipayData, '交易类型', '个');
      this.loadBarChart('monthlyConsumption', 'monthlyConsumptionColumnChart', getTotalAmountChart, '每月支出', '元');

      // 混合图表：收支对比
      this.loadMixedChart('monthlyIncomeExpense', 'monthlyIncomeExpenseBarChart', getMonthlyIncomeBarChart, '每月收支', '元', ['收入', '支出', '结余']);

      // 混合图表：还贷对比
      this.loadMixedChart('generateMonthlyLoanRepayment', 'generateMonthlyLoanRepaymentBarChart', renderLoanRepaymentComparisonChart, '还贷本息', '元', ['贷款偿还']);

      this.loadHeartProgressChart('totalRepayment', 'totalRepaymentPieChart', getTotalRepaymentPieChart);

      this.loadLiquidChart('expenseLiquid', 'expenseLiquidChart', getYearIncomeExpenseRatio, '支出');
      this.loadLiquidChart('incomeLiquid', 'incomeLiquidChart', getYearIncomeExpenseRatio, '结余');

      this.loadLineChart('profitLine', 'profitLineChart', getProfitLineData);
    },

    // ------------------------------------------
    // 1. 饼图/环形图渲染器
    // ------------------------------------------
    loadPieChart(key, domId, apiFn, title, unit) {
      apiFn().then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;
        if (!data || !Array.isArray(data)) return;

        const seriesData = data.map((i) => ({
          name: i.category,
          value: i.amount,
        }));

        chart.setOption({
          textStyle: {fontFamily: FONT_FAMILY}, // 全局字体
          title: {show: false},
          tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            textStyle: {fontFamily: FONT_FAMILY},
            formatter: (params) => {
              return `
                <div style="font-family:${FONT_FAMILY}; font-size:14px; font-weight:bold; margin-bottom:5px;">${params.name}</div>
                <div style="font-family:${FONT_FAMILY}; display:flex; justify-content:space-between; min-width:120px;">
                  <span>金额:</span>
                  <span style="font-weight:bold; color:${params.color}">${params.value} ${unit}</span>
                </div>
                <div style="font-family:${FONT_FAMILY}; display:flex; justify-content:space-between; margin-top:3px;">
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
            itemWidth: 10, itemHeight: 10,
            textStyle: {fontFamily: FONT_FAMILY}
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
                formatter: `{b}\n{c} ${unit}`,
                fontFamily: FONT_FAMILY
              }
            },
            label: {show: false, position: 'center'},
            data: seriesData
          }]
        });
      }).catch(e => console.error(e));
    },

    // ------------------------------------------
    // 2. 柱状图渲染器
    // ------------------------------------------
    loadBarChart(key, domId, apiFn, title, unit) {
      apiFn().then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;
        if (!data || !Array.isArray(data)) return;

        const getGradient = (start, end) => new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
          offset: 0,
          color: start
        }, {offset: 1, color: end}]);

        chart.setOption({
          textStyle: {fontFamily: FONT_FAMILY},
          grid: {top: 40, left: '3%', right: '4%', bottom: '10%', containLabel: true},
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255,255,255,0.95)',
            textStyle: {fontFamily: FONT_FAMILY},
            formatter: (params) => {
              let html = `<div style="font-family:${FONT_FAMILY}; margin-bottom:5px;font-weight:bold;border-bottom:1px solid #eee;padding-bottom:5px;">${params[0].axisValue}</div>`;
              params.forEach(item => {
                const color = item.color.colorStops ? item.color.colorStops[0].color : item.color;
                html += `
                  <div style="font-family:${FONT_FAMILY}; display:flex; align-items:center; justify-content:space-between; margin-top:5px;">
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
            axisLine: {lineStyle: {color: '#ccc'}},
            axisLabel: {fontFamily: FONT_FAMILY}
          },
          yAxis: {
            type: 'value',
            splitLine: {lineStyle: {type: 'dashed', color: '#eee'}},
            axisLabel: {fontFamily: FONT_FAMILY}
          },
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
                color: '#3AA1FF',
                fontFamily: FONT_FAMILY
              }
            }
          }]
        });
      }).catch(e => console.error(e));
    },

    // ------------------------------------------
    // 3. 混合图表渲染器
    // ------------------------------------------
    loadMixedChart(key, domId, apiFn, title, unit, legendData = []) {
      apiFn().then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;

        if (!data || !Array.isArray(data)) {
          console.warn(`Chart [${title}] returned empty or invalid data.`);
          return;
        }

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
                fontWeight: 'bold',
                fontFamily: FONT_FAMILY
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

        chart.setOption({
          textStyle: {fontFamily: FONT_FAMILY},
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255,255,255,0.95)',
            textStyle: {fontFamily: FONT_FAMILY},
            axisPointer: {type: 'shadow'},
            formatter: (params) => {
              let html = `<div style="font-family:${FONT_FAMILY}; font-weight:bold;margin-bottom:5px;">📅 ${params[0].axisValue}</div>`;
              params.filter(p => p.seriesType === 'bar').forEach(item => {
                let color = item.color;
                if (typeof color === 'object' && color.colorStops) color = color.colorStops[0].color;
                html += `
                  <div style="font-family:${FONT_FAMILY}; display:flex; justify-content:space-between; margin:3px 0;">
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
          legend: {
            data: legendData,
            top: 0,
            textStyle: {fontFamily: FONT_FAMILY}
          },
          grid: {top: 40, left: '3%', right: '4%', bottom: 40, containLabel: true},
          xAxis: {
            type: 'category',
            data: xData,
            axisLine: {lineStyle: {color: '#ddd'}},
            axisLabel: {fontFamily: FONT_FAMILY}
          },
          yAxis: {
            type: 'value',
            name: unit,
            splitLine: {lineStyle: {type: 'dashed', color: '#f0f0f0'}},
            axisLabel: {fontFamily: FONT_FAMILY}
          },
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

    // ------------------------------------------
    // 4. 利润折线图渲染器
    // ------------------------------------------
    loadLineChart(key, domId, apiFn) {
      apiFn().then(response => {
        const chart = this.initChart(key, domId);
        if (!chart) return;

        let lineData = [];
        let barData = [];

        // 判断返回格式：如果是 Map/Object，则拆分数据
        if (response && typeof response === 'object' && !Array.isArray(response)) {
          lineData = response.line || [];
          barData = response.bar || [];
        } else if (Array.isArray(response)) {
          // 兼容旧接口，全量当作 lineData
          lineData = response;
        }

        const xDataLine = lineData.map(item => item.recordDate);
        const yDataLine = lineData.map(item => item.profit);

        // 柱状图X轴只显示年份
        const xDataBar = barData.map(item => new Date(item.recordDate).getFullYear());
        const yDataBar = barData.map(item => item.profit);

        chart.setOption({
          textStyle: {fontFamily: FONT_FAMILY},
          backgroundColor: '#fff',
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255,255,255,0.95)',
            padding: 12,
            textStyle: {fontFamily: FONT_FAMILY},
            axisPointer: {type: 'cross', label: {backgroundColor: '#6a7985', fontFamily: FONT_FAMILY}},
            formatter: (params) => {
              let html = `<div style="font-family:${FONT_FAMILY}; font-weight:bold; margin-bottom:5px;">📅 ${params[0].axisValue}</div>`;
              params.forEach(p => {
                html += `<div style="font-family:${FONT_FAMILY}; display:flex; justify-content:space-between; align-items:center;">
                      <span>${p.marker} ${p.seriesName}</span>
                      <span style="font-weight:bold; color:#409EFF; margin-left:15px; font-size:16px;">${p.value} 元</span>
                    </div>`;
              });
              return html;
            }
          },
          grid: [
            {left: '3%', right: '55%', bottom: '3%', containLabel: true},
            {left: '55%', right: '4%', bottom: '3%', containLabel: true}
          ],
          xAxis: [
            {
              type: 'category',
              boundaryGap: false,
              data: xDataLine,
              axisLine: {lineStyle: {color: '#ccc'}},
              axisLabel: {fontFamily: FONT_FAMILY},
              gridIndex: 0
            },
            {
              type: 'category',
              data: xDataBar,
              axisLine: {lineStyle: {color: '#ccc'}},
              axisLabel: {fontFamily: FONT_FAMILY},
              gridIndex: 1
            }
          ],
          yAxis: [
            {
              type: 'value',
              splitLine: {lineStyle: {color: '#f0f0f0'}},
              axisLabel: {fontFamily: FONT_FAMILY},
              gridIndex: 0
            },
            {
              type: 'value',
              splitLine: {lineStyle: {color: '#f0f0f0'}},
              axisLabel: {fontFamily: FONT_FAMILY},
              gridIndex: 1
            }
          ],
          series: [{
            name: '今年利润',
            type: 'line',
            xAxisIndex: 0,
            yAxisIndex: 0,
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
            data: yDataLine,
            markPoint: {
              data: [
                {type: 'max', name: '最高', label: {formatter: '{c}', fontFamily: FONT_FAMILY}},
                {type: 'min', name: '最低', label: {formatter: '{c}', fontFamily: FONT_FAMILY}}
              ],
              label: {fontFamily: FONT_FAMILY}
            }
          },
            {
              name: '年度对比',
              type: 'bar',
              xAxisIndex: 1,
              yAxisIndex: 1,
              data: yDataBar,
              barWidth: '40%',
              itemStyle: {
                color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
                  {offset: 0, color: '#36D1DC'},
                  {offset: 1, color: '#5B86E5'}
                ]),
                borderRadius: [4, 4, 0, 0]
              },
              label: {
                show: true,
                position: 'top',
                fontFamily: FONT_FAMILY,
                color: '#333'
              }
            }]
        });
      }).catch(e => console.error(e));
    },

    // ------------------------------------------
    // 5. 水滴图渲染器
    // ------------------------------------------
    loadLiquidChart(key, domId, apiFn, categoryLabel) {
      apiFn().then(raw => {
        if (!raw || !Array.isArray(raw)) return;

        const chart = this.initChart(key, domId);
        if (!chart) return;

        // 兼容后端返回的繁体或简体 Key
        const keywords = [categoryLabel];
        if (categoryLabel === '结余') {
          keywords.push('结余'); // 简体
          keywords.push('結余'); // 繁体兼容
        }

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
              fontWeight: 'bold',
              fontFamily: FONT_FAMILY
            },
            outline: {
              show: true,
              borderDistance: 4,
              itemStyle: {borderWidth: 2, borderColor: color[0]}
            }
          }],
          tooltip: {
            show: true,
            textStyle: {fontFamily: FONT_FAMILY},
            formatter: () => `<div style="font-family:${FONT_FAMILY}">${categoryLabel}: <b>${amount} 元</b><br/>总流动: ${total} 元</div>`
          }
        });
      }).catch(e => console.error("水滴图加载失败:", e));
    },

    // ------------------------------------------
    // 6. 心形进度条渲染器 (贷款偿还进度)
    // ------------------------------------------
    loadHeartProgressChart(key, domId, apiFn) {
      apiFn().then(rawList => {
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

        // 兼容繁简关键字
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
          textStyle: {fontFamily: FONT_FAMILY},
          grid: {left: '5%', right: '15%', top: '10%', bottom: '5%', containLabel: true},
          tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(255,255,255,0.98)',
            textStyle: {fontFamily: FONT_FAMILY},
            formatter: (params) => {
              const isInterest = params.dataIndex === 0;
              const type = isInterest ? '利息' : '本金';
              const paid = isInterest ? interestPaid : principalPaid;
              const unpaid = isInterest ? interestUnpaid : principalUnpaid;
              const percent = isInterest ? iPercent : pPercent;

              if (params.seriesName === '已偿还') {
                return `<div style="font-family:${FONT_FAMILY}"><span style="font-weight:bold">${type} - 已偿还</span><br/>
                        金额：${paid.toLocaleString()} 元<br/>
                        进度：${percent}%</div>`;
              } else {
                return `<div style="font-family:${FONT_FAMILY}"><span style="font-weight:bold">${type} - 未偿还</span><br/>
                        金额：${unpaid.toLocaleString()} 元<br/>
                        剩余：${(100 - percent).toFixed(1)}%</div>`;
              }
            }
          },
          xAxis: {max: 100, show: false},
          yAxis: {
            data: ['利息', '本金'],
            axisLine: {show: false},
            axisTick: {show: false},
            axisLabel: {fontWeight: 'bold', color: '#666', fontSize: 14, fontFamily: FONT_FAMILY}
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
                fontFamily: FONT_FAMILY,
                formatter: (p) => p.value > 10 ? `${p.value}%` : ''
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
                color: '#FF1E4D',
                fontFamily: FONT_FAMILY
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
// 定义变量防止 SCSS 编译报错
$text-primary: #303133;
// 定义中文字体：宋体
$font-family-song: "SimSun", "Songti SC", "STSong", "AR PL New Sung", "NSimSun", serif;

.home {
  // 应用宋体到整个页面容器
  font-family: $font-family-song;

  .page-header {
    margin-bottom: 20px;
    border: none;

    .header-content {
      display: flex;
      justify-content: flex-start;
      align-items: center;
    }

    .page-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: $text-primary;
      font-family: $font-family-song; // 确保标题使用宋体
    }
  }

  .el-row + .el-row {
    margin-top: 20px;
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
      font-family: $font-family-song; // 确保卡片标题使用宋体
    }
  }

  // Element UI 标签内部字体覆盖
  ::v-deep .el-tag {
    font-family: $font-family-song;
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
