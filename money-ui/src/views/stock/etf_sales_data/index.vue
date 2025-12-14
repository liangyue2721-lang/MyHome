<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="数据记录日期" prop="recordDate">
        <el-date-picker clearable
                        v-model="queryParams.recordDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择数据记录日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="ETF代码" prop="etfCode">
        <el-input
          v-model="queryParams.etfCode"
          placeholder="请输入ETF代码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="ETF名称" prop="etfName">
        <el-input
          v-model="queryParams.etfName"
          placeholder="请输入ETF名称"
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
          v-hasPermi="['stock:etf_sales_data:add']"
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
          v-hasPermi="['stock:etf_sales_data:edit']"
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
          v-hasPermi="['stock:etf_sales_data:remove']"
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
          v-hasPermi="['stock:etf_sales_data:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="etf_sales_dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
<!--      <el-table-column label="主键，自增" align="center" prop="id"/>-->
      <el-table-column label="数据记录日期" align="center" prop="recordDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.recordDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="ETF代码" align="center" prop="etfCode"/>
      <el-table-column label="ETF名称" align="center" prop="etfName"/>
      <el-table-column label="分时利润" align="center" prop="profit"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:etf_sales_data:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['stock:etf_sales_data:remove']"
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

    <!-- 添加或修改eft折线图数据对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="数据记录日期" prop="recordDate">
          <el-date-picker clearable
                          v-model="form.recordDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择数据记录日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="ETF代码" prop="etfCode">
          <el-input v-model="form.etfCode" placeholder="请输入ETF代码"/>
        </el-form-item>
        <el-form-item label="ETF名称" prop="etfName">
          <el-input v-model="form.etfName" placeholder="请输入ETF名称"/>
        </el-form-item>
        <el-form-item label="分时利润" prop="profit">
          <el-input v-model="form.profit" placeholder="请输入分时利润"/>
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
  listEtf_sales_data,
  getEtf_sales_data,
  delEtf_sales_data,
  addEtf_sales_data,
  updateEtf_sales_data
} from "@/api/stock/etf_sales_data"

export default {
  name: "Etf_sales_data",
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
      // eft折线图数据表格数据
      etf_sales_dataList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        recordDate: null,
        etfCode: null,
        etfName: null,
        profit: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        recordDate: [
          {required: true, message: "数据记录日期不能为空", trigger: "blur"}
        ],
        etfCode: [
          {required: true, message: "ETF代码不能为空", trigger: "blur"}
        ],
        etfName: [
          {required: true, message: "ETF名称不能为空", trigger: "blur"}
        ],
        profit: [
          {required: true, message: "分时利润不能为空", trigger: "blur"}
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询eft折线图数据列表 */
    getList() {
      this.loading = true
      listEtf_sales_data(this.queryParams).then(response => {
        this.etf_sales_dataList = response.rows
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
        etfCode: null,
        etfName: null,
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
      this.title = "添加eft折线图数据"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getEtf_sales_data(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改eft折线图数据"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateEtf_sales_data(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addEtf_sales_data(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除eft折线图数据编号为"' + ids + '"的数据项？').then(function () {
        return delEtf_sales_data(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/etf_sales_data/export', {
        ...this.queryParams
      }, `etf_sales_data_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
