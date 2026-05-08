<template>
  <el-card class="chart-card fund-flow-card" shadow="hover">
    <div slot="header" class="chart-header">
      <span>{{ stockName }} ({{ stockCode }}) 资金博弈</span>
    </div>
    <div class="fund-flow-container">
      <div class="flow-title">
        <h3 class="main-title">{{ analyzeTitle }}</h3>
        <p class="sub-title">主力成交占比 {{ mainTotalRatio }}%</p>
      </div>
      <div class="chart-content">
        <!-- 左侧：流出 -->
        <div class="side-info left-side">
          <div class="info-item main-out">
            <div class="amount-val"><span class="dot green-dot"></span>{{ formatAmount(flowData.superLargeOutflow + flowData.largeAbsOutflow) }}千万</div>
            <div class="desc">主力流出 {{ formatRatio(mainOutRatio) }}%</div>
          </div>
          <div class="info-item small-out" style="margin-top: 80px;">
            <div class="amount-val"><span class="dot light-green-dot"></span>{{ formatAmount(flowData.mediumAbsOutflow + flowData.smallAbsOutflow) }}千万</div>
            <div class="desc">散户流出 {{ formatRatio(smallOutRatio) }}%</div>
          </div>
        </div>

        <!-- 中间：饼图 -->
        <div class="pie-chart-wrapper">
          <div ref="pieChart" class="pie-chart"></div>
        </div>

        <!-- 右侧：流入 -->
        <div class="side-info right-side">
          <div class="info-item main-in">
            <div class="amount-val"><span class="dot red-dot"></span>{{ formatAmount(flowData.superLargeAbsInflow + flowData.largeAbsInflow) }}千万</div>
            <div class="desc">主力流入 {{ formatRatio(mainInRatio) }}%</div>
          </div>
          <div class="info-item small-in" style="margin-top: 80px;">
            <div class="amount-val"><span class="dot light-red-dot"></span>{{ formatAmount(flowData.mediumAbsInflow + flowData.smallAbsInflow) }}千万</div>
            <div class="desc">散户流入 {{ formatRatio(smallInRatio) }}%</div>
          </div>
        </div>
      </div>
    </div>
  </el-card>
</template>

<script>
import * as echarts from 'echarts';

const FONT_FAMILY = '"SimSun", "Songti SC", "STSong", "AR PL New Sung", "NSimSun", serif';

