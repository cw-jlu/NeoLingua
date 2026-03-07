<template>
  <el-card>
    <!-- 搜索栏 -->
    <el-form :inline="true" style="margin-bottom: 16px">
      <el-form-item>
        <el-input v-model="query.keyword" placeholder="搜索日志内容" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item>
        <el-select v-model="query.level" placeholder="日志级别" clearable>
          <el-option label="INFO" value="INFO" />
          <el-option label="WARN" value="WARN" />
          <el-option label="ERROR" value="ERROR" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="level" label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="row.level === 'ERROR' ? 'danger' : row.level === 'WARN' ? 'warning' : 'info'" size="small">{{ row.level }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="action" label="操作" width="120" />
      <el-table-column prop="content" label="内容" show-overflow-tooltip />
      <el-table-column prop="operator" label="操作人" width="100" />
      <el-table-column prop="ip" label="IP地址" width="140" />
      <el-table-column prop="createTime" label="时间" width="180" />
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
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getLogList, deleteLog } from '@/api/system'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ keyword: '', level: '', page: 1, size: 20 })

const loadData = async () => {
  loading.value = true
  try {
    const data = await getLogList(query)
    tableData.value = data?.content || data?.records || data || []
    total.value = data?.totalElements || data?.total || 0
  } catch (e) {} finally { loading.value = false }
}

const handleDelete = async (row) => {
  await deleteLog(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(loadData)
</script>
