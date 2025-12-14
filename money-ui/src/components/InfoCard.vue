<template>
  <div class="app-container">
    <el-row :gutter="10">
      <!-- CPU 信息卡片 -->
      <InfoCard title="CPU" icon="el-icon-cpu">
        <InfoTable :data="cpuTable"/>
      </InfoCard>

      <!-- 内存信息卡片 -->
      <InfoCard title="内存" icon="el-icon-tickets">
        <MemoryTable :mem="server.mem" :jvm="server.jvm"/>
      </InfoCard>

      <!-- 服务器基本信息 -->
      <InfoCard title="服务器信息" icon="el-icon-monitor">
        <SystemInfo :sys="server.sys"/>
      </InfoCard>

      <!-- JVM 信息 -->
      <InfoCard title="Java虚拟机信息" icon="el-icon-coffee-cup">
        <JvmInfo :jvm="server.jvm" :sys="server.sys"/>
      </InfoCard>

      <!-- 磁盘状态 -->
      <InfoCard title="磁盘状态" icon="el-icon-receiving">
        <DiskTable :disks="server.sysFiles"/>
      </InfoCard>
    </el-row>
  </div>
</template>

<script>
import {getServer} from "@/api/monitor/server"
import InfoCard from "@/components/InfoCard.vue"
import InfoTable from "@/components/InfoTable.vue"
import MemoryTable from "@/components/MemoryTable.vue"
import SystemInfo from "@/components/SystemInfo.vue"
import JvmInfo from "@/components/JvmInfo.vue"
import DiskTable from "@/components/DiskTable.vue"

export default {
  name: "Server",
  components: {
    InfoCard,
    InfoTable,
    MemoryTable,
    SystemInfo,
    JvmInfo,
    DiskTable
  },
  data() {
    return {
      server: []
    }
  },
  created() {
    this.getList()
    this.openLoading()
  },
  methods: {
    getList() {
      getServer().then(response => {
        this.server = response.data
        this.$modal.closeLoading()
      })
    },
    openLoading() {
      this.$modal.loading("正在加载服务监控数据，请稍候！")
    }
  }
}
</script>
