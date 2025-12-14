<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="缓存名称" prop="cacheName">
        <el-input
          v-model="queryParams.cacheName"
          placeholder="请输入缓存名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="缓存键名" prop="cacheKey">
        <el-input
          v-model="queryParams.cacheKey"
          placeholder="请输入缓存键名"
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
          v-hasPermi="['system:cache_metadata:add']"
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
          v-hasPermi="['system:cache_metadata:edit']"
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
          v-hasPermi="['system:cache_metadata:remove']"
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
          v-hasPermi="['system:cache_metadata:export']"
        >导出
        </el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="cache_metadataList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center"/>
<!--      <el-table-column label="主键ID" align="center" prop="id"/>-->
      <el-table-column label="缓存名称" align="center" prop="cacheName"/>
      <el-table-column label="缓存键名" align="center" prop="cacheKey"/>
      <el-table-column label="缓存内容" align="center" prop="cacheValue"/>
      <el-table-column label="备注" align="center" prop="remark"/>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:cache_metadata:edit']"
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

    <!-- 添加或修改缓存元数据对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="缓存名称" prop="cacheName">
          <el-input v-model="form.cacheName" placeholder="请输入缓存名称"/>
        </el-form-item>
        <el-form-item label="缓存键名" prop="cacheKey">
          <el-input v-model="form.cacheKey" placeholder="请输入缓存键名"/>
        </el-form-item>
        <el-form-item label="缓存内容" prop="cacheValue">
          <el-input v-model="form.cacheValue" type="textarea" placeholder="请输入内容"/>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
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
import {
  listCache_metadata,
  getCache_metadata,
  delCache_metadata,
  addCache_metadata,
  updateCache_metadata
} from "@/api/system/cache_metadata"

export default {
  name: "Cache_metadata",
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
      // 缓存元数据表格数据
      cache_metadataList: [],
      // 弹出层标题
      title: "",
      // 是否显示弹出层
      open: false,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        cacheName: null,
        cacheKey: null,
        cacheValue: null,
      },
      // 表单参数
      form: {},
      // 表单校验
      rules: {
        cacheName: [
          {required: true, message: "缓存名称不能为空", trigger: "blur"}
        ],
        // cacheKey: [
        //   {required: true, message: "缓存键名不能为空", trigger: "blur"}
        // ],
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询缓存元数据列表 */
    getList() {
      this.loading = true
      listCache_metadata(this.queryParams).then(response => {
        this.cache_metadataList = response.rows
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
        cacheName: null,
        cacheKey: null,
        cacheValue: null,
        remark: null,
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
      this.title = "添加缓存元数据"
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id || this.ids
      getCache_metadata(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = "修改缓存元数据"
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateCache_metadata(this.form).then(response => {
              this.$modal.msgSuccess("修改成功")
              this.open = false
              this.getList()
            })
          } else {
            addCache_metadata(this.form).then(response => {
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
      this.$modal.confirm('是否确认删除缓存元数据编号为"' + ids + '"的数据项？').then(function () {
        return delCache_metadata(ids)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess("删除成功")
      }).catch(() => {
      })
    },
    /** 导出按钮操作 */
    handleExport() {
      this.download('system/cache_metadata/export', {
        ...this.queryParams
      }, `cache_metadata_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>
