<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="证券代码" prop="securityCode">
        <el-input
          v-model="queryParams.securityCode"
          placeholder="请输入证券代码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="证券名称" prop="securityName">
        <el-input
          v-model="queryParams.securityName"
          placeholder="请输入证券名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="申购日期" prop="applyDate">
        <el-date-picker clearable
                        v-model="queryParams.applyDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择申购日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="上市日期" prop="listingDate">
        <el-date-picker clearable
                        v-model="queryParams.listingDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择上市日期">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="中签号公布日" prop="ballotNumDate">
        <el-date-picker clearable
                        v-model="queryParams.ballotNumDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择中签号公布日">
        </el-date-picker>
      </el-form-item>
      <el-form-item label="中签缴款日" prop="ballotPayDate">
        <el-date-picker clearable
                        v-model="queryParams.ballotPayDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择中签缴款日">
        </el-date-picker>
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
          v-hasPermi="['stock:stock_issue_info:add']"
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
          v-hasPermi="['stock:stock_issue_info:edit']"
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
          v-hasPermi="['stock:stock_issue_info:remove']"
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
          v-hasPermi="['stock:stock_issue_info:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="stock_issue_infoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="板块类型" align="center" prop="ticker">
        <template slot-scope="scope">
          {{ getChineseName(String(scope.row.securityCode).substr(0, 3)) }}
        </template>
      </el-table-column>
<!--      <el-table-column label="申购代码" align="center" prop="applyCode"/>-->
<!--      <el-table-column label="信息编号" align="center" prop="infoCode"/>-->
      <el-table-column label="证券代码" align="center" prop="securityCode"/>
      <el-table-column label="证券名称" align="center" prop="securityName"/>
<!--      <el-table-column label="交易市场代码" align="center" prop="tradeMarketCode"/>-->
      <el-table-column label="交易市场" align="center" prop="tradeMarket"/>
<!--      <el-table-column label="市场类型" align="center" prop="marketType"/>-->
<!--      <el-table-column label="机构类型" align="center" prop="orgType"/>-->
      <el-table-column label="主营业务" align="center">
        <template #default="{ row }">
          <div class="content-wrapper">
          <span v-show="hoverRow === row.infoCode" class="full-content">
            {{ row.mainBusiness }}
          </span>
            <span v-show="hoverRow !== row.infoCode" class="truncated-content">
            {{ truncateText(row.mainBusiness) }}
          </span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="发行数量(万股)" align="center" prop="issueNum"/>
<!--      <el-table-column label="网上发行数量" align="center" prop="onlineIssueNum"/>-->
<!--      <el-table-column label="网下配售数量" align="center" prop="offlinePlacingNum"/>-->
      <el-table-column label="发行价格" align="center" prop="issuePrice"/>
      <el-table-column label="发行后市盈率" align="center" prop="afterIssuePe"/>
<!--      <el-table-column label="初始倍数" align="center" prop="initialMultiple"/>-->
      <el-table-column label="最新价格" align="center" prop="latelyPrice"/>
      <el-table-column label="收盘价格" align="center" prop="closePrice"/>
      <el-table-column label="涨停价格" align="center" prop="limitUpPrice"/>
      <el-table-column label="最新价格" align="center" prop="newestPrice"/>
      <el-table-column label="申购日期" align="center" prop="applyDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.applyDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="中签号公布日" align="center" prop="ballotNumDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.ballotNumDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="中签缴款日" align="center" prop="ballotPayDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.ballotPayDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="上市日期" align="center" prop="listingDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.listingDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开盘价格" align="center" prop="openPrice"/>
