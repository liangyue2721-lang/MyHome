<template>
  <div class="app-container home">
    <!-- 1. 顶部欢迎与信息区域 -->
    <el-card class="welcome-card mb-20" shadow="hover">
      <div class="welcome-wrapper">
        <!-- 左侧：头像与问候 -->
        <div class="welcome-left">
          <!-- 修复点：使用 require() 加载本地图片，或者换回网络图片 -->
          <!-- 方式一：加载本地图片 (确保文件存在) -->
          <!-- <el-avatar :size="60" :src="require('@/assets/images/profile.jpg')" class="user-avatar"></el-avatar> -->

          <!-- 方式二：为了演示效果，这里暂时改回网络图片，如果您有本地图片，请使用方式一的写法 -->
          <el-avatar :size="60" src="https://wpimg.wallstcn.com/f778738c-e4f8-4870-b634-56703b4acafe.gif"
                     class="user-avatar"></el-avatar>

          <div class="welcome-text">
            <div class="greeting">{{ greeting }}，{{ username || 'Admin' }}</div>
            <div class="subtitle">祝您今天心情愉快，工作顺利！</div>
          </div>
        </div>

        <!-- 右侧：时间与天气 -->
        <div class="welcome-right">
          <div class="weather-box" v-loading="weatherLoading">
            <i :class="weather.icon" class="weather-icon"></i>
            <span class="weather-text">{{ weather.type }} {{ weather.temp }}°C</span>
            <span class="weather-tips">{{ weather.tips }}</span>
          </div>
          <div class="time-box">
            <div class="time">{{ currentTime }}</div>
            <div class="date">{{ currentDate }} {{ currentWeek }}</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 2. 利润趋势分析图表 -->
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <div class="header-title">
              <i class="el-icon-data-line" style="color: #409EFF; margin-right: 8px;"></i>
              <span>利润趋势分析</span>
            </div>
            <el-tag size="small" effect="plain">年度数据</el-tag>
          </div>
          <div id="profitLineChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 3. 近一年还贷对比图表 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :span="24">
        <el-card class="chart-card" shadow="hover">
          <div slot="header" class="chart-header">
            <div class="header-title">
              <i class="el-icon-money" style="color: #E6A23C; margin-right: 8px;"></i>
              <span>近一年还贷对比</span>
            </div>
            <el-tooltip content="显示每月贷款偿还金额趋势" placement="top">
              <i class="el-icon-info text-gray"></i>
            </el-tooltip>
          </div>
          <div id="generateMonthlyLoanRepaymentBarChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import {
  getProfitLineData,
  renderLoanRepaymentComparisonChart
} from "@/api/finance/pieChart";
import {listUser} from "@/api/stock/dropdown_component";
import Cookies from 'js-cookie';

