<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="用户" prop="userId">
        <el-select v-model="queryParams.userId" placeholder="请选择用户" filterable style="width: 100%">
          <el-option v-for="user in users" :key="user.userId" :label="user.nickName" :value="user.userId"/>
        </el-select>
      </el-form-item>
      <el-form-item label="银行卡号" prop="bankCardNumber">
        <el-input
          v-model="queryParams.bankCardNumber"
          placeholder="请输入银行卡号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="银行卡状态" prop="bankCardStatus">
        <el-select v-model="queryParams.bankCardStatus" placeholder="请选择银行卡状态" clearable>
          <el-option
            v-for="dict in dict.type.sys_normal_disable"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
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
          v-hasPermi="['finance:user_accounts:add']"
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
          v-hasPermi="['finance:user_accounts:edit']"
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
          v-hasPermi="['finance:user_accounts:remove']"
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
          v-hasPermi="['finance:user_accounts:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="user_accountsList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <!--      <el-table-column label="主键ID" align="center" prop="id"/>-->
      <el-table-column label="用户姓名" align="center">
        <template slot-scope="scope">
          {{ getUserName(scope.row.userId) }}
        </template>
      </el-table-column>
      <el-table-column label="银行卡号" align="center" prop="bankCardNumber"/>
      <el-table-column label="银行卡状态" align="center" prop="bankCardStatus">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.sys_normal_disable" :value="scope.row.bankCardStatus"/>
        </template>
      </el-table-column>
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
            v-hasPermi="['finance:user_accounts:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:user_accounts:remove']"
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

    <!-- 添加或修改用户账户银行卡信息对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="选择用户" prop="userId">
          <el-select v-model="form.userId" placeholder="请选择用户" filterable style="width: 100%">
            <el-option v-for="user in users" :key="user.userId" :label="user.nickName" :value="user.userId"/>
          </el-select>
        </el-form-item>
        <el-form-item label="银行卡号" prop="bankCardNumber">
          <el-input v-model="form.bankCardNumber" placeholder="请输入银行卡号"/>
        </el-form-item>
        <el-form-item label="银行卡状态" prop="bankCardStatus">
          <el-select v-model="form.bankCardStatus" placeholder="请选择银行卡状态">
            <el-option
              v-for="dict in dict.type.sys_normal_disable"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
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
  listUser_accounts,
  getUser_accounts,
  delUser_accounts,
  addUser_accounts,
  updateUser_accounts
} from "@/api/finance/user_accounts"
import {listUser} from "@/api/stock/dropdown_component"

export default {
  name: "User_accounts",
  dicts: ['sys_normal_disable'],
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
      // 用户账户银行卡信息表格数据
      user_accountsList: [],
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
        bankCardNumber: null,
        bankCardStatus: null,
        updatedAt: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        userId: [
          {required: true, message: "用户名不能为空", trigger: "blur"}
        ],
        bankCardNumber: [
          {required: true, message: "银行卡号不能为空", trigger: "blur"}
        ],
        bankCardStatus: [
          {required: true, message: "银行卡状态不能为空", trigger: "blur"}
        ],
      }
    }
  },
  created() {
    this.getUserList();
    this.getList();
  },
  methods: {
    /** 查询用户账户银行卡信息列表 */
    getList() {
      this.loading = true
      listUser_accounts(this.queryParams).then(response => {
        this.user_accountsList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 取消按钮
    cancel() {
      this.open = false
      this.reset()
    },
    // 初始化用户下拉列表
    async getUserList() {
      try {
        this.isLoading = true;
        const response = await listUser({pageSize: this.pageSize || 1000});
        const data = response.data || response;
        if (data.code === 200) {
          this.users = data.rows || [];
          console.info("获取用户列表成功:", this.users);
        } else {
          console.error("获取用户列表失败, 返回码:", data.code, data.msg);
        }
      } catch (error) {
        console.error("获取用户列表失败:", error);
      } finally {
        this.isLoading = false;
      }
    },
    // 根据 userId 获取用户姓名
    getUserName(userId) {
      if (!userId) throw new Error("用户ID不能为空");
      if (!this.users || this.users.length === 0) throw new Error("用户列表为空");
      const targetId = String(userId);
      const user = this.users.find(u => String(u.userId) === targetId);
      if (!user) {
        console.warn(`未找到匹配的用户，用户ID: ${userId}`);
        return "未知用户";
      }
      return user.nickName || "匿名用户";
    },
    // 表单重置
    reset() {
      this.form = {
        id: null,
        userId: null,
        bankCardNumber: null,
        bankCardStatus: null,
        createdAt: null,
        updatedAt: null
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
      this.title = "添加用户账户银行卡信息"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getUser_accounts(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改用户账户银行卡信息"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateUser_accounts(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addUser_accounts(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除用户账户银行卡信息编号为"' + ids + '"的数据项？').then(function () {
        return delUser_accounts(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/user_accounts/export', {
        ...this.queryParams
      }, `user_accounts_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style lang="scss" scoped>
@import "@/assets/styles/global.scss";
</style>
