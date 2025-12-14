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
          v-hasPermi="['stock:us:add']"
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
          v-hasPermi="['stock:us:edit']"
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
          v-hasPermi="['stock:us:remove']"
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
          v-hasPermi="['stock:us:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="usList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
      <!--      <el-table-column label="主键ID" align="center" prop="id"/>-->
      <el-table-column label="股票代码" align="center" prop="code"/>
      <el-table-column label="股票名称" align="center" prop="name"/>
      <el-table-column label="最新价" align="center" prop="priceNow"/>
      <el-table-column label="当日最高价" align="center" prop="priceHighDay"/>
      <el-table-column label="当日最低价" align="center" prop="priceLowDay"/>
      <el-table-column label="今日开盘价" align="center" prop="priceOpenDay"/>
      <el-table-column label="昨日收盘价" align="center" prop="priceCloseYesterday"/>
      <el-table-column label="周内最低价" align="center" prop="priceLowWeek"/>
      <el-table-column label="年内最低价" align="center" prop="priceLowYear"/>
      <!-- 对比阈值涨跌幅 -->
      <el-table-column label="对比周内涨跌幅" align="center">
        <template slot-scope="scope">
      <span :style="{ color: getWeekRate(scope.row) >= 0 ? 'red' : 'green' }">
        {{ getWeekRate(scope.row) }}%
      </span>
        </template>
      </el-table-column>
      <!-- 对比阈值涨跌幅 -->
      <el-table-column label="对比年内涨跌幅" align="center">
        <template slot-scope="scope">
      <span :style="{ color: getYearLowRate(scope.row) >= 0 ? 'red' : 'green' }">
        {{ getYearLowRate(scope.row) }}%
      </span>
        </template>
      </el-table-column>
      <el-table-column label="更新时间" align="center" prop="updateTime"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:us:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['stock:us:remove']"
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

    <!-- 添加或修改美股阶段行情信息对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="股票代码" prop="code">
          <el-input v-model="form.code" placeholder="请输入股票代码"/>
        </el-form-item>
        <el-form-item label="股票名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入股票名称"/>
        </el-form-item>
        <el-form-item label="最新价" prop="priceNow">
          <el-input v-model="form.priceNow" placeholder="请输入最新价"/>
        </el-form-item>
        <el-form-item label="当日最高价" prop="priceHighDay">
          <el-input v-model="form.priceHighDay" placeholder="请输入当日最高价"/>
        </el-form-item>
        <el-form-item label="当日最低价" prop="priceLowDay">
          <el-input v-model="form.priceLowDay" placeholder="请输入当日最低价"/>
        </el-form-item>
        <el-form-item label="今日开盘价" prop="priceOpenDay">
          <el-input v-model="form.priceOpenDay" placeholder="请输入今日开盘价"/>
        </el-form-item>
        <el-form-item label="昨日收盘价" prop="priceCloseYesterday">
          <el-input v-model="form.priceCloseYesterday" placeholder="请输入昨日收盘价"/>
        </el-form-item>
        <el-form-item label="周内最低价" prop="priceLowWeek">
          <el-input v-model="form.priceLowWeek" placeholder="请输入周内最低价"/>
        </el-form-item>
        <el-form-item label="年内最低价" prop="priceLowYear">
          <el-input v-model="form.priceLowYear" placeholder="请输入年内最低价"/>
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
import {listUs, getUs, delUs, addUs, updateUs} from "@/api/stock/us"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
export default {
  name: "Us",
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
      // 美股阶段行情信息表格数据
      usList: [],
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
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        code: [
          {required: true, message: "股票代码不能为空", trigger: "blur"}
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
    /** 查询美股阶段行情信息列表 */
    getList() {
      this.loading = true
      listUs(this.queryParams).then(response => {
        this.usList = response.rows
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
        code: null,
        name: null,
        priceNow: null,
        priceHighDay: null,
        priceLowDay: null,
        priceOpenDay: null,
        priceCloseYesterday: null,
        priceLowWeek: null,
        priceLowYear: null,
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
      this.title = "添加美股阶段行情信息"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getUs(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改美股阶段行情信息"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateUs(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addUs(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除美股阶段行情信息编号为"' + ids + '"的数据项？').then(function () {
        return delUs(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/us/export', {
        ...this.queryParams
      }, `us_${new Date().getTime()}.xlsx`)
    },
    /**
     * 计算对比周内涨跌幅（百分比）
     * @param {Object} row 当前行
     * @returns {string} 百分比字符串
     */
    getWeekRate(row) {
      const {priceNow, priceLowWeek} = row;
      if (!priceLowWeek || priceLowWeek === 0) return '-';
      const rate = ((priceNow - priceLowWeek) / priceLowWeek) * 100;
      return rate.toFixed(2);
    },
    /**
     * 对比今年最低价位涨跌幅（百分比）
     * @param {Object} row 当前行
     * @returns {string} 百分比字符串
     */
    getYearLowRate(row) {
      const {priceNow, priceLowYear} = row;
      if (!priceLowYear || priceLowYear === 0) return '-';
      const rate = ((priceNow - priceLowYear) / priceLowYear) * 100;
      return rate.toFixed(2);
    }
  }
}
</script>
