<template>
  <div class="app-container">
    <el-tabs v-model="activeName" type="card" @tab-click="handleTabClick">
      <el-tab-pane label="Topics" name="topics">
        <el-table v-loading="loading" :data="topics" style="width: 100%">
          <el-table-column prop="name" label="Topic Name" />
          <el-table-column prop="partitionCount" label="Partitions" width="120" />
          <el-table-column prop="replicationFactor" label="Replicas" width="120" />
          <el-table-column prop="totalMessageCount" label="Total Messages (Approx)" width="200" />
          <el-table-column label="Actions" width="300" align="center">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-search" @click="handleViewMessages(scope.row)">Inspect</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" class="text-danger" @click="handleClearMessages(scope.row)">Clear Msg</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete-solid" class="text-danger" @click="handleDeleteTopic(scope.row)">Delete</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="Consumer Groups" name="consumers">
        <el-row :gutter="20">
          <el-col :span="6">
            <el-card class="box-card">
              <div slot="header">
                <span>Groups</span>
                <el-button style="float: right; padding: 3px 0" type="text" @click="refreshConsumers">Refresh</el-button>
              </div>
              <el-table
                :data="consumerGroups"
                highlight-current-row
                @current-change="handleGroupChange"
                style="width: 100%">
                <el-table-column label="Group ID">
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
                <span>Details: {{ selectedGroup }}</span>
              </div>
              <div v-if="selectedGroupDetails">
                <el-descriptions :column="3" border>
                  <el-descriptions-item label="State">{{ selectedGroupDetails.state }}</el-descriptions-item>
                  <el-descriptions-item label="Coordinator">{{ selectedGroupDetails.coordinator }}</el-descriptions-item>
                  <el-descriptions-item label="Total Lag">{{ selectedGroupDetails.totalLag }}</el-descriptions-item>
                </el-descriptions>

                <el-divider content-position="left">Partitions</el-divider>

                <el-table :data="selectedGroupDetails.partitions" style="width: 100%" stripe>
                  <el-table-column prop="topic" label="Topic" />
                  <el-table-column prop="partition" label="Partition" width="80" />
                  <el-table-column prop="currentOffset" label="Current Offset" />
                  <el-table-column prop="logEndOffset" label="End Offset" />
                  <el-table-column prop="lag" label="Lag">
                    <template slot-scope="scope">
                      <span :class="scope.row.lag > 0 ? 'text-danger' : 'text-success'">{{ scope.row.lag }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="clientId" label="Client ID" show-overflow-tooltip />
                  <el-table-column prop="host" label="Host" show-overflow-tooltip />
                </el-table>
              </div>
              <div v-else class="text-center text-muted">
                Select a group to view details
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
import { listTopics, listConsumers, getConsumerDetails, deleteTopic, deleteTopicMessages, getTopicMessages } from "@/api/tool/kafka";

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
      topicMessages: []
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
      }
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
