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
              <span class="amount">
                <span v-if="!isPrivacyMode">¥ {{ wealthStage.totalAssets.toLocaleString() }}</span>
                <span v-else>¥ ******</span>
                <i class="el-icon-view" @click="togglePrivacy"
                   style="margin-left: 8px; cursor: pointer; color: #909399;"></i>
              </span>
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
                <span v-if="wealthStage.totalAssets >= stage.max" style="font-size: 18px;">{{ stage.icon }}</span>
                <!-- Current: Show Icon (White on Custom Background) -->
                <span v-else-if="wealthStage.totalAssets >= stage.min && wealthStage.totalAssets < stage.max"
                      style="font-size: 18px; color: #fff;">{{ stage.icon }}</span>
                <!-- Future: Show Icon (Gray) -->
                <span v-else style="font-size: 18px; filter: grayscale(100%); opacity: 0.5;">{{ stage.icon }}</span>
              </div>
              <div class="stage-content">
                <div class="stage-name"
                     :style="wealthStage.totalAssets >= stage.min && wealthStage.totalAssets < stage.max ? { color: stage.customColor, fontWeight: 'bold' } : {}">
                  {{ stage.name }}
                </div>
                <div class="stage-range">{{ formatMoney(stage.min) }}</div>
              </div>
            </div>
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
          <div id="profitLineChart" class="chart-box" :class="{ 'privacy-blur': isPrivacyMode }"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>💳 近一年还贷对比</span>
          </div>
          <div id="generateMonthlyLoanRepaymentBarChart" class="chart-box"
               :class="{ 'privacy-blur': isPrivacyMode }"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :lg="12">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <span>💰 月度收支对比</span>
          </div>
          <div id="monthlyIncomeExpenseBarChart" class="chart-box" :class="{ 'privacy-blur': isPrivacyMode }"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts';
// 1. 仅引入页面实际渲染需要的接口
import {
  getMonthlyIncomeBarChart,           // 对应 id="monthlyIncomeExpenseBarChart"
  getProfitLineData,                  // 对应 id="profitLineChart"
  renderLoanRepaymentComparisonChart  // 对应 id="generateMonthlyLoanRepaymentBarChart"
} from "@/api/finance/pieChart";
import {getAnnualSummary} from "@/api/finance/annual_deposit_summary"; // 对应 财富阶段
import Cookies from 'js-cookie';

const WEALTH_STAGES = [
  {name: '负债阶段', min: -Infinity, max: 0, desc: '随时可能被风雨（风险）摧毁', icon: '⛺', customColor: '#F56C6C'},
  {name: '生存艰难', min: 0, max: 27000, desc: '仅能满足遮风避雨的最低需求', icon: '🛖', customColor: '#67C23A'},
  {name: '贫穷阶段', min: 27000, max: 60000, desc: '有了固定的形状，但设施简陋', icon: '🏠', customColor: '#909399'},
  {name: '低收入阶段', min: 60000, max: 150000, desc: '标准化生活，依靠集体设施', icon: '🏢', customColor: '#E6A23C'},
  {name: '中下产阶段', min: 150000, max: 300000, desc: '有了私人空间（安全缓冲）', icon: '🏘️', customColor: '#409EFF'},
  {name: '中产阶段', min: 300000, max: 500000, desc: '典型的中产标志，独立且舒适', icon: '🏡', customColor: '#67C23A'},
  {
    name: '中上产阶段',
    min: 500000,
    max: 1000000,
    desc: '资产属性大于居住属性，象征投资',
    icon: '🏬',
    customColor: '#1890FF'
  },
  {name: '富人阶段', min: 1000000, max: 8000000, desc: '奢侈、享受、财务自由的象征', icon: '🏰', customColor: '#722ED1'},
  {
    name: '富豪阶段',
    min: 8000000,
    max: 20000000,
    desc: '家族基业，防御性强，代代相传',
    icon: '🏯',
    customColor: '#C71585'
  },
  {
    name: '大富豪阶段',
    min: 20000000,
    max: Infinity,
    desc: '拥有并规划一座城市，制定规则',
    icon: '🏙️',
    customColor: '#FFD700'
  }
];

