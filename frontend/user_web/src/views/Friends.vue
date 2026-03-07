<template>
  <div class="page">
    <van-nav-bar title="好友" left-arrow @click-left="$router.back()" />

    <!-- 搜索添加 -->
    <van-search v-model="searchKey" placeholder="搜索用户ID添加好友" @search="handleSearch" />

    <!-- 搜索结果 -->
    <van-cell-group inset v-if="searchResult" style="margin-bottom: 12px">
      <van-cell :title="searchResult.username" :label="`ID: ${searchResult.id}`">
        <template #right-icon>
          <van-button type="primary" size="mini" round @click="handleAdd(searchResult.id)">添加</van-button>
        </template>
      </van-cell>
    </van-cell-group>

    <!-- 待处理请求 -->
    <van-cell-group inset title="好友请求" v-if="pendingRequests.length" style="margin-bottom: 12px">
      <van-cell v-for="r in pendingRequests" :key="r.id" :title="`用户${r.fromUserId}`">
        <template #right-icon>
          <van-button type="primary" size="mini" round style="margin-right: 8px" @click="handleAccept(r.id)">接受</van-button>
          <van-button size="mini" round @click="handleReject(r.id)">拒绝</van-button>
        </template>
      </van-cell>
    </van-cell-group>

    <!-- 好友列表 -->
    <van-cell-group inset title="我的好友">
      <van-swipe-cell v-for="f in friends" :key="f.id">
        <van-cell :title="f.friendName || `用户${f.friendId}`" icon="user-o" />
        <template #right>
          <van-button square type="danger" text="删除" @click="handleDelete(f.id)" />
        </template>
      </van-swipe-cell>
      <van-empty v-if="!friends.length" description="还没有好友" image-size="80" />
    </van-cell-group>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { showToast } from 'vant'
import { getFriends, searchUser, sendFriendRequest, acceptFriend, rejectFriend, deleteFriend, getPendingRequests } from '@/api/meeting'

const friends = ref([])
const pendingRequests = ref([])
const searchKey = ref('')
const searchResult = ref(null)

const handleSearch = async () => {
  if (!searchKey.value) return
  try { searchResult.value = await searchUser(searchKey.value) } catch (e) { searchResult.value = null; showToast('未找到用户') }
}
const handleAdd = async (id) => { try { await sendFriendRequest(id); showToast('请求已发送') } catch (e) {} }
const handleAccept = async (id) => { try { await acceptFriend(id); showToast('已接受'); loadData() } catch (e) {} }
const handleReject = async (id) => { try { await rejectFriend(id); showToast('已拒绝'); loadData() } catch (e) {} }
const handleDelete = async (id) => { try { await deleteFriend(id); showToast('已删除'); loadData() } catch (e) {} }

const loadData = async () => {
  try { friends.value = await getFriends() || [] } catch (e) {}
  try { pendingRequests.value = await getPendingRequests() || [] } catch (e) {}
}

onMounted(loadData)
</script>
