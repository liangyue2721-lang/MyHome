<template>
  <div class="app-container">
    <el-tabs v-model="activeName" type="card" @tab-click="handleTabClick">
      <el-tab-pane label="Topics (主题)" name="topics">
        <el-table v-loading="loading" :data="topics" style="width: 100%">
          <el-table-column prop="name" label="Topic Name (主题名称)" />
          <el-table-column prop="partitionCount" label="Partitions (分区数)" width="150" />
          <el-table-column prop="replicationFactor" label="Replicas (副本数)" width="150" />
          <el-table-column prop="totalMessageCount" label="Total Messages (消息总数)" width="220" />
          <el-table-column label="Actions (操作)" width="300" align="center">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-search" @click="handleViewMessages(scope.row)">Inspect (查看)</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" class="text-danger" @click="handleClearMessages(scope.row)">Clear Msg (清空)</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete-solid" class="text-danger" @click="handleDeleteTopic(scope.row)">Delete (删除)</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="Stock Tasks (股票任务)" name="stockTasks">
        <el-row style="margin-bottom: 10px;">
          <el-button type="danger" icon="el-icon-delete" @click="handleClearStockStatus">Clear All Status (清空所有状态)</el-button>
          <span style="margin-left: 10px; color: #909399; font-size: 12px;">Warning: This clears the monitoring view only. (此操作仅清空监控视图)</span>
        </el-row>
        <el-table v-loading="stockLoading" :data="stockTasks" style="width: 100%">
          <el-table-column prop="stockCode" label="Code (代码)" width="120" />
          <el-table-column prop="stockName" label="Name (名称)" width="150" />
          <el-table-column prop="status" label="Status (状态)" width="120">
            <template slot-scope="scope">
              <el-tag :type="getStatusType(scope.row.status)">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="occupiedByNode" label="Node (节点)" width="140" show-overflow-tooltip />
          <el-table-column prop="traceId" label="Trace ID (追踪ID)" width="250" show-overflow-tooltip />
          <el-table-column prop="lastResult" label="Last Result (最近结果)" show-overflow-tooltip />
          <el-table-column prop="lastUpdateTime" label="Update Time (更新时间)" width="180">
            <template slot-scope="scope">
              {{ parseTime(scope.row.lastUpdateTime) }}
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-container">
          <el-pagination
            background
            layout="total, sizes, prev, pager, next, jumper"
            :total="stockTotal"
            :page-sizes="[10, 20, 50]"
            :page-size.sync="stockParams.pageSize"
            :current-page.sync="stockParams.pageNum"
            @current-change="handleStockPageChange"
            @size-change="handleStockSizeChange"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="Consumer Groups (消费组)" name="consumers">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="box-card">
              <div slot="header">
                <span>Groups (列表)</span>
                <el-button style="float: right; padding: 3px 0" type="text" @click="refreshConsumers">Refresh</el-button>
              </div>
              <el-table
                :data="consumerGroups"
                highlight-current-row
                @current-change="handleGroupChange"
                style="width: 100%">
                <el-table-column label="Group ID (组ID)">
                  <template slot-scope="scope">
                    {{ scope.row }}
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
          <el-col :span="18">
            <el-card class="box-card">
              <div slot="header">
                <span>Details (详情): {{ selectedGroup }}</span>
                <el-button
                  v-if="selectedGroup && selectedGroupDetails && selectedGroupDetails.totalLag > 0"
                  style="float: right; padding: 3px 0; color: #F56C6C"
                  type="text"
                  @click="handleResetOffset">
                  Skip Backlog (跳过积压)
                </el-button>
              </div>
              <div v-if="selectedGroupDetails">
                <el-descriptions :column="3" border>
                  <el-descriptions-item label="State (状态)">{{ selectedGroupDetails.state }}</el-descriptions-item>
                  <el-descriptions-item label="Coordinator (协调节点)">{{ selectedGroupDetails.coordinator }}</el-descriptions-item>
                  <el-descriptions-item label="Total Lag (总堆积)">{{ selectedGroupDetails.totalLag }}</el-descriptions-item>
                </el-descriptions>

                <el-divider content-position="left">Partitions (分区详情)</el-divider>

                <el-table :data="selectedGroupDetails.partitions" style="width: 100%" stripe>
                  <el-table-column prop="topic" label="Topic (主题)" />
                  <el-table-column prop="partition" label="Partition (分区)" width="120" />
                  <el-table-column prop="currentOffset" label="Current Offset (当前位移)" width="150" />
                  <el-table-column prop="logEndOffset" label="End Offset (结束位移)" width="150" />
                  <el-table-column prop="lag" label="Lag (堆积)">
                    <template slot-scope="scope">
                      <span :class="scope.row.lag > 0 ? 'text-danger' : 'text-success'">{{ scope.row.lag }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="clientId" label="Client ID (客户端ID)" show-overflow-tooltip />
                  <el-table-column prop="host" label="Host (主机)" show-overflow-tooltip />
                </el-table>
              </div>
              <div v-else class="text-center text-muted">
                Select a group to view details (请选择消费组查看详情)
              </div>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>

    <!-- Message Inspection Dialog -->
    <el-dialog title="Topic Messages (Recent 10)" :visible.sync="messageDialogVisible" width="80%">
      <el-table :data="topicMessages" border style="width: 100%" height="500">
        <el-table-column prop="partition" label="Part" width="60" />
        <el-table-column prop="offset" label="Offset" width="100" />
        <el-table-column prop="timestamp" label="Timestamp" width="160">
          <template slot-scope="scope">
            {{ parseTime(scope.row.timestamp) }}
          </template>
        </el-table-column>
        <el-table-column prop="key" label="Key" width="200" show-overflow-tooltip />
        <el-table-column prop="value" label="Value" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listTopics, listConsumers, getConsumerDetails, deleteTopic, deleteTopicMessages, getTopicMessages, listStockTasks, resetConsumerOffset, clearStockStatus } from "@/api/tool/kafka";

