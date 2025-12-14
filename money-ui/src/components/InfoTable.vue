<template>
  <el-table
    :data="tableData"
    style="width: 100%"
    border
    class="info-table"
    :header-cell-style="{ background: '#f5f7fa', fontWeight: 'bold' }"
  >
    <el-table-column label="属性" prop="label" />
    <el-table-column label="值" prop="value" />
  </el-table>
</template>

<script>
export default {
  name: "InfoTable",
  props: {
    tableData: {
      type: Array,
      required: true
    }
  }
}
</script>

<style scoped>
.info-table {
  font-size: 14px;
}
</style>
