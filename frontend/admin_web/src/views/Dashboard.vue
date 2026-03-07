<template>
  <div>
    <!-- 统计卡片 -->
    <el-row :gutter="20" style="margin-bottom: 20px">
      <el-col :span="6" v-for="item in statCards" :key="item.title">
        <el-card shadow="hover">
          <div style="display: flex; align-items: center; justify-content: space-between">
            <div>
              <div style="color: #909399; font-size: 14px">{{ item.title }}</div>
              <div style="font-size: 28px; font-weight: bold; margin-top: 8px">{{ item.value }}</div>
            </div>
            <el-icon :size="40" :color="item.color"><component :is="item.icon" /></el-icon>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 图表区域 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card>
          <template #header>用户增长趋势</template>
          <div ref="chartRef" style="height: 350px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <template #header>服务状态</template>
          <div v-for="svc in services" :key="svc.name" style="display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f0f0f0">
            <span>{{ svc.name }}</span>
            <el-tag :type="svc.status === 'UP' ? 'success' : 'danger'" size="small">{{ svc.status }}</el-tag>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, markRaw } from 'vue'
import { User, ChatDotRound, Document, VideoCamera } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import { getDashboard } from '@/api/system'

const chartRef = ref()

const statCards = ref([
  { title: '总用户数', value: '-', icon: markRaw(User), color: '#1890ff' },
  { title: '今日练习', value: '-', icon: markRaw(ChatDotRound), color: '#52c41a' },
  { title: '社区帖子', value: '-', icon: markRaw(Document), color: '#faad14' },
  { title: '活跃Meeting', value: '-', icon: markRaw(VideoCamera), color: '#f5222d' },
])

const services = ref([
  { name: 'User Service (8081)', status: 'UP' },
  { name: 'Practice Service (8082)', status: 'UP' },
  { name: 'Meeting Service (8083)', status: 'UP' },
  { name: 'Community Service (8084)', status: 'UP' },
  { name: 'Notification Service (8086)', status: 'UP' },
  { name: 'Admin Service (8087)', status: 'UP' },
  { name: 'AI Gateway (8088)', status: 'UP' },
  { name: 'AI Service (8089)', status: 'UP' },
])

onMounted(async () => {
  // 加载仪表盘数据
  try {
    const data = await getDashboard()
    if (data) {
      statCards.value[0].value = data.totalUsers ?? '-'
      statCards.value[1].value = data.todayPractices ?? '-'
      statCards.value[2].value = data.totalPosts ?? '-'
      statCards.value[3].value = data.activeMeetings ?? '-'
    }
  } catch (e) {
    // 使用默认值
  }

  // 初始化图表
  if (chartRef.value) {
    const chart = echarts.init(chartRef.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
      },
      yAxis: { type: 'value' },
      series: [
        { name: '新增用户', type: 'line', smooth: true, data: [120, 132, 101, 134, 90, 230, 210], areaStyle: {} },
        { name: '活跃用户', type: 'line', smooth: true, data: [220, 182, 191, 234, 290, 330, 310] },
      ],
    })
    window.addEventListener('resize', () => chart.resize())
  }
})
</script>
