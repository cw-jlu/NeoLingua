<template>
  <div class="meeting-page">
    <!-- 顶部导航 -->
    <div class="meeting-header">
      <button class="back-btn" @click="handleLeave">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
        </svg>
        离开
      </button>
      <div class="meeting-title">
        <h3>{{ meeting?.name || '会议室' }}</h3>
        <span class="meeting-status">● 进行中</span>
      </div>
      <div class="header-actions">
        <button class="btn-icon" :class="{ active: autoPlay }" @click="autoPlay = !autoPlay" title="自动播放AI回复">🔊</button>
        <select v-model="ttsVoice" class="voice-select">
          <option value="female_us">美式女声</option>
          <option value="male_us">美式男声</option>
          <option value="female_uk">英式女声</option>
          <option value="male_uk">英式男声</option>
        </select>
      </div>
    </div>

    <!-- 参与者栏 -->
    <div class="participants-bar">
      <div class="participants-list">
        <div v-for="p in participants" :key="p.id" class="participant-item">
          <div class="avatar" :class="p.userId < 0 ? 'ai-avatar' : 'user-avatar'">
            {{ p.userId < 0 ? '🤖' : (p.username || '用')[0] }}
          </div>
          <span class="participant-name">{{ p.username || `用户${p.userId}` }}</span>
          <div v-if="p.userId < 0" class="ai-actions">
            <button class="ai-btn" @click="handleEditAi(p)" title="编辑">✏️</button>
            <button class="ai-btn" @click="handleRemoveAi(p)" title="删除">🗑️</button>
          </div>
        </div>
      </div>
      <div class="participant-btns">
        <button class="action-btn invite-btn" @click="showInvite = true">+ 邀请好友</button>
        <button class="action-btn ai-btn-add" @click="showAddAi = true">+ 添加AI</button>
      </div>
    </div>

    <!-- 消息区域 -->
    <div class="messages-container" ref="msgContainer">
      <div v-if="messages.length === 0" class="empty-msg">
        <p>会议已开始，开始发言吧</p>
      </div>
      <div v-for="msg in messages" :key="msg.id"
           :class="['message-row', msg.senderName === '我' ? 'user-row' : 'ai-row']">
        <div :class="['bubble', msg.senderName === '我' ? 'user-bubble' : 'ai-bubble']">
          <div class="sender-name" v-if="msg.senderName !== '我'">{{ msg.senderName }}</div>
          <p>{{ msg.content }}</p>
          <div class="msg-actions" v-if="msg.senderName !== '我'">
            <button class="play-btn" @click="playTts(msg.content, msg.id)" :disabled="ttsLoading">
              {{ ttsLoading && currentPlayId === msg.id ? '⏳' : '🔊' }}
            </button>
          </div>
        </div>
      </div>
      <div v-if="aiTyping" class="message-row ai-row">
        <div class="bubble ai-bubble typing">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>

    <!-- 输入控制区 -->
    <div class="chat-controls">
      <div class="record-status">
        <span v-if="recordState === 'idle'">点击麦克风录音，或直接输入文字</span>
        <span v-else-if="recordState === 'recording'" class="recording-hint">🎤 录音中... 点击停止</span>
        <span v-else-if="recordState === 'processing'" class="processing-hint">🔄 处理中...</span>
        <span v-else-if="recordState === 'error'" class="error-hint">❌ 录音失败，请重试</span>
      </div>

      <button
        :class="['record-button', recordState === 'recording' ? 'recording' : '']"
        :disabled="recordState === 'processing'"
        @click="toggleRecording"
      >
        <span>{{ recordState === 'recording' ? '⏹️ 停止录音' : '🎤 开始录音' }}</span>
      </button>

      <div class="text-input-row">
        <input v-model="inputText" class="text-input" placeholder="输入消息..." @keyup.enter="sendMsg" />
        <button class="send-btn" @click="sendMsg" :disabled="!inputText.trim()">发送</button>
      </div>
    </div>

    <!-- 邀请好友弹窗 -->
    <div v-if="showInvite" class="modal-overlay" @click.self="showInvite = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>邀请好友</h3>
          <button class="close-btn" @click="showInvite = false">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="loadingFriends" class="loading-text">加载中...</div>
          <div v-else-if="friends.length === 0" class="empty-text">暂无好友</div>
          <div v-else class="friend-list">
            <div v-for="friend in friends" :key="friend.id" class="friend-item">
              <span>{{ friend.friendUsername || `用户${friend.friendId}` }}</span>
              <button class="invite-action-btn" @click="handleInviteFriend(friend.friendId)">邀请</button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 添加AI弹窗 -->
    <div v-if="showAddAi" class="modal-overlay" @click.self="showAddAi = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>添加AI助手</h3>
          <button class="close-btn" @click="showAddAi = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="tab-bar">
            <button :class="['tab-btn', aiTabActive === 0 ? 'active' : '']" @click="aiTabActive = 0">预设角色</button>
            <button :class="['tab-btn', aiTabActive === 1 ? 'active' : '']" @click="aiTabActive = 1">自定义</button>
          </div>
          <div v-if="aiTabActive === 0">
            <div v-if="loadingRoles" class="loading-text">加载中...</div>
            <div v-else class="role-list">
              <div v-for="role in aiRoles" :key="role.id" class="role-item" @click="handleSelectRole(role)">
                <div>
                  <div class="role-name">{{ role.name }}</div>
                  <div class="role-desc">{{ role.description }}</div>
                </div>
                <button class="invite-action-btn">选择</button>
              </div>
            </div>
          </div>
          <div v-else class="custom-ai-form">
            <div class="form-group">
              <label>AI名称</label>
              <input v-model="customAi.name" class="form-input" placeholder="输入AI名称" />
            </div>
            <div class="form-group">
              <label>角色设定</label>
              <textarea v-model="customAi.setting" class="form-textarea" rows="4"
                placeholder="描述AI的性格、背景等&#10;例如: 你是一个友善的英语老师..."></textarea>
            </div>
            <button class="submit-btn" @click="handleCreateCustomAi">创建并添加</button>
          </div>
        </div>
      </div>
    </div>

    <!-- 编辑AI弹窗 -->
    <div v-if="showEditAi" class="modal-overlay" @click.self="showEditAi = false">
      <div class="modal-card">
        <div class="modal-header">
          <h3>编辑AI</h3>
          <button class="close-btn" @click="showEditAi = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>名称</label>
            <input v-model="editingAi.aiRoleName" class="form-input" placeholder="AI名称" />
          </div>
          <div class="form-group">
            <label>角色设定</label>
            <textarea v-model="editingAi.aiRoleSetting" class="form-textarea" rows="4"
              placeholder="描述AI的性格、背景等"></textarea>
          </div>
          <button class="submit-btn" @click="handleSaveAi">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMeeting, leaveMeeting, inviteFriend, addAiParticipant, removeParticipant, updateAiParticipant, getAiRoles, getFriends } from '@/api/meeting'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const meetingId = route.params.id

