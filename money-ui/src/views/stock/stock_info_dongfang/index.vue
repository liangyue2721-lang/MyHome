<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="股票代码" prop="stockCode">
        <el-input
          v-model="queryParams.stockCode"
          placeholder="请输入股票的代码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="公司名称" prop="companyName">
        <el-input
          v-model="queryParams.companyName"
          placeholder="请输入股票的公司名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="板块类型" prop="ticker">
        <el-select v-model="selectedTicker" placeholder="请选择板块类型" @change="updatePlateType">
          <el-option
            v-for="option in tickerOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          ></el-option>
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
          v-hasPermi="['stock:dongfang:add']"
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
          v-hasPermi="['stock:stock_info_dongfang:edit']"
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
          v-hasPermi="['stock:stock_info_dongfang:remove']"
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
          v-hasPermi="['stock:stock_info_dongfang:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="stock_info_dongfangList" @selection-change="handleSelectionChange" :stripe="true" :border="false">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <!--      <el-table-column label="股票类型" align="center" prop="type" />-->
      <el-table-column label="板块类型" align="center" prop="ticker">
        <template slot-scope="scope">
          <el-tag size="mini" type="info">{{ getChineseName(scope.row.ticker) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="股票代码" align="center" prop="stockCode">
        <template slot-scope="scope">
          <b>{{ scope.row.stockCode }}</b>
        </template>
      </el-table-column>
      <el-table-column label="公司名称" align="center" prop="companyName"/>
      <el-table-column label="当前价格" align="right" prop="price">
        <template slot-scope="scope">
          <span :style="{ color: scope.row.netChange > 0 ? '#F56C6C' : (scope.row.netChange < 0 ? '#67C23A' : '') }">
            {{ scope.row.price }}
          </span>
        </template>
      </el-table-column>
<!--      <el-table-column label="涨跌幅" align="center" prop="circulationShares"/>-->
      <el-table-column label="涨跌额" align="right" prop="netChange">
        <template slot-scope="scope">
          <span :style="{ color: scope.row.netChange > 0 ? '#F56C6C' : (scope.row.netChange < 0 ? '#67C23A' : '') }">
            {{ scope.row.netChange }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="最高价" align="right" prop="highPrice"/>
      <el-table-column label="最低价" align="right" prop="lowPrice"/>
      <el-table-column label="开盘价" align="right" prop="openPrice"/>
      <el-table-column label="收盘价" align="right" prop="closePrice"/>
      <el-table-column label="交易量" align="right" prop="tradingVolume"/>
      <!--      <el-table-column label="股票的市值" align="center" prop="marketValue" />-->
      <!--      <el-table-column label="股票的总资产" align="center" prop="totalAssets" />-->
      <el-table-column label="总股数" align="right" prop="totalShares"/>
      <!--      <el-table-column label="股票的流通股数" align="center" prop="circulationShares" />-->
      <!--      <el-table-column label="股票所属市场类别" align="center" prop="marketCategory" />-->
      <!--      <el-table-column label="股票的交易量" align="center" prop="tradingVolume" />-->
      <!--      <el-table-column label="附加信息字段" align="center" prop="additionalInfo" />-->
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:stock_info_dongfang:edit']"
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

    <!-- 添加或修改东方财富股票对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="板块类型" prop="ticker">
          <el-input v-model="form.ticker" placeholder="请输入板块类型"/>
        </el-form-item>
        <el-form-item label="股票当前价格" prop="price">
          <el-input v-model="form.price" placeholder="请输入股票当前价格"/>
        </el-form-item>
        <el-form-item label="股票的交易量" prop="volume">
          <el-input v-model="form.volume" placeholder="请输入股票的交易量"/>
        </el-form-item>
        <el-form-item label="股票的市值，通常为股票价格 * 流通股数" prop="marketValue">
          <el-input v-model="form.marketValue" placeholder="请输入股票的市值，通常为股票价格 * 流通股数"/>
        </el-form-item>
        <el-form-item label="股票的总资产" prop="totalAssets">
          <el-input v-model="form.totalAssets" placeholder="请输入股票的总资产"/>
        </el-form-item>
        <el-form-item label="股票的总股数" prop="totalShares">
          <el-input v-model="form.totalShares" placeholder="请输入股票的总股数"/>
        </el-form-item>
        <el-form-item label="股票的流通股数" prop="circulationShares">
          <el-input v-model="form.circulationShares" placeholder="请输入股票的流通股数"/>
        </el-form-item>
        <el-form-item label="股票的涨跌额" prop="netChange">
          <el-input v-model="form.netChange" placeholder="请输入股票的涨跌额"/>
        </el-form-item>
        <el-form-item label="股票的涨跌幅，通常以百分比显示" prop="netChangePercentage">
          <el-input v-model="form.netChangePercentage" placeholder="请输入股票的涨跌幅，通常以百分比显示"/>
        </el-form-item>
        <el-form-item label="股票的代码" prop="stockCode">
          <el-input v-model="form.stockCode" placeholder="请输入股票的代码"/>
        </el-form-item>
        <el-form-item label="股票所属市场类别" prop="marketCategory">
          <el-input v-model="form.marketCategory" placeholder="请输入股票所属市场类别"/>
        </el-form-item>
        <el-form-item label="股票的公司名称" prop="companyName">
          <el-input v-model="form.companyName" placeholder="请输入股票的公司名称"/>
        </el-form-item>
        <el-form-item label="股票的最高价" prop="highPrice">
          <el-input v-model="form.highPrice" placeholder="请输入股票的最高价"/>
        </el-form-item>
        <el-form-item label="股票的最低价" prop="lowPrice">
          <el-input v-model="form.lowPrice" placeholder="请输入股票的最低价"/>
        </el-form-item>
        <el-form-item label="股票的开盘价" prop="openPrice">
          <el-input v-model="form.openPrice" placeholder="请输入股票的开盘价"/>
        </el-form-item>
        <el-form-item label="股票的收盘价" prop="closePrice">
          <el-input v-model="form.closePrice" placeholder="请输入股票的收盘价"/>
        </el-form-item>
        <el-form-item label="股票的交易量" prop="tradingVolume">
          <el-input v-model="form.tradingVolume" placeholder="请输入股票的交易量"/>
        </el-form-item>
        <el-form-item label="附加信息字段" prop="additionalInfo">
          <el-input v-model="form.additionalInfo" placeholder="请输入附加信息字段"/>
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
  listStock_info_dongfang,
  getStock_info_dongfang,
  delStock_info_dongfang,
  addStock_info_dongfang,
  updateStock_info_dongfang
} from "@/api/stock/stock_info_dongfang"

export default {
  name: "Stock_info_dongfang",
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
      // 东方财富股票表格数据
      stock_info_dongfangList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      tickerOptions: [
        {value: '600', label: '600 - 沪市A股'},
        {value: '601', label: '601 - 沪市A股'},
        {value: '603', label: '603 - 沪市A股'},
        {value: '605', label: '605 - 沪市A股'},
        {value: '900', label: '900 - 沪市B股'},
        {value: '000', label: '000 - 深市A股'},
        {value: '200', label: '200 - 深市B股'},
        {value: '300', label: '300 - 创业板'},
        {value: '301', label: '301 - 创业板'},
        {value: '688', label: '688 - 科创板'},
        {value: '002', label: '002 - 中小板'},
        {value: '920', label: '920 - 北交所'}
      ],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        ticker: null,
        type: null,
        price: null,
        volume: null,
        marketValue: null,
        totalAssets: null,
        totalShares: null,
        circulationShares: null,
        netChange: null,
        netChangePercentage: null,
        stockCode: null,
        marketCategory: null,
        companyName: null,
        highPrice: null,
        lowPrice: null,
        openPrice: null,
        closePrice: null,
        tradingVolume: null,
        additionalInfo: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {}
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询东方财富股票列表 */
    getList() {
      this.loading = true
      listStock_info_dongfang(this.queryParams).then(response => {
        this.stock_info_dongfangList = response.rows
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
        ticker: null,
        type: null,
        price: null,
        volume: null,
        marketValue: null,
        totalAssets: null,
        totalShares: null,
        circulationShares: null,
        netChange: null,
        netChangePercentage: null,
        stockCode: null,
        marketCategory: null,
        companyName: null,
        highPrice: null,
        lowPrice: null,
        openPrice: null,
        closePrice: null,
        tradingVolume: null,
        additionalInfo: null,
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
      this.title = "添加东方财富股票"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getStock_info_dongfang(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改东方财富股票"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateStock_info_dongfang(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addStock_info_dongfang(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除东方财富股票编号为"' + ids + '"的数据项？').then(function () {
        return delStock_info_dongfang(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    getChineseName(ticker) {
      switch (true) {
        case ticker.startsWith('300') || ticker.startsWith('301'):
          return '创业板';
        case ticker.startsWith('688'):
          return '科创板';
        case ticker.startsWith('200'):
          return '深市B股';
        case ticker.startsWith('900'):
          return '沪市B股';
        case ticker.startsWith('002'):
          return '中小板';
        case ticker.startsWith('600') || ticker.startsWith('601') || ticker.startsWith('603'):
          return '沪市主板';
        case ticker.startsWith('000') || ticker.startsWith('001') || ticker.startsWith('003'):
          return '深市主板';
        case ticker.startsWith('8') || ticker.startsWith('920'):
          return '北交所';
        default:
          return '未知板块';
      }
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/stock_info_dongfang/export', {
        ...this.queryParams
      }, `stock_info_dongfang_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
