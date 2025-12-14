<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="服务器名称" prop="serverName">
        <el-input
          v-model="queryParams.serverName"
          placeholder="请输入服务器名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="服务器IP地址" prop="ipAddress">
        <el-input
          v-model="queryParams.ipAddress"
          placeholder="请输入服务器IP地址"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="服务提供商" prop="provider">
        <el-input
          v-model="queryParams.provider"
          placeholder="请输入服务提供商"
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
          v-hasPermi="['finance:serverInfo:add']"
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
          v-hasPermi="['finance:serverInfo:edit']"
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
          v-hasPermi="['finance:serverInfo:remove']"
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
          v-hasPermi="['finance:serverInfo:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="serverInfoList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
<!--      <el-table-column label="主键ID" align="center" prop="id"/>-->
      <el-table-column label="服务器名称" align="center" prop="serverName"/>
      <el-table-column label="服务器公网IP地址" align="center" prop="ipAddress"/>
      <el-table-column label="服务提供商" align="center" prop="provider"/>
<!--      <el-table-column label="操作系统类型" align="center" prop="osType"/>-->
      <el-table-column label="购买日期" align="center" prop="purchaseDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.purchaseDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="到期日期" align="center" prop="expireDate" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.expireDate, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="距离到期日还有（天）" align="center" prop="remindDays"/>
      <el-table-column label="购买或续费价格" align="center" prop="price"/>
      <el-table-column label="状态" align="center" prop="status">
        <template slot-scope="scope">
          <dict-tag :options="dict.type.server_status" :value="scope.row.status"/>
        </template>
      </el-table-column>
      <el-table-column label="负责人姓名" align="center" prop="adminUser"/>
<!--      <el-table-column label="负责人邮箱" align="center" prop="contactEmail"/>-->
<!--      <el-table-column label="是否已发送到期提醒" align="center" prop="notifySent"/>-->
      <el-table-column label="备注信息" align="center" prop="remark"/>
      <el-table-column label="更新时间" align="center" prop="updateTime" width="180">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.updateTime, '{y}-{m}-{d}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['finance:serverInfo:edit']"
          >修改
          </el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['finance:serverInfo:remove']"
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

    <!-- 添加或修改服务器有效期管理（MySQL5.7兼容版）对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="服务器名称" prop="serverName">
          <el-input v-model="form.serverName" placeholder="请输入服务器名称"/>
        </el-form-item>
        <el-form-item label="服务器IP地址" prop="ipAddress">
          <el-input v-model="form.ipAddress" placeholder="请输入服务器IP地址"/>
        </el-form-item>
        <el-form-item label="服务提供商" prop="provider">
          <el-input v-model="form.provider" placeholder="请输入服务提供商"/>
        </el-form-item>
        <el-form-item label="购买日期" prop="purchaseDate">
          <el-date-picker clearable
                          v-model="form.purchaseDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择购买日期">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="到期日期" prop="expireDate">
          <el-date-picker clearable
                          v-model="form.expireDate"
                          type="date"
                          value-format="yyyy-MM-dd"
                          placeholder="请选择到期日期">
          </el-date-picker>
        </el-form-item>
<!--        <el-form-item label="到期前提醒天数" prop="remindDays">-->
<!--          <el-input v-model="form.remindDays" placeholder="请输入到期前提醒天数"/>-->
<!--        </el-form-item>-->
        <el-form-item label="购买或续费价格" prop="price">
          <el-input v-model="form.price" placeholder="请输入购买或续费价格"/>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio
              v-for="dict in dict.type.server_status"
              :key="dict.value"
              :label="dict.value"
            >{{ dict.label }}
            </el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="负责人姓名" prop="adminUser">
          <el-input v-model="form.adminUser" placeholder="请输入负责人姓名"/>
        </el-form-item>
<!--        <el-form-item label="负责人邮箱" prop="contactEmail">-->
<!--          <el-input v-model="form.contactEmail" placeholder="请输入负责人邮箱"/>-->
<!--        </el-form-item>-->
<!--        <el-form-item label="是否已发送到期提醒" prop="notifySent">-->
<!--          <el-input v-model="form.notifySent" placeholder="请输入是否已发送到期提醒"/>-->
<!--        </el-form-item>-->
        <el-form-item label="备注信息" prop="remark">
          <el-input v-model="form.remark" type="textarea" placeholder="请输入内容"/>
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
import {listServerInfo, getServerInfo, delServerInfo, addServerInfo, updateServerInfo} from "@/api/finance/serverInfo"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
export default {
  name: "ServerInfo",
  dicts: ['server_status'],
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
      // 服务器有效期管理（MySQL5.7兼容版）表格数据
      serverInfoList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        serverName: null,
        ipAddress: null,
        provider: null,
        purchaseDate: null,
        expireDate: null,
        adminUser: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        serverName: [
          {required: true, message: "服务器名称不能为空", trigger: "blur"}
        ],
        ipAddress: [
          {required: true, message: "服务器IP地址不能为空", trigger: "blur"}
        ],
        expireDate: [
          {required: true, message: "到期日期不能为空", trigger: "blur"}
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
    /** 查询服务器有效期管理（MySQL5.7兼容版）列表 */
    getList() {
      this.loading = true
      listServerInfo(this.queryParams).then(response => {
        this.serverInfoList = response.rows
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
        serverName: null,
        ipAddress: null,
        provider: null,
        osType: null,
        purchaseDate: null,
        expireDate: null,
        remindDays: null,
        price: null,
        status: null,
        adminUser: null,
        contactEmail: null,
        notifySent: null,
        remark: null,
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
      this.title = "添加服务器管理"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getServerInfo(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改服务器管理"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateServerInfo(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addServerInfo(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除服务器管理编号为"' + ids + '"的数据项？').then(function () {
        return delServerInfo(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/serverInfo/export', {
        ...this.queryParams
      }, `serverInfo_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
