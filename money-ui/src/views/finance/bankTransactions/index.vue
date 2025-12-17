<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="账户号码" prop="accountNo">
        <el-input
          v-model="queryParams.AccountNo"
          placeholder="请输入账户号码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="交易日期" prop="date">
        <el-date-picker clearable
                        v-model="queryParams.date"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择交易日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="交易类型" prop="transactionType">
        <el-input
          v-model="queryParams.transactionType"
          placeholder="请输入交易类型"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="交易对方" prop="counterParty">
        <el-input
          v-model="queryParams.counterParty"
          placeholder="请输入交易对方"
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
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['finance:bankTransactions:edit']"
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
          v-hasPermi="['finance:bankTransactions:remove']"
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
          v-hasPermi="['finance:bankTransactions:export']"
        >导出
        </el-button>
      </el-col>
      <el-col :span="1.5">
        <el-upload
          ref="upload"
          size="mini"
          :show-file-list="false"
          :before-upload="beforeUpload"
          :on-change="handleFileChange"
          accept=".xlsx,.xls"
        >
          <el-button
            type="success"
            plain
            icon="el-icon-upload"
            size="mini"
            v-hasPermi="['finance:bankTransactions:import']"
          >导入
          </el-button>
          <input type="file" style="display: none;"/>
        </el-upload>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="transactionsList" @selection-change="handleSelectionChange" :stripe="true" :border="false">
      <el-table-column type="selection" width="55" align="center"/>
