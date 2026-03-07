<template>
  <div class="chat-page">
    <div class="chat-header">
      <button class="back-btn" @click="$router.back()">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
          <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
        </svg>
        返回
      </button>
      <div class="chat-info">
        <h3>{{ sessionInfo?.themeName || '对话练习' }}</h3>
        <span class="chat-level">{{ sessionInfo?.roleName || 'AI助手' }}</span>
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

    <div ref="msgContainer" class="messages-container">
      <div v-if="messages.length === 0" class="message-row ai-row">
        <div class="bubble ai-bubble">
          <p>Hello! I'm your English practice assistant. Let's start our conversation!</p>
        </div>
      </div>
      <div v-for="msg in messages" :key="msg.id" :class="['message-row', msg.sender === 'user' ? 'user-row' : 'ai-row']">
        <div :class="['bubble', msg.sender === 'user' ? 'user-bubble' : 'ai-bubble']">
          <p>{{ msg.content }}</p>
          <div v-if="msg.feedback" class="feedback-hint">📝 {{ msg.feedback }}</div>
          <div class="msg-actions" v-if="msg.sender === 'ai'">
            <button class="play-btn" @click="playTts(msg.content, msg.id)" :disabled="ttsLoading">
              {{ ttsLoading && currentPlayId === msg.id ? '⏳' : '🔊' }}
            </button>
          </div>
        </div>
      </div>
      <div v-if="aiTyping" class="message-row ai-row">
        <div class="bubble ai-bubble typing"><span></span><span></span><span></span></div>
      </div>
    </div>

    <div class="chat-controls">
      <div class="record-status">
        <span v-if="recordState === 'idle'">点击麦克风开始录音，或直接输入文字</span>
        <span v-else-if="recordState === 'recording'" class="recording-hint">🎤 录音中... 点击停止</span>
        <span v-else-if="recordState === 'processing'" class="processing-hint">🔄 处理中...</span>
        <span v-else-if="recordState === 'error'" class="error-hint">❌ 录音失败，请重试</span>
      </div>
      <button :class="['record-button', recordState === 'recording' ? 'recording' : '']"
              :disabled="recordState === 'processing'" @click="toggleRecording">
        <span>{{ recordState === 'recording' ? '⏹️ 停止录音' : '🎤 开始录音' }}</span>
      </button>
      <div class="text-input-row">
        <input v-model="inputText" class="text-input" placeholder="或直接输入英语..." @keyup.enter="sendText" />
        <button class="send-btn" @click="sendText" :disabled="!inputText.trim()">发送</button>
      </div>
      <div class="tts-controls">
        <button class="btn-secondary" @click="repeatLast" :disabled="!lastAiMsg">🔄 重复上条</button>
        <button class="btn-secondary" @click="cycleSpeed">{{ speedLabels[speedIdx] }}</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { getSession, getMessages, sendMessage } from '@/api/practice'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const auth = useAuthStore()
const sessionId = route.params.sessionId

const sessionInfo = ref(null)
const messages = ref([])
const inputText = ref('')
const aiTyping = ref(false)
const msgContainer = ref(null)
const autoPlay = ref(true)
const ttsVoice = ref('female_us')
const ttsLoading = ref(false)
const currentPlayId = ref(null)
const recordState = ref('idle')
const speedIdx = ref(0)
const speedLabels = ['🐢 慢速', '👤 正常', '🐇 快速']
const speedRates = ['-20%', '+0%', '+20%']

let mediaRecorder = null
let audioChunks = []
let currentAudio = null

const lastAiMsg = computed(() => {
  const aiMsgs = messages.value.filter(m => m.sender === 'ai')
  return aiMsgs.length ? aiMsgs[aiMsgs.length - 1] : null
})

const scrollToBottom = () => {
  nextTick(() => { if (msgContainer.value) msgContainer.value.scrollTop = msgContainer.value.scrollHeight })
}

const playTts = async (text, msgId = null) => {
  if (ttsLoading.value) return
  if (currentAudio) { currentAudio.pause(); currentAudio = null }
  ttsLoading.value = true
  currentPlayId.value = msgId
  try {
    const token = auth.token
    const res = await fetch('/api/ai/tts/speak', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify({ text, voice: ttsVoice.value, rate: speedRates[speedIdx.value] })
    })
    if (!res.ok) throw new Error('TTS failed')
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

const repeatLast = () => { if (lastAiMsg.value) playTts(lastAiMsg.value.content, lastAiMsg.value.id) }
const cycleSpeed = () => { speedIdx.value = (speedIdx.value + 1) % speedRates.length }

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
    // 录音完成，目前提示用户（STT接入后可自动识别）
    recordState.value = 'idle'
    messages.value.push({ id: Date.now(), sender: 'user', content: '[语音消息 - STT接入后自动识别]' })
    scrollToBottom()
  } catch (e) {
    recordState.value = 'error'
    setTimeout(() => { recordState.value = 'idle' }, 3000)
  }
}

const sendText = async () => {
  const text = inputText.value.trim()
  if (!text) return
  messages.value.push({ id: Date.now(), sender: 'user', content: text })
  inputText.value = ''
  scrollToBottom()
  aiTyping.value = true
  try {
    const reply = await sendMessage(sessionId, { content: text, type: 'text' })
    aiTyping.value = false
    if (reply) {
      const aiMsg = { id: reply.id || Date.now() + 1, sender: 'ai', content: reply.content || reply.text || '', feedback: reply.feedback || null }
      messages.value.push(aiMsg)
      scrollToBottom()
      if (autoPlay.value && aiMsg.content) playTts(aiMsg.content, aiMsg.id)
    }
  } catch (e) { aiTyping.value = false }
}

