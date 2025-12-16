<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="交易时间" prop="transactionTime">
        <el-date-picker clearable
                        v-model="queryParams.transactionTime"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择交易时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="交易对方" prop="counterparty">
        <el-input
          v-model="queryParams.counterparty"
          placeholder="请输入交易对方"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品类型" prop="productType">
        <el-input
          v-model="queryParams.productType"
          placeholder="请输入商品类型"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="流水来源" prop="source">
        <el-input
          v-model="queryParams.source"
          placeholder="请输入流水来源"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商品" prop="product">
        <el-input
          v-model="queryParams.product"
          placeholder="请输入商品"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="支付方式" prop="paymentMethod">
        <el-input
          v-model="queryParams.paymentMethod"
          placeholder="请输入支付方式"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="交易单号" prop="transactionId">
        <el-input
          v-model="queryParams.transactionId"
          placeholder="请输入交易单号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="商户单号" prop="merchantId">
        <el-input
          v-model="queryParams.merchantId"
          placeholder="请输入商户单号"
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
          v-hasPermi="['finance:weChatRecords:edit']"
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
          v-hasPermi="['finance:weChatRecords:remove']"
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
          v-hasPermi="['finance:weChatRecords:export']"
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
          accept=".csv, .xlsx"
        >
          <el-button
            type="success"
            plain
            icon="el-icon-upload"
            size="mini"
            action=""
            v-hasPermi="['finance:weChatRecords:import']"
          >导入
          </el-button>
          <input type="file" style="display: none;"/>
        </el-upload>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="weChatRecordsList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="ID" align="center" prop="id"/>
      <el-table-column label="交易时间" align="center" prop="transactionTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.transactionTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="交易类型" align="center" prop="transactionType"/>
      <el-table-column label="交易对方" align="center" prop="counterparty"/>
      <el-table-column label="商品" align="center" prop="product"/>
      <el-table-column label="商品类型" align="center" prop="productType"/>
      <el-table-column label="流水来源" align="center" prop="source"/>
      <el-table-column label="收入/支出" align="center" prop="inOut"/>
      <el-table-column label="金额(元)" align="center" prop="amount"/>
      <el-table-column label="支付方式" align="center" prop="paymentMethod"/>
      <el-table-column label="当前状态" align="center" prop="transactionStatus"/>
      <el-table-column label="交易单号" align="center" prop="transactionId"/>
      <el-table-column label="商户单号" align="center" prop="merchantId"/>
      <el-table-column label="备注" align="center" prop="note"/>
      <el-table-column label="创建时间" align="center" prop="createdAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createdAt, '{y}-{m}-{d}') }}</span>
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
            v-hasPermi="['finance:weChatRecords:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:weChatRecords:remove']"
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

    <!-- 添加或修改微信支付宝流水对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="交易时间" prop="transactionTime">
          <el-date-picker clearable
                          v-model="form.transactionTime"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择交易时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="交易对方" prop="counterparty">
          <el-input v-model="form.counterparty" placeholder="请输入交易对方"/>
        </el-form-item>
        <el-form-item label="商品" prop="product">
          <el-input v-model="form.product" placeholder="请输入商品"/>
        </el-form-item>
        <el-form-item label="商品类型" prop="productType">
          <el-input v-model="form.productType" placeholder="请输入商品类型"/>
        </el-form-item>
        <el-form-item label="流水来源" prop="source">
          <el-input v-model="form.source" placeholder="请输入流水来源"/>
        </el-form-item>
        <el-form-item label="收入/支出" prop="inOut">
          <el-input v-model="form.inOut" placeholder="请输入收入/支出"/>
        </el-form-item>
        <el-form-item label="金额(元)" prop="amount">
          <el-input v-model="form.amount" placeholder="请输入金额(元)"/>
        </el-form-item>
        <el-form-item label="支付方式" prop="paymentMethod">
          <el-input v-model="form.paymentMethod" placeholder="请输入支付方式"/>
        </el-form-item>
        <el-form-item label="交易单号" prop="transactionId">
          <el-input v-model="form.transactionId" placeholder="请输入交易单号"/>
        </el-form-item>
        <el-form-item label="商户单号" prop="merchantId">
          <el-input v-model="form.merchantId" placeholder="请输入商户单号"/>
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
  listWeChatRecords,
  getWeChatRecords,
  delWeChatRecords,
  addWeChatRecords,
  updateWeChatRecords,
  importWeChatRecords
} from "@/api/finance/weChatRecords"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API