<!--      <el-table-column label="主键" align="center" prop="id"/>-->
      <el-table-column label="账户号码" align="center" prop="accountNo">
        <template slot-scope="scope">
          <el-tag size="mini" type="info">{{ scope.row.accountNo }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="归属银行" align="center" prop="bank"/>
      <el-table-column label="分行" align="center" prop="subBranch"/>
      <el-table-column label="交易日期" align="center" prop="Date" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.Date, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="货币类型" align="center" prop="currency"/>
      <el-table-column label="交易类型" align="center" prop="transactionType">
        <template slot-scope="scope">
          <el-tag size="mini" effect="plain">{{ scope.row.transactionType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="交易金额" align="right" prop="amount">
        <template slot-scope="scope">
          <span :style="{ color: scope.row.amount >= 0 ? '#F56C6C' : '#67C23A', fontWeight: 'bold' }">
            {{ scope.row.amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="账户余额" align="right" prop="balance"/>
      <el-table-column label="交易对方" align="center" prop="counterParty"/>
      <el-table-column label="交易详情" align="left" prop="transaction" show-overflow-tooltip/>
      <el-table-column label="备注" align="center" prop="note" show-overflow-tooltip/>
      <!--      <el-table-column label="创建时间" align="center" prop="createdAt" width="180">-->
      <!--        <template slot-scope="scope">-->
      <!--          <span>{{ parseTime(scope.row.createdAt, '{y}-{m}-{d}') }}</span>-->
      <!--        </template>-->
      <!--      </el-table-column>-->
      <!--      <el-table-column label="更新时间" align="center" prop="updatedAt" width="180">-->
      <!--        <template slot-scope="scope">-->
      <!--          <span>{{ parseTime(scope.row.updatedAt, '{y}-{m}-{d}') }}</span>-->
      <!--        </template>-->
      <!--      </el-table-column>-->
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['finance:bankTransactions:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:bankTransactions:remove']"
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

    <!-- 添加或修改银行卡流水解析对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="账户号码" prop="AccountNo">
          <el-input v-model="form.AccountNo" placeholder="请输入账户号码"/>
        </el-form-item>
        <el-form-item label="分行" prop="SubBranch">
          <el-input v-model="form.SubBranch" placeholder="请输入分行"/>
        </el-form-item>
        <el-form-item label="交易日期" prop="Date">
          <el-date-picker clearable
                          v-model="form.Date"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择交易日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="货币类型" prop="Currency">
          <el-input v-model="form.Currency" placeholder="请输入货币类型"/>
        </el-form-item>
        <el-form-item label="交易详情" prop="Transaction">
          <el-input v-model="form.Transaction" placeholder="请输入交易详情"/>
        </el-form-item>
        <el-form-item label="交易金额" prop="Amount">
          <el-input v-model="form.Amount" placeholder="请输入交易金额"/>
        </el-form-item>
        <el-form-item label="账户余额" prop="Balance">
          <el-input v-model="form.Balance" placeholder="请输入账户余额"/>
        </el-form-item>
        <el-form-item label="交易对方" prop="CounterParty">
          <el-input v-model="form.CounterParty" placeholder="请输入交易对方"/>
        </el-form-item>
        <el-form-item label="备注" prop="note">
          <el-input v-model="form.note" type="textarea" placeholder="请输入内容"/>
        </el-form-item>
        <el-form-item label="创建时间" prop="createdAt">
          <el-date-picker clearable
                          v-model="form.createdAt"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择创建时间">
          </el-date-picker>
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
  listBankTransactions,
  getBankTransactions,
  delBankTransactions,
  addBankTransactions,
  updateBankTransactions,
  importBankTransactions
} from "@/api/finance/bankTransactions";
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
import Cookies from 'js-cookie';  // 使用 js-cookie 替代 vue-cookies


export default {
  name: "Transactions",
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
      // 银行卡流水解析表格数据
      transactionsList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        accountNo: null,
        subBranch: null,
        date: null,
        currency: null,
        transaction: null,
        amount: null,
        balance: null,
        transactionType: null,
        counterParty: null,
        note: null,
        createdAt: null,
        updatedAt: null,
        userId: null,        // 直接在 form 中使用 userId
      },
      // 表单参数
      form: {
        userId: null         // 直接在 form 中使用 userId
      },
      // 表单校验
      rules: {
        // createdAt: [
        //   {required: true, message: "创建时间不能为空", trigger: "blur"}
        // ],
        // updatedAt: [
        //   {required: true, message: "更新时间不能为空", trigger: "blur"}
        // ]
      }
    };
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
          // 从 cookie 中获取保存的用户名，使用 js-cookie 替代 vue-cookies
          const savedUsername = Cookies.get('username');
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
    /** 查询银行卡流水解析列表 */
    getList() {
      this.loading = true;
      listBankTransactions(this.queryParams).then(response => {
        this.transactionsList = response.rows;
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
        id: null,
        AccountNo: null,
        SubBranch: null,
        Date: null,
        Currency: null,
        Transaction: null,
        Amount: null,
        Balance: null,
        TransactionType: null,
        CounterParty: null,
        note: null,
        createdAt: null,
        updatedAt: null
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
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加银行卡流水解析";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids
      getBankTransactions(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改银行卡流水解析";
      });
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateBankTransactions(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            });
          } else {
            addBankTransactions(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除银行卡流水解析编号为"' + ids + '"的数据项？').then(function () {
        return delBankTransactions(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {
      });
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/bankTransactions/export', {
        ...this.queryParams
      }, `transactions_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleFileChange(file, fileList) {
      // 只在用户选择文件后保存文件并显示确认框
      this.selectedFile = file.raw; // 获取选中的文件
      if (this.selectedFile) {
        // 确保文件类型为 Excel
        if (
          this.selectedFile.type !== 'application/vnd.ms-excel' &&
          this.selectedFile.type !== 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        ) {
          this.$message.error('上传文件只能是 Excel 格式!');
          return;
        }

        // 弹出确认框
        this.$modal.confirm(`是否确认导入文件 "${this.selectedFile.name}" ?`).then(() => {
          this.confirmImport(); // 用户确认后调用导入方法
        }).catch(() => {
          this.resetUpload(); // 用户取消后重置状态
        });
      }
    },

    beforeUpload(file) {
      const isExcel = file.type === 'application/vnd.ms-excel' || file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
      if (!isExcel) {
        this.$message.error('上传文件只能是 Excel 格式!');
      }
      return isExcel;
    },
    confirmImport() {
      if (!this.selectedFile) return; // 确保文件已选中
      this.isUploading = true; // 设置上传状态

      importBankTransactions(this.selectedFile)
        .then(response => {
          this.$message.success('文件上传成功');
          // 处理成功逻辑，例如刷新列表
          this.getList(); // 刷新列表或其他操作
        })
        .catch(error => {
          this.$message.error(`文件上传失败: ${error.response?.data?.message || error.message}`);
          // 处理错误逻辑
        })
        .finally(() => {
          this.isUploading = false; // 结束上传状态
          this.resetUpload(); // 重置上传状态
        });
    },

    resetUpload() {
      this.selectedFile = null; // 清除选择的文件
      this.$refs.upload.clearFiles(); // 清空文件输入框
    }
  }
};
</script>

<style lang="scss" scoped>
</style>
