<template>
  <div ref="chart" class="treemap-container"></div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'MarketTreemap',
  props: {
    data: {
      type: Array,
      required: true
    }
  },
  mounted() {
    this.initChart();
    window.addEventListener('resize', this.resizeChart);
  },
  methods: {
    initChart() {
      if (!this.$refs.chart) return;
      this.chart = echarts.init(this.$refs.chart);
      this.setOption();
    },
    // 计算透明度：防止 percent 为 0 时颜色完全透明（看起来像白色）
    _alphaFromPercent(percent) {
      const p = typeof percent === 'number' && !isNaN(percent) ? Math.abs(percent) : 0;
      const a = Math.min(p / 5, 1); // 5% -> 1 的映射
      const MIN_ALPHA = 0.2;       // **关键：给一个下限，避免全部变成白色**
      return Math.max(a, MIN_ALPHA);
    },
    setOption() {
      if (!this.chart) return;

      const seriesData = (this.data || []).map(item => {
        const rawValue = Number(item.value) || 0;          // 原始资金流（可正可负）
        const percent = (item.percent === undefined || item.percent === null)
          ? 0
          : Number(item.percent);
        const alpha = this._alphaFromPercent(percent);
        const color = rawValue >= 0
          ? `rgba(255,0,0,${alpha})`   // 资金**流入** -> 红色
          : `rgba(0,128,0,${alpha})`;  // 资金**流出** -> 绿色

        return {
          name: item.name || '未知',
          value: Math.abs(rawValue),   // treemap 面积需非负
          rawValue,
          percent,
          itemStyle: { color }         // **把颜色直接写到每个节点，避免被层级样式影响**
        };
      });

      const option = {
        tooltip: {
          formatter: (info) => {
            const { name, rawValue, percent } = info.data;
            const sign = rawValue > 0 ? '+' : '';
            return `板块：${name || '未知'}<br/>资金：${sign}${rawValue}元<br/>涨跌幅：${percent}%`;
          }
        },
        series: [
          {
            type: 'treemap',
            roam: false,
            nodeClick: false,
            breadcrumb: { show: false },
            label: {
              show: true,
              formatter: (params) => {
                const { name, rawValue, percent } = params.data;
                const sign = rawValue > 0 ? '+' : '';
                return `${name || '未知'}\n${sign}${rawValue}元\n${percent > 0 ? '+' : ''}${percent}%`;
              },
              fontSize: 14,
              align: 'center'
            },
            // 下面是边框/间隙样式，与颜色无关
            itemStyle: {
              borderColor: '#fff',
              borderWidth: 2,
              borderRadius: 6
            },
            levels: [
              {
                itemStyle: {
                  borderColor: '#fff',
                  borderWidth: 2,
                  gapWidth: 2
                }
              }
            ],
            data: seriesData
          }
        ]
      };

      // notMerge 为 true，避免旧颜色样式残留
      this.chart.setOption(option, true);
    },
    resizeChart() {
      if (this.chart) {
        this.chart.resize();
      }
    }
  },
  watch: {
    data: {
      handler() {
        this.setOption();
      },
      deep: true
    }
  },
  beforeDestroy() {
    if (this.chart) {
      this.chart.dispose();
    }
    window.removeEventListener('resize', this.resizeChart);
  }
}
</script>

<style scoped>
.treemap-container {
  width: 100%;
  height: 600px;
}
</style>
