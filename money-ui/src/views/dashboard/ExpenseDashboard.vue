<template>
  <div class="expense-dashboard">
    <!-- Header Section -->
    <div class="header-section">
      <div class="header-left">
        <h3 class="title">月度支出明细</h3>
      </div>
      <div class="header-right">
        <span class="total-label">总支出</span>
        <span class="total-amount">¥ {{ formattedTotal }}</span>
        <el-tag size="small" type="info" class="item-count">{{ items.length }} 项</el-tag>
      </div>
    </div>

    <!-- Grid List Section -->
    <el-row :gutter="16" class="grid-container">
      <el-col
        :xs="24" :sm="12" :md="8" :lg="6"
        v-for="(item, index) in processedItems"
        :key="index"
        class="grid-item-col"
      >
        <div class="expense-card">
          <!-- Key Expenditure Badge -->
          <div v-if="item.isKeyExpense" class="key-badge">关键支出</div>

          <div class="card-content">
            <div class="item-info">
              <div class="item-icon">{{ item.icon }}</div>
              <div class="item-name">{{ item.name }}</div>
            </div>
            <div class="item-amount">¥ {{ item.formattedAmount }}</div>
          </div>

          <!-- Progress Section -->
          <div class="progress-section">
            <div class="progress-track">
              <div
                class="progress-fill"
                :style="{ width: item.percentage + '%', backgroundColor: item.isKeyExpense ? '#F56C6C' : '#409EFF' }"
              ></div>
            </div>
            <span class="percentage-text" :class="{ 'text-danger': item.isKeyExpense }">
              {{ item.percentage }}%
            </span>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script>
export default {
  name: 'ExpenseDashboard',
  props: {
    totalAmount: {
      type: Number,
      required: true,
      default: 0
    },
    items: {
      type: Array,
      required: true,
      default: () => []
    }
  },
  computed: {
    formattedTotal() {
      return this.formatNumber(this.totalAmount);
    },
    processedItems() {
      if (!this.totalAmount || this.totalAmount <= 0) return [];

      return this.items.map(item => {
        const percentageValue = (item.amount / this.totalAmount) * 100;
        const percentageStr = percentageValue.toFixed(1);

        return {
          ...item,
          icon: this.getIconFromName(item.name),
          formattedAmount: this.formatNumber(item.amount),
          percentage: percentageStr,
          isKeyExpense: percentageValue > 30
        };
      });
    }
  },
  methods: {
    formatNumber(num) {
      if (num === null || num === undefined) return '0.00';
      return Number(num).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    getIconFromName(name) {
      const iconMap = {
        '房租': '🏠',
        '月供': '🏦',
        '餐饮': '🍔',
        '交通': '🚗',
        '购物': '🛍️',
        '娱乐': '🎮',
        '水电煤': '⚡',
        '通信': '📱',
        '医疗': '🏥',
        '教育': '📚',
        '人情': '🤝',
        '信用卡': '💳',
        '零食': '🍩',
        '服饰': '👕',
        '旅行': '✈️',
        '日用': '🧼'
      };

      for (const key in iconMap) {
        if (name && name.includes(key)) {
          return iconMap[key];
        }
      }
      return '💸'; // Default icon
    }
  }
};
</script>

<style lang="scss" scoped>
.expense-dashboard {
  background-color: var(--theme-bg-color, transparent);
  margin-bottom: 20px;
  border-radius: 8px;

  .header-section {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    padding: 0 4px;

    .header-left {
      .title {
        margin: 0;
        font-size: 16px;
        font-weight: 600;
        color: #303133;
        position: relative;
        padding-left: 10px;

        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 50%;
          transform: translateY(-50%);
          width: 4px;
          height: 16px;
          background-color: #409EFF;
          border-radius: 2px;
        }
      }
    }

    .header-right {
      display: flex;
      align-items: center;
      font-size: 14px;

      .total-label {
        color: #909399;
        margin-right: 8px;
      }

      .total-amount {
        font-weight: bold;
        font-size: 18px;
        color: #303133;
        margin-right: 12px;
      }

      .item-count {
        border-radius: 12px;
      }
    }
  }

  .grid-container {
    .grid-item-col {
      margin-bottom: 16px;
    }
  }

  .expense-card {
    background-color: #f8f9fb; /* 浅一层深灰/灰白背景 */
    border-radius: 8px;
    padding: 16px;
    position: relative;
    border: 1px solid #ebeef5;
    transition: all 0.3s;
    height: 100%;
    display: flex;
    flex-direction: column;
    justify-content: space-between;

    &:hover {
      box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
      transform: translateY(-2px);
    }

    .key-badge {
      position: absolute;
      top: 12px;
      right: 12px;
      font-size: 10px;
      color: #F56C6C;
      border: 1px solid #F56C6C;
      padding: 2px 6px;
      border-radius: 4px;
      background: transparent;
      transform: scale(0.9);
      transform-origin: right top;
      font-weight: 500;
    }

    .card-content {
      margin-bottom: 16px;

      .item-info {
        display: flex;
        align-items: center;
        margin-bottom: 12px;

        .item-icon {
          font-size: 20px;
          margin-right: 10px;
          display: flex;
          align-items: center;
          justify-content: center;
          width: 32px;
          height: 32px;
          background-color: #fff;
          border-radius: 6px;
          box-shadow: 0 1px 4px rgba(0, 0, 0, 0.05);
        }

        .item-name {
          font-size: 14px;
          color: #606266;
          font-weight: 500;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }
      }

      .item-amount {
        font-size: 20px;
        font-weight: bold;
        color: #303133;
        font-family: 'DIN Alternate', 'Helvetica Neue', Helvetica, Arial, sans-serif;
      }
    }

    .progress-section {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .progress-track {
        flex: 1;
        height: 3px; /* 极细进度条 */
        background-color: #EBEEF5;
        border-radius: 2px;
        margin-right: 10px;
        overflow: hidden;

        .progress-fill {
          height: 100%;
          border-radius: 2px;
          transition: width 0.5s ease-out;
        }
      }

      .percentage-text {
        font-size: 12px;
        color: #909399;
        font-weight: 500;
        min-width: 40px;
        text-align: right;

        &.text-danger {
          color: #F56C6C;
        }
      }
    }
  }
}
</style>
