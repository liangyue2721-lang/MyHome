<template>
  <div class="app-container">
    <!-- 总体进度条渲染 -->
    <div class="progress-bars" v-if="indicatorsList && indicatorsList.length">
      <div class="progress-bar-item">
        <span class="progress-label">总体进度</span>
        <div class="block-progress">
          <div
            class="progress-block"
            v-for="n in totalBlocks"
            :key="n"
            :class="{ filled: n <= totalBlocksFilled }"
          ></div>
        </div>
        <span class="progress-percent">
      {{ totalPercent }}%
      <span class="value-label">
        ({{ totalCurrent }}/{{ totalTarget }})
      </span>
    </span>
      </div>
    </div>

    <!--    &lt;!&ndash; 进度条展示结构，只有列表不为空时才渲染 &ndash;&gt;-->
    <!--    <div class="progress-bars" v-if="indicatorsList && indicatorsList.length">-->
    <!--      <div-->
    <!--        class="progress-bar-item"-->
    <!--        v-for="item in indicatorsList"-->
    <!--        :key="item.id"-->
    <!--      >-->
    <!--        <span class="progress-label">{{ item.name }}</span>-->
    <!--        <div class="block-progress">-->
    <!--          <div-->
    <!--            class="progress-block"-->
    <!--            v-for="n in totalBlocks"-->
    <!--            :key="n"-->
    <!--            :class="{ filled: n <= blocksFor(item) }"-->
    <!--          ></div>-->
    <!--        </div>-->
    <!--        <span class="progress-percent">-->
    <!--          {{ percentFor(item) }}%-->
    <!--          <span class="value-label">-->
    <!--            ({{ item.currentValue || 0 }}/{{ item.targetValue || 0 }})-->
    <!--          </span>-->
    <!--        </span>-->
    <!--      </div>-->
    <!--    </div>-->

    <!-- 搜索表单区域 -->
    <el-form
      :model="queryParams"
      ref="queryForm"
      size="small"
      inline
      v-show="showSearch"
      label-width="68px"
    >
      <el-form-item label="指标名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入指标名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="指标状态" prop="status">
        <el-select
          v-model="queryParams.status"
          placeholder="请选择指标状态"
          clearable
        >
          <el-option
            v-for="dict in dict.type.indicators_idx_status"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button
          type="primary"
          icon="el-icon-search"
          size="mini"
          @click="handleQuery"
        >搜索
        </el-button
        >
        <el-button
          icon="el-icon-refresh"
          size="mini"
          @click="resetQuery"
        >重置
        </el-button
        >
      </el-form-item>
    </el-form>

    <!-- 操作按钮组 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['finance:indicators:add']"
        >新增
        </el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['finance:indicators:edit']"
        >修改
        </el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['finance:indicators:remove']"
        >删除
        </el-button
        >
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['finance:indicators:export']"
        >导出
        </el-button
        >
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"/>
    </el-row>

    <!-- 数据表格 -->
    <el-table
      v-loading="loading"
      :data="indicatorsList"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="指标名称" align="center" prop="name"/>
      <el-table-column label="指标描述" align="center" prop="description"/>
      <el-table-column label="目标数值" align="center" prop="targetValue"/>
      <el-table-column label="当前进度值" align="center" prop="currentValue"/>
      <el-table-column
        label="开始日期"
        align="center"
        prop="startDate"
        width="180"
      >
        <template #default="{ row }">
          <span>{{ parseTime(row.startDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column
        label="计划完成日期"
        align="center"
        prop="endDate"
        width="180"
      >
        <template #default="{ row }">
          <span>{{ parseTime(row.endDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="指标状态" align="center" prop="status">
        <template #default="{ row }">
          <dict-tag
            :options="dict.type.indicators_idx_status"
            :value="row.status"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作人" align="changedBy">
        <template #default="{ row }">
          {{ getUserName(row.changedBy) }}
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updatedAt" width="180">
        <template #default="{ row }">
          <span>{{ parseTime(row.updatedAt, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template #default="{ row }">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(row)"
            v-hasPermi="['finance:indicators:edit']"
          >修改
          </el-button
          >
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(row)"
            v-hasPermi="['finance:indicators:remove']"
          >删除
          </el-button
          >
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加／修改对话框 -->
    <el-dialog
      :title="title"
      :visible.sync="open"
      width="500px"
      append-to-body
    >
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="指标名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入指标名称"/>
        </el-form-item>
        <el-form-item label="指标描述" prop="description">
          <el-input
            v-model="form.description"
            type="textarea"
            placeholder="请输入内容"
          />
        </el-form-item>
        <el-form-item label="目标数值" prop="targetValue">
          <el-input v-model="form.targetValue" placeholder="请输入目标数值"/>
        </el-form-item>
        <el-form-item label="当前进度值" prop="currentValue">
          <el-input v-model="form.currentValue" placeholder="请输入当前进度值"/>
        </el-form-item>
        <el-form-item label="开始日期" prop="startDate">
          <el-date-picker
            clearable
            v-model="form.startDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择开始日期"
          />
        </el-form-item>
        <el-form-item label="计划完成日期" prop="endDate">
          <el-date-picker
            clearable
            v-model="form.endDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择计划完成日期"
          />
        </el-form-item>
        <el-form-item label="指标状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择指标状态">
            <el-option
              v-for="dict in dict.type.indicators_idx_status"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确定</el-button>
        <el-button @click="cancel">取消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listIndicators,
  getIndicators,
  delIndicators,
  addIndicators,
  updateIndicators
} from '@/api/finance/indicators';
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API

export default {
  name: 'Indicators',
  dicts: ['indicators_idx_status'],
  data() {
    return {
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      indicatorsList: [],
      users: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: null,
        startDate: null,
        status: null,
        userId: null,        // 直接在 form 中使用 userId
      },
      // 表单参数
      form: {
        userId: null         // 直接在 form 中使用 userId
      },
      rules: {
        name: [{required: true, message: '指标名称不能为空', trigger: 'blur'}],
        targetValue: [{required: true, message: '目标数值不能为空', trigger: 'blur'}],
        startDate: [{required: true, message: '开始日期不能为空', trigger: 'blur'}],
        endDate: [{required: true, message: '计划完成日期不能为空', trigger: 'blur'}],
        status: [{required: true, message: '指标状态不能为空', trigger: 'change'}]
      },
      totalBlocks: 20
    };
  },
  async created() {
    // 获取用户列表并设置 userId
    await this.initUserList();
    // 加载数据
    this.getList();
    this.getUserList();
  },
  methods: {
    /**
     * 初始化用户列表数据
     * @returns {Promise<void>} 异步操作完成Promise
     */
    async initUserList() {
      try {
        // 调用后端接口获取用户列表，传入分页参数
        const response = await listUser({pageSize: this.pageSize});
        // 兼容接口返回格式，优先取 response.data，再取 response 本身
        const payload = response.data || response;
        // 根据返回数据格式判断用户列表位置，支持两种结构
        const rawUsers = Array.isArray(payload.rows)
          ? payload.rows
          : Array.isArray(payload)
            ? payload
            : [];

        // 格式化用户列表，只保留用户ID和名称字段
        const userList = rawUsers.map(u => ({
          id: u.userId,
          // 优先使用昵称，没昵称用用户名，最后用默认“用户+ID”
          name: u.userName || u.nickName || `用户${u.userId}`
        }));

        console.log('用户列表加载完成，列表数据:', userList);

        if (userList.length) {
          // 从 cookie 中获取保存的用户名，假设使用 vue-cookies 插件
          const savedUsername = this.$cookies.get('username');
          console.log('从cookie获取的用户名:', savedUsername);

          // 查找与 cookie 中用户名匹配的用户
          const matchedUser = userList.find(u => u.name === savedUsername);
          if (matchedUser) {
            this.queryParams.userId = matchedUser.id; // 匹配成功，选中对应用户
            this.form.userId= matchedUser.id; // 匹配成功，选中对应用户
            console.log('选中cookie中的用户:', matchedUser);
          }
        } else {
          this.queryParams.userId = null; // 没有用户列表，清空选中状态
          this.$message.info('暂无用户数据');
          console.log('用户列表为空');
        }
      } catch (err) {
        console.error('用户列表加载失败:', err);
        this.$message.error('用户列表加载失败，请稍后重试');
      } finally {
      }
    },
    // 计算填充块数量
    blocksFor(item) {
      if (!item.targetValue || item.targetValue <= 0) return 0;
      const ratio = (item.currentValue || 0) / item.targetValue;
      return Math.min(this.totalBlocks, Math.round(ratio * this.totalBlocks));
    },
    // 计算进度百分比
    percentFor(item) {
      if (!item.targetValue || item.targetValue <= 0) return 0;
      return Math.min(100, Math.round(((item.currentValue || 0) / item.targetValue) * 100));
    },
    // 获取用户列表
    async getUserList() {
      this.loading = true;
      try {
        const response = await listUser({pageSize: 1000});
        const data = response.data || response;
        if (data.code === 200) {
          this.users = data.rows || [];
        }
      } catch (err) {
        console.error('获取用户列表失败', err);
      } finally {
        this.loading = false;
      }
    },
    // 根据ID获取用户名
    getUserName(userId) {
      const user = this.users.find(u => String(u.userId) === String(userId));
      return user ? user.nickName : '未知用户';
    },
    // 获取指标列表
    getList() {
      this.loading = true;
      listIndicators(this.queryParams).then(res => {
        this.indicatorsList = res.rows || [];
        this.total = res.total || 0;
        this.loading = false;
      });
    },
    // 查询操作
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    // 重置查询条件
    resetQuery() {
      this.$refs.queryForm.resetFields();
      this.handleQuery();
    },
    // 选中改变
    handleSelectionChange(selection) {
      this.ids = selection.map(i => i.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    // 新增
    handleAdd() {
      this.form = {};
      this.open = true;
      this.title = '添加存储核心指标信息';
    },
    // 修改
    handleUpdate(row) {
      const id = row.id || this.ids;
      getIndicators(id).then(res => {
        this.form = res.data;
        this.open = true;
        this.title = '修改存储核心指标信息';
      });
    },
    // 提交表单
    submitForm() {
      this.$refs.form.validate(valid => {
        if (valid) {
          const action = this.form.id ? updateIndicators : addIndicators;
          action(this.form).then(() => {
            this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功');
            this.open = false;
            this.getList();
          });
        }
      });
    },
    // 取消对话框
    cancel() {
      this.open = false;
      this.$refs.form.resetFields();
    },
    // 删除
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal
        .confirm(`是否确认删除序号为 ${ids} 的指标？`)
        .then(() => delIndicators(ids))
        .then(() => {
          this.$modal.msgSuccess('删除成功');
          this.getList();
        });
    },
    // 导出
    handleExport() {
      this.download(
        'finance/indicators/export',
        {...this.queryParams},
        `indicators_${Date.now()}.xlsx`
      );
    }
  },
  computed: {
    totalCurrent() {
      return this.indicatorsList.reduce((sum, item) => sum + (item.currentValue || 0), 0);
    },
    totalTarget() {
      return this.indicatorsList.reduce((sum, item) => sum + (item.targetValue || 0), 0);
    },
    totalPercent() {
      if (this.totalTarget === 0) return '0.00';
      return ((this.totalCurrent / this.totalTarget) * 100).toFixed(2);
    },
    totalBlocksFilled() {
      if (this.totalTarget === 0) return 0;
      return Math.round((this.totalCurrent / this.totalTarget) * this.totalBlocks);
    }
  }
}
</script>
<style scoped>
@import "@/assets/styles/global.scss";
/* 进度条容器 */
.progress-bars {
  margin-bottom: 24px;
  padding: 20px;
  background: #f8f9fc;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

/* 单个进度条项 */
.progress-bar-item {
  display: flex;
  align-items: center;
  margin-bottom: 18px;
  position: relative;
}

/* 进度标签 */
.progress-label {
  min-width: 140px;
  font-weight: 600;
  color: #2c3e50;
  font-size: 14px;
  letter-spacing: 0.5px;
}

/* 块状进度条容器 */
.block-progress {
  flex-grow: 1;
  display: flex;
  gap: 3px;
  height: 28px;
  padding: 2px;
  background: #f0f2f5;
  border-radius: 6px;
  overflow: hidden;
  position: relative;
}

/* 单个进度块 */
.progress-block {
  flex: 1;
  height: 100%;
  border-radius: 4px;
  transition: transform 0.3s ease,
  box-shadow 0.3s ease;
  position: relative;
  background: #e0e3e8;
}

/* 扩展彩色渐变逻辑 - 20种颜色循环 */
.progress-block.filled:nth-child(20n+1) {
  --block-color: #4CAF50;
}

/* 绿色系 */
.progress-block.filled:nth-child(20n+2) {
  --block-color: #66BB6A;
}

.progress-block.filled:nth-child(20n+3) {
  --block-color: #81C784;
}

.progress-block.filled:nth-child(20n+4) {
  --block-color: #A5D6A7;
}

.progress-block.filled:nth-child(20n+5) {
  --block-color: #C8E6C9;
}

.progress-block.filled:nth-child(20n+6) {
  --block-color: #FFF59D;
}

/* 黄色系 */
.progress-block.filled:nth-child(20n+7) {
  --block-color: #FFEE58;
}

.progress-block.filled:nth-child(20n+8) {
  --block-color: #FFD54F;
}

.progress-block.filled:nth-child(20n+9) {
  --block-color: #FFB300;
}

.progress-block.filled:nth-child(20n+10) {
  --block-color: #FF8F00;
}

.progress-block.filled:nth-child(20n+11) {
  --block-color: #FF6F00;
}

/* 橙色/红色系 */
.progress-block.filled:nth-child(20n+12) {
  --block-color: #E64A19;
}

.progress-block.filled:nth-child(20n+13) {
  --block-color: #D32F2F;
}

.progress-block.filled:nth-child(20n+14) {
  --block-color: #C2185B;
}

.progress-block.filled:nth-child(20n+15) {
  --block-color: #7B1FA2;
}

/* 紫色系 */
.progress-block.filled:nth-child(20n+16) {
  --block-color: #512DA8;
}

.progress-block.filled:nth-child(20n+17) {
  --block-color: #303F9F;
}

.progress-block.filled:nth-child(20n+18) {
  --block-color: #1976D2;
}

/* 蓝色系 */
.progress-block.filled:nth-child(20n+19) {
  --block-color: #0288D1;
}

.progress-block.filled:nth-child(20n+20) {
  --block-color: #0097A7;
}

/* 动态渐变调整 */
.progress-block.filled {
  background: linear-gradient(
    135deg,
    var(--block-color),
    color-mix(in srgb, var(--block-color) 80%, white),
    color-mix(in srgb, var(--block-color) 90%, rgba(255, 255, 255, 0.3))
  );
}

/* 新增光泽效果 */
.progress-block.filled::after {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(
    120deg,
    rgba(255, 255, 255, 0.3) 10%,
    transparent 30%,
    rgba(0, 0, 0, 0.05) 90%
  );
  border-radius: 4px;
}

/* 悬停动画 */
.progress-block.filled:hover {
  transform: scale(1.08);
  z-index: 1;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 百分比显示 */
.progress-percent {
  min-width: 80px;
  text-align: right;
  font-weight: 700;
  font-size: 14px;
  color: #2c3e50;
  margin-left: 16px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}

/* 数值标签 */
.value-label {
  font-size: 12px;
  color: #666;
  margin-left: 8px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .progress-bar-item {
    flex-wrap: wrap;
  }

  .block-progress {
    width: 100%;
    margin: 8px 0;
  }

  .progress-percent {
    width: 100%;
    margin-left: 0;
    justify-content: flex-start;
  }
}
</style>
