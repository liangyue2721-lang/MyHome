<template>
  <InfoCard icon="el-icon-monitor" title="服务器信息">
    <div class="el-table el-table--enable-row-hover el-table--medium">
      <table cellspacing="0" style="width: 100%;">
        <tbody>
        <tr>
          <td class="el-table__cell is-leaf"><div class="cell">服务器名称</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">{{ sys?.computerName }}</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">操作系统</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">{{ sys?.osName }}</div></td>
        </tr>
        <tr>
          <td class="el-table__cell is-leaf"><div class="cell">服务器IP</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">{{ sys?.computerIp }}</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">系统架构</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">{{ sys?.osArch }}</div></td>
        </tr>
        </tbody>
      </table>
    </div>
  </InfoCard>
</template>

<script>
import InfoCard from './InfoCard.vue'

export default {
  name: 'SystemInfo',
  components: { InfoCard },
  props: {
    sys: Object
  }
}
</script>
