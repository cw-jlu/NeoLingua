/**
 * 高级功能API客户端
 * 提供纠错反馈、实时交互、评估评分等高级功能的前端接口
 */
import axios from 'axios'

const API_BASE = process.env.VUE_APP_AI_SERVICE_URL || 'http://localhost:8001'

// 创建axios实例
const apiClient = axios.create({
  baseURL: `${API_BASE}/ai/advanced`,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器 - 添加认证token
apiClient.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器 - 统一错误处理
apiClient.interceptors.response.use(
  response => response.data,
  error => {
    console.error('API Error:', error.response?.data || error.message)
    return Promise.reject(error.response?.data || error)
  }
)

export const advancedFeaturesAPI = {
  // ==================== 发音评估 ====================
  
  /**
   * 高级发音评估
   * @param {string} userId 用户ID
   * @param {string} sessionId 会话ID
   * @param {File} audioFile 音频文件
   * @param {string} text 要评估的文本
   * @param {Object} userProfile 用户画像
   */
  async assessPronunciation(userId, sessionId, audioFile, text, userProfile = null) {
    const formData = new FormData()
    formData.append('audio_file', audioFile)
    formData.append('text', text)
    if (userProfile) {
      formData.append('user_profile', JSON.stringify(userProfile))
    }
    
    return apiClient.post(`/pronunciation/assess`, formData, {
      params: { user_id: userId, session_id: sessionId },
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  // ==================== 实时交互 ====================
  
  /**
   * 启动实时对话会话
   * @param {string} userId 用户ID
   * @param {string} sessionId 会话ID
   * @param {string} mode 交互模式
   * @param {Object} userProfile 用户画像
   */
  async startRealtimeSession(userId, sessionId, mode = 'half_duplex', userProfile = null) {
    return apiClient.post('/realtime/start', {
      mode,
      user_profile: userProfile
    }, {
      params: { user_id: userId, session_id: sessionId }
    })
  },

  /**
   * 处理实时音频流
   * @param {string} sessionId 会话ID
   * @param {Object} audioFeatures 音频特征
   */
  async processRealtimeAudio(sessionId, audioFeatures) {
    return apiClient.post('/realtime/audio', audioFeatures, {
      params: { session_id: sessionId }
    })
  },

  /**
   * 结束实时对话会话
   * @param {string} sessionId 会话ID
   */
  async endRealtimeSession(sessionId) {
    return apiClient.post('/realtime/end', {}, {
      params: { session_id: sessionId }
    })
  },

  /**
   * 获取会话状态
   * @param {string} sessionId 会话ID
   */
  async getSessionStatus(sessionId) {
    return apiClient.get(`/session/status/${sessionId}`)
  },

  /**
   * 更新交互模式
   * @param {string} sessionId 会话ID
   * @param {string} newMode 新的交互模式
   */
  async updateInteractionMode(sessionId, newMode) {
    return apiClient.put(`/session/mode/${sessionId}`, {}, {
      params: { new_mode: newMode }
    })
  },

  // ==================== 评估评分 ====================
  
  /**
   * 口语表现评估
   * @param {string} userId 用户ID
   * @param {string} sessionId 会话ID
   * @param {string} userMessage 用户消息
   * @param {Object} audioAnalysis 音频分析结果
   * @param {string} assessmentType 评估类型
   */
  async assessSpeakingPerformance(userId, sessionId, userMessage, audioAnalysis = null, assessmentType = 'ielts') {
    return apiClient.post('/assessment/speaking', {
      user_message: userMessage,
      audio_analysis: audioAnalysis,
      assessment_type: assessmentType
    }, {
      params: { user_id: userId, session_id: sessionId }
    })
  },

  /**
   * 获取学习进度追踪
   * @param {string} userId 用户ID
   * @param {number} days 天数
   */
  async getProgressTracking(userId, days = 30) {
    return apiClient.get(`/progress/${userId}`, {
      params: { days }
    })
  },

  /**
   * 批量评估
   * @param {string} userId 用户ID
   * @param {Array} sessions 会话ID列表
   * @param {string} assessmentType 评估类型
   */
  async batchAssessment(userId, sessions, assessmentType = 'ielts') {
    return apiClient.post('/batch/assess', {
      sessions,
      assessment_type: assessmentType
    }, {
      params: { user_id: userId }
    })
  },

  /**
   * 获取分析概览
   * @param {string} userId 用户ID
   * @param {number} days 天数
   */
  async getAnalyticsOverview(userId, days = 30) {
    return apiClient.get(`/analytics/overview/${userId}`, {
      params: { days }
    })
  },

  // ==================== 智能辅助 ====================
  
  /**
   * 检测中式英语
   * @param {string} text 要检测的文本
   */
  async detectChinglish(text) {
    return apiClient.post('/chinglish/detect', { text })
  },

  /**
   * 生成引导式补全帮助
   * @param {string} userId 用户ID
   * @param {string} sessionId 会话ID
   * @param {string} incompleteText 不完整的文本
   * @param {Object} userProfile 用户画像
   */
  async generateCompletionHelp(userId, sessionId, incompleteText, userProfile = null) {
    return apiClient.post('/completion/help', {
      incomplete_text: incompleteText,
      user_profile: userProfile
    }, {
      params: { user_id: userId, session_id: sessionId }
    })
  }
}

// ==================== 实时音频处理工具 ====================

export class RealtimeAudioProcessor {
  constructor(sessionId, onAudioFeatures, onResponse) {
    this.sessionId = sessionId
    this.onAudioFeatures = onAudioFeatures
    this.onResponse = onResponse
    this.isProcessing = false
    this.audioContext = null
    this.analyser = null
    this.mediaStream = null
  }

  /**
   * 启动音频处理
   */
  async start() {
    try {
      this.mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true })
      this.audioContext = new (window.AudioContext || window.webkitAudioContext)()
      this.analyser = this.audioContext.createAnalyser()
      
      const source = this.audioContext.createMediaStreamSource(this.mediaStream)
      source.connect(this.analyser)
      
      this.analyser.fftSize = 256
      this.isProcessing = true
      
      this.processAudio()
    } catch (error) {
      console.error('启动音频处理失败:', error)
      throw error
    }
  }

  /**
   * 停止音频处理
   */
  stop() {
    this.isProcessing = false
    
    if (this.mediaStream) {
      this.mediaStream.getTracks().forEach(track => track.stop())
    }
    
    if (this.audioContext) {
      this.audioContext.close()
    }
  }

  /**
   * 处理音频数据
   */
  processAudio() {
    if (!this.isProcessing) return

    const bufferLength = this.analyser.frequencyBinCount
    const dataArray = new Uint8Array(bufferLength)
    this.analyser.getByteFrequencyData(dataArray)

    // 计算音频特征
    const audioFeatures = this.extractAudioFeatures(dataArray)
    
    // 调用回调函数
    if (this.onAudioFeatures) {
      this.onAudioFeatures(audioFeatures)
    }

    // 发送到服务器处理
    this.sendAudioFeatures(audioFeatures)

    // 继续处理
    requestAnimationFrame(() => this.processAudio())
  }

  /**
   * 提取音频特征
   */
  extractAudioFeatures(dataArray) {
    // 计算能量水平
    const sum = dataArray.reduce((a, b) => a + b, 0)
    const energyLevel = sum / (dataArray.length * 255)

    // 计算音调变化（简化版）
    const variance = this.calculateVariance(dataArray)
    const pitchVariance = Math.min(variance / 1000, 1)

    // 检测语音活动
    const voiceActivity = energyLevel > 0.1

    return {
      energy_level: energyLevel,
      pitch_variance: pitchVariance,
      speech_rate: 150, // 需要更复杂的算法计算
      pause_duration: voiceActivity ? 0 : 0.1,
      voice_activity: voiceActivity,
      confidence_score: 0.8
    }
  }

  /**
   * 计算方差
   */
  calculateVariance(array) {
    const mean = array.reduce((a, b) => a + b) / array.length
    const variance = array.reduce((a, b) => a + Math.pow(b - mean, 2), 0) / array.length
    return variance
  }

  /**
   * 发送音频特征到服务器
   */
  async sendAudioFeatures(audioFeatures) {
    try {
      const response = await advancedFeaturesAPI.processRealtimeAudio(
        this.sessionId, 
        audioFeatures
      )
      
      if (this.onResponse) {
        this.onResponse(response)
      }
    } catch (error) {
      console.error('发送音频特征失败:', error)
    }
  }
}

// ==================== 语音识别集成 ====================

export class SpeechRecognitionManager {
  constructor(onResult, onError) {
    this.onResult = onResult
    this.onError = onError
    this.recognition = null
    this.isListening = false
  }

  /**
   * 启动语音识别
   */
  start() {
    if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
      this.onError('浏览器不支持语音识别')
      return
    }

    const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
    this.recognition = new SpeechRecognition()
    
    this.recognition.continuous = true
    this.recognition.interimResults = true
    this.recognition.lang = 'en-US'

    this.recognition.onresult = (event) => {
      let finalTranscript = ''
      let interimTranscript = ''

      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript
        if (event.results[i].isFinal) {
          finalTranscript += transcript
        } else {
          interimTranscript += transcript
        }
      }

      if (this.onResult) {
        this.onResult({
          final: finalTranscript,
          interim: interimTranscript,
          confidence: event.results[event.results.length - 1][0].confidence
        })
      }
    }

    this.recognition.onerror = (event) => {
      if (this.onError) {
        this.onError(event.error)
      }
    }

    this.recognition.onend = () => {
      this.isListening = false
    }

    this.recognition.start()
    this.isListening = true
  }

  /**
   * 停止语音识别
   */
  stop() {
    if (this.recognition && this.isListening) {
      this.recognition.stop()
    }
  }
}

export default advancedFeaturesAPI