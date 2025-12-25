<template>
  <div class="app-container">
    <stock-task-table />
  </div>
</template>

<script>
import StockTaskTable from "./StockTaskTable";

export default {
  name: "StockTaskMonitor",
  components: {
    StockTaskTable
  }
};
</script>
