<template>
  <div class="page">
    <van-nav-bar title="成就徽章" left-arrow @click-left="$router.back()" />

    <van-grid :column-num="3" :border="false">
      <van-grid-item v-for="b in badges" :key="b.id">
        <div style="text-align: center">
          <div :style="{ width: '64px', height: '64px', borderRadius: '50%', background: b.unlocked ? '#fff7e6' : '#f5f5f5', display: 'flex', alignItems: 'center', justifyContent: 'center', margin: '0 auto', fontSize: '28px', opacity: b.unlocked ? 1 : 0.4 }">
            {{ b.icon || '🏅' }}
          </div>
          <p style="font-size: 12px; margin-top: 6px; color: #323233">{{ b.name }}</p>
          <p style="font-size: 10px; color: #969799">{{ b.description }}</p>
        </div>
      </van-grid-item>
    </van-grid>

    <van-empty v-if="!badges.length" description="暂无徽章数据" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getBadges } from '@/api/user'

const badges = ref([])

onMounted(async () => {
  try {
    const data = await getBadges()
    badges.value = Array.isArray(data) ? data : data?.content || []
  } catch (e) {}
})
</script>
