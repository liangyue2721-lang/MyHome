<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
<!--      <el-form-item label="关联用户" prop="userId">-->
<!--        <el-input-->
<!--          v-model="queryParams.userId"-->
<!--          placeholder="请输入关联用户"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
<!--      <el-form-item label="消费金额" prop="amount">-->
<!--        <el-input-->
<!--          v-model="queryParams.amount"-->
<!--          placeholder="请输入消费金额"-->
<!--          clearable-->
<!--          @keyup.enter.native="handleQuery"-->
<!--        />-->
<!--      </el-form-item>-->
      <el-form-item label="商户名称" prop="merchant">
        <el-input
          v-model="queryParams.merchant"
          placeholder="请输入商户名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="消费日期" prop="expenseDate">
        <el-date-picker clearable
                        v-model="queryParams.expenseDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择消费日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="记录时间" prop="createdAt">
        <el-date-picker clearable
                        v-model="queryParams.createdAt"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择记录时间">
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
          v-hasPermi="['finance:expense:add']"
        >新增
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['finance:expense:export']"
        >导出
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-refresh-right"
          size="mini"
          @click="handleSyncExpense"
          v-hasPermi="['finance:expense:syncExpense']"
        >同步消费信息
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="expenseList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="消费ID" align="center" prop="expenseId"/>
      <el-table-column label="用户姓名" align="center">
        <template slot-scope="scope">
          {{ getUserName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column label="消费金额" align="center" prop="amount"/>
      <el-table-column label="商户名称" align="center" prop="merchant"/>
      <el-table-column label="消费日期" align="center" prop="expenseDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.expenseDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="消费分类" align="center" prop="category"/>
      <el-table-column label="记录时间" align="center" prop="updatedAt" width="180">
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
            v-hasPermi="['finance:expense:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:expense:remove']"
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

    <!-- 添加或修改消费对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="选择用户" prop="userId">
          <el-select
            v-model="form.userId"
            placeholder="请选择用户"
            filterable
            style="width: 100%"
          >
            <el-option
              v-for="user in users"
              :key="user.userId"
              :label="user.nickName"
              :value="user.userId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="消费金额" prop="amount">
          <el-input v-model="form.amount" placeholder="请输入消费金额"/>
        </el-form-item>
        <el-form-item label="商户名称" prop="merchant">
          <el-input v-model="form.merchant" placeholder="请输入商户名称"/>
        </el-form-item>
        <el-form-item label="消费日期" prop="expenseDate">
          <el-date-picker clearable
                          v-model="form.expenseDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择消费日期">
          </el-date-picker>
        </el-form-item>
<!--        <el-form-item label="记录时间" prop="createdAt">-->
<!--          <el-date-picker clearable-->
<!--                          v-model="form.createdAt"-->
<!--                          type="date"-->
<!--                          value-format="yyyy-MM-dd"-->
<!--                          placeholder="请选择记录时间">-->
<!--          </el-date-picker>-->
<!--        </el-form-item>-->
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {listExpense, getExpense, delExpense, addExpense, updateExpense, syncExpense} from "@/api/finance/expense";
import {listUser} from "@/api/stock/dropdown_component";

export default {
  name: "Expense",
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
      // 消费表格数据
      expenseList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      users: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        amount: null,
        merchant: null,
        expenseDate: null,
        category: null,
        createdAt: null
      },
      // 表单参数
      form: {
        userId: null         // 直接在 form 中使用 userId
      },
      // 表单校验
      rules: {
        userId: [
          {required: true, message: "关联用户不能为空", trigger: "blur"}
        ],
        amount: [
          {required: true, message: "消费金额不能为空", trigger: "blur"}
        ],
        expenseDate: [
          {required: true, message: "消费日期不能为空", trigger: "blur"}
        ],
      }
    };
  },
  created() {
    this.getUserList();
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
    // 修改后的 getUserList 方法，兼容返回 {code, msg, rows, total} 格式
    async getUserList() {
      try {
        this.isLoading = true;
        const response = await listUser({pageSize: this.pageSize || 1000});
        // 如果返回结果在 data 属性中，则取 data，否则直接使用 response
        const data = response.data || response;
        if (data.code === 200) {
          this.users = data.rows || [];
          console.info("获取用户列表成功:", this.users);
        } else {
          console.error("获取用户列表失败, 返回码:", data.code, data.msg);
          this.error = data.msg || "获取用户列表失败，请稍后再试";
        }
      } catch (error) {
        console.error("获取用户列表失败:", error);
        this.error = "获取用户列表失败，请稍后再试";
      } finally {
        this.isLoading = false;
      }
    },
    getUserName(userId) {
      if (!userId) {
        throw new Error("用户ID不能为空");
      }
      if (!this.users || this.users.length === 0) {
        throw new Error("用户列表为空");
      }
      const targetId = String(userId);
      const user = this.users.find(u => String(u.userId) === targetId);
      if (!user) {
        console.warn(`未找到匹配的用户，用户ID: ${userId}`);
        return "未知用户";
      }
      return user.nickName || "匿名用户";
    },
    /** 查询消费列表 */
    getList() {
      this.loading = true;
      listExpense(this.queryParams).then(response => {
        this.expenseList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    // 取消按钮
    cancel() {
      this.open = false;
      this.reset();
    },
    // 表单重置
    reset() {
      this.form = {
        expenseId: null,
        userId: null,
        amount: null,
        merchant: null,
        expenseDate: null,
        category: null,
        createdAt: null
      };
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.expenseId)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加消费";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const expenseId = row.expenseId || this.ids
      getExpense(expenseId).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改消费";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.expenseId != null) {
            updateExpense(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addExpense(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 同步按钮操作 */
    handleSyncExpense() {
      this.$confirm('确认同步数据？').then(() => syncExpense()).then(() => {
        this.$message.success('同步成功');
        this.getList();
        this.loadTotalValueDonutChart();
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const expenseIds = row.expenseId || this.ids;
      this.$modal.confirm('是否确认删除消费编号为"' + expenseIds + '"的数据项？').then(function () {
        return delExpense(expenseIds);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {
      });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/expense/export', {
        ...this.queryParams
      }, `expense_${new Date().getTime()}.xlsx`)
    }
  }
};
</script>