<!--      <el-table-column label="最新开盘溢价率(%)" align="center" prop="ldOpenPremium"/>-->
<!--      <el-table-column label="最新收盘涨幅(%)" align="center" prop="ldCloseChange"/>-->
<!--      <el-table-column label="换手率(%)" align="center" prop="turnoverrate"/>-->
<!--      <el-table-column label="最新最高涨幅(%)" align="center" prop="ldHighChang"/>-->
<!--      <el-table-column label="最新平均价格" align="center" prop="ldAveragePrice"/>-->
<!--      <el-table-column label="预测网上顶格申购需配市值(万)" align="center" prop="predictOnfundUpper"/>-->
<!--      <el-table-column label="预测pe值" align="center" prop="predictPeThree"/>-->
<!--      <el-table-column label="行业市盈率" align="center" prop="industryPe"/>-->
<!--      &lt;!&ndash;      <el-table-column label="是否北京企业 0=否 1=是" align="center" prop="isBeijing"/>&ndash;&gt;-->
<!--      <el-table-column label="是否北京企业" align="center" prop="isBeijing">-->
<!--        <template slot-scope="scope">-->
<!--          {{ scope.row.isBeijing === 1 ? '是' : '否' }}-->
<!--        </template>-->
<!--      </el-table-column>-->
<!--      <el-table-column label="是否注册制" align="center" prop="isRegistration">-->
<!--        <template slot-scope="scope">-->
<!--          {{ scope.row.isRegistration === 1 ? '是' : '否' }}-->
<!--        </template>-->
<!--      </el-table-column>-->
<!--      <el-table-column label="总变化量" align="center" prop="totalChange"/>-->
      <el-table-column label="利润" align="center" prop="profit"/>
      <el-table-column label="网上发行中签率" align="center" prop="onlineIssueLwr"/>
      <el-table-column label="预测顶格申购需配市值" align="center" prop="topApplyMarketcap"/>
<!--      <el-table-column label="网上申购上限" align="center" prop="onlineApplyUpper"/>-->
<!--      <el-table-column label="预测申购上限" align="center" prop="predictOnapplyUpper"/>-->
      <el-table-column label="行业新市盈率" align="center" prop="industryPeNew"/>
