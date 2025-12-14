<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="提醒类型" prop="alertType">
        <el-select v-model="queryParams.alertType" placeholder="请选择提醒类型" clearable>
          <el-option
            v-for="dict in dict.type.alert_type"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="触发时间" prop="alertDate">
        <el-date-picker clearable
                        v-model="queryParams.alertDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择提醒触发时间">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="ETF代码" prop="stockCode">
        <el-input
          v-model="queryParams.stockCode"
          placeholder="请输入ETF代码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="ETF名称" prop="stockName">
        <el-input
          v-model="queryParams.stockName"
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
          v-hasPermi="['stock:etf_price_alerts:add']"
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
          v-hasPermi="['stock:etf_price_alerts:edit']"
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
          v-hasPermi="['stock:etf_price_alerts:remove']"
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
          v-hasPermi="['stock:etf_price_alerts:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="etf_price_alertsList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
<!--      <el-table-column label="主键ID" align="center" prop="id"/>-->
      <el-table-column label="提醒类型" align="center" prop="alertType">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.alert_type" :value="scope.row.alertType"/>
        </template>
      </el-table-column>
      <el-table-column label="提醒触发时间" align="center" prop="alertDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.alertDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="ETF代码" align="center" prop="stockCode"/>
      <el-table-column label="ETF名称" align="center" prop="stockName"/>
      <el-table-column label="最新价格" align="center" prop="latestPrice"/>
      <el-table-column label="触发阈值" align="center" prop="thresholdPrice"/>
      <el-table-column label="触发次数" align="center" prop="indexCount"/>
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
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:etf_price_alerts:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['stock:etf_price_alerts:remove']"
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

    <!-- 添加或修改ETF买入卖出价格提醒对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="提醒类型" prop="alertType">
          <el-select v-model="form.alertType" placeholder="请选择提醒类型">
            <el-option
              v-for="dict in dict.type.alert_type"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="提醒触发时间" prop="alertDate">
          <el-date-picker clearable
                          v-model="form.alertDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择提醒触发时间">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="ETF代码" prop="stockCode">
          <el-input v-model="form.stockCode" placeholder="请输入ETF代码"/>
        </el-form-item>
        <el-form-item label="ETF名称" prop="stockName">
          <el-input v-model="form.stockName" placeholder="请输入ETF名称"/>
        </el-form-item>
        <el-form-item label="最新价格" prop="latestPrice">
          <el-input v-model="form.latestPrice" placeholder="请输入最新价格"/>
        </el-form-item>
        <el-form-item label="触发提醒的价格阈值" prop="thresholdPrice">
          <el-input v-model="form.thresholdPrice" placeholder="请输入触发提醒的价格阈值"/>
        </el-form-item>
        <el-form-item label="触发提醒的次数" prop="indexCount">
          <el-input v-model="form.indexCount" placeholder="请输入触发提醒的次数"/>
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
  listEtf_price_alerts,
  getEtf_price_alerts,
  delEtf_price_alerts,
  addEtf_price_alerts,
  updateEtf_price_alerts
} from "@/api/stock/etf_price_alerts"

export default {
  name: "Etf_price_alerts",
  dicts: ['alert_type', 'position_status'],
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
      // ETF买入卖出价格提醒表格数据
      etf_price_alertsList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        alertType: null,
        alertDate: null,
        stockCode: null,
        stockName: null,
        latestPrice: null,
        thresholdPrice: null,
        indexCount: null,
        isEnabled: null,
        updatedAt: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        alertType: [
          {required: true, message: "提醒类型：buy=买入提醒，sell=卖出提醒不能为空", trigger: "change"}
        ],
        stockCode: [
          {required: true, message: "ETF代码不能为空", trigger: "blur"}
        ],
        stockName: [
          {required: true, message: "ETF名称不能为空", trigger: "blur"}
        ],
        updatedAt: [
          {required: true, message: "更新时间不能为空", trigger: "blur"}
        ]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询ETF买入卖出价格提醒列表 */
    getList() {
      this.loading = true
      listEtf_price_alerts(this.queryParams).then(response => {
        this.etf_price_alertsList = response.rows
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
        alertType: null,
        alertDate: null,
        stockCode: null,
        stockName: null,
        latestPrice: null,
        thresholdPrice: null,
        indexCount: null,
        isEnabled: null,
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
      this.title = "添加ETF买入卖出价格提醒"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getEtf_price_alerts(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改ETF买入卖出价格提醒"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateEtf_price_alerts(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addEtf_price_alerts(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除ETF买入卖出价格提醒编号为"' + ids + '"的数据项？').then(function () {
        return delEtf_price_alerts(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/etf_price_alerts/export', {
        ...this.queryParams
      }, `etf_price_alerts_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
