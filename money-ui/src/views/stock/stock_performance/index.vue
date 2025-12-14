<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="股票代码" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="请输入股票代码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="股票名称" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="请输入股票名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="涨跌幅" prop="yearLowPriceRate">
        <el-input
          v-model="queryParams.yearLowPriceRate"
          placeholder="请输入当年涨跌幅"
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
          v-hasPermi="['stock:stock_performance:add']"
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
          v-hasPermi="['stock:stock_performance:edit']"
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
          v-hasPermi="['stock:stock_performance:remove']"
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
          v-hasPermi="['stock:stock_performance:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="stock_performanceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <el-table-column label="主键ID" align="center" prop="id"/>
      <el-table-column label="日期" align="center" prop="date" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.date, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="股票代码" align="center" prop="code"/>
      <el-table-column label="股票名称" align="center" prop="name"/>
      <el-table-column label="最新价格" align="center" prop="newPrice"/>
      <el-table-column label="昨收价格" align="center" prop="previousClose"/>
      <el-table-column label="当年最低价" align="center" prop="yearLowPrice"/>
      <el-table-column label="去年最低价" align="center" prop="thresholdPrice"/>
      <!-- 对比昨日涨跌幅 -->
      <el-table-column label="对比昨日涨跌幅" align="center">
        <template slot-scope="scope">
      <span :style="{ color: getChangeRate(scope.row) >= 0 ? 'red' : 'green' }">
        {{ getChangeRate(scope.row) }}%
      </span>
        </template>
      </el-table-column>
      <!-- 对比阈值涨跌幅 -->
      <el-table-column label="对比去年涨跌幅" align="center">
        <template slot-scope="scope">
      <span :style="{ color: getThresholdRate(scope.row) >= 0 ? 'red' : 'green' }">
        {{ getThresholdRate(scope.row) }}%
      </span>
        </template>
      </el-table-column>
      <!-- 对比周内最高涨跌幅 -->
      <el-table-column label="对比周内最高涨跌幅" align="center">
        <template slot-scope="scope">
          <span :style="{ color: getWeekHighRate(scope.row) >= 0 ? 'red' : 'green' }">
            {{ getWeekHighRate(scope.row) }}%
          </span>
        </template>
      </el-table-column>
      <!-- 对比年内最高涨跌幅 -->
      <el-table-column label="对比年内最高涨跌幅" align="center">
        <template slot-scope="scope">
          <span :style="{ color: getYearHighRate(scope.row) >= 0 ? 'red' : 'green' }">
            {{ getYearHighRate(scope.row) }}%
          </span>
        </template>
      </el-table-column>
      <!-- 对比去年最高涨跌幅 -->
      <el-table-column label="对比去年最高涨跌幅" align="center">
        <template slot-scope="scope">
          <span :style="{ color: getLastYearHighRate(scope.row) >= 0 ? 'red' : 'green' }">
            {{ getLastYearHighRate(scope.row) }}%
          </span>
        </template>
      </el-table-column>
      <!-- 当年涨跌幅 -->
      <el-table-column label="全年涨跌幅" align="center">
        <template slot-scope="scope">
    <span :style="{ color: scope.row.yearLowPriceRate >= 0 ? 'red' : 'green' }">
      {{ formatRate(scope.row.yearLowPriceRate) }}
    </span>
        </template>
      </el-table-column>

      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:stock_performance:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['stock:stock_performance:remove']"
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

    <!-- 添加或修改股票当年现数据对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="股票代码" prop="code">
          <el-input v-model="form.code" placeholder="请输入股票代码"/>
        </el-form-item>
        <el-form-item label="股票名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入股票名称"/>
        </el-form-item>
        <el-form-item label="最新价格" prop="newPrice">
          <el-input v-model="form.newPrice" placeholder="请输入最新价格"/>
        </el-form-item>
        <el-form-item label="昨收价格" prop="previousClose">
          <el-input v-model="form.previousClose" placeholder="请输入昨收价格"/>
        </el-form-item>
        <el-form-item label="当年最低价" prop="yearLowPrice">
          <el-input v-model="form.yearLowPrice" placeholder="请输入当年最低价"/>
        </el-form-item>
        <el-form-item label="当年涨跌幅" prop="yearLowPriceRate">
          <el-input v-model="form.yearLowPriceRate" placeholder="请输入当年涨跌幅"/>
        </el-form-item>
        <el-form-item label="阈值价格" prop="thresholdPrice">
          <el-input v-model="form.thresholdPrice" placeholder="请输入阈值价格"/>
        </el-form-item>
        <el-form-item label="周内最高价" prop="weekHigh">
          <el-input v-model="form.weekHigh" placeholder="请输入周内最高价"/>
        </el-form-item>
        <el-form-item label="年内最高价" prop="yearHigh">
          <el-input v-model="form.yearHigh" placeholder="请输入年内最高价"/>
        </el-form-item>
        <el-form-item label="去年最高价" prop="lastYearHigh">
          <el-input v-model="form.lastYearHigh" placeholder="请输入去年最高价"/>
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
  listStock_performance,
  getStock_performance,
  delStock_performance,
  addStock_performance,
  updateStock_performance
} from "@/api/stock/stock_performance"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
export default {
  name: "Stock_performance",
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
      // 股票当年现数据表格数据
      stock_performanceList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        code: null,
        name: null,
        newPrice: null,
        previousClose: null,
        yearLowPrice: null,
        yearLowPriceRate: null,
        thresholdPrice: null,
        weekHigh: null,
        yearHigh: null,
        lastYearHigh: null,
        updateTime: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        code: [
          {required: true, message: "股票代码不能为空", trigger: "blur"}
        ],
        name: [
          {required: true, message: "股票名称不能为空", trigger: "blur"}
        ],
        newPrice: [
          {required: true, message: "最新价格不能为空", trigger: "blur"}
        ],
        previousClose: [
          {required: true, message: "昨收价格不能为空", trigger: "blur"}
        ],
        yearLowPrice: [
          {required: true, message: "当年最低价不能为空", trigger: "blur"}
        ],
        yearLowPriceRate: [
          {required: true, message: "当年涨跌幅不能为空", trigger: "blur"}
        ],
        thresholdPrice: [
          {required: true, message: "阈值价格不能为空", trigger: "blur"}
        ],
        weekHigh: [
          {required: true, message: "周内最高价不能为空", trigger: "blur"}
        ],
        yearHigh: [
          {required: true, message: "年内最高价不能为空", trigger: "blur"}
        ],
        lastYearHigh: [
          {required: true, message: "去年最高价不能为空", trigger: "blur"}
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
    /** 查询股票当年现数据列表 */
    getList() {
      this.loading = true
      listStock_performance(this.queryParams).then(response => {
        this.stock_performanceList = response.rows
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
        date: null,
        code: null,
        name: null,
        newPrice: null,
        previousClose: null,
        yearLowPrice: null,
        yearLowPriceRate: null,
        thresholdPrice: null,
        weekHigh: null,
        yearHigh: null,
        lastYearHigh: null,
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
      this.title = "添加股票当年现数据"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getStock_performance(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改股票当年现数据"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateStock_performance(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addStock_performance(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除股票当年现数据编号为"' + ids + '"的数据项？').then(function () {
        return delStock_performance(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/stock_performance/export', {
        ...this.queryParams
      }, `stock_performance_${new Date().getTime()}.xlsx`)
    },
    /**
     * 计算对比昨日涨跌幅（百分比）
     * @param {Object} row 当前行
     * @returns {string} 百分比字符串
     */
    getChangeRate(row) {
      const {newPrice, previousClose} = row;
      if (!previousClose || previousClose === 0) return '-';
      const rate = ((newPrice - previousClose) / previousClose) * 100;
      return rate.toFixed(2);
    },

    /**
     * 计算对比阈值涨跌幅（百分比）
     * @param {Object} row 当前行
     * @returns {string} 百分比字符串
     */
    getThresholdRate(row) {
      const {newPrice, thresholdPrice} = row;
      if (!thresholdPrice || thresholdPrice === 0) return '-';
      const rate = ((newPrice - thresholdPrice) / thresholdPrice) * 100;
      return rate.toFixed(2);
    },
    /**
     * 计算对比周内最高涨跌幅（百分比）
     * @param {Object} row 当前行
     * @returns {string} 百分比字符串
     */
    getWeekHighRate(row) {
      const {newPrice, weekHigh} = row;
      if (!weekHigh || weekHigh === 0) return '-';
      const rate = ((newPrice - weekHigh) / weekHigh) * 100;
      return rate.toFixed(2);
    },
    /**
     * 计算对比年内最高涨跌幅（百分比）
     * @param {Object} row 当前行
     * @returns {string} 百分比字符串
     */
    getYearHighRate(row) {
      const {newPrice, yearHigh} = row;
      if (!yearHigh || yearHigh === 0) return '-';
      const rate = ((newPrice - yearHigh) / yearHigh) * 100;
      return rate.toFixed(2);
    },
    /**
     * 计算对比去年最高涨跌幅（百分比）
     * @param {Object} row 当前行
     * @returns {string} 百分比字符串
     */
    getLastYearHighRate(row) {
      const {newPrice, lastYearHigh} = row;
      if (!lastYearHigh || lastYearHigh === 0) return '-';
      const rate = ((newPrice - lastYearHigh) / lastYearHigh) * 100;
      return rate.toFixed(2);
    },
    /**
     * 格式化涨跌幅，添加正负号与百分号
     * @param {Number|String} rate 涨跌幅
     * @returns {String}
     */
    formatRate(rate) {
      const num = Number(rate);
      if (isNaN(num)) return '-';
      const formatted = num > 0 ? `+${num.toFixed(2)}%` : `${num.toFixed(2)}%`;
      return formatted;
    }
  }
}
</script>