<!--      <el-table-column label="网下发行对象" align="center" prop="offlineEpObject"/>-->
<!--      <el-table-column label="连续一字涨停天数" align="center" prop="continuous1wordNum"/>-->
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:stock_issue_info:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['stock:stock_issue_info:remove']"
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

    <!-- 添加或修改新股发行信息对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="信息编号" prop="infoCode">
          <el-input v-model="form.infoCode" placeholder="请输入信息编号"/>
        </el-form-item>
        <el-form-item label="证券代码" prop="securityCode">
          <el-input v-model="form.securityCode" placeholder="请输入证券代码"/>
        </el-form-item>
        <el-form-item label="证券名称" prop="securityName">
          <el-input v-model="form.securityName" placeholder="请输入证券名称"/>
        </el-form-item>
        <el-form-item label="交易市场代码" prop="tradeMarketCode">
          <el-input v-model="form.tradeMarketCode" placeholder="请输入交易市场代码"/>
        </el-form-item>
        <el-form-item label="交易市场" prop="tradeMarket">
          <el-input v-model="form.tradeMarket" placeholder="请输入交易市场"/>
        </el-form-item>
        <el-form-item label="主营业务" prop="mainBusiness">
          <el-input v-model="form.mainBusiness" type="textarea" placeholder="请输入内容"/>
        </el-form-item>
        <el-form-item label="发行数量(万股)" prop="issueNum">
          <el-input v-model="form.issueNum" placeholder="请输入发行数量(万股)"/>
        </el-form-item>
        <el-form-item label="网上发行数量" prop="onlineIssueNum">
          <el-input v-model="form.onlineIssueNum" placeholder="请输入网上发行数量"/>
        </el-form-item>
        <el-form-item label="网下配售数量" prop="offlinePlacingNum">
          <el-input v-model="form.offlinePlacingNum" placeholder="请输入网下配售数量"/>
        </el-form-item>
        <el-form-item label="发行价格" prop="issuePrice">
          <el-input v-model="form.issuePrice" placeholder="请输入发行价格"/>
        </el-form-item>
        <el-form-item label="发行后市盈率" prop="afterIssuePe">
          <el-input v-model="form.afterIssuePe" placeholder="请输入发行后市盈率"/>
        </el-form-item>
        <el-form-item label="初始倍数" prop="initialMultiple">
          <el-input v-model="form.initialMultiple" placeholder="请输入初始倍数"/>
        </el-form-item>
        <el-form-item label="最新价格" prop="latelyPrice">
          <el-input v-model="form.latelyPrice" placeholder="请输入最新价格"/>
        </el-form-item>
        <el-form-item label="收盘价格" prop="closePrice">
          <el-input v-model="form.closePrice" placeholder="请输入收盘价格"/>
        </el-form-item>
        <el-form-item label="涨停价格" prop="limitUpPrice">
          <el-input v-model="form.limitUpPrice" placeholder="请输入涨停价格"/>
        </el-form-item>
        <el-form-item label="最新价格" prop="newestPrice">
          <el-input v-model="form.newestPrice" placeholder="请输入最新价格"/>
        </el-form-item>
        <el-form-item label="申购日期" prop="applyDate">
          <el-date-picker clearable
                          v-model="form.applyDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择申购日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="中签号公布日" prop="ballotNumDate">
          <el-date-picker clearable
                          v-model="form.ballotNumDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择中签号公布日">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="中签缴款日" prop="ballotPayDate">
          <el-date-picker clearable
                          v-model="form.ballotPayDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择中签缴款日">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="上市日期" prop="listingDate">
          <el-date-picker clearable
                          v-model="form.listingDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择上市日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="开盘价格" prop="openPrice">
          <el-input v-model="form.openPrice" placeholder="请输入开盘价格"/>
        </el-form-item>
        <el-form-item label="最新开盘溢价率(%)" prop="ldOpenPremium">
          <el-input v-model="form.ldOpenPremium" placeholder="请输入最新开盘溢价率(%)"/>
        </el-form-item>
        <el-form-item label="最新收盘涨幅(%)" prop="ldCloseChange">
          <el-input v-model="form.ldCloseChange" placeholder="请输入最新收盘涨幅(%)"/>
        </el-form-item>
        <el-form-item label="换手率(%)" prop="turnoverrate">
          <el-input v-model="form.turnoverrate" placeholder="请输入换手率(%)"/>
        </el-form-item>
        <el-form-item label="最新最高涨幅(%)" prop="ldHighChang">
          <el-input v-model="form.ldHighChang" placeholder="请输入最新最高涨幅(%)"/>
        </el-form-item>
        <el-form-item label="最新平均价格" prop="ldAveragePrice">
          <el-input v-model="form.ldAveragePrice" placeholder="请输入最新平均价格"/>
        </el-form-item>
        <el-form-item label="预测网上顶格申购需配市值(万)" prop="predictOnfundUpper">
          <el-input v-model="form.predictOnfundUpper" placeholder="请输入预测网上顶格申购需配市值(万)"/>
        </el-form-item>
        <el-form-item label="预测pe值" prop="predictPeThree">
          <el-input v-model="form.predictPeThree" placeholder="请输入预测pe值"/>
        </el-form-item>
        <el-form-item label="行业市盈率" prop="industryPe">
          <el-input v-model="form.industryPe" placeholder="请输入行业市盈率"/>
        </el-form-item>
        <el-form-item label="是否北京企业 0=否 1=是" prop="isBeijing">
          <el-input v-model="form.isBeijing" placeholder="请输入是否北京企业 0=否 1=是"/>
        </el-form-item>
        <el-form-item label="是否注册制" prop="isRegistration">
          <el-input v-model="form.isRegistration" placeholder="请输入是否注册制"/>
        </el-form-item>
        <el-form-item label="总变化量" prop="totalChange">
          <el-input v-model="form.totalChange" placeholder="请输入总变化量"/>
        </el-form-item>
        <el-form-item label="利润" prop="profit">
          <el-input v-model="form.profit" placeholder="请输入利润"/>
        </el-form-item>
        <el-form-item label="网上发行中签率" prop="onlineIssueLwr">
          <el-input v-model="form.onlineIssueLwr" placeholder="请输入网上发行中签率"/>
        </el-form-item>
        <el-form-item label="预测顶格申购需配市值" prop="topApplyMarketcap">
          <el-input v-model="form.topApplyMarketcap" placeholder="请输入预测顶格申购需配市值"/>
        </el-form-item>
        <el-form-item label="网上申购上限" prop="onlineApplyUpper">
          <el-input v-model="form.onlineApplyUpper" placeholder="请输入网上申购上限"/>
        </el-form-item>
        <el-form-item label="预测申购上限" prop="predictOnapplyUpper">
          <el-input v-model="form.predictOnapplyUpper" placeholder="请输入预测申购上限"/>
        </el-form-item>
        <el-form-item label="行业新市盈率" prop="industryPeNew">
          <el-input v-model="form.industryPeNew" placeholder="请输入行业新市盈率"/>
        </el-form-item>
        <el-form-item label="网下发行对象" prop="offlineEpObject">
          <el-input v-model="form.offlineEpObject" placeholder="请输入网下发行对象"/>
        </el-form-item>
        <el-form-item label="连续一字涨停天数" prop="continuous1wordNum">
          <el-input v-model="form.continuous1wordNum" placeholder="请输入连续一字涨停天数"/>
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
  listStock_issue_info,
  getStock_issue_info,
  delStock_issue_info,
  addStock_issue_info,
  updateStock_issue_info
} from "@/api/stock/stock_issue_info"

