<template>
  <el-card>
    <!-- 搜索栏 -->
    <el-form :inline="true" style="margin-bottom: 16px">
      <el-form-item>
        <el-input v-model="query.keyword" placeholder="搜索帖子标题" clearable @keyup.enter="loadData" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="loadData">搜索</el-button>
      </el-form-item>
    </el-form>

    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column prop="userId" label="作者ID" width="100" />
      <el-table-column prop="likeCount" label="点赞" width="80" />
      <el-table-column prop="commentCount" label="评论" width="80" />
      <el-table-column prop="viewCount" label="浏览" width="80" />
      <el-table-column prop="pinned" label="置顶" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.pinned" type="warning" size="small">置顶</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="hidden" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.hidden ? 'danger' : 'success'" size="small">{{ row.hidden ? '隐藏' : '正常' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button v-if="!row.pinned" type="warning" text size="small" @click="handlePin(row)">置顶</el-button>
          <el-button v-else text size="small" @click="handleUnpin(row)">取消置顶</el-button>
          <el-button v-if="!row.hidden" type="info" text size="small" @click="handleHide(row)">隐藏</el-button>
          <el-button v-else type="success" text size="small" @click="handleShow(row)">显示</el-button>
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
import { getPostList, deletePost, pinPost, unpinPost, hidePost, showPost } from '@/api/community'

const loading = ref(false)
const tableData = ref([])
const total = ref(0)
const query = reactive({ keyword: '', page: 1, size: 20 })

const loadData = async () => {
  loading.value = true
  try {
    const data = await getPostList(query)
    tableData.value = data?.content || data?.records || data || []
    total.value = data?.totalElements || data?.total || 0
  } catch (e) {} finally { loading.value = false }
}

const handlePin = async (row) => { await pinPost(row.id); ElMessage.success('置顶成功'); loadData() }
const handleUnpin = async (row) => { await unpinPost(row.id); ElMessage.success('取消置顶'); loadData() }
const handleHide = async (row) => { await hidePost(row.id); ElMessage.success('已隐藏'); loadData() }
const handleShow = async (row) => { await showPost(row.id); ElMessage.success('已显示'); loadData() }
const handleDelete = async (row) => { await deletePost(row.id); ElMessage.success('删除成功'); loadData() }

onMounted(loadData)
</script>