const meeting = ref(null)
const participants = ref([])
const messages = ref([])
const inputText = ref('')
const msgContainer = ref(null)
const aiTyping = ref(false)
const autoPlay = ref(true)
const ttsVoice = ref('female_us')
const ttsLoading = ref(false)
const currentPlayId = ref(null)
const recordState = ref('idle')

const showInvite = ref(false)
const showAddAi = ref(false)
const showEditAi = ref(false)
const friends = ref([])
const loadingFriends = ref(false)
const aiRoles = ref([])
const loadingRoles = ref(false)
const aiTabActive = ref(0)
const customAi = reactive({ name: '', setting: '' })
const editingAi = reactive({ id: null, aiRoleName: '', aiRoleSetting: '' })

let ws = null
let mediaRecorder = null
let audioChunks = []
let currentAudio = null

const scrollToBottom = () => {
  nextTick(() => {
    if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight
  })
}

// ===== WebSocket =====
const connectWs = () => {
  const token = auth.token
  const wsUrl = `ws://localhost:8083/ws/meeting/${meetingId}?token=${token}`
  ws = new WebSocket(wsUrl)
  ws.onopen = () => console.log('WebSocket已连接')
  ws.onmessage = (e) => {
    try {
      const data = JSON.parse(e.data)
      const msg = {
        id: data.id || Date.now(),
        senderName: data.senderName || data.sender || '用户',
        content: data.content || data.message || ''
      }
      messages.value.push(msg)
      scrollToBottom()
      // AI消息自动播放TTS
      if (autoPlay.value && msg.senderName !== '我' && msg.content) {
        playTts(msg.content, msg.id)
      }
    } catch (err) {
      console.error('解析消息失败:', err)
    }
  }
  ws.onerror = (e) => console.error('WebSocket错误:', e)
  ws.onclose = () => console.log('WebSocket已断开')
}

const sendMsg = () => {
  const text = inputText.value.trim()
  if (!text) return
  const msg = { id: Date.now(), senderName: '我', content: text }
  messages.value.push(msg)
  inputText.value = ''
  scrollToBottom()
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'chat', content: text, meetingId }))
  }
}