export default {
  name: 'FundFlowPieChart',
  props: {
    flowData: {
      type: Object,
      required: true,
      default: () => ({})
    }
  },
  data() {
    return {
      chartInstance: null
    };
  },
  computed: {
    stockName() {
      return this.flowData.stockName || '--';
    },
    stockCode() {
      return this.flowData.stockCode || '--';
    },
    mainIn() {
      return (this.flowData.superLargeAbsInflow || 0) + (this.flowData.largeAbsInflow || 0);
    },
    mainOut() {
      return (this.flowData.superLargeOutflow || 0) + (this.flowData.largeAbsOutflow || 0);
    },
    smallIn() {
      return (this.flowData.mediumAbsInflow || 0) + (this.flowData.smallAbsInflow || 0);
    },
    smallOut() {
      return (this.flowData.mediumAbsOutflow || 0) + (this.flowData.smallAbsOutflow || 0);
    },
    totalFlow() {
      return this.mainIn + this.mainOut + this.smallIn + this.smallOut;
    },
    mainTotalRatio() {
      if (this.totalFlow === 0) return '0.00';
      return (((this.mainIn + this.mainOut) / this.totalFlow) * 100).toFixed(2);
    },
    mainInRatio() {
      if (this.totalFlow === 0) return 0;
      return (this.mainIn / this.totalFlow) * 100;
    },
    mainOutRatio() {
      if (this.totalFlow === 0) return 0;
      return (this.mainOut / this.totalFlow) * 100;
    },
    smallInRatio() {
      if (this.totalFlow === 0) return 0;
      return (this.smallIn / this.totalFlow) * 100;
    },
    smallOutRatio() {
      if (this.totalFlow === 0) return 0;
      return (this.smallOut / this.totalFlow) * 100;
    },
    analyzeTitle() {
      if (this.totalFlow === 0) return "暂无数据";

      const smallRatio = (this.smallIn + this.smallOut) / this.totalFlow;
      if (smallRatio > 0.80) {
        return "散户交投活跃，主力参与度低";
      }

      const netMain = this.mainIn - this.mainOut;
      const diffRatio = Math.abs(netMain) / this.totalFlow;

      if (diffRatio <= 0.05) {
        return "多空博弈激烈，资金未见明显方向";
      }

      if (netMain > 0) {
        return "主力资金呈现明显净流入";
      }

      return "主力资金呈现明显净流出";
    }
  },
  mounted() {
    this.initChart();
    window.addEventListener('resize', this.resizeChart);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeChart);
    if (this.chartInstance) {
      this.chartInstance.dispose();
    }
  },
  watch: {
    flowData: {
      deep: true,
      handler() {
        this.updateChart();
      }
    }
  },
  methods: {
    formatAmount(val) {
      if (!val) return '0.00';
      return (val / 10000000).toFixed(2); // 转换为千万
    },
    formatRatio(val) {
      if (!val) return '0.0';
      return val.toFixed(1);
    },
    initChart() {
      if (!this.$refs.pieChart) return;
      this.chartInstance = echarts.init(this.$refs.pieChart);
      this.updateChart();
    },
    updateChart() {
      if (!this.chartInstance) return;

      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {d}%',
          textStyle: { fontFamily: FONT_FAMILY }
        },
        series: [
          {
            name: '资金流向',
            type: 'pie',
            radius: ['50%', '80%'],
            avoidLabelOverlap: false,
            itemStyle: {
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: false
              }
            },
            labelLine: {
              show: false
            },
            data: [
              { value: this.mainInRatio, name: '主力流入', itemStyle: { color: '#E82B3E' } },
              { value: this.smallInRatio, name: '散户流入', itemStyle: { color: '#FFA0A8' } },
              { value: this.smallOutRatio, name: '散户流出', itemStyle: { color: '#BDECE0' } },
              { value: this.mainOutRatio, name: '主力流出', itemStyle: { color: '#099364' } }
            ]
          }
        ]
      };

      this.chartInstance.setOption(option);
    },
    resizeChart() {
      if (this.chartInstance) {
        this.chartInstance.resize();
      }
    }
  }
};
</script>

<style lang="scss" scoped>
$font-family-song: "SimSun", "Songti SC", "STSong", "AR PL New Sung", "NSimSun", serif;

.fund-flow-card {
  font-family: $font-family-song;
  margin-bottom: 20px;

  .chart-header {
    span {
      font-weight: bold;
      font-size: 16px;
    }
  }

  .fund-flow-container {
    padding: 10px;

    .flow-title {
      margin-bottom: 20px;
      .main-title {
        margin: 0 0 5px 0;
        font-size: 18px;
        font-weight: bold;
        color: #333;
      }
      .sub-title {
        margin: 0;
        font-size: 13px;
        color: #888;
      }
    }

    .chart-content {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .pie-chart-wrapper {
        flex: 1;
        display: flex;
        justify-content: center;
        align-items: center;

        .pie-chart {
          width: 200px;
          height: 200px;
        }
      }

      .side-info {
        display: flex;
        flex-direction: column;
        justify-content: center;
        width: 120px;

        .info-item {
          .amount-val {
            font-size: 18px;
            font-weight: bold;
            display: flex;
            align-items: center;

            .dot {
              display: inline-block;
              width: 8px;
              height: 8px;
              border-radius: 50%;
              margin-right: 6px;
            }
            .green-dot { background-color: #099364; }
            .light-green-dot { background-color: #BDECE0; }
            .red-dot { background-color: #E82B3E; }
            .light-red-dot { background-color: #FFA0A8; }
          }
          .desc {
            font-size: 12px;
            color: #666;
            margin-top: 4px;
            margin-left: 14px;
          }
        }
      }

      .left-side {
        .amount-val { color: #099364; }
        .small-out .amount-val { color: #83C7B6; } /* Slightly darker for text legibility than the pie slice */
      }
      .right-side {
        .amount-val { color: #E82B3E; }
        .small-in .amount-val { color: #F06E7D; } /* Slightly darker for text legibility */
      }
    }
  }
}

@media (max-width: 768px) {
  .fund-flow-container .chart-content {
    flex-direction: column;

    .side-info {
      width: 100%;
      flex-direction: row;
      justify-content: space-around;
      margin: 15px 0;

      .info-item {
        margin-top: 0 !important;
      }
    }
  }
}
</style>