onMounted(async () => {
  try { sessionInfo.value = await getSession(sessionId) } catch (e) {}
  try {
    const data = await getMessages(sessionId)
    messages.value = data?.records || (Array.isArray(data) ? data : data?.content || [])
    scrollToBottom()
  } catch (e) {}
})
</script>

<style scoped>
.chat-page { height: 100vh; display: flex; flex-direction: column; background: #0f172a; color: #f8fafc; font-family: 'Segoe UI', sans-serif; }
.chat-header { padding: 14px 20px; border-bottom: 1px solid #334155; background: #0f172a; display: flex; justify-content: space-between; align-items: center; flex-shrink: 0; }
.back-btn { background: #1e293b; border: 1px solid #334155; color: #f8fafc; padding: 8px 14px; border-radius: 8px; cursor: pointer; display: flex; align-items: center; gap: 6px; font-size: 14px; transition: background 0.2s; }
.back-btn:hover { background: #3b82f6; }
.chat-info { text-align: center; }
.chat-info h3 { font-size: 16px; margin: 0 0 2px; }
.chat-level { color: #06b6d4; font-size: 12px; font-weight: 600; }
.header-actions { display: flex; align-items: center; gap: 8px; }
.btn-icon { background: #1e293b; border: 1px solid #334155; color: #94a3b8; padding: 6px 10px; border-radius: 6px; cursor: pointer; font-size: 16px; transition: all 0.2s; }
.btn-icon.active { color: #06b6d4; border-color: #06b6d4; }
.voice-select { background: #1e293b; border: 1px solid #334155; color: #f8fafc; padding: 6px 8px; border-radius: 6px; font-size: 12px; cursor: pointer; }
.messages-container { flex: 1; overflow-y: auto; padding: 16px 20px; background: #0f172a; }
.message-row { display: flex; margin-bottom: 14px; }
.user-row { justify-content: flex-end; }
.ai-row { justify-content: flex-start; }
.bubble { max-width: 72%; padding: 12px 16px; border-radius: 16px; font-size: 14px; line-height: 1.6; word-break: break-word; }
.user-bubble { background: #3b82f6; color: #fff; border-bottom-right-radius: 4px; }
.ai-bubble { background: #1e293b; border: 1px solid #334155; color: #f8fafc; border-bottom-left-radius: 4px; }
.bubble p { margin: 0 0 4px; }
.feedback-hint { margin-top: 8px; padding: 6px 10px; background: rgba(16,185,129,0.1); border-left: 3px solid #10b981; border-radius: 4px; font-size: 12px; color: #6ee7b7; }
.msg-actions { margin-top: 6px; }
.play-btn { background: none; border: none; cursor: pointer; font-size: 14px; padding: 2px 6px; border-radius: 4px; color: #94a3b8; transition: color 0.2s; }
.play-btn:hover { color: #06b6d4; }
.play-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.typing { display: flex; align-items: center; gap: 4px; padding: 14px 18px; }
.typing span { width: 8px; height: 8px; background: #64748b; border-radius: 50%; animation: bounce 1.2s infinite; }
.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }
@keyframes bounce { 0%,80%,100% { transform: scale(0.8); opacity: 0.5; } 40% { transform: scale(1.2); opacity: 1; } }
.chat-controls { padding: 14px 20px; border-top: 1px solid #334155; background: #0f172a; flex-shrink: 0; }
.record-status { text-align: center; font-size: 13px; color: #94a3b8; margin-bottom: 10px; min-height: 18px; }
.recording-hint { color: #ef4444; }
.processing-hint { color: #f59e0b; }
.error-hint { color: #ef4444; }
.record-button { display: flex; align-items: center; justify-content: center; gap: 10px; width: 100%; padding: 14px; background: linear-gradient(135deg, #3b82f6, #06b6d4); color: #fff; border: none; border-radius: 50px; font-size: 15px; font-weight: 600; cursor: pointer; transition: all 0.3s; margin-bottom: 10px; }
.record-button.recording { background: linear-gradient(135deg, #ef4444, #f97316); animation: pulse 1.5s infinite; }
.record-button:disabled { background: #334155; cursor: not-allowed; }
.record-button:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 8px 20px rgba(59,130,246,0.4); }
@keyframes pulse { 0%,100% { transform: scale(1); } 50% { transform: scale(1.03); } }
.text-input-row { display: flex; gap: 8px; margin-bottom: 10px; }
.text-input { flex: 1; padding: 10px 14px; background: #1e293b; border: 1px solid #334155; border-radius: 8px; color: #f8fafc; font-size: 14px; outline: none; transition: border-color 0.2s; }
.text-input:focus { border-color: #3b82f6; }
.text-input::placeholder { color: #64748b; }
.send-btn { padding: 10px 18px; background: #3b82f6; color: #fff; border: none; border-radius: 8px; cursor: pointer; font-size: 14px; font-weight: 600; transition: background 0.2s; }
.send-btn:hover:not(:disabled) { background: #2563eb; }
.send-btn:disabled { background: #334155; cursor: not-allowed; }
.tts-controls { display: flex; justify-content: center; gap: 10px; }
.btn-secondary { background: #1e293b; border: 1px solid #334155; color: #cbd5e1; padding: 8px 16px; border-radius: 8px; cursor: pointer; font-size: 13px; transition: all 0.2s; }
.btn-secondary:hover:not(:disabled) { background: #334155; }
.btn-secondary:disabled { opacity: 0.5; cursor: not-allowed; }
</style>