<template>
  <div class="page">
    <van-nav-bar title="Meeting" />

    <van-button type="primary" block round style="margin-bottom: 12px" @click="showCreate = true">
      + 创建Meeting
    </van-button>

    <!-- Meeting列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="loadMeetings">
      <van-cell-group inset v-if="meetings.length">
        <van-cell v-for="m in meetings" :key="m.id" :title="m.name"
          :label="`${m.currentParticipants || 0}人 · ${m.status === 1 ? '进行中' : m.status === 0 ? '等待中' : '已结束'}`"
          is-link @click="$router.push(`/meeting/${m.id}`)" />
      </van-cell-group>
      <van-empty v-else description="还没有Meeting，创建一个吧" />
    </van-pull-refresh>

    <!-- 创建弹窗 -->
    <van-popup v-model:show="showCreate" position="bottom" round style="padding: 24px">
      <h3 style="margin-bottom: 16px">创建Meeting</h3>
      <van-field v-model="createForm.name" label="名称" placeholder="Meeting名称" />
      <van-field v-model="createForm.maxParticipants" label="人数上限" type="number" placeholder="最大参与人数" />
      <van-button type="primary" block round style="margin-top: 16px" :loading="creating" @click="handleCreate">创建</van-button>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getMyMeetings, createMeeting } from '@/api/meeting'

const router = useRouter()
const meetings = ref([])
const refreshing = ref(false)
const showCreate = ref(false)
const creating = ref(false)
const createForm = reactive({ name: '', maxParticipants: 4 })

const loadMeetings = async () => {
  try {
    const data = await getMyMeetings()
    meetings.value = Array.isArray(data) ? data : data?.content || []
  } catch (e) {}
  refreshing.value = false
}

const handleCreate = async () => {
  if (!createForm.name) { showToast('请输入名称'); return }
  creating.value = true
  try {
    const m = await createMeeting(createForm)
    showCreate.value = false
    router.push(`/meeting/${m.id || m}`)
  } catch (e) {} finally { creating.value = false }
}

onMounted(loadMeetings)
</script>
