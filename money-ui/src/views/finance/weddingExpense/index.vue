<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="婚礼名称" prop="weddingName">
        <el-input
          v-model="queryParams.weddingName"
          placeholder="请输入婚礼名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="婚礼日期" prop="weddingDate">
        <el-date-picker clearable
                        v-model="queryParams.weddingDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择婚礼日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="婚礼举办城市" prop="weddingCity">
        <el-input
          v-model="queryParams.weddingCity"
          placeholder="请输入婚礼举办城市"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支出分类" prop="expenseCategory">
        <el-input
          v-model="queryParams.expenseCategory"
          placeholder="请输入支出分类"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="具体支出项目" prop="expenseItem">
        <el-input
          v-model="queryParams.expenseItem"
          placeholder="请输入具体支出项目"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支出金额" prop="amount">
        <el-input
          v-model="queryParams.amount"
          placeholder="请输入支出金额"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付日期" prop="paymentDate">
        <el-date-picker clearable
                        v-model="queryParams.paymentDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择支付日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="收款方" prop="payee">
        <el-input
          v-model="queryParams.payee"
          placeholder="请输入收款方"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="更新时间" prop="updatedAt">
        <el-date-picker clearable
                        v-model="queryParams.updatedAt"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择更新时间">
        </el-date-picker>
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
          v-hasPermi="['finance:weddingExpense:add']"
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
          v-hasPermi="['finance:weddingExpense:edit']"
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
          v-hasPermi="['finance:weddingExpense:remove']"
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
          v-hasPermi="['finance:weddingExpense:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="weddingExpenseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="主键ID" align="center" prop="id"/>
      <el-table-column label="婚礼名称" align="center" prop="weddingName"/>
      <el-table-column label="婚礼日期" align="center" prop="weddingDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.weddingDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="婚礼举办城市" align="center" prop="weddingCity"/>
      <el-table-column label="支出分类" align="center" prop="expenseCategory"/>
      <el-table-column label="具体支出项目" align="center" prop="expenseItem"/>
      <el-table-column label="支出金额" align="center" prop="amount"/>
      <el-table-column label="支付日期" align="center" prop="paymentDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.paymentDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="收款方" align="center" prop="payee"/>
      <el-table-column label="备注说明" align="center" prop="notes"/>
      <el-table-column label="更新时间" align="center" prop="updatedAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updatedAt, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['finance:weddingExpense:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:weddingExpense:remove']"
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

    <!-- 添加或修改婚礼支出记录对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="婚礼名称" prop="weddingName">
          <el-input v-model="form.weddingName" placeholder="请输入婚礼名称"/>
        </el-form-item>
        <el-form-item label="婚礼日期" prop="weddingDate">
          <el-date-picker clearable
                          v-model="form.weddingDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择婚礼日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="婚礼举办城市" prop="weddingCity">
          <el-input v-model="form.weddingCity" placeholder="请输入婚礼举办城市"/>
        </el-form-item>
        <el-form-item label="支出分类" prop="expenseCategory">
          <el-input v-model="form.expenseCategory" placeholder="请输入支出分类"/>
        </el-form-item>
        <el-form-item label="具体支出项目" prop="expenseItem">
          <el-input v-model="form.expenseItem" placeholder="请输入具体支出项目"/>
        </el-form-item>
        <el-form-item label="支出金额" prop="amount">
          <el-input v-model="form.amount" placeholder="请输入支出金额"/>
        </el-form-item>
        <el-form-item label="支付日期" prop="paymentDate">
          <el-date-picker clearable
                          v-model="form.paymentDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择支付日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="收款方" prop="payee">
          <el-input v-model="form.payee" placeholder="请输入收款方"/>
        </el-form-item>
        <el-form-item label="备注说明" prop="notes">
          <el-input v-model="form.notes" type="textarea" placeholder="请输入内容"/>
        </el-form-item>
        <el-form-item label="更新时间" prop="updatedAt">
          <el-date-picker clearable
                          v-model="form.updatedAt"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择更新时间">
          </el-date-picker>
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
import {
  listWeddingExpense,
  getWeddingExpense,
  delWeddingExpense,
  addWeddingExpense,
  updateWeddingExpense
} from "@/api/finance/weddingExpense"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
export default {
  name: "WeddingExpense",
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
      // 婚礼支出记录表格数据
      weddingExpenseList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        weddingName: null,
        weddingDate: null,
        weddingCity: null,
        expenseCategory: null,
        expenseItem: null,
        amount: null,
        paymentDate: null,
        payee: null,
        notes: null,
        updatedAt: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        expenseCategory: [
          {required: true, message: "支出分类不能为空", trigger: "blur"}
        ],
        amount: [
          {required: true, message: "支出金额不能为空", trigger: "blur"}
        ],
        updatedAt: [
          {required: true, message: "更新时间不能为空", trigger: "blur"}
        ],
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
    /** 查询婚礼支出记录列表 */
    getList() {
      this.loading = true
      listWeddingExpense(this.queryParams).then(response => {
        this.weddingExpenseList = response.rows
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
        userId: null,
        weddingName: null,
        weddingDate: null,
        weddingCity: null,
        expenseCategory: null,
        expenseItem: null,
        amount: null,
        paymentDate: null,
        payee: null,
        notes: null,
        createdAt: null,
        updatedAt: null,
        createdBy: null
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
      this.title = "添加婚礼支出记录"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getWeddingExpense(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改婚礼支出记录"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateWeddingExpense(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addWeddingExpense(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除婚礼支出记录编号为"' + ids + '"的数据项？').then(function () {
        return delWeddingExpense(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/weddingExpense/export', {
        ...this.queryParams
      }, `weddingExpense_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
