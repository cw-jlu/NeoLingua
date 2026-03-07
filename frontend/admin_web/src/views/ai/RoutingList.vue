<template>
  <el-card>
    <!-- 操作栏 -->
    <el-form :inline="true" style="margin-bottom: 16px">
      <el-form-item>
        <el-button type="primary" @click="loadData">刷新</el-button>
        <el-button @click="handleCreate">新增规则</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="规则名称" />
      <el-table-column prop="strategy" label="路由策略" width="120">
        <template #default="{ row }">
          <el-tag size="small">{{ row.strategy }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="modelId" label="模型ID" width="100" />
      <el-table-column prop="priority" label="优先级" width="100" />
      <el-table-column prop="enabled" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'danger'" size="small">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
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

    <!-- 编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑规则' : '新增规则'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="策略">
          <el-select v-model="form.strategy">
            <el-option label="权重" value="WEIGHT" />
            <el-option label="优先级" value="PRIORITY" />
            <el-option label="轮询" value="ROUND_ROBIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型ID"><el-input-number v-model="form.modelId" :min="1" /></el-form-item>
        <el-form-item label="优先级"><el-input-number v-model="form.priority" :min="0" :max="100" /></el-form-item>
        <el-form-item label="条件"><el-input v-model="form.condition" type="textarea" :rows="2" placeholder="JSON格式条件" /></el-form-item>
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
import { getRoutingRules, createRoutingRule, updateRoutingRule, deleteRoutingRule } from '@/api/ai'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ id: null, name: '', strategy: 'WEIGHT', modelId: 1, priority: 0, condition: '' })

const loadData = async () => {
  loading.value = true
  try {
    const data = await getRoutingRules()
    tableData.value = Array.isArray(data) ? data : data?.content || data?.records || []
  } catch (e) {} finally { loading.value = false }
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(form, { id: null, name: '', strategy: 'WEIGHT', modelId: 1, priority: 0, condition: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, row)
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (isEdit.value) { await updateRoutingRule(form.id, form) } else { await createRoutingRule(form) }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {}
}

const handleDelete = async (row) => {
  await deleteRoutingRule(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
