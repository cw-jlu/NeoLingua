<template>
  <el-card>
    <!-- 搜索栏 -->
    <el-form :inline="true" style="margin-bottom: 16px">
      <el-form-item>
        <el-input v-model="query.keyword" placeholder="搜索通知内容" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">搜索</el-button>
        <el-button type="success" @click="broadcastVisible = true">广播通知</el-button>
        <el-button @click="targetedVisible = true">定向通知</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="userId" label="用户ID" width="100" />
      <el-table-column prop="title" label="标题" />
      <el-table-column prop="content" label="内容" show-overflow-tooltip />
      <el-table-column prop="type" label="类型" width="100" />
      <el-table-column prop="read" label="已读" width="80">
        <template #default="{ row }">
          <el-tag :type="row.read ? 'success' : 'warning'" size="small">{{ row.read ? '已读' : '未读' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发送时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
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

    <!-- 广播通知弹窗 -->
    <el-dialog v-model="broadcastVisible" title="广播通知" width="500px">
      <el-form :model="broadcastForm" label-width="80px">
        <el-form-item label="标题"><el-input v-model="broadcastForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="broadcastForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="broadcastVisible = false">取消</el-button>
        <el-button type="primary" @click="handleBroadcast">发送</el-button>
      </template>
    </el-dialog>

    <!-- 定向通知弹窗 -->
    <el-dialog v-model="targetedVisible" title="定向通知" width="500px">
      <el-form :model="targetedForm" label-width="80px">
        <el-form-item label="用户ID"><el-input v-model="targetedForm.userId" /></el-form-item>
        <el-form-item label="标题"><el-input v-model="targetedForm.title" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="targetedForm.content" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="targetedVisible = false">取消</el-button>
        <el-button type="primary" @click="handleTargeted">发送</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getNotificationList, deleteNotification, broadcast, sendTargeted } from '@/api/notification'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const broadcastVisible = ref(false)
const targetedVisible = ref(false)
const query = reactive({ keyword: '', page: 1, size: 20 })
const broadcastForm = reactive({ title: '', content: '' })
const targetedForm = reactive({ userId: '', title: '', content: '' })

const loadData = async () => {
  loading.value = true
  try {
    const data = await getNotificationList(query)
    tableData.value = data?.content || data?.records || data || []
    total.value = data?.totalElements || data?.total || 0
  } catch (e) {} finally { loading.value = false }
}

const handleDelete = async (row) => {
  await deleteNotification(row.id)
  ElMessage.success('删除成功')
  loadData()
}

const handleBroadcast = async () => {
  await broadcast(broadcastForm)
  ElMessage.success('广播发送成功')
  broadcastVisible.value = false
  Object.assign(broadcastForm, { title: '', content: '' })
  loadData()
}

const handleTargeted = async () => {
  await sendTargeted(targetedForm)
  ElMessage.success('定向通知发送成功')
  targetedVisible.value = false
  Object.assign(targetedForm, { userId: '', title: '', content: '' })
  loadData()
}

onMounted(loadData)
</script>
