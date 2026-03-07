<template>
  <el-card>
    <el-button type="primary" style="margin-bottom: 16px" @click="handleCreate">新增徽章</el-button>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="名称" />
      <el-table-column prop="description" label="描述" />
      <el-table-column prop="icon" label="图标" width="100" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
          <el-popconfirm title="确定删除?" @confirm="handleDelete(row)">
            <template #reference><el-button type="danger" text size="small">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑徽章' : '新增徽章'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="图标"><el-input v-model="form.icon" /></el-form-item>
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
import { getBadgeList, createBadge, updateBadge, deleteBadge } from '@/api/user'

const loading = ref(false)
const tableData = ref([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({ id: null, name: '', description: '', icon: '' })

const loadData = async () => {
  loading.value = true
  try { const data = await getBadgeList(); tableData.value = data?.content || data || [] } catch (e) {} finally { loading.value = false }
}
const handleCreate = () => { isEdit.value = false; Object.assign(form, { id: null, name: '', description: '', icon: '' }); dialogVisible.value = true }
const handleEdit = (row) => { isEdit.value = true; Object.assign(form, row); dialogVisible.value = true }
const handleSubmit = async () => {
  try { isEdit.value ? await updateBadge(form.id, form) : await createBadge(form); ElMessage.success('操作成功'); dialogVisible.value = false; loadData() } catch (e) {}
}
const handleDelete = async (row) => { await deleteBadge(row.id); ElMessage.success('删除成功'); loadData() }
onMounted(loadData)
</script>