export default {
  name: 'Index',
  data() {
    return {
      isPrivacyMode: localStorage.getItem('money_privacy_mode') !== 'false',
      selectedUserId: null,

      // 财富阶段数据
      wealthStage: {
        current: null,
        next: null,
        totalAssets: 0,
        progress: 0,
        gap: 0,
        loading: false
      },
      allStages: WEALTH_STAGES,

      // 仅保留页面存在的3个图表实例
      charts: {
        monthlyIncomeExpense: null,
        generateMonthlyLoanRepayment: null,
        profitLine: null,
      },
    };
  },
  mounted() {
    // 1. 获取财富阶段数据
    this.fetchWealthStage();

    // 2. 初始化用户并加载图表
    this.initUserList().then(() => {
      this.$nextTick(() => {
        this.loadAllCharts();
        window.addEventListener('resize', this.resizeCharts);
      });
    });
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    this.disposeCharts();
  },
  methods: {
    initUserList() {
      return new Promise((resolve) => {
        const userId = Cookies.get('userId');
        if (userId) this.selectedUserId = userId;
        resolve();
      });
    },

    togglePrivacy() {
      this.isPrivacyMode = !this.isPrivacyMode;
      localStorage.setItem('money_privacy_mode', this.isPrivacyMode);
    },

    fetchWealthStage() {
      this.wealthStage.loading = true;
      getAnnualSummary().then(response => {
        const payload = response.data || response;
        if (payload) {
          const totalAssets = Number(payload.totalDeposit) || 0;
          this.wealthStage.totalAssets = totalAssets;

          let stageIndex = WEALTH_STAGES.findIndex(s => totalAssets >= s.min && totalAssets < s.max);
          if (stageIndex === -1) {
            if (totalAssets >= WEALTH_STAGES[WEALTH_STAGES.length - 1].min) {
              stageIndex = WEALTH_STAGES.length - 1;
            } else {
              stageIndex = 0;
            }
          }

          this.wealthStage.current = WEALTH_STAGES[stageIndex];

          if (stageIndex < WEALTH_STAGES.length - 1) {
            this.wealthStage.next = WEALTH_STAGES[stageIndex + 1];
            const currentMin = this.wealthStage.current.min === -Infinity ? 0 : this.wealthStage.current.min;
            const currentMax = this.wealthStage.current.max;
            const range = currentMax - currentMin;
            this.wealthStage.gap = currentMax - totalAssets;
            this.wealthStage.progress = range > 0
              ? Math.min(100, Math.max(0, ((totalAssets - currentMin) / range) * 100))
              : 100;
          } else {
            this.wealthStage.next = null;
            this.wealthStage.gap = 0;
            this.wealthStage.progress = 100;
          }
        }
      }).catch(err => console.error("Failed to fetch wealth stage", err))
        .finally(() => {
          this.wealthStage.loading = false;
        });
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

    disposeCharts() {
      Object.values(this.charts).forEach(chart => chart && chart.dispose());
    },

    initChart(key, domId) {
      const dom = document.getElementById(domId);
      if (!dom) return null;
      if (this.charts[key]) this.charts[key].dispose();
      this.charts[key] = echarts.init(dom);
      return this.charts[key];
    },

    // 仅加载页面上存在的 3 个图表
    loadAllCharts() {
      // 1. 月度收支对比 (Mixed Chart)
      this.loadMixedChart('monthlyIncomeExpense', 'monthlyIncomeExpenseBarChart', getMonthlyIncomeBarChart, '每月收支', '元', ['收入', '支出', '结余']);
      // 2. 近一年还贷对比 (Mixed Chart)
      this.loadMixedChart('generateMonthlyLoanRepayment', 'generateMonthlyLoanRepaymentBarChart', renderLoanRepaymentComparisonChart, '还贷本息', '元', ['贷款偿还']);
      // 3. 利润趋势分析 (Line Chart)
      this.loadLineChart('profitLine', 'profitLineChart', getProfitLineData);
    },

    // 混合图表加载器（柱状 + 折线），用于收支和还贷
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
              label: {show: true, position: 'top', formatter: `{c}`, color: colorStart, fontWeight: 'bold'}
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
          legend: {data: legendData, top: 0},
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

    // 折线图加载器，用于利润分析
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
                </div>`;
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

          .stage-name {
            color: #606266;
          }
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

          .stage-range {
            color: #606266;
          }
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

  .privacy-blur {
    filter: blur(10px);
    transition: filter 0.3s ease;
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
