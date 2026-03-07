<template>
  <div class="page">
    <van-nav-bar title="口语练习" />

    <!-- 新建对话 -->
    <van-button type="primary" block round style="margin-bottom: 12px" @click="$router.push('/chat/new')">
      + 开始新对话
    </van-button>

    <!-- 历史会话列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="loadSessions">
      <van-list v-model:loading="loading" :finished="finished" @load="loadSessions">
        <van-cell-group inset v-if="sessions.length">
          <van-cell v-for="s in sessions" :key="s.id" :title="s.themeName || '自由对话'"
            :label="`${s.messageCount || 0}条消息 · ${s.createTime || ''}`"
            is-link @click="$router.push(`/chat/${s.id}`)" />
        </van-cell-group>
        <van-empty v-else description="还没有练习记录，开始第一次对话吧" />
      </van-list>
    </van-pull-refresh>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMySessions } from '@/api/practice'

const sessions = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const page = ref(1)

const loadSessions = async () => {
  try {
    const data = await getMySessions({ page: page.value, size: 20 })
    const list = data?.records || data?.content || (Array.isArray(data) ? data : [])
    if (refreshing.value) { sessions.value = []; refreshing.value = false }
    sessions.value.push(...list)
    if (list.length < 20) finished.value = true
    else page.value++
  } catch (e) { finished.value = true }
  loading.value = false
}

onMounted(loadSessions)
</script>
