<template>
  <div class="tab-layout">
    <router-view />
    <van-tabbar v-model="active" route>
      <van-tabbar-item to="/home" icon="home-o">首页</van-tabbar-item>
      <van-tabbar-item to="/chat" icon="chat-o">练习</van-tabbar-item>
      <van-tabbar-item to="/meeting" icon="video-o">Meeting</van-tabbar-item>
      <van-tabbar-item to="/analysis" icon="bar-chart-o">分析</van-tabbar-item>
      <van-tabbar-item to="/community" icon="friends-o">社区</van-tabbar-item>
      <van-tabbar-item to="/profile" icon="user-o">我的</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { ref } from 'vue'
const active = ref(0)
</script>

<style scoped>
.tab-layout {
  height: 100%;
}
</style>
