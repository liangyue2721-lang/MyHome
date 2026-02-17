<template>
  <div class="app-container home">
    <el-row class="wealth-stage-row" v-if="wealthStage.current">
      <el-col :span="24">
        <el-card shadow="never" class="wealth-stage-card">
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

          <div class="stages-container">
            <div
              v-for="(stage) in visibleStages"
              :key="stage.name"
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
                <span v-if="wealthStage.totalAssets >= stage.max" style="font-size: 18px;">{{ stage.icon }}</span>
                <span v-else-if="wealthStage.totalAssets >= stage.min && wealthStage.totalAssets < stage.max"
                      style="font-size: 18px; color: #fff;">{{ stage.icon }}</span>
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
import {
  getMonthlyIncomeBarChart,
  getProfitLineData,
  renderLoanRepaymentComparisonChart
} from "@/api/finance/pieChart";
import {getAnnualSummary} from "@/api/finance/annual_deposit_summary";
import Cookies from 'js-cookie';

// === 核心修改：细化后的财富阶梯 (19个阶段) ===
const WEALTH_STAGES = [
  {name: '负债阶段', min: -Infinity, max: 0, desc: '需优先处理债务黑洞', icon: '⛺', customColor: '#F56C6C'},
  {name: '生存艰难', min: 0, max: 30000, desc: '解决温饱是首要任务', icon: '🛖', customColor: '#E6A23C'}, // 橙色
  {name: '起步基石', min: 30000, max: 50000, desc: '积累原始资本的开始', icon: '🧱', customColor: '#E6A23C'}, // 橙色
  {name: '半程冲刺', min: 50000, max: 60000, desc: '距离下一大关仅一步之遥', icon: '🏃', customColor: '#67C23A'}, // 绿色-转折点
  {name: '温饱无忧', min: 60000, max: 100000, desc: '生活开始有了基本保障', icon: '🏠', customColor: '#67C23A'},
  {name: '第一桶金', min: 100000, max: 150000, desc: '六位数存款，信心倍增', icon: '💰', customColor: '#67C23A'},
  {name: '小康入门', min: 150000, max: 200000, desc: '抗风险能力显著提升', icon: '🚲', customColor: '#409EFF'}, // 蓝色-成长期
  {name: '稳健筑基', min: 200000, max: 300000, desc: '拥有约一辆车的等值资产', icon: '🚗', customColor: '#409EFF'},
  {name: '中产起步', min: 300000, max: 400000, desc: '典型的城市中产门槛', icon: '🏘️', customColor: '#409EFF'},
  {name: '中产进阶', min: 400000, max: 500000, desc: '生活质量有质的飞跃', icon: '🚤', customColor: '#409EFF'},
  {name: '资深中产', min: 500000, max: 800000, desc: '半个百万富翁，房产首付', icon: '🏡', customColor: '#1890FF'}, // 深蓝
  {name: '百万冲刺', min: 800000, max: 1000000, desc: '蓄力冲击七位数大关', icon: '🚀', customColor: '#1890FF'},
  {name: '百万富翁', min: 1000000, max: 2000000, desc: '资产达到A7，进入富人圈', icon: '💎', customColor: '#722ED1'}, // 紫色-财富期
  {name: '房产自由', min: 2000000, max: 3000000, desc: '非一线城市房产自由', icon: '🔑', customColor: '#722ED1'},
  {name: '初级财自', min: 3000000, max: 5000000, desc: 'Lean FIRE，被动收入', icon: '🌴', customColor: '#722ED1'},
  {name: '高净值圈', min: 5000000, max: 8000000, desc: '银行私行客户门槛', icon: '🏦', customColor: '#C71585'}, // 紫红
  {name: '千万预备', min: 8000000, max: 10000000, desc: '向A8资产发起最后冲击', icon: '🏰', customColor: '#C71585'},
  {name: 'A8俱乐部', min: 10000000, max: 20000000, desc: '千万富翁，阶级跨越', icon: '👑', customColor: '#FFD700'}, // 金色
  {name: '顶级富豪', min: 20000000, max: Infinity, desc: '用资本制定规则', icon: '🏙️', customColor: '#FFD700'}
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

      // 图表实例
      charts: {
        monthlyIncomeExpense: null,
        generateMonthlyLoanRepayment: null,
        profitLine: null,
      },
    };
  },
  computed: {
    // === 核心逻辑：智能聚焦窗口 ===
    // 只显示 [当前阶段前1个] ~ [当前阶段后5个]，避免页面过于拥挤
    visibleStages() {
      // 1. 如果尚未获取到当前阶段，默认显示前6个
      if (!this.wealthStage.current) return this.allStages.slice(0, 6);

      const all = this.allStages;
      const currentIndex = all.findIndex(s => s.name === this.wealthStage.current.name);

      // 异常情况兜底
      if (currentIndex === -1) return all.slice(0, 6);

      // 2. 计算显示窗口
      // 总是尝试显示前一个阶段作为回顾（index - 1）
      let start = Math.max(0, currentIndex - 1);
      // 总共显示 6-7 个节点
      let end = start + 7;

      // 3. 边界处理：如果接近尾部，向左调整窗口
      if (end > all.length) {
        end = all.length;
        // 保证窗口大小不变，除非总长度不够
        start = Math.max(0, end - 7);
      }

      return all.slice(start, end);
    }
  },
  mounted() {
    this.fetchWealthStage();
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

          // 查找当前阶段
          let stageIndex = WEALTH_STAGES.findIndex(s => totalAssets >= s.min && totalAssets < s.max);
          // 处理边界：如果超出最大值或小于最小值
          if (stageIndex === -1) {
            if (totalAssets >= WEALTH_STAGES[WEALTH_STAGES.length - 1].min) {
              stageIndex = WEALTH_STAGES.length - 1;
            } else {
              stageIndex = 0;
            }
          }

          this.wealthStage.current = WEALTH_STAGES[stageIndex];

          // 计算下一阶段距离
          if (stageIndex < WEALTH_STAGES.length - 1) {
            this.wealthStage.next = WEALTH_STAGES[stageIndex + 1];
            const currentMin = this.wealthStage.current.min === -Infinity ? 0 : this.wealthStage.current.min;
            const currentMax = this.wealthStage.current.max;
            const range = currentMax - currentMin;
            this.wealthStage.gap = currentMax - totalAssets;
            // 计算进度百分比
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
      if (val >= 10000) return (val / 10000).toFixed(0) + '万'; // 简化显示为“3万”
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

    loadAllCharts() {
      this.loadMixedChart('monthlyIncomeExpense', 'monthlyIncomeExpenseBarChart', getMonthlyIncomeBarChart, '每月收支', '元', ['收入', '支出', '结余']);
      this.loadMixedChart('generateMonthlyLoanRepayment', 'generateMonthlyLoanRepaymentBarChart', renderLoanRepaymentComparisonChart, '还贷本息', '元', ['贷款偿还']);
      this.loadLineChart('profitLine', 'profitLineChart', getProfitLineData);
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

    loadLineChart(key, domId, apiFn) {
      apiFn({userId: this.selectedUserId}).then(response => {
        const chart = this.initChart(key, domId);
        if (!chart) return;

        let lineData = [];
        let barData = [];

        if (response && typeof response === 'object' && !Array.isArray(response)) {
          lineData = response.line || [];
          barData = response.bar || [];
        } else if (Array.isArray(response)) {
          lineData = response;
        }

        const xDataLine = lineData.map(item => item.recordDate);
        const yDataLine = lineData.map(item => item.profit);
        const xDataBar = barData.map(item => new Date(item.recordDate).getFullYear());
        const yDataBar = barData.map(item => item.profit);

        chart.setOption({
          backgroundColor: '#fff',
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255,255,255,0.95)',
            padding: 12,
            axisPointer: {type: 'cross', label: {backgroundColor: '#6a7985'}},
            formatter: (params) => {
              let html = `<div style="font-weight:bold; margin-bottom:5px;">📅 ${params[0].axisValue}</div>`;
              params.forEach(p => {
                html += `<div style="display:flex; justify-content:space-between; align-items:center;">
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
              gridIndex: 0
            },
            {
              type: 'category',
              data: xDataBar,
              axisLine: {lineStyle: {color: '#ccc'}},
              gridIndex: 1
            }
          ],
          yAxis: [
            {type: 'value', splitLine: {lineStyle: {color: '#f0f0f0'}}, gridIndex: 0},
            {type: 'value', splitLine: {lineStyle: {color: '#f0f0f0'}}, gridIndex: 1}
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
                {type: 'max', name: '最高', label: {formatter: '{c}'}},
                {type: 'min', name: '最低', label: {formatter: '{c}'}}
              ]
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
                color: '#333'
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
      /* 增加滚动适配，防止小屏幕挤在一起 */
      overflow-x: auto;

      &::-webkit-scrollbar {
        display: none;
      }

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
        min-width: 80px; /* 防止压缩太小 */

        .stage-bar {
          display: none;
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
    .stages-container {
      /* 移动端增加一些padding防止切边 */
      padding: 0 15px !important;
    }
  }
}
</style>
