<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="账单月份，格式 YYYY-MM" prop="billMonth">
        <el-input
          v-model="queryParams.billMonth"
          placeholder="请输入账单月份，格式 YYYY-MM"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
          v-hasPermi="['finance:bills:add']"
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
          v-hasPermi="['finance:bills:edit']"
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
          v-hasPermi="['finance:bills:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="warning"
          plain
          icon="el-icon-download"
          size="mini"
          @click="handleExport"
          v-hasPermi="['finance:bills:export']"
        >导出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="billsList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="账单主键 ID" align="center" prop="id" />
      <el-table-column label="所属用户 ID" align="center" prop="userId" />
      <el-table-column label="账单月份" align="center" prop="billMonth" />
      <el-table-column label="当月总支出" align="center" prop="totalAmount" />
      <el-table-column label="动态明细数据" align="center" prop="itemsData" min-width="200">
        <template slot-scope="scope">
          <div v-if="scope.row.itemsData" class="tags-container">
            <el-tag
              v-for="(item, index) in parseItemsData(scope.row.itemsData)"
              :key="index"
              size="small"
              type="info"
              style="margin-right: 5px; margin-bottom: 5px;"
            >
              {{ getIconFromName(item.name) }} {{ item.name }}: {{ item.amount }}
            </el-tag>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['finance:bills:edit']"
          >修改</el-button>
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

    <!-- 添加或修改月度账单 (单JSON架构)对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="账单月份" prop="billMonth">
          <el-date-picker clearable
            v-model="form.billMonth"
            type="month"
            value-format="yyyy-MM"
            placeholder="请选择账单月份">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="当月总支出" prop="totalAmount">
          <el-input v-model="form.totalAmount" placeholder="请输入当月总支出" />
        </el-form-item>
        <el-form-item label="动态明细数据" prop="itemsData">
          <div v-for="(item, index) in dynamicItems" :key="index" style="display: flex; margin-bottom: 10px; align-items: center;">
            <div style="width: 100px; display: flex; align-items: center;">
              <span style="margin-right: 5px;">{{ getIconFromName(item.name) }}</span>
              <el-input v-model="item.name" placeholder="名称" size="small" :disabled="isDefaultItem(item.name)" />
            </div>
            <span style="margin: 0 10px;">-</span>
            <el-input-number v-model="item.amount" :precision="2" :step="10" placeholder="金额" size="small" style="flex: 1;" />
            <el-button v-if="!isDefaultItem(item.name)" type="danger" icon="el-icon-delete" circle size="mini" style="margin-left: 10px;" @click="removeDynamicItem(index)"></el-button>
          </div>
          <el-button type="primary" plain size="small" icon="el-icon-plus" @click="addDynamicItem">添加自定义分类</el-button>
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
import { listBills, getBills, delBills, addBills, updateBills } from "@/api/finance/bills"
import {listUser} from "@/api/stock/dropdown_component";  // 获取用户列表API
export default {
  name: "Bills",
  data() {
    return {
      dynamicItems: [],
      defaultCategories: [
        '房租', '月供', '餐饮', '交通', '购物', '娱乐', '水电煤', '通信',
        '医疗', '教育', '人情', '信用卡', '零食', '服饰', '旅行', '日用'
      ],
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
      // 月度账单 (单JSON架构)表格数据
      billsList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        billMonth: null,
        totalAmount: null,
        itemsData: null,
        createdAt: null,
        updatedAt: null
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        billMonth: [
          { required: true, message: "账单月份，格式 YYYY-MM不能为空", trigger: "blur" }
        ],
        itemsData: [
          { required: false, message: "动态明细数据", trigger: "blur" }
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
    /** 查询月度账单 (单JSON架构)列表 */
    getList() {
      this.loading = true
      listBills(this.queryParams).then(response => {
        this.billsList = response.rows
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
        userId: null,
        billMonth: null,
        totalAmount: null,
        itemsData: null,
        createdAt: null,
        updatedAt: null
      }
      this.resetForm("form")
    },
    getIconFromName(name) {
      const iconMap = {
        '房租': '🏠',
        '月供': '🏦',
        '餐饮': '🍔',
        '交通': '🚗',
        '购物': '🛍️',
        '娱乐': '🎮',
        '水电煤': '⚡',
        '通信': '📱',
        '医疗': '🏥',
        '教育': '📚',
        '人情': '🤝',
        '信用卡': '💳',
        '零食': '🍩',
        '服饰': '👕',
        '旅行': '✈️',
        '日用': '🧼'
      };
      return iconMap[name] || '📌';
    },
    parseItemsData(dataStr) {
      if (!dataStr) return [];
      try {
        return JSON.parse(dataStr);
      } catch (e) {
        return [];
      }
    },
    isDefaultItem(name) {
      return this.defaultCategories.includes(name);
    },
    initDynamicItems(existingDataStr = null) {
      // Initialize with default categories
      let itemsMap = {};
      this.defaultCategories.forEach(cat => {
        itemsMap[cat] = { name: cat, amount: 0 };
      });

      if (existingDataStr) {
        try {
          const parsed = JSON.parse(existingDataStr);
          if (Array.isArray(parsed)) {
            parsed.forEach(item => {
              if (item.name) {
                itemsMap[item.name] = { name: item.name, amount: item.amount || 0 };
              }
            });
          }
        } catch (e) {
          console.error("Failed to parse itemsData:", e);
        }
      }

      // Convert map back to array, ensuring default categories come first
      let result = [];
      this.defaultCategories.forEach(cat => {
        result.push(itemsMap[cat]);
        delete itemsMap[cat];
      });
      // Add any remaining custom categories
      Object.values(itemsMap).forEach(item => {
        result.push(item);
      });
      this.dynamicItems = result;
    },
    addDynamicItem() {
      this.dynamicItems.push({ name: '', amount: 0 });
    },
    removeDynamicItem(index) {
      this.dynamicItems.splice(index, 1);
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
      this.initDynamicItems()
      this.open = true
      this.title = "添加月度账单 (单JSON架构)"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getBills(id).then(response => {
        this.form = response.data
        this.initDynamicItems(this.form.itemsData)
        this.open = true
        this.title = "修改月度账单 (单JSON架构)"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // Prepare dynamicItems for saving
          const itemsToSave = this.dynamicItems
            .filter(item => item.name && item.name.trim() !== '')
            .map(item => ({
              name: item.name,
              amount: item.amount || 0
            }));
          this.form.itemsData = JSON.stringify(itemsToSave);

          if (this.form.id != null) {
            updateBills(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addBills(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除月度账单 (单JSON架构)编号为"' + ids + '"的数据项？').then(function() {
        return delBills(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {})
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('finance/bills/export', {
        ...this.queryParams
      }, `bills_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
