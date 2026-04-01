<template>
  <div class="app-container">
    <el-card class="search-card" shadow="never" v-show="showSearch">
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
        <el-form-item label="账单月份" prop="billMonth">
          <el-date-picker
            v-model="queryParams.billMonth"
            type="month"
            value-format="yyyy-MM"
            placeholder="请选择账单月份"
            clearable
            @keyup.enter.native="handleQuery"
          ></el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd"
                     v-hasPermi="['finance:bills:add']">新增
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="success" plain icon="el-icon-edit" size="mini" :disabled="single" @click="handleUpdate"
                     v-hasPermi="['finance:bills:edit']">修改
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete"
                     v-hasPermi="['finance:bills:remove']">删除
          </el-button>
        </el-col>
        <el-col :span="1.5">
          <el-button type="warning" plain icon="el-icon-download" size="mini" @click="handleExport"
                     v-hasPermi="['finance:bills:export']">导出
          </el-button>
        </el-col>
        <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
      </el-row>

      <el-table
        v-loading="loading"
        :data="billsList"
        @selection-change="handleSelectionChange"
        stripe
        border
        highlight-current-row
      >
        <el-table-column type="selection" width="55" align="center"/>
        <el-table-column label="账单 ID" align="center" prop="id" width="80"/>
        <el-table-column label="账单月份" align="center" prop="billMonth" width="120">
          <template slot-scope="scope">
            <el-tag type="primary" plain>{{ scope.row.billMonth }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当月总支出" align="center" prop="totalAmount" width="150">
          <template slot-scope="scope">
            <span class="total-amount">¥ {{ scope.row.totalAmount }}</span>
          </template>
        </el-table-column>

        <el-table-column label="支出明细" align="left" prop="itemsData" min-width="350">
          <template slot-scope="scope">
            <div class="tags-container">
              <template v-if="parseItemsData(scope.row.itemsData).length > 0">
                <el-tag
                  v-for="(item, index) in parseItemsData(scope.row.itemsData)"
                  :key="index"
                  size="medium"
                  :type="getTagType(index)"
                  class="bill-tag"
                  effect="light"
                >
                  <span class="tag-icon">{{ getIconFromName(item.name) }}</span>
                  <span class="tag-name">{{ item.name }}</span>
                  <span class="tag-divider">|</span>
                  <span class="tag-amount">¥{{ item.amount }}</span>
                </el-tag>
              </template>
              <span v-else class="no-data-text">暂无支出明细</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="操作" align="center" width="120" class-name="small-padding fixed-width">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)"
                       v-hasPermi="['finance:bills:edit']">修改
            </el-button>
            <el-button size="mini" type="text" icon="el-icon-delete" class="text-danger"
                       @click="handleDelete(scope.row)" v-hasPermi="['finance:bills:remove']">删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination v-show="total>0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
                  @pagination="getList"/>
    </el-card>

    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body custom-class="bill-dialog">
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <div class="dialog-header-info">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="账单月份" prop="billMonth">
                <el-date-picker
                  clearable
                  style="width: 100%;"
                  v-model="form.billMonth"
                  type="month"
                  value-format="yyyy-MM"
                  placeholder="请选择账单月份">
                </el-date-picker>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="当月总支出" prop="totalAmount">
                <el-input v-model="form.totalAmount" placeholder="系统自动求和" disabled>
                  <template slot="prepend">¥</template>
                </el-input>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <el-divider content-position="left">支出明细清单</el-divider>

        <el-form-item label-width="0" prop="itemsData">
          <div class="dynamic-items-container">
            <el-row :gutter="10" v-for="(item, index) in dynamicItems" :key="index" class="dynamic-item-row">
              <el-col :span="9">
                <el-input v-model="item.name" placeholder="分类名称" size="small" :disabled="isDefaultItem(item.name)">
                  <template slot="prepend"><span class="form-icon">{{ getIconFromName(item.name) }}</span></template>
                </el-input>
              </el-col>
              <el-col :span="11">
                <el-input-number
                  v-model="item.amount"
                  :precision="2"
                  :step="50"
                  :min="0"
                  placeholder="金额"
                  size="small"
                  style="width: 100%;">
                </el-input-number>
              </el-col>
              <el-col :span="4" class="text-right">
                <el-button
                  v-if="!isDefaultItem(item.name)"
                  type="danger"
                  plain
                  icon="el-icon-delete"
                  circle
                  size="mini"
                  @click="removeDynamicItem(index)">
                </el-button>
              </el-col>
            </el-row>
          </div>
          <div class="add-btn-container">
            <el-button type="primary" plain size="small" icon="el-icon-plus" @click="addDynamicItem"
                       style="width: 100%;">添加自定义分类
            </el-button>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">保 存</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {listBills, getBills, delBills, addBills, updateBills} from "@/api/finance/bills"
