<template>
  <div class="app-container">
    <el-row :gutter="20" class="mb8">
      <el-col :span="6">
        <el-card>
          <div slot="header">
            <span>本年度最高价 vs 去年度最高价</span>
          </div>
          <div ref="chartHighVsHigh" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div slot="header">
            <span>本年度最低价 vs 去年度最低价</span>
          </div>
          <div ref="chartLowVsLow" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div slot="header">
            <span>本年度最新价 vs 去年度最高价</span>
          </div>
          <div ref="chartLatestVsHigh" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card>
          <div slot="header">
            <span>本年度最新价 vs 去年度最低价</span>
          </div>
          <div ref="chartLatestVsLow" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="股票代码" prop="stockCode">
        <el-input
          v-model="queryParams.stockCode"
          placeholder="请输入股票代码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="市场标识" prop="market">
        <el-select v-model="queryParams.market" placeholder="请选择市场标识，如 SH、SZ" clearable>
          <el-option
            v-for="dict in dict.type.market_identity"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="交易日期" prop="tradeDate">
        <el-date-picker clearable
                        v-model="queryParams.tradeDate"
                        type="date"
                        value-format="yyyy-MM-dd"
                        placeholder="请选择交易日期">
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
          v-hasPermi="['stock:kline:add']"
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
          v-hasPermi="['stock:kline:edit']"
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
          v-hasPermi="['stock:kline:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['stock:kline:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="klineList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="主键ID" align="center" prop="id" />
      <el-table-column label="股票代码" align="center" prop="stockCode" />
      <el-table-column label="市场标识" align="center" prop="market">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.market_identity" :value="scope.row.market"/>
        </template>
      </el-table-column>
      <el-table-column label="交易日期" align="center" prop="tradeDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.tradeDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="开盘价" align="center" prop="open" />
      <el-table-column label="收盘价" align="center" prop="close" />
      <el-table-column label="最高价" align="center" prop="high" />
      <el-table-column label="最低价" align="center" prop="low" />
      <el-table-column label="成交量" align="center" prop="volume" />
      <el-table-column label="成交额" align="center" prop="amount" />
      <el-table-column label="涨跌额" align="center" prop="change" />
      <el-table-column label="涨跌幅(%)" align="center" prop="changePercent" />
      <el-table-column label="换手率(%)" align="center" prop="turnoverRatio" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:kline:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['stock:kline:remove']"
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

    <!-- 添加或修改股票K线数据对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="股票代码" prop="stockCode">
          <el-input v-model="form.stockCode" placeholder="请输入股票代码，例如 600519" />
        </el-form-item>
        <el-form-item label="市场标识" prop="market">
          <el-select v-model="form.market" placeholder="请选择市场标识，如 SH、SZ">
            <el-option
              v-for="dict in dict.type.market_identity"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            ></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="交易日期" prop="tradeDate">
          <el-date-picker clearable
                          v-model="form.tradeDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择交易日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="开盘价" prop="open">
          <el-input v-model="form.open" placeholder="请输入开盘价" />
        </el-form-item>
        <el-form-item label="收盘价" prop="close">
          <el-input v-model="form.close" placeholder="请输入收盘价" />
        </el-form-item>
        <el-form-item label="最高价" prop="high">
          <el-input v-model="form.high" placeholder="请输入最高价" />
        </el-form-item>
        <el-form-item label="最低价" prop="low">
          <el-input v-model="form.low" placeholder="请输入最低价" />
        </el-form-item>
        <el-form-item label="成交量" prop="volume">
          <el-input v-model="form.volume" placeholder="请输入成交量" />
        </el-form-item>
        <el-form-item label="成交额" prop="amount">
          <el-input v-model="form.amount" placeholder="请输入成交额" />
        </el-form-item>
        <el-form-item label="涨跌额" prop="change">
          <el-input v-model="form.change" placeholder="请输入涨跌额" />
        </el-form-item>
        <el-form-item label="涨跌幅(%)" prop="changePercent">
          <el-input v-model="form.changePercent" placeholder="请输入涨跌幅(%)" />
        </el-form-item>
        <el-form-item label="换手率(%)" prop="turnoverRatio">
          <el-input v-model="form.turnoverRatio" placeholder="请输入换手率(%)" />
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
import { listKline, getKline, delKline, addKline, updateKline, getRankingStats } from "@/api/stock/kline"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
import * as echarts from 'echarts'

export default {
  name: "Kline",
  dicts: ['market_identity'],
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
      // 股票K线数据表格数据
      klineList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        stockCode: null,
        market: null,
        tradeDate: null,
        open: null,
        close: null,
        high: null,
        low: null,
        volume: null,
        amount: null,
        change: null,
        changePercent: null,
        turnoverRatio: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        stockCode: [
          { required: true, message: "股票代码，例如 600519不能为空", trigger: "blur" }
        ],
        tradeDate: [
          { required: true, message: "交易日期不能为空", trigger: "blur" }
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
  mounted() {
    this.initCharts();
  },
  methods: {
    initCharts() {
      this.initRankingChart('HIGH_VS_HIGH', 'chartHighVsHigh', '本年度最高价', '去年度最高价');
      this.initRankingChart('LOW_VS_LOW', 'chartLowVsLow', '本年度最低价', '去年度最低价');
      this.initRankingChart('LATEST_VS_HIGH', 'chartLatestVsHigh', '本年度最新价', '去年度最高价');
      this.initRankingChart('LATEST_VS_LOW', 'chartLatestVsLow', '本年度最新价', '去年度最低价');
    },
    initRankingChart(type, refName, labelCurrent, labelPrev) {
      getRankingStats(type).then(response => {
        const data = response.data;
        const xAxisData = data.map(item => `${item.stockName || ''} (${item.stockCode})`);
        const currentValues = data.map(item => item.currentValue);
        const prevValues = data.map(item => item.prevValue);
        // Calculate percentage change for each item
        const percentageChanges = data.map(item => {
           if (item.prevValue && item.prevValue !== 0) {
             return ((item.currentValue - item.prevValue) / item.prevValue * 100).toFixed(2) + '%';
           }
           return '-';
        });

        const chart = echarts.init(this.$refs[refName]);
        const option = {
          tooltip: {
            trigger: 'axis',
            axisPointer: { type: 'shadow' }
          },
          legend: {
            data: [labelCurrent, labelPrev]
          },
          grid: {
            left: '3%',
            right: '4%',
            bottom: '3%',
            containLabel: true
          },
          xAxis: {
            type: 'category',
            data: xAxisData,
            axisLabel: { interval: 0, rotate: 30 }
          },
          yAxis: {
            type: 'value'
          },
          series: [
            {
              name: labelCurrent,
              type: 'bar',
              data: currentValues,
              label: {
                show: true,
                position: 'top',
                formatter: function (params) {
                  return percentageChanges[params.dataIndex];
                }
              }
            },
            {
              name: labelPrev,
              type: 'bar',
              data: prevValues
            }
          ]
        };
        chart.setOption(option);
      });
    },
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
    /** 查询股票K线数据列表 */
    getList() {
      this.loading = true
      listKline(this.queryParams).then(response => {
        this.klineList = response.rows
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
        market: null,
        tradeDate: null,
        open: null,
        close: null,
        high: null,
        low: null,
        volume: null,
        amount: null,
        change: null,
        changePercent: null,
        turnoverRatio: null,
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
      this.title = "添加股票K线数据"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getKline(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改股票K线数据"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateKline(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addKline(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除股票K线数据编号为"' + ids + '"的数据项？').then(function() {
        return delKline(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/kline/export', {
        ...this.queryParams
      }, `kline_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
