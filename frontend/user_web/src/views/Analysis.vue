<template>
  <div class="page">
    <van-nav-bar title="学习分析" />

    <!-- 总体评分 -->
    <div class="card" style="text-align: center">
      <h2 style="color: #1989fa; font-size: 48px">{{ stats.avg_overall_score || 0 }}</h2>
      <p style="color: #969799; font-size: 13px">综合评分</p>
      <van-grid :column-num="3" :border="false" style="margin-top: 12px">
        <van-grid-item>
          <template #text><span style="font-size: 20px; font-weight: bold">{{ stats.avg_grammar_score || 0 }}</span><br><span style="font-size: 12px; color: #969799">语法</span></template>
        </van-grid-item>
        <van-grid-item>
          <template #text><span style="font-size: 20px; font-weight: bold">{{ stats.avg_pronunciation_score || 0 }}</span><br><span style="font-size: 12px; color: #969799">发音</span></template>
        </van-grid-item>
        <van-grid-item>
          <template #text><span style="font-size: 20px; font-weight: bold">{{ stats.avg_fluency_score || 0 }}</span><br><span style="font-size: 12px; color: #969799">流利度</span></template>
        </van-grid-item>
      </van-grid>
    </div>

    <!-- 排名 -->
    <div class="card">
      <div style="display: flex; justify-content: space-between; align-items: center">
        <h4>我的排名</h4>
        <van-tag type="primary" size="large">第 {{ myRank.rank || '-' }} 名</van-tag>
      </div>
      <p style="font-size: 12px; color: #969799; margin-top: 4px">共 {{ myRank.total || 0 }} 人参与排名</p>
    </div>

    <!-- 最近报告 -->
    <div class="card">
      <h4 style="margin-bottom: 12px">最近分析</h4>
      <van-cell v-for="r in reports" :key="r.id"
        :title="`综合 ${r.overall_score} 分`"
        :label="`语法${r.grammar_score} | 发音${r.pronunciation_score} | 流利${r.fluency_score}`"
        :value="r.created_at?.substring(0, 10)" />
      <van-empty v-if="!reports.length" description="暂无分析数据" image-size="80" />
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { getMyStatistics, getMyRank, getMyReports } from '@/api/analysis'

const authStore = useAuthStore()
const userId = authStore.userInfo?.id
const stats = reactive({ avg_grammar_score: 0, avg_pronunciation_score: 0, avg_fluency_score: 0, avg_overall_score: 0 })
const myRank = reactive({ rank: 0, total: 0 })
const reports = ref([])

onMounted(async () => {
  if (!userId) return
  try { Object.assign(stats, await getMyStatistics(userId)) } catch (e) {}
  try { Object.assign(myRank, await getMyRank(userId)) } catch (e) {}
  try {
    const data = await getMyReports({ user_id: userId, page: 1, size: 10 })
    reports.value = data?.content || data || []
  } catch (e) {}
})
</script>