// ===== TTS =====
const playTts = async (text, msgId = null) => {
  if (ttsLoading.value) return
  if (currentAudio) { currentAudio.pause(); currentAudio = null }
  ttsLoading.value = true
  currentPlayId.value = msgId
  try {
    const token = auth.token
    const res = await fetch('/api/ai/tts/speak', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {})
      },
      body: JSON.stringify({ text, voice: ttsVoice.value, rate: '+0%' })
    })
    if (!res.ok) throw new Error('TTS失败')
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    currentAudio = new Audio(url)
    currentAudio.onended = () => { ttsLoading.value = false; currentPlayId.value = null }
    currentAudio.onerror = () => { ttsLoading.value = false; currentPlayId.value = null }
    await currentAudio.play()
  } catch (e) {
    ttsLoading.value = false
    currentPlayId.value = null
    const utter = new SpeechSynthesisUtterance(text)
    utter.lang = 'en-US'
    window.speechSynthesis.speak(utter)
  }
}

// ===== 录音 =====
const toggleRecording = async () => {
  if (recordState.value === 'recording') stopRecording()
  else await startRecording()
}

const startRecording = async () => {
  try {
    const stream = await navigator.mediaDevices.getUserMedia({
      audio: { sampleRate: 16000, channelCount: 1, echoCancellation: true, noiseSuppression: true }
    })
    audioChunks = []
    mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' })
    mediaRecorder.ondataavailable = e => { if (e.data.size > 0) audioChunks.push(e.data) }
    mediaRecorder.onstop = handleRecordStop
    mediaRecorder.start(100)
    recordState.value = 'recording'
  } catch (e) {
    recordState.value = 'error'
    setTimeout(() => { recordState.value = 'idle' }, 3000)
  }
}

const stopRecording = () => {
  if (mediaRecorder && recordState.value === 'recording') {
    mediaRecorder.stop()
    mediaRecorder.stream.getTracks().forEach(t => t.stop())
    recordState.value = 'processing'
  }
}

const handleRecordStop = async () => {
  try {
    // TODO: 接入 STT 后自动识别并发送
    recordState.value = 'idle'
    const msg = { id: Date.now(), senderName: '我', content: '[语音消息已录制，STT接口接入后自动识别]' }
    messages.value.push(msg)
    scrollToBottom()
  } catch (e) {
    recordState.value = 'error'
    setTimeout(() => { recordState.value = 'idle' }, 3000)
  }
}

// ===== 会议操作 =====
const handleLeave = async () => {
  try {
    await leaveMeeting(meetingId)
  } catch (e) {}
  if (ws) ws.close()
  router.back()
}

const loadMeeting = async () => {
  try {
    const data = await getMeeting(meetingId)
    meeting.value = data
    participants.value = data?.participants || []
  } catch (e) {}
}

const loadFriends = async () => {
  loadingFriends.value = true
  try {
    const data = await getFriends()
    friends.value = Array.isArray(data) ? data : (data?.content || data?.records || [])
  } finally {
    loadingFriends.value = false
  }
}

const loadAiRoles = async () => {
  loadingRoles.value = true
  try {
    const data = await getAiRoles()
    aiRoles.value = Array.isArray(data) ? data : []
  } finally {
    loadingRoles.value = false
  }
}

const handleInviteFriend = async (friendId) => {
  try {
    await inviteFriend(meetingId, friendId)
    showInvite.value = false
    await loadMeeting()
  } catch (e) {}
}

const handleSelectRole = async (role) => {
  try {
    await addAiParticipant(meetingId, role.id, role.name)
    showAddAi.value = false
    await loadMeeting()
  } catch (e) {}
}

const handleCreateCustomAi = async () => {
  if (!customAi.name || !customAi.setting) return
  try {
    await addAiParticipant(meetingId, null, customAi.name)
    showAddAi.value = false
    customAi.name = ''
    customAi.setting = ''
    await loadMeeting()
  } catch (e) {}
}

const handleEditAi = (ai) => {
  editingAi.id = ai.id
  editingAi.aiRoleName = ai.aiRoleName || ai.username
  editingAi.aiRoleSetting = ai.aiRoleSetting || ''
  showEditAi.value = true
}

const handleSaveAi = async () => {
  try {
    await updateAiParticipant(meetingId, editingAi.id, {
      aiRoleName: editingAi.aiRoleName,
      aiRoleSetting: editingAi.aiRoleSetting
    })
    showEditAi.value = false
    await loadMeeting()
  } catch (e) {}
}

