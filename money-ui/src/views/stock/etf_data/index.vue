<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
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
          v-hasPermi="['stock:etf_data:add']"
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
          v-hasPermi="['stock:etf_data:edit']"
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
          v-hasPermi="['stock:etf_data:remove']"
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
          v-hasPermi="['stock:etf_data:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="etf_dataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="ETF代码" align="center" prop="etfCode"/>
      <el-table-column label="ETF名称" align="center" prop="etfName"/>
      <el-table-column label="交易日期" align="center" prop="tradeDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.tradeDate, '{y}-{m}-{d} {h}:{m}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开盘价" align="center" prop="openPrice"/>
      <el-table-column label="最高价" align="center" prop="highPrice"/>
      <el-table-column label="最低价" align="center" prop="lowPrice"/>
      <el-table-column label="最新价/收盘价" align="center" prop="closePrice"/>
      <el-table-column label="成交量" align="center" prop="volume"/>
      <el-table-column label="成交额" align="center" prop="turnover"/>
      <el-table-column label="买入价" align="center" prop="bidPrice"/>
      <el-table-column label="卖出价" align="center" prop="askPrice"/>
      <el-table-column label="外盘成交量" align="center" prop="externalMarketVolume"/>
      <el-table-column label="内盘成交量" align="center" prop="internalMarketVolume"/>
      <el-table-column label="买1价" align="center" prop="buy1Price"/>
      <el-table-column label="买1量" align="center" prop="buy1Volume"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:etf_data:edit']"
          >修改
          </el-button>
          <!--          <el-button-->
          <!--            size="mini"-->
          <!--            type="text"-->
          <!--            icon="el-icon-delete"-->
          <!--            @click="handleDelete(scope.row)"-->
          <!--            v-hasPermi="['stock:etfData:remove']"-->
          <!--          >删除-->
          <!--          </el-button>-->
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

    <!-- 添加或修改ETF交易数据对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="ETF名称" prop="etfName">
          <el-input v-model="form.etfName" placeholder="请输入ETF名称"/>
        </el-form-item>
        <el-form-item label="开盘价" prop="openPrice">
          <el-input v-model="form.openPrice" placeholder="请输入开盘价"/>
        </el-form-item>
        <el-form-item label="最高价" prop="highPrice">
          <el-input v-model="form.highPrice" placeholder="请输入最高价"/>
        </el-form-item>
        <el-form-item label="最低价" prop="lowPrice">
          <el-input v-model="form.lowPrice" placeholder="请输入最低价"/>
        </el-form-item>
        <el-form-item label="最新价/收盘价" prop="closePrice">
          <el-input v-model="form.closePrice" placeholder="请输入最新价/收盘价"/>
        </el-form-item>
        <el-form-item label="成交量" prop="volume">
          <el-input v-model="form.volume" placeholder="请输入成交量"/>
        </el-form-item>
        <el-form-item label="成交额" prop="turnover">
          <el-input v-model="form.turnover" placeholder="请输入成交额"/>
        </el-form-item>
        <el-form-item label="买入价" prop="bidPrice">
          <el-input v-model="form.bidPrice" placeholder="请输入买入价"/>
        </el-form-item>
        <el-form-item label="卖出价" prop="askPrice">
          <el-input v-model="form.askPrice" placeholder="请输入卖出价"/>
        </el-form-item>
        <el-form-item label="外盘成交量" prop="externalMarketVolume">
          <el-input v-model="form.externalMarketVolume" placeholder="请输入外盘成交量"/>
        </el-form-item>
        <el-form-item label="内盘成交量" prop="internalMarketVolume">
          <el-input v-model="form.internalMarketVolume" placeholder="请输入内盘成交量"/>
        </el-form-item>
        <el-form-item label="买1价" prop="buy1Price">
          <el-input v-model="form.buy1Price" placeholder="请输入买1价"/>
        </el-form-item>
        <el-form-item label="买1量" prop="buy1Volume">
          <el-input v-model="form.buy1Volume" placeholder="请输入买1量"/>
        </el-form-item>
        <el-form-item label="api接口" prop="stockApi">
          <el-input v-model="form.stockApi" type="textarea" placeholder="请输入内容"/>
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
import {listEtf_data, getEtf_data, delEtf_data, addEtf_data, updateEtf_data} from "@/api/stock/etf_data"

export default {
  name: "Etf_data",
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
      // ETF交易数据表格数据
      etf_dataList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        etfName: null,
        openPrice: null,
        highPrice: null,
        lowPrice: null,
        closePrice: null,
        volume: null,
        turnover: null,
        bidPrice: null,
        askPrice: null,
        externalMarketVolume: null,
        internalMarketVolume: null,
        buy1Price: null,
        buy1Volume: null,
        stockApi: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        etfCode: [
          {required: true, message: "ETF代码不能为空", trigger: "blur"}
        ],
        etfName: [
          {required: true, message: "ETF名称不能为空", trigger: "blur"}
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询ETF交易数据列表 */
    getList() {
      this.loading = true
      listEtf_data(this.queryParams).then(response => {
        this.etf_dataList = response.rows
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
        etfCode: null,
        etfName: null,
        tradeDate: null,
        openPrice: null,
        highPrice: null,
        lowPrice: null,
        closePrice: null,
        volume: null,
        turnover: null,
        bidPrice: null,
        askPrice: null,
        externalMarketVolume: null,
        internalMarketVolume: null,
        buy1Price: null,
        buy1Volume: null,
        stockApi: null,
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
      this.ids = selection.map(item => item.etfCode)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加ETF交易数据"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const etfCode = row.etfCode || this.ids
      getEtf_data(etfCode).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改ETF交易数据"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.etfCode != null) {
            updateEtf_data(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addEtf_data(this.form).then(response => {
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
      const etfCodes = row.etfCode || this.ids
      this.$modal.confirm('是否确认删除ETF交易数据编号为"' + etfCodes + '"的数据项？').then(function () {
        return delEtf_data(etfCodes)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/etf_data/export', {
        ...this.queryParams
      }, `etf_data_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
