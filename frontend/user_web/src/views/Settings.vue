<template>
  <div class="page">
    <van-nav-bar title="设置" left-arrow @click-left="$router.back()" />

    <van-cell-group inset title="语言偏好">
      <van-cell title="界面语言">
        <template #right-icon>
          <van-radio-group v-model="lang" direction="horizontal">
            <van-radio name="zh">中文</van-radio>
            <van-radio name="en">English</van-radio>
          </van-radio-group>
        </template>
      </van-cell>
      <van-cell title="口音偏好">
        <template #right-icon>
          <van-radio-group v-model="accent" direction="horizontal">
            <van-radio name="us">美式</van-radio>
            <van-radio name="uk">英式</van-radio>
          </van-radio-group>
        </template>
      </van-cell>
    </van-cell-group>

    <van-cell-group inset title="隐私" style="margin-top: 12px">
      <van-cell title="排名公开">
        <template #right-icon><van-switch v-model="rankingPublic" size="20" /></template>
      </van-cell>
      <van-cell title="离线模式">
        <template #right-icon><van-switch v-model="offlineMode" size="20" /></template>
      </van-cell>
    </van-cell-group>

    <van-cell-group inset title="通知" style="margin-top: 12px">
      <van-cell title="推送通知">
        <template #right-icon><van-switch v-model="pushEnabled" size="20" /></template>
      </van-cell>
    </van-cell-group>

    <van-cell-group inset title="账号" style="margin-top: 12px">
      <van-cell title="清除缓存" is-link @click="clearCache" />
      <van-cell title="注销账号" is-link @click="deleteAccount" />
    </van-cell-group>

    <van-cell-group inset title="关于" style="margin-top: 12px">
      <van-cell title="版本" value="1.0.0" />
      <van-cell title="帮助教程" is-link />
    </van-cell-group>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { showToast, showDialog } from 'vant'

const lang = ref('zh')
const accent = ref('us')
const rankingPublic = ref(false)
const offlineMode = ref(false)
const pushEnabled = ref(true)

const clearCache = () => { showToast('缓存已清除') }
const deleteAccount = () => {
  showDialog({ title: '注销账号', message: '注销后数据将无法恢复，确定吗？', showCancelButton: true })
    .then(() => showToast('功能开发中'))
    .catch(() => {})
}
</script>
