<template>
  <div class="app-container home song-font">
    <el-card shadow="never" class="page-header">
      <div class="header-content">
        <h2 class="page-title">儀表板</h2>
      </div>
    </el-card>

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

export default {
  name: 'Charts',
  data() {
    return {
      // 查询参数，防止未定义报错
      queryParams: {
        userId: null
      },
      // 图表实例容器
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
      window.addEventListener('resize', this.resizeCharts);
    });
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    this.disposeCharts();
  },
  methods: {
    /**
     * 销毁所有图表实例
     */
    disposeCharts() {
      Object.values(this.charts).forEach(chart => chart && chart.dispose());
    },

    /**
     * 调整所有图表大小
     */
    resizeCharts() {
      Object.values(this.charts).forEach(chart => chart && chart.resize());
    },

    /**
     * 初始化图表通用方法
     */
    initChart(key, domId) {
      const dom = document.getElementById(domId);
      if (!dom) return null;
      if (this.charts[key]) this.charts[key].dispose();
      this.charts[key] = echarts.init(dom);
      return this.charts[key];
    },

    /**
     * 加载所有图表
     */
    loadAllCharts() {
      const params = this.queryParams;

      // 1. 交易类型分布
      this.loadPieChart('transactionType', 'clientPieChart', () => getWechatAlipayData(params), '交易類型', '個');

      // 2. 每月支出趋势
      this.loadBarChart('monthlyConsumption', 'monthlyConsumptionColumnChart', () => getTotalAmountChart(params), '每月支出', '元');

      // 3. 月度收支对比
      this.loadMixedChart('monthlyIncomeExpense', 'monthlyIncomeExpenseBarChart', () => getMonthlyIncomeBarChart(params), '每月收支', '元', ['收入', '支出', '結余']);

      // 4. 还贷对比
      this.loadMixedChart('generateMonthlyLoanRepayment', 'generateMonthlyLoanRepaymentBarChart', () => renderLoanRepaymentComparisonChart(params), '還貸本息', '元', ['貸款償還']);

      // 5. 贷款偿还进度 (心形)
      this.loadHeartProgressChart('totalRepayment', 'totalRepaymentPieChart', () => getTotalRepaymentPieChart(params));

      // 6. 年度水滴图
      this.loadLiquidChart('expenseLiquid', 'expenseLiquidChart', () => getYearIncomeExpenseRatio(params), '支出');
      this.loadLiquidChart('incomeLiquid', 'incomeLiquidChart', () => getYearIncomeExpenseRatio(params), '結余');

      // 7. 利润趋势
      this.loadLineChart('profitLine', 'profitLineChart', () => getProfitLineData(params));
    },

    // --- 图表渲染逻辑 ---

    loadPieChart(key, domId, apiFn, title, unit) {
      apiFn().then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;

        const seriesData = data.map((i) => ({ name: i.category, value: i.amount }));
        chart.setOption({
          title: { show: false },
          tooltip: {
            trigger: 'item',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            textStyle: { color: '#333', fontFamily: '"SimSun", serif' },
            formatter: (params) => `
                <div style="font-size:14px; font-weight:bold; margin-bottom:5px;">${params.name}</div>
                <div style="display:flex; justify-content:space-between; min-width:120px;">
                  <span>金額:</span><span style="font-weight:bold; color:${params.color}">${params.value} ${unit}</span>
                </div>
                <div style="display:flex; justify-content:space-between; margin-top:3px;">
                  <span>佔比:</span><span>${params.percent}%</span>
                </div>`
          },
          legend: { type: 'scroll', bottom: 0, itemWidth: 10, itemHeight: 10, textStyle: { fontFamily: '"SimSun", serif' } },
          series: [{
            name: title,
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['50%', '45%'],
            itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
            emphasis: {
              scale: true,
              scaleSize: 10,
              label: {
                show: true,
                fontSize: 18,
                fontWeight: 'bold',
                fontFamily: '"SimSun", serif',
                formatter: `{b}\n{c} ${unit}`
              }
            },
            label: { show: false, position: 'center' },
            data: seriesData
          }]
        });
      }).catch(e => console.error(e));
    },

    loadBarChart(key, domId, apiFn, title, unit) {
      apiFn().then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;

        chart.setOption({
          grid: { top: 40, left: '3%', right: '4%', bottom: '10%', containLabel: true },
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255,255,255,0.95)',
            textStyle: { fontFamily: '"SimSun", serif' },
            formatter: (params) => {
              let html = `<div style="margin-bottom:5px;font-weight:bold;border-bottom:1px solid #eee;">${params[0].axisValue}</div>`;
              params.forEach(item => {
                const color = item.color.colorStops ? item.color.colorStops[0].color : item.color;
                html += `<div style="margin-top:5px;"><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${color};margin-right:5px;"></span>${item.seriesName}: <b>${item.value} ${unit}</b></div>`;
              });
              return html;
            }
          },
          xAxis: { type: 'category', data: data.map(i => i.transactionTime), axisTick: { show: false }, axisLabel: { fontFamily: '"SimSun", serif' } },
          yAxis: { type: 'value', splitLine: { lineStyle: { type: 'dashed' } }, axisLabel: { fontFamily: '"SimSun", serif' } },
          series: [{
            type: 'bar',
            name: title,
            data: data.map(i => i.amount),
            barWidth: '50%',
            itemStyle: {
              borderRadius: [4, 4, 0, 0],
              color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#3AA1FF' }, { offset: 1, color: '#36D1DC' }])
            },
            emphasis: { focus: 'series', label: { show: true, position: 'top', formatter: `{c} ${unit}` } }
          }]
        });
      }).catch(e => console.error(e));
    },

    loadMixedChart(key, domId, apiFn, title, unit, legendData = []) {
      apiFn().then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;

        const xData = data.map(i => i.transactionTime);
        const series = [];

        const createSeries = (name, colorStart, colorEnd) => {
          let amountKey = name === '收入' ? 'supportInAmount' : (name === '結余' ? 'balanceAmount' : 'supportOutAmount');
          series.push({
            name: name, type: 'bar', data: data.map(i => i[amountKey]), barWidth: '30%',
            itemStyle: { borderRadius: [4, 4, 0, 0], color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: colorStart }, { offset: 1, color: colorEnd }]) },
            emphasis: { label: { show: true, position: 'top' } }
          });
          series.push({ name: `${name}趨勢`, type: 'line', data: data.map(i => i[amountKey]), smooth: true, symbol: 'none', lineStyle: { width: 3, color: colorStart }, tooltip: { show: false } });
        };

        legendData.forEach(name => {
          if (name === '收入') createSeries('收入', '#67C23A', '#95D475');
          else if (name === '支出') createSeries('支出', '#F56C6C', '#FAB6B6');
          else if (name === '貸款償還') createSeries('貸款償還', '#E6A23C', '#F3D19E');
          else if (name === '結余') createSeries('結余', '#409EFF', '#79BBFF');
        });

        chart.setOption({
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255,255,255,0.95)',
            textStyle: { fontFamily: '"SimSun", serif' },
            formatter: (params) => {
              let html = `<div style="font-weight:bold;margin-bottom:5px;">📅 ${params[0].axisValue}</div>`;
              params.filter(p => p.seriesType === 'bar').forEach(item => {
                let color = typeof item.color === 'object' ? item.color.colorStops[0].color : item.color;
                html += `<div><span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${color};margin-right:5px;"></span>${item.seriesName}: <b>${item.value} ${unit}</b></div>`;
              });
              return html;
            }
          },
          legend: { data: legendData, top: 0, textStyle: { fontFamily: '"SimSun", serif' } },
          grid: { top: 40, left: '3%', right: '4%', bottom: 40, containLabel: true },
          xAxis: { type: 'category', data: xData, axisLabel: { fontFamily: '"SimSun", serif' } },
          yAxis: { type: 'value', name: unit, splitLine: { lineStyle: { type: 'dashed' } }, axisLabel: { fontFamily: '"SimSun", serif' }, nameTextStyle: { fontFamily: '"SimSun", serif' } },
          dataZoom: [{ type: 'slider', height: 15, bottom: 5, backgroundColor: '#f5f7fa', handleStyle: { color: '#409EFF' } }],
          series
        });
      }).catch(e => console.error(e));
    },

    loadLineChart(key, domId, apiFn) {
      apiFn().then(data => {
        const chart = this.initChart(key, domId);
        if (!chart) return;
        chart.setOption({
          backgroundColor: '#fff',
          tooltip: {
            trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.95)', padding: 12,
            textStyle: { fontFamily: '"SimSun", serif' },
            formatter: (params) => `
              <div style="font-weight:bold; margin-bottom:5px;">📅 ${params[0].axisValue}</div>
              <div>${params[0].marker} 利潤: <span style="font-weight:bold; color:#409EFF; margin-left:10px;">${params[0].value} 元</span></div>
            `
          },
          grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
          xAxis: { type: 'category', boundaryGap: false, data: data.map(i => i.recordDate), axisLabel: { fontFamily: '"SimSun", serif' } },
          yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f0f0f0' } }, axisLabel: { fontFamily: '"SimSun", serif' } },
          series: [{
            name: '利潤', type: 'line', smooth: true, symbol: 'circle', symbolSize: 8,
            itemStyle: { color: '#409EFF', borderColor: '#fff', borderWidth: 2 },
            areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(64, 158, 255, 0.4)' }, { offset: 1, color: 'rgba(64, 158, 255, 0.05)' }]) },
            data: data.map(i => i.profit),
            markPoint: { data: [{ type: 'max', name: '最高' }, { type: 'min', name: '最低' }], label: { fontFamily: '"SimSun", serif' } }
          }]
        });
      }).catch(e => console.error(e));
    },

    loadLiquidChart(key, domId, apiFn, categoryLabel) {
      apiFn().then(raw => {
        if (!raw || !Array.isArray(raw)) return;
        const chart = this.initChart(key, domId);
        if (!chart) return;

        const keywords = [categoryLabel, categoryLabel === '結余' ? '结余' : ''];
        const item = raw.find(i => keywords.some(k => k && i.category && i.category.includes(k)));
        const amount = item ? Number(item.amount) : 0;
        const total = raw.reduce((sum, i) => sum + Number(i.amount), 0);
        const ratio = total > 0 ? amount / total : 0;
        const color = categoryLabel.includes('支出') ? ['#F56C6C', 'rgba(245, 108, 108, 0.6)'] : ['#67C23A', 'rgba(103, 194, 58, 0.6)'];

        chart.setOption({
          series: [{
            type: 'liquidFill', radius: '85%', center: ['50%', '50%'],
            data: [ratio, ratio > 0.1 ? ratio - 0.05 : ratio],
            color: color,
            backgroundStyle: { color: '#fff', borderWidth: 1, borderColor: '#e0e0e0' },
            label: {
              formatter: () => `${(ratio * 100).toFixed(1)}%\n${categoryLabel}`,
              fontSize: 22, color: color[0], insideColor: '#fff', fontWeight: 'bold', fontFamily: '"SimSun", serif'
            },
            outline: { show: true, borderDistance: 4, itemStyle: { borderWidth: 2, borderColor: color[0] } }
          }],
          tooltip: { show: true, textStyle: { fontFamily: '"SimSun", serif' }, formatter: () => `${categoryLabel}: <b>${amount} 元</b><br/>總流動: ${total} 元` }
        });
      }).catch(e => console.error(e));
    },

    loadHeartProgressChart(key, domId, apiFn) {
      apiFn().then(rawList => {
        if (!rawList || !Array.isArray(rawList)) return;
        const chart = this.initChart(key, domId);
        if (!chart) return;

        const findVal = (keywords) => {
          const item = rawList.find(i => keywords.some(k => i.category && i.category.includes(k)));
          return item ? Number(item.amount) : 0;
        };

        const principalPaid = findVal(['已償還本金', '已偿还本金']);
        const principalUnpaid = findVal(['未還本金', '未还本金']);
        const interestPaid = findVal(['已償還利息', '已偿还利息']);
        const interestUnpaid = findVal(['未還利息', '未还利息']);

        const pPercent = (principalPaid + principalUnpaid) > 0 ? +((principalPaid / (principalPaid + principalUnpaid)) * 100).toFixed(1) : 0;
        const iPercent = (interestPaid + interestUnpaid) > 0 ? +((interestPaid / (interestPaid + interestUnpaid)) * 100).toFixed(1) : 0;

        chart.setOption({
          grid: { left: '5%', right: '15%', top: '10%', bottom: '5%', containLabel: true },
          tooltip: {
            trigger: 'item', backgroundColor: 'rgba(255,255,255,0.98)', textStyle: { fontFamily: '"SimSun", serif' },
            formatter: (params) => {
              const isInterest = params.dataIndex === 0;
              const paid = isInterest ? interestPaid : principalPaid;
              const unpaid = isInterest ? interestUnpaid : principalUnpaid;
              return params.seriesName === '已償還'
                ? `<div style="font-weight:bold">${isInterest ? '利息' : '本金'} - 已償還</div><div>金額：${paid.toLocaleString()}</div><div>進度：${isInterest ? iPercent : pPercent}%</div>`
                : `<div style="font-weight:bold">${isInterest ? '利息' : '本金'} - 未償還</div><div>金額：${unpaid.toLocaleString()}</div>`;
            }
          },
          xAxis: { max: 100, show: false },
          yAxis: { data: ['利息', '本金'], axisLine: { show: false }, axisTick: { show: false }, axisLabel: { fontWeight: 'bold', color: '#666', fontSize: 14, fontFamily: '"SimSun", serif' } },
          series: [
            {
              name: '已償還', type: 'bar', stack: 'total', data: [iPercent, pPercent], barWidth: 30,
              itemStyle: { borderRadius: [15, 0, 0, 15], color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#FF4D6D' }, { offset: 1, color: '#FF8FA3' }]) },
              label: { show: true, position: 'inside', color: '#fff', fontWeight: 'bold', fontFamily: '"SimSun", serif', formatter: p => p.value > 10 ? `${p.value}%` : '' }
            },
            {
              name: '未償還', type: 'bar', stack: 'total', data: [100 - iPercent, 100 - pPercent], barWidth: 30,
              itemStyle: { color: '#FFE6EB', borderRadius: [0, 15, 15, 0] }, label: { show: false }
            },
            {
              type: 'scatter', data: [[iPercent, 0], [pPercent, 1]], symbolSize: 1,
              label: { show: true, position: 'right', offset: [-5, -2], formatter: '❤️', fontSize: 24, color: '#FF1E4D' }, z: 10
            }
          ]
        });
      }).catch(e => console.error(e));
    },
  }
};
</script>

<style lang="scss" scoped>
// 统一应用中文宋体
.song-font {
  font-family: "SimSun", "Songti SC", "STSong", serif;

  ::v-deep * {
    font-family: "SimSun", "Songti SC", "STSong", serif;
  }
}

.home {
  .page-header {
    margin-bottom: 20px;
    border: none;
    .page-title {
      margin: 0;
      font-size: 24px;
      font-weight: 600;
      color: #303133;
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
    // 修复底部边距叠加问题
    margin-bottom: 20px;
    @media (min-width: 1200px) {
      margin-bottom: 0;
    }
  }

  // 优化 Card Header
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
      color: #303133;
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
    .chart-box { height: 300px; }
    .chart-box-small { height: 220px; }
  }
}
</style>