export default {
  name: "Stock_issue_info",
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
      // 新股发行信息表格数据
      stock_issue_infoList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      hoverRow: null,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        infoCode: null,
        securityCode: null,
        securityName: null,
        tradeMarketCode: null,
        tradeMarket: null,
        marketType: null,
        orgType: null,
        mainBusiness: null,
        issueNum: null,
        onlineIssueNum: null,
        offlinePlacingNum: null,
        issuePrice: null,
        afterIssuePe: null,
        initialMultiple: null,
        latelyPrice: null,
        closePrice: null,
        limitUpPrice: null,
        newestPrice: null,
        applyDate: null,
        ballotNumDate: null,
        ballotPayDate: null,
        listingDate: null,
        openPrice: null,
        ldOpenPremium: null,
        ldCloseChange: null,
        turnoverrate: null,
        ldHighChang: null,
        ldAveragePrice: null,
        predictOnfundUpper: null,
        predictPeThree: null,
        industryPe: null,
        isBeijing: null,
        isRegistration: null,
        totalChange: null,
        profit: null,
        onlineIssueLwr: null,
        topApplyMarketcap: null,
        onlineApplyUpper: null,
        predictOnapplyUpper: null,
        industryPeNew: null,
        offlineEpObject: null,
        continuous1wordNum: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        securityCode: [
          {required: true, message: "证券代码不能为空", trigger: "blur"}
        ],
        securityName: [
          {required: true, message: "证券名称不能为空", trigger: "blur"}
        ],
        tradeMarketCode: [
          {required: true, message: "交易市场代码不能为空", trigger: "blur"}
        ],
        tradeMarket: [
          {required: true, message: "交易市场不能为空", trigger: "blur"}
        ],
        marketType: [
          {required: true, message: "市场类型不能为空", trigger: "change"}
        ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    onRowHover(row) {
      this.hoverRow = row.infoCode;
    },
    onRowLeave() {
      this.hoverRow = null;
    },
    truncateText(text) {
      if (!text) return '';
      return text.length > 10 ? text.slice(0, 10) + '...' : text;
    },
    /** 查询新股发行信息列表 */
    getList() {
      this.loading = true
      listStock_issue_info(this.queryParams).then(response => {
        console.log(response);              // 把整包结果打印出来
        console.log(response.data);         // 很多后端会把真正数据放在 data 里
        console.log(response.rows);         // ruoyi 脚手架默认在 rows 里
        this.stock_issue_infoList = response.rows
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
        applyCode: null,
        infoCode: null,
        securityCode: null,
        securityName: null,
        tradeMarketCode: null,
        tradeMarket: null,
        marketType: null,
        orgType: null,
        mainBusiness: null,
        issueNum: null,
        onlineIssueNum: null,
        offlinePlacingNum: null,
        issuePrice: null,
        afterIssuePe: null,
        initialMultiple: null,
        latelyPrice: null,
        closePrice: null,
        limitUpPrice: null,
        newestPrice: null,
        applyDate: null,
        ballotNumDate: null,
        ballotPayDate: null,
        listingDate: null,
        openPrice: null,
        ldOpenPremium: null,
        ldCloseChange: null,
        turnoverrate: null,
        ldHighChang: null,
        ldAveragePrice: null,
        predictOnfundUpper: null,
        predictPeThree: null,
        industryPe: null,
        isBeijing: null,
        isRegistration: null,
        totalChange: null,
        profit: null,
        onlineIssueLwr: null,
        topApplyMarketcap: null,
        onlineApplyUpper: null,
        predictOnapplyUpper: null,
        industryPeNew: null,
        offlineEpObject: null,
        continuous1wordNum: null,
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
      this.ids = selection.map(item => item.applyCode)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = "添加新股发行信息"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const applyCode = row.applyCode || this.ids
      getStock_issue_info(applyCode).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改新股发行信息"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.applyCode != null) {
            updateStock_issue_info(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addStock_issue_info(this.form).then(response => {
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
      const applyCodes = row.applyCode || this.ids
      this.$modal.confirm('是否确认删除新股发行信息编号为"' + applyCodes + '"的数据项？').then(function () {
        return delStock_issue_info(applyCodes)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    getChineseName(ticker) {
      console.log('ticker:', ticker); // 输出ticker
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
        case ticker.startsWith('600') || ticker.startsWith('601') || ticker.startsWith('603') || ticker.startsWith('605'):
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
      this.download('stock/stock_issue_info/export', {
        ...this.queryParams
      }, `stock_issue_info_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style>
.full-content {
  white-space: normal;
}

.truncated-content {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  display: inline-block;
  max-width: 200px;
}
</style>
