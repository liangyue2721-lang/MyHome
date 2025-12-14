<template>
  <div class="chart-container">
    <div ref="chart" class="chart"></div>
  </div>
</template>

<script>
import * as echarts from "echarts";
import "echarts-gl";

export default {
  name: "Investment3DPieChart",
  props: {
    data: {
      type: Array,
      default: () => []
    }
  },
  data() {
    return {
      chart: null,
      sliceMap: {} // 记录每个扇形的3个series索引
    };
  },
  mounted() {
    this.initChart();
    window.addEventListener("resize", this.resizeChart);
  },
  beforeDestroy() {
    // 清理事件监听器
    if (this.chart) {
      this.chart.off("mouseover");
      this.chart.off("mouseout");
      this.chart.dispose();
    }
    window.removeEventListener("resize", this.resizeChart);
  },
  watch: {
    data: {
      handler() {
        this.updateChart();
      },
      deep: true
    }
  },
  methods: {
    initChart() {
      this.chart = echarts.init(this.$refs.chart);
      this.updateChart();
    },

    updateChart() {
      // 如果没有数据，显示提示信息
      if (!this.chart || !this.data || this.data.length === 0) {
        const option = {
          title: {
            text: '暂无数据',
            left: 'center',
            top: 'center'
          }
        };
        this.chart.setOption(option, true);
        return;
      }

      // 处理投资记录数据
      const processedData = this.processInvestmentData(this.data);
      
      if (processedData.length === 0) {
        const option = {
          title: {
            text: '暂无数据',
            left: 'center',
            top: 'center'
          }
        };
        this.chart.setOption(option, true);
        return;
      }

      const total = processedData.reduce((s, i) => s + i.value, 0);
      const colors = ["#4ECDC4", "#45B7D1", "#FF6B6B", "#9B59B6", "#F1C40F", "#E67E22", "#1ABC9C"];
      let start = 0;
      let series = [];
      this.sliceMap = {};

      processedData.forEach((item, idx) => {
        const value = item.value;
        const end = start + value / total;
        const color = colors[idx % colors.length];
        const name = item.name;
        const percent = ((value / total) * 100).toFixed(1);

        // 创建扇形的三个部分，增大初始大小 (半径从1增加到1.2，厚度从0.3增加到0.4)
        const sliceParts = this.createCylinderSlice(start, end, 1.2, 0.4, color, name, value, percent);

        // 记录索引映射
        this.sliceMap[idx] = [];
        sliceParts.forEach((s) => {
          this.sliceMap[idx].push(series.length);
          series.push(s);
        });

        start = end;
      });

      const option = {
        title: {
          text: '投资收益分析图',
          left: 'center',
          textStyle: {
            color: '#333',
            fontSize: 18,
            fontWeight: 'bold'
          }
        },
        tooltip: {
          formatter: (params) => {
            const item = processedData[params.seriesIndex];
            if (item) {
              const percent = ((item.value / total) * 100).toFixed(1);
              return `${params.seriesName}<br/>收益: ${item.value.toFixed(2)}<br/>占比: ${percent}%`;
            }
            return params.seriesName;
          }
        },
        xAxis3D: { min: -2.5, max: 2.5 },
        yAxis3D: { min: -2.5, max: 2.5 },
        zAxis3D: { min: -1.5, max: 1.5 },
        grid3D: {
          show: false,
          viewControl: {
            projection: "perspective",
            distance: 200,
            autoRotate: false,
            autoRotateSpeed: 20,
            rotateSensitivity: 1,
            zoomSensitivity: 1,
            panSensitivity: 1,
            rotateX: 45,
            rotateY: 45,
            zoom: 1.5 // 增大缩放比例以适应增大的饼图
          }
        },
        series
      };

      this.chart.setOption(option, true);

      // 添加鼠标交互事件
      this.addMouseEvents();
    },

    /**
     * 处理投资记录数据
     * @param {Array} data 投资记录数据
     * @returns {Array} 处理后的数据 [{name, value}]
     */
    processInvestmentData(data) {
      if (!data || !Array.isArray(data) || data.length === 0) {
        return [];
      }

      // 按股票名称合并数据
      const mergedData = {};
      
      data.forEach(item => {
        const investType = item.investType || '未知股票';
        const periodProfit = Math.abs(parseFloat(item.periodProfit) || 0);
        
        if (!mergedData[investType]) {
          mergedData[investType] = {
            name: investType,
            value: 0,
            count: 0
          };
        }
        
        mergedData[investType].value += periodProfit;
        mergedData[investType].count++;
      });
      
      // 转换为数组并按收益排序
      const result = Object.values(mergedData)
        .filter(item => item.value > 0) // 过滤掉收益为0的项目
        .sort((a, b) => b.value - a.value); // 按收益降序排列
      
      // 如果数据项超过5个，将后面的合并为"其他"
      if (result.length > 5) {
        const topFive = result.slice(0, 4);
        const others = result.slice(4);
        
        let othersValue = 0;
        others.forEach(item => {
          othersValue += item.value;
        });
        
        if (othersValue > 0) {
          topFive.push({
            name: '其他',
            value: othersValue
          });
        }
        
        return topFive;
      }
      
      return result;
    },

    /**
     * 生成一个圆柱扇形的三个部分
     * @param {Number} startRatio 起始比例 (0-1)
     * @param {Number} endRatio   结束比例 (0-1)
     * @param {Number} r 半径
     * @param {Number} h 厚度
     * @param {String} color 颜色
     * @param {String} name 名称
     * @param {Number} value 收益值
     * @param {Number} percent 百分比
     */
    createCylinderSlice(startRatio, endRatio, r, h, color, name, value, percent) {
      const startAng = startRatio * Math.PI * 2;
      const endAng = endRatio * Math.PI * 2;

      // 顶面 - 修改为实心扇形
      const top = {
        name: `${name}\n${value.toFixed(2)}\n${percent}%`,
        type: "surface",
        parametric: true,
        wireframe: { show: false },
        shading: "realistic",
        itemStyle: { color, opacity: 0.95 },
        label: {
          show: true,
          position: 'outside',
          formatter: `{name|{b}}\n{value|收益: ${value.toFixed(2)}}\n{percent|占比: ${percent}%}`,
          fontSize: 14,
          fontWeight: 'bold',
          backgroundColor: 'rgba(255,255,255,0.8)',
          padding: [5, 10],
          borderRadius: 4,
          rich: {
            name: {
              color: '#333',
              fontSize: 14,
              fontWeight: 'bold'
            },
            value: {
              color: '#666',
              fontSize: 12,
              lineHeight: 20
            },
            percent: {
              color: '#666',
              fontSize: 12
            }
          }
        },
        parametricEquation: {
          u: { min: startAng, max: endAng, step: Math.PI / 180 },
          v: { min: 0, max: r, step: 0.01 },
          x: (u, v) => v * Math.cos(u),
          y: (u, v) => v * Math.sin(u),
          z: () => h / 2
        }
      };

      // 底面 - 修改为实心扇形
      const bottom = {
        ...top,
        parametricEquation: {
          u: { min: startAng, max: endAng, step: Math.PI / 180 },
          v: { min: 0, max: r, step: 0.01 },
          x: (u, v) => v * Math.cos(u),
          y: (u, v) => v * Math.sin(u),
          z: () => -h / 2
        }
      };

      // 侧壁
      const side = {
        name: `${name}\n${value.toFixed(2)}\n${percent}%`,
        type: "surface",
        parametric: true,
        wireframe: { show: false },
        shading: "realistic",
        itemStyle: { color, opacity: 0.95 },
        parametricEquation: {
          u: { min: startAng, max: endAng, step: Math.PI / 180 },
          v: { min: -h / 2, max: h / 2, step: 0.01 },
          x: (u, v) => r * Math.cos(u),
          y: (u, v) => r * Math.sin(u),
          z: (u, v) => v
        }
      };

      return [top, bottom, side];
    },

    addMouseEvents() {
      // 悬停事件
      this.chart.on("mouseover", (p) => {
        if (p.seriesIndex != null) {
          const idx = this.findSliceIndex(p.seriesIndex);
          if (idx != null) this.explodeSlice(idx, true);
        }
      });
      
      this.chart.on("mouseout", (p) => {
        if (p.seriesIndex != null) {
          const idx = this.findSliceIndex(p.seriesIndex);
          if (idx != null) this.explodeSlice(idx, false);
        }
      });
    },

    /** 根据 seriesIndex 找到所属 slice */
    findSliceIndex(seriesIdx) {
      for (let k in this.sliceMap) {
        if (this.sliceMap[k].includes(seriesIdx)) return parseInt(k);
      }
      return null;
    },

    /** 扇形突出/恢复 */
    explodeSlice(idx, expand) {
      const option = this.chart.getOption();
      const sliceSeriesIdxs = this.sliceMap[idx];
      if (!sliceSeriesIdxs) return;

      // 找到角度区间
      const series = option.series[sliceSeriesIdxs[0]];
      const startAng = series.parametricEquation.u.min;
      const endAng = series.parametricEquation.u.max;
      const midAng = (startAng + endAng) / 2;

      const targetOffset = expand ? 0.2 : 0;

      sliceSeriesIdxs.forEach((si) => {
        // 保存原始函数引用，如果尚未保存
        if (!option.series[si].hasOwnProperty('originalX')) {
          option.series[si].originalX = option.series[si].parametricEquation.x;
          option.series[si].originalY = option.series[si].parametricEquation.y;
        }

        // 应用偏移量到原始函数结果上
        option.series[si].parametricEquation.x = (u, v) => {
          const originalX = option.series[si].originalX(u, v);
          return originalX + targetOffset * Math.cos(midAng);
        };
        
        option.series[si].parametricEquation.y = (u, v) => {
          const originalY = option.series[si].originalY(u, v);
          return originalY + targetOffset * Math.sin(midAng);
        };
      });

      // 使用setOption的过渡动画
      this.chart.setOption(option, {
        notMerge: false,
        replaceMerge: [],
        transition: {
          duration: 300,
          easing: 'cubicOut'
        }
      });
    },

    resizeChart() {
      if (this.chart) this.chart.resize();
    }
  }
};
</script>

<style scoped>
.chart-container {
  width: 100%;
  height: 100%;
}
.chart {
  width: 100%;
  height: 700px;
}
</style>