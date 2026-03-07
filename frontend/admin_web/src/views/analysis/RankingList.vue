<template>
  <el-card>
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>用户排名榜</span>
        <el-button type="primary" size="small" @click="loadData">刷新</el-button>
      </div>
    </template>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="rank" label="排名" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.rank <= 3" :type="row.rank === 1 ? 'danger' : row.rank === 2 ? 'warning' : ''" size="small">
            {{ row.rank === 1 ? '🥇' : row.rank === 2 ? '🥈' : '🥉' }} {{ row.rank }}
          </el-tag>
          <span v-else>{{ row.rank }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="user_id" label="用户ID" width="120" />
      <el-table-column prop="ranking_score" label="排名分数" />
    </el-table>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAnalysisRanking } from '@/api/analysis'

const loading = ref(false)
const tableData = ref([])

const loadData = async () => {
  loading.value = true
  try {
    const data = await getAnalysisRanking({ top: 100 })
    tableData.value = Array.isArray(data) ? data : []
  } catch (e) {} finally { loading.value = false }
}

onMounted(loadData)
</script>