const handleRemoveAi = async (ai) => {
  if (!confirm(`确定要移除 ${ai.username || 'AI'} 吗?`)) return
  try {
    await removeParticipant(meetingId, ai.id)
    await loadMeeting()
  } catch (e) {}
}

onMounted(async () => {
  await loadMeeting()
  connectWs()
  loadFriends()
  loadAiRoles()
})

onUnmounted(() => {
  if (ws) ws.close()
  if (currentAudio) currentAudio.pause()
})
</script>

<style scoped>
.meeting-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0f172a;
  color: #f8fafc;
  font-family: 'Segoe UI', sans-serif;
}

.meeting-header {
  padding: 14px 20px;
  border-bottom: 1px solid #334155;
  background: #0f172a;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
}

.back-btn {
  background: #ef4444;
  border: none;
  color: #fff;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  transition: background 0.2s;
}
.back-btn:hover { background: #dc2626; }

.meeting-title { text-align: center; }
.meeting-title h3 { font-size: 16px; margin: 0 0 2px; }
.meeting-status { color: #10b981; font-size: 12px; font-weight: 600; }

.header-actions { display: flex; align-items: center; gap: 8px; }
.btn-icon {
  background: #1e293b;
  border: 1px solid #334155;
  color: #94a3b8;
  padding: 6px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 16px;
  transition: all 0.2s;
}
.btn-icon.active { color: #06b6d4; border-color: #06b6d4; }
.voice-select {
  background: #1e293b;
  border: 1px solid #334155;
  color: #f8fafc;
  padding: 6px 8px;
  border-radius: 6px;
  font-size: 12px;
  cursor: pointer;
}

/* 参与者栏 */
.participants-bar {
  padding: 12px 20px;
  border-bottom: 1px solid #334155;
  background: #1e293b;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;
  gap: 12px;
}
.participants-list {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  flex: 1;
}
.participant-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.avatar {
  width: 40px; height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
}
.user-avatar { background: #3b82f6; }
.ai-avatar { background: #8b5cf6; }
.participant-name { font-size: 11px; color: #94a3b8; max-width: 50px; text-align: center; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-actions { display: flex; gap: 2px; }
.ai-btn {
  background: none;
  border: none;
  cursor: pointer;
  font-size: 12px;
  padding: 2px;
  border-radius: 3px;
  transition: background 0.2s;
}
.ai-btn:hover { background: #334155; }

.participant-btns { display: flex; flex-direction: column; gap: 6px; flex-shrink: 0; }
.action-btn {
  padding: 6px 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s;
  white-space: nowrap;
}
.invite-btn { background: #3b82f6; color: #fff; }
.invite-btn:hover { background: #2563eb; }
.ai-btn-add { background: #8b5cf6; color: #fff; }
.ai-btn-add:hover {
 background: #7c3aed; }

/* 消息区域 */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  background: #0f172a;
}
.empty-msg { text-align: center; color: #64748b; padding: 40px 0; }
.message-row { display: flex; margin-bottom: 14px; }
.user-row { justify-content: flex-end; }
.ai-row { justify-content: flex-start; }
.bubble {
  max-width: 72%;
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}
.user-bubble { background: #3b82f6; color: #fff; border-bottom-right-radius: 4px; }
.ai-bubble { background: #1e293b; border: 1px solid #334155; color: #f8fafc; border-bottom-left-radius: 4px; }
.sender-name { font-size: 11px; color: #06b6d4; font-weight: 600; margin-bottom: 4px; }
.bubble p { margin: 0 0 4px; }
.msg-actions { margin-top: 6px; }
.play-btn {
  background: none; border: none; cursor: pointer;
  font-size: 14px; padding: 2px 6px; border-radius: 4px;
  color: #94a3b8; transition: color 0.2s;
}
.play-btn:hover { color: #06b6d4; }
.play-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.typing { display: flex; align-items: center; gap: 4px; padding: 14px 18px; }
.typing span { width: 8px; height: 8px; background: #64748b; border-radius: 50%; animation: bounce 1.2s infinite; }
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce {
  0%, 80%, 100% { transform: scale(0.8); opacity: 0.5; }
  40% { transform: scale(1.2); opacity: 1; }
}

/* 输入区 */
.chat-controls {
  padding: 16px 20px;
  border-top: 1px solid #334155;
  background: #0f172a;
  flex-shrink: 0;
}
.record-status { text-align: center; font-size: 13px; color: #94a3b8; margin-bottom: 12px; min-height: 18px; }
.recording-hint { color: #ef4444; }
.processing-hint { color: #f59e0b; }
.error-hint { color: #ef4444; }
.record-button {
  display: flex; align-items: center; justify-content: center;
  width: 100%; padding: 14px;
  background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: #fff; border: none; border-radius: 50px;
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: all 0.3s; margin-bottom: 12px;
}
.record-button.recording { background: linear-gradient(135deg, #ef4444, #f97316); animation: pulse 1.5s infinite; }
.record-button:disabled { background: #334155; cursor: not-allowed; }
.record-button:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(59,130,246,0.4); }
@keyframes pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.03); } }

.text-input-row { display: flex; gap: 8px; }
.text-input {
  flex: 1; padding: 10px 14px;
  background: #1e293b; border: 1px solid #334155;
  border-radius: 8px; color: #f8fafc; font-size: 14px; outline: none;
  transition: border-color 0.2s;
}
.text-input:focus { border-color: #3b82f6; }
.text-input::placeholder { color: #64748b; }
.send-btn {
  padding: 10px 18px; background: #3b82f6; color: #fff;
  border: none; border-radius: 8px; cursor: pointer;
  font-size: 14px; font-weight: 600; transition: background 0.2s;
}
.send-btn:hover:not(:disabled) { background: #2563eb; }
.send-btn:disabled { background: #334155; cursor: not-allowed; }

/* 弹窗 */
.modal-overlay {
  position: fixed; inset: 0;
  background: rgba(0,0,0,0.6);
  display: flex; align-items: flex-end; justify-content: center;
  z-index: 100;
}
.modal-card {
  background: #1e293b;
  border: 1px solid #334155;
  border-radius: 16px 16px 0 0;
  width: 100%; max-width: 600px;
  max-height: 70vh;
  display: flex; flex-direction: column;
}
.modal-header {
  padding: 20px 20px 16px;
  border-bottom: 1px solid #334155;
  display: flex; justify-content: space-between; align-items: center;
  flex-shrink: 0;
}
.modal-header h3 { font-size: 16px; margin: 0; }
.close-btn {
  background: none; border: none; color: #94a3b8;
  font-size: 18px; cursor: pointer; padding: 4px;
  transition: color 0.2s;
}
.close-btn:hover { color: #f8fafc; }
.modal-body { padding: 16px 20px; overflow-y: auto; flex: 1; }
.loading-text { text-align: center; color: #64748b; padding: 20px; }
.empty-text { text-align: center; color: #64748b; padding: 20px; }

.friend-list, .role-list { display: flex; flex-direction: column; gap: 8px; }
.friend-item, .role-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 16px;
  background: #0f172a; border: 1px solid #334155; border-radius: 8px;
  cursor: pointer; transition: border-color 0.2s;
}
.friend-item:hover, .role-item:hover { border-color: #3b82f6; }
.role-name { font-size: 14px; font-weight: 600; margin-bottom: 2px; }
.role-desc { font-size: 12px; color: #94a3b8; }
.invite-action-btn {
  padding: 6px 14px; background: #3b82f6; color: #fff;
  border: none; border-radius: 6px; cursor: pointer;
  font-size: 12px; font-weight: 600; flex-shrink: 0;
  transition: background 0.2s;
}
.invite-action-btn:hover { background: #2563eb; }

.tab-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.tab-btn {
  flex: 1; padding: 8px; background: #0f172a;
  border: 1px solid #334155; border-radius: 8px;
  color: #94a3b8; cursor: pointer; font-size: 14px;
  transition: all 0.2s;
}
.tab-btn.active { background: #3b82f6; border-color: #3b82f6; color: #fff; }

.custom-ai-form { display: flex; flex-direction: column; gap: 14px; }
.form-group { display: flex; flex-direction: column; gap: 6px; }
.form-group label { font-size: 13px; color: #94a3b8; }
.form-input, .form-textarea {
  padding: 10px 14px;
  background: #0f172a; border: 1px solid #334155;
  border-radius: 8px; color: #f8fafc; font-size: 14px; outline: none;
  transition: border-color 0.2s; resize: vertical;
}
.form-input:focus, .form-textarea:focus { border-color: #3b82f6; }
.submit-btn {
  padding: 12px; background: linear-gradient(135deg, #3b82f6, #06b6d4);
  color: #fff; border: none; border-radius: 8px;
  font-size: 15px; font-weight: 600; cursor: pointer;
  transition: opacity 0.2s;
}
.submit-btn:hover { opacity: 0.9; }
</style>
