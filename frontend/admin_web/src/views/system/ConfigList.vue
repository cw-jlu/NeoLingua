<template>
  <el-card>
    <!-- 搜索栏 -->
    <el-form :inline="true" style="margin-bottom: 16px">
      <el-form-item>
        <el-input v-model="query.keyword" placeholder="搜索配置键名" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">搜索</el-button>
        <el-button @click="handleCreate">新增配置</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="configKey" label="配置键" />
      <el-table-column prop="configValue" label="配置值" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除?" @confirm="handleDelete(row)">
            <template #reference>
              <el-button type="danger" text size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <el-pagination
      style="margin-top: 16px; justify-content: flex-end"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      layout="total, sizes, prev, pager, next"
      @change="loadData"
    />

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑配置' : '新增配置'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="配置键"><el-input v-model="form.configKey" :disabled="isEdit" /></el-form-item>
        <el-form-item label="配置值"><el-input v-model="form.configValue" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="分类"><el-input v-model="form.category" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getConfigList, createConfig, updateConfig, deleteConfig } from '@/api/system'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const query = reactive({ keyword: '', page: 1, size: 20 })
const form = reactive({ id: null, configKey: '', configValue: '', category: '', description: '' })

const loadData = async () => {
  loading.value = true
  try {
    const data = await getConfigList(query)
    tableData.value = data?.content || data?.records || data || []
    total.value = data?.totalElements || data?.total || 0
  } catch (e) {} finally { loading.value = false }
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(form, { id: null, configKey: '', configValue: '', category: '', description: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, { id: row.id, configKey: row.configKey, configValue: row.configValue, category: row.category, description: row.description })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (isEdit.value) { await updateConfig(form.configKey, form) } else { await createConfig(form) }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {}
}

const handleDelete = async (row) => {
  await deleteConfig(row.configKey)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