import {listUser} from "@/api/stock/dropdown_component";

export default {
  name: "Bills",
  data() {
    return {
      dynamicItems: [],
      defaultCategories: [
        '房租', '月供', '餐饮', '交通', '购物', '娱乐', '水电煤', '通信',
        '医疗', '教育', '人情', '信用卡', '零食', '服饰', '旅行', '日用'
      ],
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      billsList: [],
      title: "",
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        userId: null,
        billMonth: null,
      },
      form: {},
      rules: {
        billMonth: [
          {required: true, message: "账单月份不能为空", trigger: "change"}
        ]
      }
    }
  },
  watch: {
    // 监听动态明细变动，自动计算总金额
    dynamicItems: {
      handler(newItems) {
        let total = 0;
        newItems.forEach(item => {
          if (item.amount && !isNaN(item.amount)) {
            total += parseFloat(item.amount);
          }
        });
        this.$set(this.form, 'totalAmount', parseFloat(total.toFixed(2)));
      },
      deep: true
    }
  },
  async created() {
    await this.initUserList();
    this.getList();
  },
  methods: {
    async initUserList() {
      try {
        const response = await listUser({pageSize: this.queryParams.pageSize});
        const payload = response.data || response;
        const rawUsers = Array.isArray(payload.rows) ? payload.rows : Array.isArray(payload) ? payload : [];

        const userList = rawUsers.map(u => ({
          id: u.userId,
          name: u.userName || u.nickName || `用户${u.userId}`
        }));

        if (userList.length) {
          const savedUsername = this.$cookies.get('username');
          const matchedUser = userList.find(u => u.name === savedUsername);
          if (matchedUser) {
            this.queryParams.userId = matchedUser.id;
            this.$set(this.form, 'userId', matchedUser.id);
          }
        }
      } catch (err) {
        console.error('用户列表加载失败:', err);
      }
    },
    getList() {
      this.loading = true
      listBills(this.queryParams).then(response => {
        this.billsList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => {
        this.loading = false
      });
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: null,
        userId: this.queryParams.userId, // 保持默认用户ID
        billMonth: null,
        totalAmount: 0,
        itemsData: null
      }
      this.dynamicItems = [];
      this.resetForm("form")
    },
    getIconFromName(name) {
      const iconMap = {
        '房租': '🏠', '月供': '🏦', '餐饮': '🍔', '交通': '🚗',
        '购物': '🛍️', '娱乐': '🎮', '水电煤': '⚡', '通信': '📱',
        '医疗': '🏥', '教育': '📚', '人情': '🤝', '信用卡': '💳',
        '零食': '🍩', '服饰': '👕', '旅行': '✈️', '日用': '🧼'
      };
      return iconMap[name] || '📌';
    },
    // 为Tag分配不同的颜色主题
    getTagType(index) {
      const types = ['', 'success', 'warning', 'danger'];
      return types[index % types.length];
    },
    /**
     * 核心修复：解析 JSON 字符串，并过滤掉金额为 0 的项
     */
    parseItemsData(dataStr) {
      if (!dataStr) return [];
      let parsed = [];

      // 处理已经是数组对象的情况 (Axios 有时会自动 Parse)
      if (typeof dataStr === 'object') {
        parsed = dataStr;
      } else {
        try {
          parsed = JSON.parse(dataStr);
        } catch (e) {
          console.error("解析 JSON 失败:", e);
          return [];
        }
      }

      // 过滤掉金额为空或为 0 的项目，保持表格清爽
      if (Array.isArray(parsed)) {
        return parsed.filter(item => item.amount && Number(item.amount) !== 0);
      }
      return [];
    },
    isDefaultItem(name) {
      return this.defaultCategories.includes(name);
    },
    initDynamicItems(existingDataStr = null) {
      let itemsMap = {};
      this.defaultCategories.forEach(cat => {
        itemsMap[cat] = {name: cat, amount: undefined}; // 使用 undefined 让 NumberInput 占位符生效
      });

      if (existingDataStr) {
        let parsed = [];
        if (typeof existingDataStr === 'object') {
          parsed = existingDataStr;
        } else {
          try {
            parsed = JSON.parse(existingDataStr);
          } catch (e) {
            console.error("初始化解析失败", e);
          }
        }

        if (Array.isArray(parsed)) {
          parsed.forEach(item => {
            if (item.name) {
              itemsMap[item.name] = {name: item.name, amount: item.amount || undefined};
            }
          });
        }
      }

      let result = [];
      this.defaultCategories.forEach(cat => {
        result.push(itemsMap[cat]);
        delete itemsMap[cat];
      });
      Object.values(itemsMap).forEach(item => {
        result.push(item);
      });
      this.dynamicItems = result;
    },
    addDynamicItem() {
      this.dynamicItems.push({name: '', amount: undefined});
    },
    removeDynamicItem(index) {
      this.dynamicItems.splice(index, 1);
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm("queryForm")
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.single = selection.length !== 1
      this.multiple = !selection.length
    },
    handleAdd() {
      this.reset()
      this.initDynamicItems()
      this.open = true
      this.title = "新增月度账单"
    },
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getBills(id).then(response => {
        this.form = response.data
        this.initDynamicItems(this.form.itemsData)
        this.open = true
        this.title = "修改月度账单"
      })
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          // 保存时：只保存有名字，且金额不为空且不为0的数据，避免存入无用垃圾数据
          const itemsToSave = this.dynamicItems
            .filter(item => item.name && item.name.trim() !== '' && item.amount)
            .map(item => ({
              name: item.name,
              amount: Number(item.amount) || 0
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
    handleDelete(row) {
      const ids = row.id || this.ids
      this.$modal.confirm('是否确认删除账单编号为"' + ids + '"的数据项？').then(function () {
        return delBills(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    handleExport() {
      this.download('finance/bills/export', {
        ...this.queryParams
      }, `bills_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
/* 页面基础容器优化 */
.app-container {
  padding: 20px;
  background-color: #f4f6f8;
  min-height: calc(100vh - 84px);
}

.search-card {
  margin-bottom: 15px;
  border-radius: 8px;
}

.table-card {
  border-radius: 8px;
}

/* 支出明细 Tag 美化 */
.tags-container {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  padding: 5px 0;
}

.bill-tag {
  border-radius: 6px;
  padding: 0 10px;
  height: 28px;
  line-height: 26px;
  display: flex;
  align-items: center;
  border: none;
  background: #f0f2f5;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  transition: all 0.3s;
}

.bill-tag:hover {
  transform: translateY(-1px);
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
}

.tag-icon {
  margin-right: 4px;
  font-size: 14px;
}

.tag-name {
  font-weight: 500;
}

.tag-divider {
  margin: 0 6px;
  color: #dcdfe6;
  font-size: 12px;
}

.tag-amount {
  font-weight: 600;
  font-family: monospace;
}

.no-data-text {
  color: #909399;
  font-size: 13px;
  font-style: italic;
}

/* 红色删除文字按钮 */
.text-danger {
  color: #F56C6C;
}

.text-danger:hover {
  color: #f78989;
}

/* 金额文字高亮 */
.total-amount {
  font-weight: bold;
  color: #f56c6c;
  font-size: 15px;
}

/* 弹窗内部样式优化 */
.dialog-header-info {
  background-color: #f8f9fa;
  padding: 15px 15px 0;
  border-radius: 6px;
  margin-bottom: 10px;
}

/* 动态列表滚动容器 */
.dynamic-items-container {
  max-height: 40vh;
  overflow-y: auto;
  padding-right: 10px;
  margin-bottom: 15px;
}

/* 自定义滚动条 */
.dynamic-items-container::-webkit-scrollbar {
  width: 6px;
}

.dynamic-items-container::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.dynamic-items-container::-webkit-scrollbar-track {
  background: #f1f1f1;
}

.dynamic-item-row {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  background: #fafafa;
  padding: 8px;
  border-radius: 6px;
  border: 1px solid #ebeef5;
  transition: all 0.2s;
}

.dynamic-item-row:hover {
  background: #f0f7ff;
  border-color: #c6e2ff;
}

.form-icon {
  font-size: 14px;
}

.add-btn-container {
  margin-top: 10px;
}
</style>
