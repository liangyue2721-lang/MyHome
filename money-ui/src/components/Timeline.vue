<template>
  <div class="timeline-container">
    <div class="timeline" :class="{ 'mobile': isMobile }">
      <transition-group name="timeline" tag="div" class="timeline-group">
        <div 
          class="timeline-item"
          v-for="(item, index) in timelineData"
          :key="index"
          :class="{ 'alternate': index % 2 === 1 && !isMobile }"
        >
          <div class="timeline-content-wrapper">
            <div class="timeline-icon" :style="{ backgroundColor: item.color }">
              <i :class="item.icon"></i>
            </div>
            <div class="timeline-card" :style="{ borderLeftColor: item.color }">
              <div class="timeline-year">{{ item.year }}</div>
              <h3 class="timeline-title">{{ item.title }}</h3>
              <p class="timeline-desc">{{ item.desc }}</p>
            </div>
          </div>
          <div class="timeline-line"></div>
        </div>
      </transition-group>
    </div>
  </div>
</template>

<script>
export default {
  name: 'Timeline',
  props: {
    timelineData: {
      type: Array,
      required: true,
      default: () => []
    }
  },
  data() {
    return {
      isMobile: false
    }
  },
  mounted() {
    this.checkIsMobile()
    window.addEventListener('resize', this.checkIsMobile)
  },
  beforeUnmount() {
    window.removeEventListener('resize', this.checkIsMobile)
  },
  methods: {
    checkIsMobile() {
      this.isMobile = window.innerWidth <= 768
    }
  }
}
</script>

<style lang="scss" scoped>
.timeline-container {
  width: 100%;
  padding: 20px 0;
}

.timeline {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    width: 4px;
    background: linear-gradient(to bottom, #4285f4, #34a853, #f4b400, #ea4335, #673ab7);
    border-radius: 2px;
  }
  
  &.mobile::before {
    left: 30px;
  }
  
  &::before {
    left: 50%;
    transform: translateX(-50%);
  }
}

.timeline-group {
  width: 100%;
}

.timeline-item {
  position: relative;
  width: 100%;
  padding: 20px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  
  &.alternate {
    align-self: flex-end;
    
    .timeline-content-wrapper {
      flex-direction: row-reverse;
      
      .timeline-card {
        text-align: right;
        margin-right: 30px;
        margin-left: 0;
        border-left: none;
        border-right: 4px solid;
      }
    }
  }
}

.timeline-content-wrapper {
  display: flex;
  align-items: center;
  width: 50%;
  max-width: 500px;
  z-index: 2;
  
  .timeline-card {
    flex: 1;
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
    margin-left: 30px;
    border-left: 4px solid;
    transition: transform 0.3s ease, box-shadow 0.3s ease;
    
    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 6px 16px rgba(0, 0, 0, 0.15);
    }
    
    .timeline-year {
      font-size: 14px;
      font-weight: bold;
      margin-bottom: 8px;
      color: #666;
    }
    
    .timeline-title {
      margin: 0 0 10px 0;
      font-size: 18px;
      color: #333;
    }
    
    .timeline-desc {
      margin: 0;
      color: #666;
      line-height: 1.5;
      white-space: pre-line;
    }
  }
}

.timeline-icon {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 24px;
  z-index: 3;
  transition: transform 0.3s ease;
  
  &:hover {
    transform: scale(1.1);
  }
}

.timeline-line {
  position: absolute;
  top: 0;
  bottom: 0;
  width: 4px;
  background: transparent;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    bottom: 0;
    width: 100%;
    background: linear-gradient(to bottom, #4285f4, #34a853, #f4b400, #ea4335, #673ab7);
    border-radius: 2px;
  }
}

.timeline-item:nth-child(1) .timeline-line::before {
  background: #4285f4;
}

.timeline-item:nth-child(2) .timeline-line::before {
  background: #34a853;
}

.timeline-item:nth-child(3) .timeline-line::before {
  background: #f4b400;
}

.timeline-item:nth-child(4) .timeline-line::before {
  background: #ea4335;
}

.timeline-item:nth-child(5) .timeline-line::before {
  background: #673ab7;
}

/* 动画效果 */
.timeline-enter-active {
  transition: all 0.5s ease;
}

.timeline-enter-from {
  opacity: 0;
  transform: scale(0.8);
}

.timeline-enter-to {
  opacity: 1;
  transform: scale(1);
}

.mobile {
  &::before {
    left: 30px;
  }
  
  .timeline-item {
    padding-left: 70px;
    align-items: flex-start;
    
    &.alternate {
      align-self: auto;
      
      .timeline-content-wrapper {
        flex-direction: row;
        
        .timeline-card {
          text-align: left;
          margin-left: 30px;
          margin-right: 0;
          border-right: none;
          border-left: 4px solid;
        }
      }
    }
  }
  
  .timeline-content-wrapper {
    width: calc(100% - 70px);
    
    .timeline-card {
      margin-left: 30px;
      margin-right: 0;
    }
  }
  
  .timeline-line {
    left: 30px;
  }
}

@media (max-width: 768px) {
  .timeline::before {
    left: 30px;
  }
  
  .timeline-item {
    padding-left: 70px;
    align-items: flex-start;
    
    &.alternate {
      align-self: auto;
      
      .timeline-content-wrapper {
        flex-direction: row;
        
        .timeline-card {
          text-align: left;
          margin-left: 30px;
          margin-right: 0;
          border-right: none;
          border-left: 4px solid;
        }
      }
    }
  }
  
  .timeline-content-wrapper {
    width: calc(100% - 70px);
    
    .timeline-card {
      margin-left: 30px;
      margin-right: 0;
    }
  }
  
  .timeline-line {
    left: 30px;
  }
}
</style>