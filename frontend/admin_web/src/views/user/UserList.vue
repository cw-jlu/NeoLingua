<template>
  <el-card>
    <!-- 搜索栏 -->
    <el-form :inline="true" style="margin-bottom: 16px">
      <el-form-item>
        <el-input v-model="query.keyword" placeholder="搜索用户名/邮箱" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">搜索</el-button>
        <el-button @click="handleCreate">新增用户</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="points" label="积分" width="100" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.banned ? 'danger' : 'success'" size="small">
            {{ row.banned ? '已封禁' : '正常' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="注册时间" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" text size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button v-if="!row.banned" type="warning" text size="small" @click="handleBan(row)">封禁</el-button>
          <el-button v-else type="success" text size="small" @click="handleUnban(row)">解封</el-button>
          <el-button type="info" text size="small" @click="handleResetPwd(row)">重置密码</el-button>
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
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑用户' : '新增用户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item v-if="!isEdit" label="密码"><el-input v-model="form.password" type="password" /></el-form-item>
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
import { getUserList, createUser, updateUser, deleteUser, banUser, unbanUser, resetPassword } from '@/api/user'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const query = reactive({ keyword: '', page: 1, size: 20 })
const form = reactive({ id: null, username: '', email: '', password: '' })

const loadData = async () => {
  loading.value = true
  try {
    const data = await getUserList(query)
    tableData.value = data?.content || data?.records || data || []
    total.value = data?.totalElements || data?.total || 0
  } catch (e) {} finally { loading.value = false }
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(form, { id: null, username: '', email: '', password: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, { id: row.id, username: row.username, email: row.email })
  dialogVisible.value = true
}

const handleSubmit = async () => {
  try {
    if (isEdit.value) {
      await updateUser(form.id, form)
    } else {
      await createUser(form)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    dialogVisible.value = false
    loadData()
  } catch (e) {}
}

const handleDelete = async (row) => {
  await deleteUser(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleBan = async (row) => {
  await banUser(row.id)
  ElMessage.success('封禁成功')
  loadData()
}

const handleUnban = async (row) => {
  await unbanUser(row.id)
  ElMessage.success('解封成功')
  loadData()
}

const handleResetPwd = async (row) => {
  await resetPassword(row.id)
  ElMessage.success('密码已重置')
}

onMounted(loadData)
</script>
