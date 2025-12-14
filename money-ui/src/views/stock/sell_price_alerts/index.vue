<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="股票代码" prop="stockCode">
        <el-input
          v-model="queryParams.stockCode"
          placeholder="请输入股票代码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="股票名称" prop="stockName">
        <el-input
          v-model="queryParams.stockName"
          placeholder="请输入股票名称"
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
          v-hasPermi="['stock:sell_price_alerts:add']"
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
          v-hasPermi="['stock:sell_price_alerts:edit']"
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
          v-hasPermi="['stock:sell_price_alerts:remove']"
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
          v-hasPermi="['stock:sell_price_alerts:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="sell_price_alertsList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
<!--      <el-table-column label="主键ID" align="center" prop="id"/>-->
      <!--      <el-table-column label="提醒日期" align="center" prop="alertDate" width="180">-->
      <!--        <template slot-scope="scope">-->
      <!--          <span>{{ parseTime(scope.row.alertDate, '{y}-{m}-{d}') }}</span>-->
      <!--        </template>-->
      <!--      </el-table-column>-->
      <el-table-column label="股票代码" align="center" prop="stockCode"/>
      <el-table-column label="股票名称" align="center" prop="stockName"/>
      <el-table-column label="最新价格" align="center" prop="latestPrice"/>
      <el-table-column label="阈值价格" align="center" prop="thresholdPrice"/>
      <el-table-column label="通知次数" align="center" prop="indexCount"/>
      <el-table-column label="是否持仓" align="center" prop="isEnabled">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.position_status" :value="scope.row.isEnabled"/>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updatedAt" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updatedAt, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <!--      <el-table-column label="API 接口" align="center" prop="stockApi"/>-->
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:sell_price_alerts:edit']"
          >修改
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

    <!-- 添加或修改卖出价位提醒对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="股票代码" prop="stockCode">
          <el-input v-model="form.stockCode" placeholder="请输入股票代码"/>
        </el-form-item>
        <el-form-item label="股票名称" prop="stockName">
          <el-input v-model="form.stockName" placeholder="请输入股票名称"/>
        </el-form-item>
        <el-form-item label="阈值价格" prop="thresholdPrice">
          <el-input v-model="form.thresholdPrice" placeholder="请输入阈值价格"/>
        </el-form-item>
        <el-form-item label="通知次数" prop="indexCount">
          <el-input v-model="form.indexCount" placeholder="请输入通知次数"/>
        </el-form-item>
        <el-form-item label="是否持仓" prop="isEnabled">
          <el-radio-group v-model="form.isEnabled">
            <el-radio
              v-for="dict in dict.type.position_status"
              :key="dict.value"
              :label="parseInt(dict.value)"
            >{{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <!--        <el-form-item label="API 接口" prop="stockApi">-->
        <!--          <el-input v-model="form.stockApi" type="textarea" placeholder="请输入内容"/>-->
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
import {
  listSell_price_alerts,
  getSell_price_alerts,
  delSell_price_alerts,
  addSell_price_alerts,
  updateSell_price_alerts
} from "@/api/stock/sell_price_alerts"

export default {
  name: "Sell_price_alerts",
  dicts: ['position_status', 'synchronization_status'],
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
      // 卖出价位提醒表格数据
      sell_price_alertsList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        alertDate: null,
        stockCode: null,
        stockName: null,
        latestPrice: null,
        thresholdPrice: null,
        indexCount: null,
        isEnabled: null,
        createdAt: null,
        updatedAt: null,
        stockApi: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        stockCode: [
          {required: true, message: "股票代码不能为空", trigger: "blur"}
        ],
        stockName: [
          {required: true, message: "股票名称不能为空", trigger: "blur"}
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询卖出价位提醒列表 */
    getList() {
      this.loading = true
      listSell_price_alerts(this.queryParams).then(response => {
        this.sell_price_alertsList = response.rows
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
        alertDate: null,
        stockCode: null,
        stockName: null,
        latestPrice: null,
        thresholdPrice: null,
        indexCount: null,
        isEnabled: null,
        createdAt: null,
        updatedAt: null,
        stockApi: null
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
      this.title = "添加卖出价位提醒"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getSell_price_alerts(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改卖出价位提醒"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateSell_price_alerts(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addSell_price_alerts(this.form).then(response => {
              this.$modal.msgSuccess("新增成功")
              this.open = false
              this.getList()
            })
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除卖出价位提醒编号为"' + ids + '"的数据项？').then(function () {
        return delSell_price_alerts(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/sell_price_alerts/export', {
        ...this.queryParams
      }, `sell_price_alerts_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
