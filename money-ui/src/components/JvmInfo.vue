<template>
  <el-card>
    <div slot="header">
      <span><i class="el-icon-coffee-cup"></i> Java虚拟机信息</span>
    </div>
    <div class="el-table el-table--enable-row-hover el-table--medium">
      <table cellspacing="0" style="width: 100%;table-layout:fixed;">
        <tbody>
        <tr>
          <td class="el-table__cell is-leaf"><div class="cell">Java名称</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">{{ jvm.name }}</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">Java版本</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">{{ jvm.version }}</div></td>
        </tr>
        <tr>
          <td class="el-table__cell is-leaf"><div class="cell">启动时间</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">{{ jvm.startTime }}</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">运行时长</div></td>
          <td class="el-table__cell is-leaf"><div class="cell">{{ jvm.runTime }}</div></td>
        </tr>
        <tr>
          <td colspan="1" class="el-table__cell is-leaf"><div class="cell">安装路径</div></td>
          <td colspan="3" class="el-table__cell is-leaf"><div class="cell">{{ jvm.home }}</div></td>
        </tr>
        <tr>
          <td colspan="1" class="el-table__cell is-leaf"><div class="cell">项目路径</div></td>
          <td colspan="3" class="el-table__cell is-leaf"><div class="cell">{{ userDir }}</div></td>
        </tr>
        <tr>
          <td colspan="1" class="el-table__cell is-leaf"><div class="cell">运行参数</div></td>
          <td colspan="3" class="el-table__cell is-leaf"><div class="cell">{{ jvm.inputArgs }}</div></td>
        </tr>
        </tbody>
      </table>
    </div>
  </el-card>
</template>

<script>
export default {
  name: "JvmInfo",
  props: {
    jvm: {
      type: Object,
      required: true
    },
    userDir: {
      type: String,
      required: true
    }
  }
};
</script>
