<template>
  <div class="el-table el-table--enable-row-hover el-table--medium">
    <table cellspacing="0" style="width: 100%;">
      <thead>
      <tr>
        <th class="el-table__cell is-leaf"><div class="cell">属性</div></th>
        <th class="el-table__cell is-leaf"><div class="cell">内存</div></th>
        <th class="el-table__cell is-leaf"><div class="cell">JVM</div></th>
      </tr>
      </thead>
      <tbody>
      <tr>
        <td class="el-table__cell is-leaf"><div class="cell">总内存</div></td>
        <td class="el-table__cell is-leaf"><div class="cell">{{ memory.total }}G</div></td>
        <td class="el-table__cell is-leaf"><div class="cell">{{ jvm.total }}M</div></td>
      </tr>
      <tr>
        <td class="el-table__cell is-leaf"><div class="cell">已用内存</div></td>
        <td class="el-table__cell is-leaf"><div class="cell">{{ memory.used }}G</div></td>
        <td class="el-table__cell is-leaf"><div class="cell">{{ jvm.used }}M</div></td>
      </tr>
      <tr>
        <td class="el-table__cell is-leaf"><div class="cell">剩余内存</div></td>
        <td class="el-table__cell is-leaf"><div class="cell">{{ memory.free }}G</div></td>
        <td class="el-table__cell is-leaf"><div class="cell">{{ jvm.free }}M</div></td>
      </tr>
      <tr>
        <td class="el-table__cell is-leaf"><div class="cell">使用率</div></td>
        <td class="el-table__cell is-leaf">
          <div class="cell" :class="{'text-danger': memory.usage > 80}">{{ memory.usage }}%</div>
        </td>
        <td class="el-table__cell is-leaf">
          <div class="cell" :class="{'text-danger': jvm.usage > 80}">{{ jvm.usage }}%</div>
        </td>
      </tr>
      </tbody>
    </table>
  </div>
</template>

<script>
export default {
  name: 'MemoryTable',
  props: {
    memory: {
      type: Object,
      required: true
    },
    jvm: {
      type: Object,
      required: true
    }
  }
}
</script>

<style scoped>
.text-danger {
  color: #f56c6c;
}
</style>
