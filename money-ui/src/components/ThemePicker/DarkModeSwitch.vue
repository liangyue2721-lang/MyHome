<template>
  <div class="dark-mode-switch">
    <el-tooltip effect="dark" :content="isDark ? '切换到日间模式' : '切换到夜间模式'" placement="bottom">
      <el-switch
        v-model="isDark"
        active-icon="el-icon-moon"
        inactive-icon="el-icon-sunny"
        @change="toggleDarkMode"
        active-color="#2c3e50"
        inactive-color="#409EFF">
      </el-switch>
    </el-tooltip>
  </div>
</template>

<script>
export default {
  name: 'DarkModeSwitch',
  data() {
    return {
      isDark: false
    }
  },
  mounted() {
    // 检查本地存储中的主题设置
    const storedTheme = localStorage.getItem('theme-mode')
    if (storedTheme) {
      this.isDark = storedTheme === 'dark'
      this.applyTheme(storedTheme)
    } else {
      // 检查系统主题偏好
      const prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
      this.isDark = prefersDark
      this.applyTheme(prefersDark ? 'dark' : 'light')
    }
  },
  methods: {
    toggleDarkMode(val) {
      const theme = val ? 'dark' : 'light'
      localStorage.setItem('theme-mode', theme)
      this.applyTheme(theme)
    },
    applyTheme(theme) {
      if (theme === 'dark') {
        document.body.classList.add('dark-mode')
        document.body.classList.remove('light-mode')
      } else {
        document.body.classList.add('light-mode')
        document.body.classList.remove('dark-mode')
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.dark-mode-switch {
  display: flex;
  align-items: center;
  margin-right: 10px;
  
  ::v-deep .el-switch__core {
    border-radius: 12px;
    width: 40px !important;
    height: 20px;
  }
  
  ::v-deep .el-switch__inner {
    display: none;
  }
  
  ::v-deep .el-switch__action {
    width: 16px;
    height: 16px;
  }
}
</style>