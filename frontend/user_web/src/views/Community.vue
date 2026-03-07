<template>
  <div class="page">
    <van-nav-bar title="社区" />

    <!-- 发帖按钮 -->
    <van-button type="primary" block round style="margin-bottom: 12px" @click="showPost = true">
      + 发布帖子
    </van-button>

    <!-- 帖子列表 -->
    <van-pull-refresh v-model="refreshing" @refresh="loadPosts">
      <van-list v-model:loading="loading" :finished="finished" @load="loadPosts">
        <div v-for="p in posts" :key="p.id" class="card" @click="$router.push(`/post/${p.id}`)">
          <h4>{{ p.title }}</h4>
          <p style="font-size: 13px; color: #646566; margin-top: 4px; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden">{{ p.content }}</p>
          <div style="display: flex; gap: 16px; margin-top: 8px; font-size: 12px; color: #969799">
            <span>👍 {{ p.likeCount || 0 }}</span>
            <span>💬 {{ p.commentCount || 0 }}</span>
            <span>👁 {{ p.viewCount || 0 }}</span>
          </div>
        </div>
        <van-empty v-if="!posts.length && finished" description="还没有帖子" />
      </van-list>
    </van-pull-refresh>

    <!-- 发帖弹窗 -->
    <van-popup v-model:show="showPost" position="bottom" round style="height: 60%; padding: 24px">
      <h3 style="margin-bottom: 16px">发布帖子</h3>
      <van-field v-model="postForm.title" label="标题" placeholder="帖子标题" />
      <van-field v-model="postForm.content" label="内容" type="textarea" rows="6" placeholder="分享你的学习心得..." />
      <van-button type="primary" block round style="margin-top: 16px" :loading="posting" @click="handlePost">发布</van-button>
    </van-popup>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { showToast } from 'vant'
import { getPosts, createPost } from '@/api/community'

const posts = ref([])
const loading = ref(false)
const finished = ref(false)
const refreshing = ref(false)
const showPost = ref(false)
const posting = ref(false)
const page = ref(1)
const postForm = reactive({ title: '', content: '' })

const loadPosts = async () => {
  try {
    const data = await getPosts({ page: page.value, size: 20 })
    const list = data?.content || data?.records || data || []
    if (refreshing.value) { posts.value = []; refreshing.value = false; page.value = 1 }
    posts.value.push(...list)
    if (list.length < 20) finished.value = true
    else page.value++
  } catch (e) { finished.value = true }
  loading.value = false
}

const handlePost = async () => {
  if (!postForm.title || !postForm.content) { showToast('请填写标题和内容'); return }
  posting.value = true
  try {
    await createPost(postForm)
    showToast('发布成功')
    showPost.value = false
    Object.assign(postForm, { title: '', content: '' })
    posts.value = []; page.value = 1; finished.value = false
    loadPosts()
  } catch (e) {} finally { posting.value = false }
}

onMounted(loadPosts)
</script>
