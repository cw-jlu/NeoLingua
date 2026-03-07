<template>
  <div class="page">
    <van-nav-bar title="帖子详情" left-arrow @click-left="$router.back()" />

    <div class="card" v-if="post">
      <h3>{{ post.title }}</h3>
      <p style="font-size: 12px; color: #969799; margin: 8px 0">{{ post.createTime }}</p>
      <p style="line-height: 1.6">{{ post.content }}</p>
      <div style="display: flex; gap: 16px; margin-top: 12px">
        <van-button size="small" icon="good-job-o" @click="handleLike">{{ post.likeCount || 0 }}</van-button>
        <van-button size="small" icon="star-o" @click="handleFavorite">收藏</van-button>
      </div>
    </div>

    <!-- 评论列表 -->
    <div class="card">
      <h4 style="margin-bottom: 12px">评论 ({{ comments.length }})</h4>
      <div v-for="c in comments" :key="c.id" style="padding: 8px 0; border-bottom: 1px solid #f5f5f5">
        <p style="font-size: 13px"><span style="color: #1989fa">用户{{ c.userId }}</span>: {{ c.content }}</p>
        <p style="font-size: 11px; color: #c8c9cc; margin-top: 2px">{{ c.createTime }}</p>
      </div>
      <van-empty v-if="!comments.length" description="暂无评论" image-size="60" />
    </div>

    <!-- 评论输入 -->
    <div style="position: fixed; bottom: 0; left: 0; right: 0; padding: 8px 12px; background: #fff; border-top: 1px solid #eee; display: flex; gap: 8px">
      <van-field v-model="commentText" placeholder="写评论..." style="flex: 1" @keyup.enter="handleComment" />
      <van-button type="primary" size="small" round @click="handleComment">发送</van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { showToast } from 'vant'
import { getPost, likePost, favoritePost, getComments, createComment } from '@/api/community'

const route = useRoute()
const postId = route.params.id
const post = ref(null)
const comments = ref([])
const commentText = ref('')

const handleLike = async () => { try { await likePost(postId); post.value.likeCount++; showToast('已点赞') } catch (e) {} }
const handleFavorite = async () => { try { await favoritePost(postId); showToast('已收藏') } catch (e) {} }
const handleComment = async () => {
  if (!commentText.value.trim()) return
  try {
    await createComment(postId, { content: commentText.value })
    comments.value.push({ id: Date.now(), userId: '我', content: commentText.value, createTime: '刚刚' })
    commentText.value = ''
  } catch (e) {}
}

onMounted(async () => {
  try { post.value = await getPost(postId) } catch (e) {}
  try { const d = await getComments(postId); comments.value = Array.isArray(d) ? d : d?.content || [] } catch (e) {}
})
</script>
