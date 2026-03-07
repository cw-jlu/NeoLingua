<template>
  <el-card>
    <!-- 搜索栏 -->
    <el-form :inline="true" style="margin-bottom: 16px">
      <el-form-item>
        <el-input v-model="query.keyword" placeholder="搜索模型名称" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.provider" placeholder="提供者类型" clearable>
          <el-option label="Ollama" value="OLLAMA" />
          <el-option label="远程API" value="REMOTE_API" />
          <el-option label="本地模型" value="LOCAL" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">搜索</el-button>
        <el-button @click="handleCreate">新增模型</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="模型名称" />
      <el-table-column prop="provider" label="提供者" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.provider }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="modelName" label="模型标识" />
      <el-table-column prop="enabled" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="weight" label="权重" width="80" />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="row.enabled" type="warning" text size="small" @click="handleDisable(row)">禁用</el-button>
          <el-button v-else type="success" text size="small" @click="handleEnable(row)">启用</el-button>
          <el-button type="info" text size="small" @click="handleTest(row)">测试</el-button>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模型' : '新增模型'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模型名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="提供者类型">
          <el-select v-model="form.provider">
            <el-option label="Ollama" value="OLLAMA" />
            <el-option label="远程API" value="REMOTE_API" />
            <el-option label="本地模型" value="LOCAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型标识"><el-input v-model="form.modelName" placeholder="如 qwen2.5:7b" /></el-form-item>
        <el-form-item label="API地址"><el-input v-model="form.apiUrl" placeholder="远程API地址" /></el-form-item>
        <el-form-item label="API密钥"><el-input v-model="form.apiKey" type="password" show-password /></el-form-item>
        <el-form-item label="权重"><el-input-number v-model="form.weight" :min="0" :max="100" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
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
import { getModelList, createModel, updateModel, deleteModel, enableModel, disableModel, testModel } from '@/api/ai'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const query = reactive({ keyword: '', provider: '', page: 1, size: 20 })
const form = reactive({ id: null, name: '', provider: 'OLLAMA', modelName: '', apiUrl: '', apiKey: '', weight: 1, description: '' })

const loadData = async () => {
  loading.value = true
  try {
    const data = await getModelList(query)
    tableData.value = data?.content || data?.records || data || []
    total.value = data?.totalElements || data?.total || 0
  } catch (e) {} finally { loading.value = false }
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(form, { id: null, name: '', provider: 'OLLAMA', modelName: '', apiUrl: '', apiKey: '', weight: 1, description: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (isEdit.value) { await updateModel(form.id, form) } else { await createModel(form) }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {}
}

const handleDelete = async (row) => { await deleteModel(row.id); ElMessage.success('删除成功'); loadData() }
const handleEnable = async (row) => { await enableModel(row.id); ElMessage.success('已启用'); loadData() }
const handleDisable = async (row) => { await disableModel(row.id); ElMessage.success('已禁用'); loadData() }
const handleTest = async (row) => {
  ElMessage.info('正在测试...')
  try {
    const result = await testModel(row.id)
    ElMessage.success(`测试${result?.success ? '成功' : '失败'}: ${result?.message || ''}`)
  } catch (e) { ElMessage.error('测试失败') }
}

onMounted(loadData)
</script>
