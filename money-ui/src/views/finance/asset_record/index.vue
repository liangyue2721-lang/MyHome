<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb-20">
      <el-col :xs="24" :sm="24" :md="10" :lg="8">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="clearfix">
            <span class="card-title"><i class="el-icon-money"></i> 资产总览</span>
          </div>
          <div ref="sumChart" class="chart-box"></div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="24" :md="14" :lg="16">
        <el-card shadow="hover" class="chart-card">
          <div slot="header" class="clearfix">
            <span class="card-title"><i class="el-icon-pie-chart"></i> 资产分布</span>
          </div>
          <div ref="pieChart" class="chart-box"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="table-card">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                     v-hasPermi="['finance:asset_record:add']">新增
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
                     v-hasPermi="['finance:asset_record:remove']">删除
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="el-icon-refresh-right" size="mini" @click="handleSync"
                     v-hasPermi="['finance:asset_record:sync']">同步
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
                     v-hasPermi="['finance:asset_record:export']">导出
          </el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table v-loading="loading" :data="recordList" @selection-change="handleSelectionChange"
                :header-cell-style="{background:'#f8f8f9', color:'#515a6e'}">
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="ID" align="center" prop="assetId" width="80"/>
        <el-table-column label="用户姓名" align="center" min-width="100">
          <template slot-scope="scope">
            <el-tag size="small" type="info" effect="plain">{{ getUserName(scope.row.userId) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="资产类型" align="center" min-width="100">
          <template slot-scope="scope">
            <el-tag size="small" effect="light">{{ getAssetTypeName(scope.row.assetType) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="资产价值" align="right" prop="amount" min-width="120">
          <template slot-scope="scope">
            <span style="font-weight: bold; color: #1890ff">{{ formatMoney(scope.row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="货币" align="center" prop="currency" width="80"/>
        <el-table-column label="状态" align="center" width="100">
          <template slot-scope="scope">
            <el-tag size="small" :type="scope.row.assetStatus === 0 ? 'success' : 'warning'">
              {{ getAssetStatusName(scope.row.assetStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源渠道" align="center" prop="sourceChannel" min-width="120"
                         :show-overflow-tooltip="true"/>
        <el-table-column label="说明" align="center" prop="remark" min-width="150" :show-overflow-tooltip="true"/>
        <el-table-column label="更新时间" align="center" prop="updatedAt" width="160">
          <template slot-scope="scope">
            <span class="time-text">{{ parseTime(scope.row.updatedAt, '{y}-{m}-{d} {h}:{i}') }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="100" fixed="right">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                       v-hasPermi="['finance:asset_record:edit']">修改
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
                  @pagination="getList"/>
    </el-card>

    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="资产价值" prop="amount">
          <el-input v-model="form.amount" placeholder="请输入资产价值" type="number">
            <template slot="append">元</template>
          </el-input>
        </el-form-item>
        <el-form-item label="资产类型" prop="assetType">
          <el-select v-model="form.assetType" placeholder="请选择资产类型" filterable style="width: 100%">
            <el-option v-for="item in assetTypes" :key="item.value" :label="item.label" :value="item.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="资产状态" prop="assetStatus">
          <el-select v-model="form.assetStatus" placeholder="请选择状态" style="width: 100%">
            <el-option v-for="status in assetStatuses" :key="status.value" :label="status.label" :value="status.value"/>
          </el-select>
        </el-form-item>
        <el-form-item label="来源渠道" prop="sourceChannel">
          <el-input v-model="form.sourceChannel" placeholder="例如：工资卡、理财APP"/>
        </el-form-item>
        <el-form-item label="资产说明" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注信息"/>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import * as echarts from 'echarts';
import 'echarts-liquidfill';
import {mapGetters} from 'vuex';
import {
  listAssetRecord,
  getAssetRecord,
  delAssetRecord,
  addAssetRecord,
  updateAssetRecord,
  getAssetType,
  getAssetStatus,
  getAssetRecordColumnChart,
  getSync
} from '@/api/finance/asset_record';
import {listUser} from '@/api/stock/dropdown_component';

const gradientColors = [
  {start: '#5470c6', end: '#91cc75'},
  {start: '#fac858', end: '#ee6666'},
  {start: '#73c0de', end: '#3ba272'},
  {start: '#fc8452', end: '#9a60b4'},
  {start: '#ea7ccc', end: '#5470c6'}
];

export default {
  name: 'Record',
  data() {
    return {
      loading: true,
      recordList: [],
      total: 0,
      showSearch: true, // 保留此变量以兼容 right-toolbar
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        assetType: null,
        amount: null,
        currency: null,
        assetStatus: null,
        sourceChannel: null,
      },
      form: {
        userId: null,
      },
      rules: {
        assetType: [{required: true, message: '资产类型不能为空', trigger: 'change'}],
        amount: [{required: true, message: '资产价值不能为空', trigger: 'blur'}],
        assetStatus: [{required: true, message: '状态不能为空', trigger: 'change'}],
        sourceChannel: [{required: true, message: '来源渠道不能为空', trigger: 'blur'}]
      },
      users: [],
      assetTypes: [],
      assetStatuses: [],
      ids: [],
      single: true,
      multiple: true,
      title: '',
      open: false,
      totalAmountPieChart: null,
      totalValueDonutChart: null
    };
  },
  computed: {
    // 从 Vuex 获取当前登录用户信息
    ...mapGetters(['userId', 'name']),
  },
  async created() {
    // 1. 加载字典
    this.getAssetStatusList();
    this.getAssetTypeList();

    // 2. 核心：加载用户列表并设置 queryParams.userId 为当前登录用户
    await this.initUserList();

    // 3. 执行查询 (此时 userId 已默认选中)
    this.getList();
    this.loadTotalValueDonutChart();
    this.loadTotalAmountPieChartData();
  },
  mounted() {
    window.addEventListener('resize', this.resizeCharts);
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.resizeCharts);
    this.totalAmountPieChart?.dispose();
    this.totalValueDonutChart?.dispose();
  },
  methods: {
    formatMoney(num) {
      if (num === null || num === undefined) return '0.00';
      return Number(num).toLocaleString('zh-CN', {minimumFractionDigits: 2, maximumFractionDigits: 2});
    },
    // 初始化用户列表并自动设置当前用户
    async initUserList() {
      try {
        const response = await listUser({pageSize: 1000});
        const payload = response.data || response;
        const rawUsers = Array.isArray(payload.rows) ? payload.rows : Array.isArray(payload) ? payload : [];

        this.users = rawUsers.map(u => ({
          userId: u.userId,
          nickName: u.nickName || u.userName || `用户${u.userId}`,
          userName: u.userName
        }));

        // --- 自动设置默认查询用户逻辑 ---
        if (this.userId) {
          this.queryParams.userId = this.userId;
          this.form.userId = this.userId;
        } else if (this.name) {
          const matched = this.users.find(u => u.userName === this.name || u.nickName === this.name);
          if (matched) {
            this.queryParams.userId = matched.userId;
            this.form.userId = matched.userId;
          }
        } else {
          const savedUsername = this.$cookies ? this.$cookies.get('username') : null;
          if (savedUsername) {
            const matched = this.users.find(u => u.userName === savedUsername || u.nickName === savedUsername);
            if (matched) {
              this.queryParams.userId = matched.userId;
              this.form.userId = matched.userId;
            }
          }
        }
      } catch (err) {
        console.error('用户列表加载失败:', err);
      }
    },
    generateGradient(colorObj) {
      return new echarts.graphic.LinearGradient(0, 0, 1, 1, [
        {offset: 0, color: colorObj.start},
        {offset: 1, color: colorObj.end}
      ]);
    },
    async getAssetStatusList() {
      try {
        const res = await getAssetStatus();
        if (res.code === 200) this.assetStatuses = res.data.map(i => ({value: Number(i.value), label: i.label}));
      } catch (e) {
      }
    },
    async getAssetTypeList() {
      try {
        const res = await getAssetType();
        if (res.code === 200) this.assetTypes = res.data.map(i => ({value: Number(i.value), label: i.label}));
      } catch (e) {
      }
    },
    getList() {
      this.loading = true;
      listAssetRecord(this.queryParams)
        .then(res => {
          this.recordList = res.rows;
          this.total = res.total;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    // 右上角刷新按钮触发
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
      this.loadTotalValueDonutChart();
      this.loadTotalAmountPieChartData();
    },
    handleSelectionChange(sel) {
      this.ids = sel.map(i => i.assetId);
      this.single = sel.length !== 1;
      this.multiple = sel.length === 0;
    },
    handleAdd() {
      this.resetFormData();
      this.open = true;
      this.title = '新增资产';
    },
    handleUpdate(row) {
      this.resetFormData();
      getAssetRecord(row.assetId).then(r => {
        this.form = r.data;
        this.open = true;
        this.title = '修改资产';
      });
    },
    handleDelete() {
      this.$confirm(`确认删除选中的 ${this.ids.length} 条记录吗？`, '警告', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => delAssetRecord(this.ids)).then(() => {
        this.$message.success('删除成功');
        this.getList();
        this.loadTotalValueDonutChart();
        this.loadTotalAmountPieChartData();
      });
    },
    handleSync() {
      this.$confirm('确认执行数据同步操作吗？', '提示', {type: 'info'}).then(() => getSync()).then(() => {
        this.$message.success('同步成功');
        this.getList();
        this.loadTotalValueDonutChart();
        this.loadTotalAmountPieChartData();
      });
    },
    handleExport() {
      this.download('finance/asset_record/export', {...this.queryParams}, `record_${Date.now()}.xlsx`);
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          (this.form.assetId ? updateAssetRecord : addAssetRecord)(this.form).then(() => {
            this.$message.success(this.form.assetId ? '修改成功' : '新增成功');
            this.open = false;
            this.getList();
            this.loadTotalValueDonutChart();
            this.loadTotalAmountPieChartData();
          });
        }
      });
    },
    cancel() {
      this.open = false;
    },
    resetFormData() {
      this.form = {
        assetId: null,
        userId: this.userId || this.queryParams.userId, // 新增时默认关联当前用户
        assetType: null,
        amount: null,
        currency: null,
        assetStatus: null,
        sourceChannel: null,
        remark: null
      };
      this.$nextTick(() => this.$refs.form?.resetFields());
    },
    getUserName(id) {
      const u = this.users.find(u => String(u.userId) === String(id));
      return u ? u.nickName : id;
    },
    getAssetTypeName(id) {
      const t = this.assetTypes.find(t => t.value === Number(id));
      return t ? t.label : id;
    },
    getAssetStatusName(id) {
      const s = this.assetStatuses.find(s => s.value === Number(id));
      return s ? s.label : id;
    },
    processChartData(raw) {
      const m = new Map();
      raw.forEach(i => {
        const n = this.getAssetTypeName(i.assetType) || '未知类型';
        m.set(n, (m.get(n) || 0) + parseFloat(i.amount));
      });
      return Array.from(m).map(([c, a]) => ({category: c, amount: Number(a.toFixed(2))}));
    },
    async loadTotalValueDonutChart() {
      try {
        const res = await listAssetRecord(this.queryParams);
        const totalValue = (res.rows || []).reduce((s, i) => s + parseFloat(i.amount), 0);

        if (this.totalValueDonutChart) this.totalValueDonutChart.dispose();
        this.totalValueDonutChart = echarts.init(this.$refs.sumChart);

        this.totalValueDonutChart.setOption({
          backgroundColor: '#fff',
          title: {
            text: '资产总价值',
            subtext: `¥ ${totalValue.toLocaleString('zh-CN', {minimumFractionDigits: 2})}`,
            left: 'center',
            top: '32%',
            textStyle: {fontSize: 16, color: '#909399', fontWeight: 'normal', fontFamily: 'Microsoft YaHei'},
            subtextStyle: {fontSize: 28, color: '#303133', fontWeight: 'bold', fontFamily: 'Arial'}
          },
          series: [{
            type: 'liquidFill',
            radius: '75%',
            center: ['50%', '55%'],
            data: [0.6, 0.55, 0.45],
            backgroundStyle: {color: '#f2f8ff'},
            outline: {
              show: true,
              borderDistance: 4,
              itemStyle: {
                borderWidth: 2,
                borderColor: '#409EFF',
                shadowBlur: 10,
                shadowColor: 'rgba(64, 158, 255, 0.2)'
              }
            },
            color: [
              {
                type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
                colorStops: [{offset: 0, color: '#a0cfff'}, {offset: 1, color: '#409EFF'}]
              },
              'rgba(64, 158, 255, 0.5)',
              'rgba(64, 158, 255, 0.3)'
            ],
            label: {show: false},
            itemStyle: {opacity: 0.8, shadowBlur: 10, shadowColor: 'rgba(0, 0, 0, 0.1)'}
          }]
        });
      } catch (e) {
        console.error('Liquid chart error', e);
      }
    },
    async loadTotalAmountPieChartData() {
      try {
        const targetUserId = this.queryParams.userId || this.userId;
        const res = await getAssetRecordColumnChart(targetUserId);
        const data = this.processChartData(res.data || []);

        if (this.totalAmountPieChart) this.totalAmountPieChart.dispose();
        this.totalAmountPieChart = echarts.init(this.$refs.pieChart);

        const seriesData = data.map((i, idx) => ({
          name: i.category,
          value: i.amount,
          itemStyle: {
            color: this.generateGradient(gradientColors[idx % gradientColors.length]),
            borderRadius: 5
          }
        }));

        this.totalAmountPieChart.setOption({
          tooltip: {
            trigger: 'item',
            formatter: '{b}: {c}元 ({d}%)',
            backgroundColor: 'rgba(255, 255, 255, 0.9)',
            textStyle: {color: '#333'},
            borderColor: '#ddd',
            borderWidth: 1
          },
          legend: {
            type: 'scroll',
            orient: 'vertical',
            right: 10,
            top: 20,
            bottom: 20,
            itemGap: 15,
            textStyle: {color: '#606266'},
            formatter: name => {
              const item = data.find(d => d.category === name);
              const val = item ? item.amount.toLocaleString() : 0;
              return name.length > 6 ? `${name.slice(0, 6)}... | ¥${val}` : `${name} | ¥${val}`;
            }
          },
          series: [{
            name: '资产分布',
            type: 'pie',
            radius: ['45%', '70%'],
            center: ['40%', '50%'],
            avoidLabelOverlap: true,
            itemStyle: {
              borderRadius: 8,
              borderColor: '#fff',
              borderWidth: 2
            },
            label: {
              show: false,
              position: 'center'
            },
            emphasis: {
              label: {
                show: true,
                fontSize: 18,
                fontWeight: 'bold',
                color: '#303133',
                formatter: '{b}\n{d}%'
              },
              scaleSize: 10
            },
            data: seriesData
          }]
        });
      } catch (e) {
        console.error('Pie chart error', e);
      }
    },
    resizeCharts() {
      this.totalAmountPieChart?.resize();
      this.totalValueDonutChart?.resize();
    }
  }
};
</script>

<style scoped>
.app-container {
  font-family: "PingFang SC", "Microsoft YaHei", "Helvetica Neue", Helvetica, Arial, sans-serif;
  background-color: #f0f2f5;
  padding: 20px;
  min-height: 100vh;
}

.mb-20 {
  margin-bottom: 20px;
}

.chart-card {
  border: none;
  border-radius: 8px;
  margin-bottom: 10px;
}

.table-card {
  border: none;
  border-radius: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.chart-box {
  height: 320px;
  width: 100%;
}

.time-text {
  color: #909399;
  font-size: 12px;
}
</style>
