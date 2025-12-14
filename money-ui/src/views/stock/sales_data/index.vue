<template>
  <div class="app-container">
    <el-card class="box-card search-wrapper" shadow="hover">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
        <el-form-item label="记录日期" prop="recordDate">
          <el-date-picker
            clearable
            v-model="queryParams.recordDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择日期"
            style="width: 220px"
          >
          </el-date-picker>
        </el-form-item>
        <el-form-item label="当天利润" prop="profit">
          <el-input
            v-model="queryParams.profit"
            placeholder="请输入利润金额"
            clearable
            @keyup.enter.native="handleQuery"
            style="width: 220px"
          >
            <template slot="prefix">￥</template>
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="box-card table-wrapper" shadow="never">
      <el-row :gutter="10" class="mb8 table-toolbar">
        <el-col :span="1.5">
          <el-button
            type="primary"
            plain
            icon="el-icon-plus"
            size="mini"
            @click="handleAdd"
            v-hasPermi="['stock:sales_data:add']"
          >新增记录
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
            v-hasPermi="['stock:sales_data:edit']"
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
            v-hasPermi="['stock:sales_data:remove']"
          >批量删除
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button
            type="warning"
            plain
            icon="el-icon-download"
            size="mini"
            @click="handleExport"
            v-hasPermi="['stock:sales_data:export']"
          >导出数据
          </el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table
        v-loading="loading"
        :data="sales_dataList"
        @selection-change="handleSelectionChange"
        border
        stripe
        :header-cell-style="{background:'#f8f8f9', color:'#515a6e', fontWeight: 'bold'}"
      >
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="序号" type="index" width="60" align="center"/>

        <el-table-column label="数据记录日期" align="center" prop="recordDate" width="180" sortable>
          <template slot-scope="scope">
            <i class="el-icon-date" style="margin-right: 5px; color: #909399"></i>
            <span>{{ parseTime(scope.row.recordDate, '{y}-{m}-{d}') }}</span>
          </template>
        </el-table-column>

        <el-table-column label="当天利润 (元)" align="center" prop="profit">
          <template slot-scope="scope">
            <span :style="{ color: scope.row.profit >= 0 ? '#13ce66' : '#ff4949', fontWeight: 'bold' }">
              {{ scope.row.profit >= 0 ? '+' : '' }}{{ scope.row.profit }}
            </span>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
          <template slot-scope="scope">
            <el-button
              size="mini"
              type="text"
              icon="el-icon-edit"
              class="text-primary"
              @click="handleUpdate(scope.row)"
              v-hasPermi="['stock:sales_data:edit']"
            >修改
            </el-button>
            <el-divider direction="vertical"></el-divider>
            <el-button
              size="mini"
              type="text"
              icon="el-icon-delete"
              class="text-danger"
              @click="handleDelete(scope.row)"
              v-hasPermi="['stock:sales_data:remove']"
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
    </el-card>

    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body :close-on-click-modal="false">
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="记录日期" prop="recordDate">
          <el-date-picker
            style="width: 100%"
            clearable
            v-model="form.recordDate"
            type="date"
            value-format="yyyy-MM-dd"
            placeholder="请选择数据记录日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="当天利润" prop="profit">
          <el-input v-model="form.profit" placeholder="请输入当天利润" type="number">
            <template slot="append">元</template>
          </el-input>
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
import {listSales_data, getSales_data, delSales_data, addSales_data, updateSales_data} from "@/api/stock/sales_data"
import {listUser} from "@/api/stock/dropdown_component";

export default {
  name: "Sales_data",
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
      // 利润折线图数据表格数据
      sales_dataList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        recordDate: null,
        profit: null,
        userId: null,        // 直接在 form 中使用 userId
      },
      form: {
        userId: null         // 直接在 form 中使用 userId
      },
      // 表单校验
      rules: {
        recordDate: [
          {required: true, message: "数据记录日期不能为空", trigger: "blur"}
        ],
        profit: [
          {required: true, message: "当天利润不能为空", trigger: "blur"}
        ],
      }
    }
  },
  async created() {
    // 获取用户列表并设置 userId
    await this.initUserList();
    this.getList()
  },
  methods: {
    /**
     * 初始化用户列表数据
     */
    async initUserList() {
      try {
        // 调用后端接口获取用户列表，传入分页参数
        const response = await listUser({pageSize: this.pageSize});
        // 兼容接口返回格式
        const payload = response.data || response;
        // 根据返回数据格式判断用户列表位置
        const rawUsers = Array.isArray(payload.rows)
          ? payload.rows
          : Array.isArray(payload)
            ? payload
            : [];

        // 格式化用户列表
        const userList = rawUsers.map(u => ({
          id: u.userId,
          name: u.userName || u.nickName || `用户${u.userId}`
        }));

        console.log('用户列表加载完成，列表数据:', userList);

        if (userList.length) {
          // 从 cookie 中获取保存的用户名
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
      }
    },
    /** 查询利润折线图数据列表 */
    getList() {
      this.loading = true
      listSales_data(this.queryParams).then(response => {
        this.sales_dataList = response.rows
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
        recordDate: null,
        profit: null,
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
      this.title = "添加利润记录"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getSales_data(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改利润记录"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateSales_data(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addSales_data(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除数据编号为"' + ids + '"的数据项？').then(function () {
        return delSales_data(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/sales_data/export', {
        ...this.queryParams
      }, `sales_data_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
/* 搜索区域样式 */
.search-wrapper {
  margin-bottom: 12px;
}

/* 按钮组样式微调 */
.table-toolbar {
  margin-bottom: 16px;
}

/* 文本按钮颜色 */
.text-primary {
  color: #409EFF;
}

.text-danger {
  color: #F56C6C;
}

.text-primary:hover, .text-danger:hover {
  opacity: 0.8;
}

/* 整个容器背景优化 */
.app-container {
  background-color: #f0f2f5;
  padding: 20px;
  min-height: calc(100vh - 84px);
}
</style>
