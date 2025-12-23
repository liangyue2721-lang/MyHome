<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="业务任务ID" prop="jobId">
        <el-input
          v-model="queryParams.jobId"
          placeholder="请输入业务任务ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任务名称" prop="jobName">
        <el-input
          v-model="queryParams.jobName"
          placeholder="请输入任务名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="任务分组" prop="jobGroup">
        <el-input
          v-model="queryParams.jobGroup"
          placeholder="请输入任务分组"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="本次执行ID" prop="executionId">
        <el-input
          v-model="queryParams.executionId"
          placeholder="请输入本次执行ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="当前执行节点ID" prop="nodeId">
        <el-input
          v-model="queryParams.nodeId"
          placeholder="请输入当前执行节点ID"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="计划执行时间" prop="scheduledTime">
        <el-date-picker clearable
                        v-model="queryParams.scheduledTime"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择计划执行时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="进入队列时间" prop="enqueueTime">
        <el-date-picker clearable
                        v-model="queryParams.enqueueTime"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择进入队列时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="实际开始执行时间" prop="startTime">
        <el-date-picker clearable
                        v-model="queryParams.startTime"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择实际开始执行时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="已重试次数" prop="retryCount">
        <el-input
          v-model="queryParams.retryCount"
          placeholder="请输入已重试次数"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="最大允许重试次数" prop="maxRetry">
        <el-input
          v-model="queryParams.maxRetry"
          placeholder="请输入最大允许重试次数"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['quartz:runtime:add']"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['quartz:runtime:edit']"
        >修改
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['quartz:runtime:remove']"
        >删除
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['quartz:runtime:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="runtimeList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="主键" align="center" prop="id"/>
      <el-table-column label="业务任务ID" align="center" prop="jobId"/>
      <el-table-column label="任务名称" align="center" prop="jobName"/>
      <el-table-column label="任务分组" align="center" prop="jobGroup"/>
      <el-table-column label="本次执行ID" align="center" prop="executionId"/>
      <el-table-column label="任务状态：WAITING / RUNNING / RETRYING" align="center" prop="status"/>
      <el-table-column label="当前执行节点ID" align="center" prop="nodeId"/>
      <el-table-column label="计划执行时间" align="center" prop="scheduledTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.scheduledTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="进入队列时间" align="center" prop="enqueueTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.enqueueTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="实际开始执行时间" align="center" prop="startTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.startTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="已重试次数" align="center" prop="retryCount"/>
      <el-table-column label="最大允许重试次数" align="center" prop="maxRetry"/>
      <el-table-column label="任务执行参数" align="center" prop="payload"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['quartz:runtime:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['quartz:runtime:remove']"
          >删除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 添加或修改实时任务（待执行 / 执行中）对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="业务任务ID" prop="jobId">
          <el-input v-model="form.jobId" placeholder="请输入业务任务ID"/>
        </el-form-item>
        <el-form-item label="任务名称" prop="jobName">
          <el-input v-model="form.jobName" placeholder="请输入任务名称"/>
        </el-form-item>
        <el-form-item label="任务分组" prop="jobGroup">
          <el-input v-model="form.jobGroup" placeholder="请输入任务分组"/>
        </el-form-item>
        <el-form-item label="本次执行ID" prop="executionId">
          <el-input v-model="form.executionId" placeholder="请输入本次执行ID"/>
        </el-form-item>
        <el-form-item label="当前执行节点ID" prop="nodeId">
          <el-input v-model="form.nodeId" placeholder="请输入当前执行节点ID"/>
        </el-form-item>
        <el-form-item label="计划执行时间" prop="scheduledTime">
          <el-date-picker clearable
                          v-model="form.scheduledTime"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择计划执行时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="进入队列时间" prop="enqueueTime">
          <el-date-picker clearable
                          v-model="form.enqueueTime"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择进入队列时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="实际开始执行时间" prop="startTime">
          <el-date-picker clearable
                          v-model="form.startTime"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择实际开始执行时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="已重试次数" prop="retryCount">
          <el-input v-model="form.retryCount" placeholder="请输入已重试次数"/>
        </el-form-item>
        <el-form-item label="最大允许重试次数" prop="maxRetry">
          <el-input v-model="form.maxRetry" placeholder="请输入最大允许重试次数"/>
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
import {listRuntime, getRuntime, delRuntime, addRuntime, updateRuntime} from "@/api/quartz/runtime"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
export default {
  name: "Runtime",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非单个禁用
      single: true,
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 实时任务（待执行 / 执行中）表格数据
      runtimeList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        jobId: null,
        jobName: null,
        jobGroup: null,
        executionId: null,
        status: null,
        nodeId: null,
        scheduledTime: null,
        enqueueTime: null,
        startTime: null,
        retryCount: null,
        maxRetry: null,
        payload: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        jobId: [
          {required: true, message: "业务任务ID不能为空", trigger: "blur"}
        ],
        jobName: [
          {required: true, message: "任务名称不能为空", trigger: "blur"}
        ],
        executionId: [
          {required: true, message: "本次执行ID不能为空", trigger: "blur"}
        ],
        status: [
          {required: true, message: "任务状态：WAITING / RUNNING / RETRYING不能为空", trigger: "change"}
        ],
        scheduledTime: [
          {required: true, message: "计划执行时间不能为空", trigger: "blur"}
        ],
        enqueueTime: [
          {required: true, message: "进入队列时间不能为空", trigger: "blur"}
        ],
        createTime: [
          {required: true, message: "创建时间不能为空", trigger: "blur"}
        ],
        updateTime: [
          {required: true, message: "更新时间不能为空", trigger: "blur"}
        ]
      }
    }
  },
  async created() {
    // 获取用户列表并设置 userId
    await this.initUserList();
    // 加载数据
    this.getList();
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
            this.form.userId = matchedUser.id; // 匹配成功，选中对应用户
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
    /** 查询实时任务（待执行 / 执行中）列表 */
    getList() {
      this.loading = true
      listRuntime(this.queryParams).then(response => {
        this.runtimeList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        jobId: null,
        jobName: null,
        jobGroup: null,
        executionId: null,
        status: null,
        nodeId: null,
        scheduledTime: null,
        enqueueTime: null,
        startTime: null,
        retryCount: null,
        maxRetry: null,
        payload: null,
        createTime: null,
        updateTime: null
      }
      this.resetForm("form")
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加实时任务（待执行 / 执行中）"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getRuntime(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改实时任务（待执行 / 执行中）"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateRuntime(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addRuntime(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除实时任务（待执行 / 执行中）编号为"' + ids + '"的数据项？').then(function () {
        return delRuntime(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('quartz/runtime/export', {
        ...this.queryParams
      }, `runtime_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
