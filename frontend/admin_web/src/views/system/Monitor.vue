<template>
  <div>
    <!-- 服务状态 -->
    <el-card style="margin-bottom: 16px">
      <template #header>
        <div style="display: flex; justify-content: space-between; align-items: center">
          <span>服务状态</span>
          <el-button type="primary" size="small" @click="loadAll">刷新</el-button>
        </div>
      </template>
      <el-row :gutter="16">
        <el-col :span="6" v-for="svc in services" :key="svc.name">
          <el-card shadow="hover" style="text-align: center; margin-bottom: 12px">
            <el-tag :type="svc.status === 'UP' ? 'success' : 'danger'" size="large">
              {{ svc.status === 'UP' ? '运行中' : '已停止' }}
            </el-tag>
            <p style="margin-top: 8px; font-weight: 500">{{ svc.name }}</p>
            <p style="color: #999; font-size: 12px">{{ svc.url || '' }}</p>
          </el-card>
        </el-col>
      </el-row>
    </el-card>

    <!-- 系统指标 -->
    <el-row :gutter="16">
      <el-col :span="8">
        <el-card>
          <template #header><span>CPU使用率</span></template>
          <el-progress :percentage="metrics.cpuUsage || 0" :color="progressColor" :stroke-width="20" />
          <p style="text-align: center; margin-top: 8px; color: #666">{{ (metrics.cpuUsage || 0).toFixed(1) }}%</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>内存使用率</span></template>
          <el-progress :percentage="metrics.memoryUsage || 0" :color="progressColor" :stroke-width="20" />
          <p style="text-align: center; margin-top: 8px; color: #666">{{ (metrics.memoryUsage || 0).toFixed(1) }}%</p>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header><span>磁盘使用率</span></template>
          <el-progress :percentage="metrics.diskUsage || 0" :color="progressColor" :stroke-width="20" />
          <p style="text-align: center; margin-top: 8px; color: #666">{{ (metrics.diskUsage || 0).toFixed(1) }}%</p>
        </el-card>
      </el-col>
    </el-row>

    <!-- 健康检查详情 -->
    <el-card style="margin-top: 16px">
      <template #header><span>健康检查详情</span></template>
      <el-table :data="healthDetails" stripe>
        <el-table-column prop="component" label="组件" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 'UP' ? 'success' : 'danger'" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="details" label="详情" show-overflow-tooltip />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMonitorHealth, getMonitorMetrics, getMonitorServices } from '@/api/system'

const services = ref([])
const metrics = reactive({ cpuUsage: 0, memoryUsage: 0, diskUsage: 0 })
const healthDetails = ref([])

const progressColor = (percentage) => {
  if (percentage < 60) return '#67c23a'
  if (percentage < 80) return '#e6a23c'
  return '#f56c6c'
}

const loadAll = async () => {
  try {
    const [svcData, metricsData, healthData] = await Promise.all([
      getMonitorServices().catch(() => []),
      getMonitorMetrics().catch(() => ({})),
      getMonitorHealth().catch(() => ({})),
    ])
    services.value = Array.isArray(svcData) ? svcData : []
    Object.assign(metrics, metricsData || {})
    // 解析健康检查数据
    if (healthData && typeof healthData === 'object') {
      healthDetails.value = Object.entries(healthData.components || healthData).map(([key, val]) => ({
        component: key,
        status: typeof val === 'object' ? val.status || 'UNKNOWN' : val,
        details: typeof val === 'object' ? JSON.stringify(val.details || val) : String(val),
      }))
    }
  } catch (e) {}
}

onMounted(loadAll)
</script>