export default {
  name: "KafkaMonitor",
  data() {
    return {
      activeName: 'topics',
      loading: false,
      topics: [],
      consumerGroups: [], // List of strings
      selectedGroup: null,
      selectedGroupDetails: null,
      messageDialogVisible: false,
      topicMessages: [],
      // Stock Task Data
      stockLoading: false,
      stockTasks: [],
      stockTotal: 0,
      stockParams: {
        pageNum: 1,
        pageSize: 10
      }
    };
  },
  created() {
    this.handleTabClick({ name: this.activeName });
  },
  methods: {
    handleTabClick(tab) {
      if (tab.name === 'topics') {
        this.getTopics();
      } else if (tab.name === 'consumers') {
        this.getConsumers();
      } else if (tab.name === 'stockTasks') {
        this.getStockTasks();
      }
    },
    getStockTasks() {
      this.stockLoading = true;
      listStockTasks(this.stockParams).then(response => {
        this.stockTasks = response.rows;
        this.stockTotal = response.total;
        this.stockLoading = false;
      });
    },
    handleStockPageChange(val) {
      this.stockParams.pageNum = val;
      this.getStockTasks();
    },
    handleStockSizeChange(val) {
      this.stockParams.pageSize = val;
      this.getStockTasks();
    },
    getStatusType(status) {
      if (status === 'RUNNING') return 'primary';
      if (status === 'SUCCESS') return 'success';
      if (status === 'FAILED') return 'danger';
      if (status === 'WAITING') return 'warning';
      return 'info';
    },
    getTopics() {
      this.loading = true;
      listTopics().then(response => {
        this.topics = response.data;
        this.loading = false;
      });
    },
    getConsumers() {
      listConsumers().then(response => {
        this.consumerGroups = response.data;
      });
    },
    refreshConsumers() {
      this.getConsumers();
      if (this.selectedGroup) {
        this.fetchGroupDetails(this.selectedGroup);
      }
    },
    handleGroupChange(currentRow) {
      if (currentRow) {
        this.selectedGroup = currentRow;
        this.fetchGroupDetails(currentRow);
      }
    },
    fetchGroupDetails(groupId) {
      getConsumerDetails([groupId]).then(response => {
        if (response.data && response.data.length > 0) {
          this.selectedGroupDetails = response.data[0];
        }
      });
    },
    handleViewMessages(row) {
      this.topicMessages = [];
      getTopicMessages(row.name, 10).then(response => {
        this.topicMessages = response.data;
        this.messageDialogVisible = true;
      });
    },
    handleClearMessages(row) {
      this.$confirm('Are you sure you want to clear all messages for topic "' + row.name + '"?', 'Warning', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        return deleteTopicMessages(row.name);
      }).then(() => {
        this.$modal.msgSuccess("Messages cleared successfully");
        this.getTopics();
      }).catch(() => {});
    },
    handleDeleteTopic(row) {
      this.$confirm('Are you sure you want to delete topic "' + row.name + '"?', 'Warning', {
        confirmButtonText: 'Confirm',
        cancelButtonText: 'Cancel',
        type: 'error'
      }).then(() => {
        return deleteTopic(row.name);
      }).then(() => {
        this.$modal.msgSuccess("Topic deleted successfully");
        this.getTopics();
      }).catch(() => {});
    },
    handleResetOffset() {
      if (!this.selectedGroup || !this.selectedGroupDetails) return;

      // Determine topic(s) to reset. For simplicity, we reset all topics in the group or ask user.
      // API currently requires topic. Let's iterate partitions or pick the first topic found in details.
      // Better: Reset logic in backend should probably handle "Group Level" reset, but user asked for button.
      // Let's assume we reset the topic with highest lag or loop through unique topics.

      const topics = [...new Set(this.selectedGroupDetails.partitions.map(p => p.topic))];

      if (topics.length === 0) return;

      const confirmMsg = `Reset offsets to LATEST for group "${this.selectedGroup}" on topics: ${topics.join(', ')}? This skips all pending messages.`;

      this.$confirm(confirmMsg, 'Critical Warning', {
        confirmButtonText: 'Confirm Reset',
        cancelButtonText: 'Cancel',
        type: 'error'
      }).then(() => {
         // Chain promises if multiple topics
         const promises = topics.map(topic => resetConsumerOffset(this.selectedGroup, topic));
         return Promise.all(promises);
      }).then(() => {
        this.$modal.msgSuccess("Offsets reset successfully. Consumers may need a moment to update.");
        this.refreshConsumers();
      }).catch(err => {
        if (err !== 'cancel') {
           this.$modal.msgError("Failed to reset offsets: " + err);
        }
      });
    },
    handleClearStockStatus() {
      this.$confirm('Clear all "Running/Waiting" stock task statuses from Redis? This fixes the dashboard if it shows stuck tasks.', 'Warning', {
        confirmButtonText: 'Confirm Clear',
        cancelButtonText: 'Cancel',
        type: 'warning'
      }).then(() => {
        return clearStockStatus();
      }).then(() => {
        this.$modal.msgSuccess("Status index cleared.");
        this.getStockTasks();
      }).catch(() => {});
    }
  }
};
</script>

<style scoped>
.text-danger {
  color: #F56C6C;
  font-weight: bold;
}
.text-success {
  color: #67C23A;
}
.text-center {
  text-align: center;
}
.text-muted {
  color: #909399;
}
</style>