export default {
  name: "WeChatRecords",
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
      // 微信支付宝流水表格数据
      weChatRecordsList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        transactionTime: null,
        transactionType: null,
        counterparty: null,
        product: null,
        productType: null,
        source: null,
        inOut: null,
        amount: null,
        paymentMethod: null,
        transactionStatus: null,
        transactionId: null,
        merchantId: null,
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
        transactionTime: [
          {required: true, message: "交易时间不能为空", trigger: "blur"}
        ],
        transactionType: [
          {required: true, message: "交易类型不能为空", trigger: "change"}
        ],
        counterparty: [
          {required: true, message: "交易对方不能为空", trigger: "blur"}
        ],
        product: [
          {required: true, message: "商品不能为空", trigger: "blur"}
        ],
        productType: [
          {required: true, message: "商品类型不能为空", trigger: "change"}
        ],
        source: [
          {required: true, message: "流水来源不能为空", trigger: "blur"}
        ],
        inOut: [
          {required: true, message: "收入/支出不能为空", trigger: "blur"}
        ],
        amount: [
          {required: true, message: "金额(元)不能为空", trigger: "blur"}
        ],
        paymentMethod: [
          {required: true, message: "支付方式不能为空", trigger: "blur"}
        ],
        transactionStatus: [
          {required: true, message: "当前状态不能为空", trigger: "change"}
        ],
        transactionId: [
          {required: true, message: "交易单号不能为空", trigger: "blur"}
        ]
      },
      selectedFile: null, // 存储选择的文件
      isUploading: false, // 上传状态
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
    /** 查询微信支付宝流水列表 */
    getList() {
      this.loading = true
      listWeChatRecords(this.queryParams).then(response => {
        this.weChatRecordsList = response.rows
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
        transactionTime: null,
        transactionType: null,
        counterparty: null,
        product: null,
        productType: null,
        source: null,
        inOut: null,
        amount: null,
        paymentMethod: null,
        transactionStatus: null,
        transactionId: null,
        merchantId: null,
        note: null,
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
      this.title = "添加微信支付宝流水"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getWeChatRecords(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改微信支付宝流水"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateWeChatRecords(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addWeChatRecords(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除微信支付宝流水编号为"' + ids + '"的数据项？').then(function () {
        return delWeChatRecords(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/weChatRecords/export', {
        ...this.queryParams
      }, `weChatRecords_${new Date().getTime()}.xlsx`)
    },
    /** 导入按钮操作 */
    handleFileChange(file, fileList) {
      // 只在用户选择文件后保存文件并显示确认框
      this.selectedFile = file.raw; // 获取选中的文件
      if (this.selectedFile) {
        // 弹出确认框
        this.$modal.confirm(`是否确认导入文件 "${this.selectedFile.name}" ?`).then(() => {
          this.confirmImport(); // 用户确认后调用导入方法
        }).catch(() => {
          this.resetUpload(); // 用户取消后重置状态
        });
      }
    },

    beforeUpload(file) {
      const isCsv = file.type === 'text/csv' || file.type === 'application/vnd.ms-excel';
      const isXlsx = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet';
      if (!isCsv && !isXlsx) {
        this.$message.error('上传文件只能是 .csv 或 .xlsx 格式!');
      }
      return isCsv || isXlsx;
    },
    confirmImport() {
      if (!this.selectedFile) return; // 确保文件已选中
      this.isUploading = true; // 设置上传状态

      importWeChatRecords(this.selectedFile, this.form.userId)
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
@import "@/assets/styles/global.scss";
</style>
