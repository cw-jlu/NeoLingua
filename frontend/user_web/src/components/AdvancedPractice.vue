<template>
  <div class="advanced-practice">
    <!-- 模式选择 -->
    <div class="mode-selector">
      <h3>选择练习模式</h3>
      <div class="mode-buttons">
        <button 
          v-for="mode in interactionModes" 
          :key="mode.value"
          :class="['mode-btn', { active: selectedMode === mode.value }]"
          @click="selectMode(mode.value)"
        >
          <i :class="mode.icon"></i>
          <span>{{ mode.label }}</span>
          <small>{{ mode.description }}</small>
        </button>
      </div>
    </div>

    <!-- 实时交互界面 -->
    <div v-if="sessionActive" class="interaction-panel">
      <!-- 状态显示 -->
      <div class="status-bar">
        <div class="status-item">
          <span class="label">模式:</span>
          <span class="value">{{ getCurrentModeLabel() }}</span>
        </div>
        <div class="status-item">
          <span class="label">状态:</span>
          <span :class="['value', sessionStatus.toLowerCase()]">{{ sessionStatus }}</span>
        </div>
        <div class="status-item">
          <span class="label">情感:</span>
          <span class="value">{{ emotionalState }}</span>
        </div>
      </div>

      <!-- 音频可视化 -->
      <div class="audio-visualizer">
        <canvas ref="visualizerCanvas" width="400" height="100"></canvas>
        <div class="audio-controls">
          <button 
            :class="['record-btn', { recording: isRecording }]"
            @click="toggleRecording"
          >
            <i :class="isRecording ? 'fas fa-stop' : 'fas fa-microphone'"></i>
            {{ isRecording ? '停止录音' : '开始录音' }}
          </button>
        </div>
      </div>

      <!-- 实时反馈 -->
      <div v-if="realtimeFeedback" class="realtime-feedback">
        <div class="feedback-header">
          <i class="fas fa-comments"></i>
          <span>实时反馈</span>
        </div>
        <div class="feedback-content">
          <div v-if="realtimeFeedback.type === 'help'" class="help-feedback">
            <p>{{ realtimeFeedback.message }}</p>
            <div v-if="realtimeFeedback.suggestions" class="suggestions">
              <span v-for="suggestion in realtimeFeedback.suggestions" 
                    :key="suggestion" 
                    class="suggestion-chip"
                    @click="useSuggestion(suggestion)">
                {{ suggestion }}
              </span>
            </div>
          </div>
          <div v-else-if="realtimeFeedback.type === 'correction'" class="correction-feedback">
            <div v-for="correction in realtimeFeedback.content" :key="correction.original">
              <span class="original">{{ correction.original }}</span>
              <i class="fas fa-arrow-right"></i>
              <span class="corrected">{{ correction.suggestion }}</span>
              <small class="explanation">{{ correction.explanation }}</small>
            </div>
          </div>
        </div>
      </div>

      <!-- 对话历史 -->
      <div class="conversation-history">
        <div v-for="message in conversationHistory" 
             :key="message.id" 
             :class="['message', message.role]">
          <div class="message-content">
            <p>{{ message.content }}</p>
            <div v-if="message.chinglish_detected" class="chinglish-indicator">
              <i class="fas fa-exclamation-triangle"></i>
              <span>检测到中式英语表达</span>
            </div>
          </div>
          <div class="message-meta">
            <span class="timestamp">{{ formatTime(message.timestamp) }}</span>
            <span v-if="message.confidence" class="confidence">
              置信度: {{ Math.round(message.confidence * 100) }}%
            </span>
          </div>
        </div>
      </div>

      <!-- 控制按钮 -->
      <div class="control-buttons">
        <button class="btn-secondary" @click="pauseSession">
          <i class="fas fa-pause"></i>
          暂停会话
        </button>
        <button class="btn-primary" @click="endSession">
          <i class="fas fa-stop"></i>
          结束会话
        </button>
      </div>
    </div>

    <!-- 会话结果 -->
    <div v-if="sessionResult" class="session-result">
      <h3>会话总结</h3>
      <div class="result-cards">
        <!-- 总体评分 -->
        <div class="result-card score-card">
          <h4>总体评分</h4>
          <div class="score-display">
            <div class="score-circle">
              <span class="score">{{ sessionResult.final_assessment?.overall_score || 0 }}</span>
              <span class="max-score">/10</span>
            </div>
            <div class="proficiency-level">
              {{ sessionResult.final_assessment?.proficiency_level || 'intermediate' }}
            </div>
          </div>
        </div>

        <!-- 维度评分 -->
        <div class="result-card dimensions-card">
          <h4>各维度表现</h4>
          <div class="dimensions-chart">
            <div v-for="(score, dimension) in sessionResult.final_assessment?.dimension_scores || {}" 
                 :key="dimension" 
                 class="dimension-item">
              <span class="dimension-name">{{ getDimensionLabel(dimension) }}</span>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: (score / 10 * 100) + '%' }"></div>
              </div>
              <span class="dimension-score">{{ score.toFixed(1) }}</span>
            </div>
          </div>
        </div>

        <!-- 改进建议 -->
        <div class="result-card suggestions-card">
          <h4>改进建议</h4>
          <ul class="suggestions-list">
            <li v-for="suggestion in sessionResult.final_assessment?.improvement_suggestions || []" 
                :key="suggestion">
              {{ suggestion }}
            </li>
          </ul>
        </div>
      </div>
    </div>

    <!-- 进度追踪 -->
    <div class="progress-section">
      <h3>学习进度</h3>
      <div v-if="progressData" class="progress-cards">
        <div class="progress-card">
          <h4>本月练习</h4>
          <div class="progress-stat">
            <span class="stat-number">{{ progressData.total_assessments }}</span>
            <span class="stat-label">次评估</span>
          </div>
        </div>
        <div class="progress-card">
          <h4>平均得分</h4>
          <div class="progress-stat">
            <span class="stat-number">{{ progressData.average_score?.toFixed(1) || 0 }}</span>
            <span class="stat-label">分</span>
          </div>
        </div>
        <div class="progress-card">
          <h4>进步趋势</h4>
          <div class="progress-stat">
            <span :class="['stat-number', progressData.trend]">
              {{ progressData.improvement > 0 ? '+' : '' }}{{ progressData.improvement?.toFixed(1) || 0 }}
            </span>
            <span class="stat-label">{{ progressData.trend }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
<script>
import { advancedFeaturesAPI, RealtimeAudioProcessor, SpeechRecognitionManager } from '@/api/advanced-features'

export default {
  name: 'AdvancedPractice',
  data() {
    return {
      // 交互模式
      interactionModes: [
        {
          value: 'half_duplex',
          label: '半双工模式',
          description: '轮流对话，适合大多数学习场景',
          icon: 'fas fa-exchange-alt'
        },
        {
          value: 'full_duplex',
          label: '全双工模式',
          description: '自然对话，可以同时说话',
          icon: 'fas fa-comments'
        },
        {
          value: 'guided',
          label: '引导模式',
          description: '系统引导，适合初学者',
          icon: 'fas fa-route'
        },
        {
          value: 'free_talk',
          label: '自由对话',
          description: '自由表达，最小干预',
          icon: 'fas fa-microphone-alt'
        }
      ],
      
      // 会话状态
      selectedMode: 'half_duplex',
      sessionActive: false,
      sessionId: null,
      sessionStatus: 'Ready',
      emotionalState: 'confident',
      
      // 音频处理
      isRecording: false,
      audioProcessor: null,
      speechRecognition: null,
      
      // 实时反馈
      realtimeFeedback: null,
      conversationHistory: [],
      
      // 会话结果
      sessionResult: null,
      progressData: null,
      
      // 用户配置
      userProfile: {
        level: 'intermediate',
        feedback_preference: 'adaptive'
      }
    }
  },
  
  mounted() {
    this.loadProgressData()
  },
  
  beforeUnmount() {
    this.cleanup()
  },
  
  methods: {
    // ==================== 模式选择 ====================
    
    selectMode(mode) {
      if (!this.sessionActive) {
        this.selectedMode = mode
      }
    },
    
    getCurrentModeLabel() {
      const mode = this.interactionModes.find(m => m.value === this.selectedMode)
      return mode ? mode.label : this.selectedMode
    },
    
    // ==================== 会话管理 ====================
    
    async startSession() {
      try {
        this.sessionId = `session_${Date.now()}`
        const userId = this.$store.state.user.id
        
        // 启动实时会话
        const response = await advancedFeaturesAPI.startRealtimeSession(
          userId, 
          this.sessionId, 
          this.selectedMode, 
          this.userProfile
        )
        
        this.sessionActive = true
        this.sessionStatus = 'Active'
        
        // 添加开场消息
        this.addMessage('system', response.opening_message)
        
        // 初始化音频处理
        this.initializeAudioProcessing()
        
        this.$message.success('会话已启动')
      } catch (error) {
        console.error('启动会话失败:', error)
        this.$message.error('启动会话失败')
      }
    },
    
    async endSession() {
      try {
        if (this.sessionId) {
          const response = await advancedFeaturesAPI.endRealtimeSession(this.sessionId)
          this.sessionResult = response
        }
        
        this.cleanup()
        this.sessionActive = false
        this.sessionStatus = 'Ended'
        
        // 重新加载进度数据
        this.loadProgressData()
        
        this.$message.success('会话已结束')
      } catch (error) {
        console.error('结束会话失败:', error)
        this.$message.error('结束会话失败')
      }
    },
    
    pauseSession() {
      this.sessionStatus = 'Paused'
      this.stopRecording()
    },
    
    // ==================== 音频处理 ====================
    
    initializeAudioProcessing() {
      // 初始化音频处理器
      this.audioProcessor = new RealtimeAudioProcessor(
        this.sessionId,
        this.onAudioFeatures,
        this.onRealtimeResponse
      )
      
      // 初始化语音识别
      this.speechRecognition = new SpeechRecognitionManager(
        this.onSpeechResult,
        this.onSpeechError
      )
    },
    
    async toggleRecording() {
      if (this.isRecording) {
        this.stopRecording()
      } else {
        this.startRecording()
      }
    },
    
    async startRecording() {
      try {
        await this.audioProcessor.start()
        this.speechRecognition.start()
        this.isRecording = true
        this.sessionStatus = 'Listening'
      } catch (error) {
        console.error('启动录音失败:', error)
        this.$message.error('启动录音失败')
      }
    },
    
    stopRecording() {
      if (this.audioProcessor) {
        this.audioProcessor.stop()
      }
      if (this.speechRecognition) {
        this.speechRecognition.stop()
      }
      this.isRecording = false
      this.sessionStatus = 'Processing'
    },
    
    // ==================== 回调处理 ====================
    
    onAudioFeatures(features) {
      // 更新音频可视化
      this.updateAudioVisualization(features)
    },
    
    onRealtimeResponse(response) {
      console.log('实时响应:', response)
      
      // 处理不同类型的响应
      switch (response.action) {
        case 'system_turn':
          if (response.system_response) {
            this.addMessage('system', response.system_response.content)
          }
          break
          
        case 'provide_help':
          this.realtimeFeedback = response.system_response
          break
          
        case 'provide_guidance':
          this.realtimeFeedback = {
            type: 'help',
            message: '需要帮助吗？',
            suggestions: response.guidance?.suggestions || []
          }
          break
      }
      
      // 更新情感状态
      if (response.emotional_state) {
        this.emotionalState = response.emotional_state
      }
    },
    
    onSpeechResult(result) {
      if (result.final) {
        // 添加用户消息
        this.addMessage('user', result.final, result.confidence)
        
        // 检测中式英语
        this.detectChinglish(result.final)
      }
    },
    
    onSpeechError(error) {
      console.error('语音识别错误:', error)
      this.$message.warning('语音识别出现问题')
    },
    
    // ==================== 消息处理 ====================
    
    addMessage(role, content, confidence = null) {
      const message = {
        id: Date.now(),
        role,
        content,
        confidence,
        timestamp: new Date(),
        chinglish_detected: false
      }
      
      this.conversationHistory.push(message)
      
      // 滚动到底部
      this.$nextTick(() => {
        const container = this.$el.querySelector('.conversation-history')
        if (container) {
          container.scrollTop = container.scrollHeight
        }
      })
    },
    
    async detectChinglish(text) {
      try {
        const result = await advancedFeaturesAPI.detectChinglish(text)
        if (result.has_chinglish) {
          // 标记最后一条用户消息
          const lastMessage = this.conversationHistory[this.conversationHistory.length - 1]
          if (lastMessage && lastMessage.role === 'user') {
            lastMessage.chinglish_detected = true
          }
          
          // 显示纠错反馈
          this.realtimeFeedback = {
            type: 'correction',
            content: result.patterns.map(p => ({
              original: p.pattern,
              suggestion: p.suggestion,
              explanation: '更自然的表达方式'
            }))
          }
        }
      } catch (error) {
        console.error('中式英语检测失败:', error)
      }
    },
    
    useSuggestion(suggestion) {
      // 使用建议的表达
      this.addMessage('user', suggestion)
      this.realtimeFeedback = null
    },
    
    // ==================== 数据加载 ====================
    
    async loadProgressData() {
      try {
        const userId = this.$store.state.user?.id
        if (userId) {
          this.progressData = await advancedFeaturesAPI.getProgressTracking(userId, 30)
        }
      } catch (error) {
        console.error('加载进度数据失败:', error)
      }
    },
    
    // ==================== 工具方法 ====================
    
    updateAudioVisualization(features) {
      const canvas = this.$refs.visualizerCanvas
      if (!canvas) return
      
      const ctx = canvas.getContext('2d')
      const width = canvas.width
      const height = canvas.height
      
      // 清空画布
      ctx.clearRect(0, 0, width, height)
      
      // 绘制能量水平
      const energyHeight = features.energy_level * height
      ctx.fillStyle = features.voice_activity ? '#4CAF50' : '#FFC107'
      ctx.fillRect(0, height - energyHeight, width, energyHeight)
      
      // 绘制音调变化
      ctx.strokeStyle = '#2196F3'
      ctx.lineWidth = 2
      ctx.beginPath()
      const pitchY = height - (features.pitch_variance * height)
      ctx.moveTo(0, pitchY)
      ctx.lineTo(width, pitchY)
      ctx.stroke()
    },
    
    formatTime(timestamp) {
      return new Date(timestamp).toLocaleTimeString()
    },
    
    getDimensionLabel(dimension) {
      const labels = {
        fluency: '流利度',
        accuracy: '准确性',
        pronunciation: '发音',
        vocabulary: '词汇',
        grammar: '语法',
        coherence: '连贯性',
        pragmatics: '语用能力'
      }
      return labels[dimension] || dimension
    },
    
    cleanup() {
      if (this.audioProcessor) {
        this.audioProcessor.stop()
        this.audioProcessor = null
      }
      
      if (this.speechRecognition) {
        this.speechRecognition.stop()
        this.speechRecognition = null
      }
      
      this.isRecording = false
    }
  }
}
</script>
<style scoped>
.advanced-practice {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

/* 模式选择 */
.mode-selector {
  margin-bottom: 30px;
}

.mode-buttons {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
  gap: 15px;
  margin-top: 15px;
}

.mode-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px;
  border: 2px solid #e0e0e0;
  border-radius: 12px;
  background: white;
  cursor: pointer;
  transition: all 0.3s ease;
}

.mode-btn:hover {
  border-color: #2196F3;
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(33, 150, 243, 0.15);
}

.mode-btn.active {
  border-color: #2196F3;
  background: #f3f9ff;
}

.mode-btn i {
  font-size: 24px;
  color: #2196F3;
  margin-bottom: 10px;
}

.mode-btn span {
  font-weight: 600;
  margin-bottom: 5px;
}

.mode-btn small {
  color: #666;
  text-align: center;
}

/* 交互面板 */
.interaction-panel {
  background: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.status-item {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.status-item .label {
  font-size: 12px;
  color: #666;
  margin-bottom: 5px;
}

.status-item .value {
  font-weight: 600;
  color: #333;
}

.status-item .value.active {
  color: #4CAF50;
}

.status-item .value.paused {
  color: #FF9800;
}

/* 音频可视化 */
.audio-visualizer {
  text-align: center;
  margin: 20px 0;
}

.audio-visualizer canvas {
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  margin-bottom: 15px;
}

.record-btn {
  padding: 12px 24px;
  border: none;
  border-radius: 25px;
  background: #4CAF50;
  color: white;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.record-btn:hover {
  background: #45a049;
  transform: scale(1.05);
}

.record-btn.recording {
  background: #f44336;
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

/* 实时反馈 */
.realtime-feedback {
  background: #fff3cd;
  border: 1px solid #ffeaa7;
  border-radius: 8px;
  padding: 15px;
  margin: 15px 0;
}

.feedback-header {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
  font-weight: 600;
  color: #856404;
}

.feedback-header i {
  margin-right: 8px;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.suggestion-chip {
  padding: 6px 12px;
  background: #2196F3;
  color: white;
  border-radius: 16px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.3s ease;
}

.suggestion-chip:hover {
  background: #1976D2;
}

.correction-feedback > div {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 8px 0;
}

.original {
  background: #ffebee;
  color: #c62828;
  padding: 4px 8px;
  border-radius: 4px;
}

.corrected {
  background: #e8f5e8;
  color: #2e7d32;
  padding: 4px 8px;
  border-radius: 4px;
}

.explanation {
  color: #666;
  font-style: italic;
}

/* 对话历史 */
.conversation-history {
  max-height: 400px;
  overflow-y: auto;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 15px;
  margin: 20px 0;
}

.message {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
}

.message.user {
  align-items: flex-end;
}

.message.system {
  align-items: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 18px;
  position: relative;
}

.message.user .message-content {
  background: #2196F3;
  color: white;
}

.message.system .message-content {
  background: #f1f3f4;
  color: #333;
}

.chinglish-indicator {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-top: 8px;
  font-size: 12px;
  color: #ff9800;
}

.message-meta {
  display: flex;
  gap: 10px;
  font-size: 12px;
  color: #666;
  margin-top: 5px;
}

/* 控制按钮 */
.control-buttons {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 20px;
}

.btn-primary, .btn-secondary {
  padding: 12px 24px;
  border: none;
  border-radius: 8px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.btn-primary {
  background: #2196F3;
  color: white;
}

.btn-primary:hover {
  background: #1976D2;
}

.btn-secondary {
  background: #f5f5f5;
  color: #333;
}

.btn-secondary:hover {
  background: #e0e0e0;
}

/* 会话结果 */
.session-result {
  background: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  margin-bottom: 30px;
}

.result-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.result-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
}

.score-display {
  display: flex;
  align-items: center;
  gap: 20px;
}

.score-circle {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #2196F3;
  color: white;
}

.score {
  font-size: 24px;
  font-weight: bold;
}

.max-score {
  font-size: 14px;
}

.proficiency-level {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  text-transform: capitalize;
}

.dimensions-chart {
  space-y: 15px;
}

.dimension-item {
  display: flex;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
}

.dimension-name {
  min-width: 80px;
  font-size: 14px;
  color: #666;
}

.score-bar {
  flex: 1;
  height: 8px;
  background: #e0e0e0;
  border-radius: 4px;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  background: linear-gradient(90deg, #4CAF50, #2196F3);
  transition: width 0.5s ease;
}

.dimension-score {
  min-width: 40px;
  text-align: right;
  font-weight: 600;
  color: #333;
}

.suggestions-list {
  list-style: none;
  padding: 0;
}

.suggestions-list li {
  padding: 8px 0;
  border-bottom: 1px solid #e0e0e0;
}

.suggestions-list li:last-child {
  border-bottom: none;
}

/* 进度追踪 */
.progress-section {
  background: white;
  border-radius: 12px;
  padding: 25px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.progress-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.progress-card {
  background: #f8f9fa;
  border-radius: 8px;
  padding: 20px;
  text-align: center;
}

.progress-stat {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 5px;
}

.stat-number {
  font-size: 32px;
  font-weight: bold;
  color: #2196F3;
}

.stat-number.improving {
  color: #4CAF50;
}

.stat-number.stable {
  color: #FF9800;
}

.stat-label {
  font-size: 14px;
  color: #666;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .advanced-practice {
    padding: 15px;
  }
  
  .mode-buttons {
    grid-template-columns: 1fr;
  }
  
  .status-bar {
    flex-direction: column;
    gap: 10px;
  }
  
  .result-cards,
  .progress-cards {
    grid-template-columns: 1fr;
  }
  
  .message-content {
    max-width: 90%;
  }
}
</style>