export default {
  name: 'Index',
  data() {
    return {
      // 用户相关
      userList: [],
      selectedUserId: null,
      username: '',
      userLoading: false,

      // 时间相关
      timer: null,
      currentTime: '',
      currentDate: '',
      currentWeek: '',
      greeting: '',

      // 天气数据
      weatherLoading: false,
      weather: {
        type: '加载中...',
        temp: '--',
        tips: '正在获取天气...',
        icon: 'el-icon-sunny' // 默认图标
      },

      // 图表实例存储
      charts: {
        profitLine: null,
        generateMonthlyLoanRepayment: null,
      },
    };
  },
  mounted() {
    // 1. 初始化时间
    this.startTimer();
    // 2. 获取用户名 (增加默认值防止为空)
    this.username = Cookies.get('username') || '用户';
    // 3. 获取天气
    this.getWeather();
    // 4. 初始化用户列表并加载图表
    this.initUserList().then(() => {
      if (this.selectedUserId) {
        this.loadAllCharts();
      }
      window.addEventListener('resize', this.resizeCharts);
    });
  },
  beforeDestroy() {
    if (this.timer) clearInterval(this.timer);
    window.removeEventListener('resize', this.resizeCharts);
    this.disposeCharts();
  },
  methods: {
    // ================= 天气逻辑 =================
    getWeather() {
      this.weatherLoading = true;
      // 使用 fetch 请求天气接口 (不经过 request.js 拦截器，避免 /dev-api 前缀问题)
      fetch('https://wttr.in/Beijing?format=j1')
        .then(response => response.json())
        .then(data => {
          if (data && data.current_condition && data.current_condition[0]) {
            const cur = data.current_condition[0];
            const descEn = cur.weatherDesc[0].value;

            this.weather = {
              type: this.translateWeather(descEn),
              temp: cur.temp_C,
              tips: `湿度 ${cur.humidity}% | 风速 ${cur.windspeedKmph}km/h`,
              icon: this.getWeatherIcon(descEn)
            };
          }
        })
        .catch(err => {
          console.error('获取天气失败:', err);
          this.weather.type = '获取失败';
          this.weather.tips = '请检查网络';
        })
        .finally(() => {
          this.weatherLoading = false;
        });
    },
    // 简单翻译天气描述
    translateWeather(desc) {
      const d = desc.toLowerCase();
      if (d.includes('sunny') || d.includes('clear')) return '晴';
      if (d.includes('partly cloudy')) return '多云';
      if (d.includes('cloudy') || d.includes('overcast')) return '阴';
      if (d.includes('rain') || d.includes('drizzle') || d.includes('shower')) return '雨';
      if (d.includes('snow') || d.includes('ice') || d.includes('blizzard')) return '雪';
      if (d.includes('fog') || d.includes('mist') || d.includes('haze')) return '雾';
      if (d.includes('thunder')) return '雷雨';
      return desc; // 默认返回英文
    },
    // 匹配 Element UI 图标
    getWeatherIcon(desc) {
      const d = desc.toLowerCase();
      if (d.includes('sun') || d.includes('clear')) return 'el-icon-sunny';
      if (d.includes('partly cloudy')) return 'el-icon-cloudy-and-sunny';
      if (d.includes('cloud') || d.includes('overcast')) return 'el-icon-cloudy';
      if (d.includes('rain') || d.includes('drizzle')) return 'el-icon-light-rain';
      if (d.includes('snow') || d.includes('ice')) return 'el-icon-heavy-rain';
      if (d.includes('thunder') || d.includes('storm')) return 'el-icon-lightning';
      return 'el-icon-sunny';
    },

    // ================= 时间逻辑 =================
    startTimer() {
      this.updateTime();
      this.timer = setInterval(this.updateTime, 1000);
    },
    updateTime() {
      const date = new Date();
      const y = date.getFullYear();
      const m = String(date.getMonth() + 1).padStart(2, '0');
      const d = String(date.getDate()).padStart(2, '0');
      this.currentDate = `${y}年${m}月${d}日`;

      const weeks = ['星期日', '星期一', '星期二', '星期三', '星期四', '星期五', '星期六'];
      this.currentWeek = weeks[date.getDay()];

      const hh = String(date.getHours()).padStart(2, '0');
      const mm = String(date.getMinutes()).padStart(2, '0');
      const ss = String(date.getSeconds()).padStart(2, '0');
      this.currentTime = `${hh}:${mm}:${ss}`;

      const hour = date.getHours();
      if (hour >= 5 && hour < 12) this.greeting = '早上好';
      else if (hour >= 12 && hour < 14) this.greeting = '中午好';
      else if (hour >= 14 && hour < 18) this.greeting = '下午好';
      else if (hour >= 18 && hour < 24) this.greeting = '晚上好';
      else this.greeting = '夜深了';
    },

    // ================= 基础数据逻辑 =================
    async initUserList() {
      this.userLoading = true;
      try {
        const response = await listUser({pageSize: 1000});
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

    // ================= 图表通用方法 =================
    initChart(key, domId) {
      const dom = document.getElementById(domId);
      if (!dom) return null;
      if (this.charts[key]) this.charts[key].dispose();
      this.charts[key] = echarts.init(dom);
      return this.charts[key];
    },
    disposeCharts() {
      Object.values(this.charts).forEach(chart => chart && chart.dispose());
    },
    resizeCharts() {
      Object.values(this.charts).forEach(chart => chart && chart.resize());
    },

    loadAllCharts() {
      this.loadProfitLineChart();
      this.loadLoanRepaymentChart();
    },

    // ================= 1. 利润趋势折线图 =================
    loadProfitLineChart() {
      getProfitLineData(this.selectedUserId).then(data => {
        const chart = this.initChart('profitLine', 'profitLineChart');
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
            textStyle: {color: '#333'},
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
          grid: {left: '3%', right: '4%', bottom: '3%', top: '15%', containLabel: true},
          xAxis: {
            type: 'category',
            boundaryGap: false,
            data: xData,
            axisLine: {lineStyle: {color: '#ccc'}}
          },
          yAxis: {
            type: 'value',
            splitLine: {lineStyle: {color: '#f0f0f0'}}
          },
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
      }).catch(e => console.error("利润图加载失败", e));
    },

    // ================= 2. 近一年还贷对比 =================
    loadLoanRepaymentChart() {
      renderLoanRepaymentComparisonChart(this.selectedUserId).then(data => {
        const chart = this.initChart('generateMonthlyLoanRepayment', 'generateMonthlyLoanRepaymentBarChart');
        if (!chart) return;

        const xData = data.map(i => i.transactionTime);
        const yData = data.map(i => i.supportOutAmount);

        const colorStart = '#E6A23C';
        const colorEnd = '#F3D19E';

        chart.setOption({
          tooltip: {
            trigger: 'axis',
            backgroundColor: 'rgba(255,255,255,0.95)',
            axisPointer: {type: 'shadow'},
            formatter: (params) => {
              const p = params[0];
              return `
                <div style="font-weight:bold;margin-bottom:5px;">📅 ${p.axisValue}</div>
                <div style="display:flex; justify-content:space-between;">
                  <span style="margin-right:15px;">${p.marker} 贷款偿还</span>
                  <span style="font-weight:bold;">${p.value} 元</span>
                </div>`;
            }
          },
          legend: {data: ['贷款偿还', '趋势'], top: 0},
          grid: {top: 40, left: '3%', right: '4%', bottom: 20, containLabel: true},
          xAxis: {type: 'category', data: xData, axisLine: {lineStyle: {color: '#ddd'}}},
          yAxis: {type: 'value', name: '元', splitLine: {lineStyle: {type: 'dashed', color: '#f0f0f0'}}},
          dataZoom: [{
            type: 'slider',
            height: 15,
            bottom: 5,
            borderColor: 'transparent',
            backgroundColor: '#f5f7fa',
            handleStyle: {color: '#E6A23C'}
          }],
          series: [
            {
              name: '贷款偿还',
              type: 'bar',
              data: yData,
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
            },
            {
              name: '趋势',
              type: 'line',
              data: yData,
              smooth: true,
              symbol: 'none',
              lineStyle: {width: 3, color: colorStart},
              tooltip: {show: false}
            }
          ]
        });
      }).catch(e => console.error("还贷图表加载失败", e));
    }
  }
};
</script>

<style scoped lang="scss">
@import "~@/assets/styles/global.scss";
.app-container {
  padding: 20px;
  background-color: #f6f8fa;
  min-height: calc(100vh - 84px);
}

/* --- 欢迎卡片样式 --- */
.welcome-card {
  border: none;
  background: linear-gradient(135deg, #ffffff 0%, #f0f7ff 100%);
  border-radius: 8px;

  ::v-deep .el-card__body {
    padding: 20px 30px;
  }
}

.welcome-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-left {
  display: flex;
  align-items: center;

  .user-avatar {
    border: 2px solid #fff;
    box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  }

  .welcome-text {
    margin-left: 20px;

    .greeting {
      font-size: 20px;
      font-weight: bold;
      color: #303133;
      margin-bottom: 8px;
    }

    .subtitle {
      font-size: 14px;
      color: #909399;
    }
  }
}

.welcome-right {
  display: flex;
  align-items: center;
  gap: 30px; /* 元素间距 */

  .user-selector {
    min-width: 120px;
  }

  .weather-box {
    display: flex;
    align-items: center;

    .weather-icon {
      font-size: 24px;
      color: #E6A23C;
      margin-right: 8px;
    }

    .weather-text {
      font-size: 16px;
      color: #606266;
      font-weight: 500;
      margin-right: 10px;
    }

    .weather-tips {
      font-size: 12px;
      color: #67C23A;
      background: #f0f9eb;
      padding: 2px 6px;
      border-radius: 4px;
    }
  }

  .time-box {
    text-align: right;
    border-left: 1px solid #e6e6e6;
    padding-left: 30px;

    .time {
      font-size: 24px;
      font-weight: bold;
      color: #303133;
      font-family: 'Helvetica Neue', sans-serif;
      line-height: 1.2;
    }

    .date {
      font-size: 13px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

/* --- 图表卡片样式 --- */
.chart-card {
  border-radius: 8px;
  border: none;
  background: #fff;
  transition: all 0.3s;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.08);
  }
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;

  .header-title {
    font-size: 16px;
    font-weight: 600;
    color: #303133;
    display: flex;
    align-items: center;
  }

  .text-gray {
    color: #909399;
    cursor: pointer;
  }
}

.chart-box {
  width: 100%;
  height: 380px;
}

/* 辅助类 */
.mb-20 {
  margin-bottom: 20px;
}

.mt-20 {
  margin-top: 20px;
}

/* 响应式适配 */
@media (max-width: 992px) {
  .welcome-wrapper {
    flex-direction: column;
    align-items: flex-start;
  }

  .welcome-right {
    margin-top: 20px;
    width: 100%;
    flex-wrap: wrap;
    justify-content: space-between;
    gap: 15px;

    .time-box {
      border-left: none;
      padding-left: 0;
      text-align: right;
    }
  }
}
</style>
