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
          v-hasPermi="['stock:notice:add']"
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
          v-hasPermi="['stock:notice:edit']"
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
          v-hasPermi="['stock:notice:remove']"
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
          v-hasPermi="['stock:notice:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="noticeList" @selection-change="handleSelectionChange" :stripe="true" :border="false">
      <el-table-column type="selection" width="55" align="center"/>
<!--      <el-table-column label="主键ID" align="center" prop="id"/>-->
      <el-table-column label="证券代码" align="center" prop="securityCode">
        <template slot-scope="scope">
          <el-tag size="mini">{{ scope.row.securityCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="证券名称" align="center" prop="securityName" style="font-weight: bold;"/>
      <el-table-column label="发行价格" align="right" prop="issuePrice">
        <template slot-scope="scope">
          <span>{{ scope.row.issuePrice }}</span>
        </template>
      </el-table-column>
      <el-table-column label="上市日期" align="center" prop="listingDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.listingDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前价格" align="right" prop="currentPrice">
        <template slot-scope="scope">
          <span style="color: #E6A23C; font-weight: bold;">{{ scope.row.currentPrice }}</span>
        </template>
      </el-table-column>
      <el-table-column label="每股净利润" align="right" prop="netProfit"/>
      <el-table-column label="利润率" align="right" prop="profitMargin"/>
      <el-table-column label="已通知次数" align="center" prop="notifyCount"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['stock:notice:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['stock:notice:remove']"
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

    <!-- 添加或修改证券上市通知对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="证券代码" prop="securityCode">
          <el-input v-model="form.securityCode" placeholder="请输入证券代码"/>
        </el-form-item>
        <el-form-item label="证券名称" prop="securityName">
          <el-input v-model="form.securityName" placeholder="请输入证券名称"/>
        </el-form-item>
        <el-form-item label="发行价格" prop="issuePrice">
          <el-input v-model="form.issuePrice" placeholder="请输入发行价格"/>
        </el-form-item>
        <el-form-item label="上市日期" prop="listingDate">
          <el-date-picker clearable
                          v-model="form.listingDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择上市日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="当前价格" prop="currentPrice">
          <el-input v-model="form.currentPrice" placeholder="请输入当前价格"/>
        </el-form-item>
        <el-form-item label="每股净利润" prop="netProfit">
          <el-input v-model="form.netProfit" placeholder="请输入每股净利润"/>
        </el-form-item>
        <el-form-item label="利润率" prop="profitMargin">
          <el-input v-model="form.profitMargin" placeholder="请输入利润率"/>
        </el-form-item>
        <el-form-item label="已通知次数" prop="notifyCount">
          <el-input v-model="form.notifyCount" placeholder="请输入已通知次数"/>
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
import {listNotice, getNotice, delNotice, addNotice, updateNotice} from "@/api/stock/notice"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
export default {
  name: "Notice",
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
      // 证券上市通知表格数据
      noticeList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        securityCode: null,
        securityName: null,
        issuePrice: null,
        listingDate: null,
        currentPrice: null,
        netProfit: null,
        profitMargin: null,
        notifyCount: null,
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
    /** 查询证券上市通知列表 */
    getList() {
      this.loading = true
      listNotice(this.queryParams).then(response => {
        this.noticeList = response.rows
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
        securityCode: null,
        securityName: null,
        issuePrice: null,
        listingDate: null,
        currentPrice: null,
        netProfit: null,
        profitMargin: null,
        notifyCount: null,
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
      this.title = "添加证券上市通知"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getNotice(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改证券上市通知"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateNotice(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addNotice(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除证券上市通知编号为"' + ids + '"的数据项？').then(function () {
        return delNotice(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('stock/notice/export', {
        ...this.queryParams
      }, `notice_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
