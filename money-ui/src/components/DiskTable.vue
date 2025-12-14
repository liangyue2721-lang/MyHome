<template>
  <div class="disk-table">
    <el-table :data="sysFiles" style="width: 100%">
      <el-table-column prop="dirName" label="盘符路径" />
      <el-table-column prop="sysTypeName" label="文件系统" />
      <el-table-column prop="typeName" label="盘符类型" />
      <el-table-column prop="total" label="总大小" />
      <el-table-column prop="free" label="可用大小" />
      <el-table-column prop="used" label="已用大小" />
      <el-table-column label="已用百分比">
        <template v-slot="scope">
          <span :class="{ 'text-danger': scope.row.usage > 80 }">
            {{ scope.row.usage }}%
          </span>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script>
export default {
  name: "DiskTable",
  props: {
    sysFiles: {
      type: Array,
      required: true
    }
  }
};
</script>

<style scoped>
.text-danger {
  color: red;
}
</style>
