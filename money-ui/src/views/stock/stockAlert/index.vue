<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="股票代码，例如 600900" prop="stockCode">
        <el-input
          v-model="queryParams.stockCode"
          placeholder="请输入股票代码，例如 600900"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="交易日期" prop="tradeDate">
        <el-date-picker clearable
          v-model="queryParams.tradeDate"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择交易日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="成交时刻" prop="tradeTime">
        <el-date-picker clearable
          v-model="queryParams.tradeTime"
          type="date"
          value-format="yyyy-MM-dd"
          placeholder="请选择成交时刻">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="成交价格" prop="price">
        <el-input
          v-model="queryParams.price"
          placeholder="请输入成交价格"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="成交数量(股)" prop="volume">
        <el-input
          v-model="queryParams.volume"
          placeholder="请输入成交数量(股)"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="每笔平均成交量 (volume / tick_count)" prop="avgVol">
        <el-input
          v-model="queryParams.avgVol"
          placeholder="请输入每笔平均成交量 (volume / tick_count)"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="预警描述" prop="alertMsg">
        <el-input
          v-model="queryParams.alertMsg"
          placeholder="请输入预警描述"
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
          v-hasPermi="['stock:stockAlert:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['stock:stockAlert:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['stock:stockAlert:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['stock:stockAlert:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="stockAlertList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="股票代码，例如 600900" align="center" prop="stockCode" />
      <el-table-column label="交易日期" align="center" prop="tradeDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.tradeDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="成交时刻" align="center" prop="tradeTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.tradeTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="成交价格" align="center" prop="price" />
      <el-table-column label="成交数量(股)" align="center" prop="volume" />
      <el-table-column label="每笔平均成交量 (volume / tick_count)" align="center" prop="avgVol" />
      <el-table-column label="预警描述" align="center" prop="alertMsg" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:stockAlert:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['stock:stockAlert:remove']"
          >删除</el-button>
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

    <!-- 添加或修改大资金入场异动预警对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="股票代码，例如 600900" prop="stockCode">
          <el-input v-model="form.stockCode" placeholder="请输入股票代码，例如 600900" />
        </el-form-item>
        <el-form-item label="交易日期" prop="tradeDate">
          <el-date-picker clearable
            v-model="form.tradeDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择交易日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="成交时刻" prop="tradeTime">
          <el-date-picker clearable
            v-model="form.tradeTime"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择成交时刻">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="成交价格" prop="price">
          <el-input v-model="form.price" placeholder="请输入成交价格" />
        </el-form-item>
        <el-form-item label="成交数量(股)" prop="volume">
          <el-input v-model="form.volume" placeholder="请输入成交数量(股)" />
        </el-form-item>
        <el-form-item label="每笔平均成交量 (volume / tick_count)" prop="avgVol">
          <el-input v-model="form.avgVol" placeholder="请输入每笔平均成交量 (volume / tick_count)" />
        </el-form-item>
        <el-form-item label="预警描述" prop="alertMsg">
          <el-input v-model="form.alertMsg" placeholder="请输入预警描述" />
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
import { listStockAlert, getStockAlert, delStockAlert, addStockAlert, updateStockAlert } from "@/api/stock/stockAlert"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
export default {
  name: "StockAlert",
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
      // 大资金入场异动预警表格数据
      stockAlertList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        stockCode: null,
        tradeDate: null,
        tradeTime: null,
        price: null,
        volume: null,
        avgVol: null,
        alertMsg: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        stockCode: [
          { required: true, message: "股票代码，例如 600900不能为空", trigger: "blur" }
        ],
        tradeDate: [
          { required: true, message: "交易日期不能为空", trigger: "blur" }
        ],
        tradeTime: [
          { required: true, message: "成交时刻不能为空", trigger: "blur" }
        ],
        price: [
          { required: true, message: "成交价格不能为空", trigger: "blur" }
        ],
        volume: [
          { required: true, message: "成交数量(股)不能为空", trigger: "blur" }
        ],
        avgVol: [
          { required: true, message: "每笔平均成交量 (volume / tick_count)不能为空", trigger: "blur" }
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
    /** 查询大资金入场异动预警列表 */
    getList() {
      this.loading = true
      listStockAlert(this.queryParams).then(response => {
        this.stockAlertList = response.rows
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
        stockCode: null,
        tradeDate: null,
        tradeTime: null,
        price: null,
        volume: null,
        avgVol: null,
        alertMsg: null,
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
      this.single = selection.length!==1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加大资金入场异动预警"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getStockAlert(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改大资金入场异动预警"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateStockAlert(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addStockAlert(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除大资金入场异动预警编号为"' + ids + '"的数据项？').then(function() {
        return delStockAlert(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/stockAlert/export', {
        ...this.queryParams
      }, `stockAlert_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
