<template>
  <div class="role-library">
    <div class="header">
      <h1>🎭 我的角色库</h1>
      <p>上传资料，创建专属的AI角色伙伴</p>
    </div>

    <!-- 角色列表 -->
    <div class="roles-section">
      <div class="section-header">
        <h2>我的角色</h2>
        <button @click="showUploadModal = true" class="btn-primary">
          <i class="icon-plus"></i>
          创建新角色
        </button>
      </div>

      <div v-if="loading" class="loading">
        <div class="spinner"></div>
        <p>加载中...</p>
      </div>

      <div v-else-if="roles.length === 0" class="empty-state">
        <div class="empty-icon">📚</div>
        <h3>还没有创建角色</h3>
        <p>上传角色相关资料，让AI更好地理解和扮演特定角色</p>
        <button @click="showUploadModal = true" class="btn-primary">
          创建第一个角色
        </button>
      </div>

      <div v-else class="roles-grid">
        <div 
          v-for="role in roles" 
          :key="role.role_name"
          class="role-card"
          @click="selectRole(role)"
        >
          <div class="role-avatar">
            {{ getRoleEmoji(role.role_name) }}
          </div>
          <div class="role-info">
            <h3>{{ role.role_name }}</h3>
            <p>{{ role.document_count }} 个文档</p>
            <p>{{ role.total_chunks }} 个知识片段</p>
          </div>
          <div class="role-actions">
            <button @click.stop="uploadForRole(role.role_name)" class="btn-secondary">
              添加资料
            </button>
            <button @click.stop="testRole(role)" class="btn-outline">
              测试对话
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 选中角色的详情 -->
    <div v-if="selectedRole" class="role-details">
      <div class="section-header">
        <h2>{{ selectedRole.role_name }} 的知识库</h2>
        <button @click="uploadForRole(selectedRole.role_name)" class="btn-secondary">
          添加更多资料
        </button>
      </div>

      <div class="documents-list">
        <div 
          v-for="doc in roleDocuments" 
          :key="doc.doc_id"
          class="document-item"
        >
          <div class="doc-icon">📄</div>
          <div class="doc-info">
            <h4>{{ doc.filename }}</h4>
            <p>{{ doc.metadata.description || '无描述' }}</p>
            <small>大小: {{ formatFileSize(doc.metadata.size) }}</small>
          </div>
          <div class="doc-actions">
            <button @click="deleteDocument(doc.doc_id)" class="btn-danger-outline">
              删除
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- 上传模态框 -->
    <div v-if="showUploadModal" class="modal-overlay" @click="closeUploadModal">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h3>{{ uploadRoleName ? `为 ${uploadRoleName} 添加资料` : '创建新角色' }}</h3>
          <button @click="closeUploadModal" class="btn-close">×</button>
        </div>

        <div class="modal-body">
          <form @submit.prevent="uploadDocument">
            <div class="form-group">
              <label>角色名称</label>
              <input 
                v-model="uploadForm.roleName" 
                type="text" 
                placeholder="例如：明日香、Emily、Professor Chen"
                required
                :disabled="!!uploadRoleName"
              >
              <small>建议使用具体的角色名称，如动漫角色、历史人物、虚拟老师等</small>
            </div>

            <div class="form-group">
              <label>上传文件</label>
              <div class="file-upload-area" @drop="handleDrop" @dragover.prevent>
                <input 
                  ref="fileInput"
                  type="file" 
                  @change="handleFileSelect"
                  accept=".txt,.md,.pdf"
                  hidden
                >
                <div v-if="!uploadForm.file" class="upload-placeholder">
                  <div class="upload-icon">📁</div>
                  <p>拖拽文件到这里，或 <button type="button" @click="$refs.fileInput.click()" class="link-btn">点击选择</button></p>
                  <small>支持 .txt, .md, .pdf 格式</small>
                </div>
                <div v-else class="file-selected">
                  <div class="file-icon">📄</div>
                  <div class="file-info">
                    <p>{{ uploadForm.file.name }}</p>
                    <small>{{ formatFileSize(uploadForm.file.size) }}</small>
                  </div>
                  <button type="button" @click="uploadForm.file = null" class="btn-remove">×</button>
                </div>
              </div>
            </div>

            <div class="form-group">
              <label>描述 (可选)</label>
              <textarea 
                v-model="uploadForm.description" 
                placeholder="描述这个文件的内容，例如：明日香的性格设定、背景故事等"
                rows="3"
              ></textarea>
            </div>

            <div class="form-actions">
              <button type="button" @click="closeUploadModal" class="btn-secondary">
                取消
              </button>
              <button type="submit" :disabled="uploading" class="btn-primary">
                {{ uploading ? '上传中...' : '上传' }}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- 测试对话模态框 -->
    <div v-if="showTestModal" class="modal-overlay" @click="closeTestModal">
      <div class="modal modal-large" @click.stop>
        <div class="modal-header">
          <h3>🎭 与 {{ testingRole?.role_name }} 对话</h3>
          <button @click="closeTestModal" class="btn-close">×</button>
        </div>

        <div class="modal-body">
          <div class="chat-container">
            <div class="chat-messages" ref="chatMessages">
              <div 
                v-for="msg in testMessages" 
                :key="msg.id"
                :class="['message', msg.role]"
              >
                <div class="message-content">{{ msg.content }}</div>
                <div class="message-time">{{ formatTime(msg.timestamp) }}</div>
              </div>
            </div>

            <div class="chat-input">
              <form @submit.prevent="sendTestMessage">
                <div class="input-group">
                  <input 
                    v-model="testInput"
                    type="text" 
                    placeholder="输入消息..."
                    :disabled="testSending"
                  >
                  <button type="submit" :disabled="testSending || !testInput.trim()" class="btn-primary">
                    {{ testSending ? '发送中...' : '发送' }}
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted, nextTick } from 'vue'
import { useAuthStore } from '@/stores/auth'

export default {
  name: 'RoleLibrary',
  setup() {
    const authStore = useAuthStore()
    
    // 响应式数据
    const loading = ref(false)
    const roles = ref([])
    const selectedRole = ref(null)
    const roleDocuments = ref([])
    
    // 上传相关
    const showUploadModal = ref(false)
    const uploadRoleName = ref('')
    const uploading = ref(false)
    const uploadForm = reactive({
      roleName: '',
      file: null,
      description: ''
    })
    
    // 测试对话相关
    const showTestModal = ref(false)
    const testingRole = ref(null)
    const testMessages = ref([])
    const testInput = ref('')
    const testSending = ref(false)

    // 获取角色列表
    const loadRoles = async () => {
      loading.value = true
      try {
        const response = await fetch('/api/ai/rag/user/roles', {
          headers: {
            'Authorization': `Bearer ${authStore.token}`,
            'X-User-Id': authStore.user.id.toString()
          }
        })
        const result = await response.json()
        if (result.code === 200) {
          roles.value = result.data
        }
      } catch (error) {
        console.error('加载角色失败:', error)
      } finally {
        loading.value = false
      }
    }

    // 选择角色
    const selectRole = async (role) => {
      selectedRole.value = role
      await loadRoleDocuments(role.role_name)
    }

    // 加载角色文档
    const loadRoleDocuments = async (roleName) => {
      try {
        const response = await fetch(`/api/ai/rag/user/documents?role_name=${encodeURIComponent(roleName)}`, {
          headers: {
            'Authorization': `Bearer ${authStore.token}`,
            'X-User-Id': authStore.user.id.toString()
          }
        })
        const result = await response.json()
        if (result.code === 200) {
          roleDocuments.value = result.data
        }
      } catch (error) {
        console.error('加载角色文档失败:', error)
      }
    }

    // 上传相关方法
    const uploadForRole = (roleName) => {
      uploadRoleName.value = roleName
      uploadForm.roleName = roleName
      showUploadModal.value = true
    }

    const closeUploadModal = () => {
      showUploadModal.value = false
      uploadRoleName.value = ''
      uploadForm.roleName = ''
      uploadForm.file = null
      uploadForm.description = ''
    }

    const handleFileSelect = (event) => {
      const file = event.target.files[0]
      if (file) {
        uploadForm.file = file
      }
    }

    const handleDrop = (event) => {
      event.preventDefault()
      const file = event.dataTransfer.files[0]
      if (file) {
        uploadForm.file = file
      }
    }

    const uploadDocument = async () => {
      if (!uploadForm.file || !uploadForm.roleName) return

      uploading.value = true
      try {
        const formData = new FormData()
        formData.append('file', uploadForm.file)
        formData.append('role_name', uploadForm.roleName)
        formData.append('description', uploadForm.description)

        const response = await fetch('/api/ai/rag/user/documents/upload', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${authStore.token}`,
            'X-User-Id': authStore.user.id.toString()
          },
          body: formData
        })

        const result = await response.json()
        if (result.code === 200) {
          alert(`成功！${result.data.message}`)
          closeUploadModal()
          await loadRoles()
          if (selectedRole.value?.role_name === uploadForm.roleName) {
            await loadRoleDocuments(uploadForm.roleName)
          }
        } else {
          alert(`上传失败: ${result.msg}`)
        }
      } catch (error) {
        console.error('上传失败:', error)
        alert('上传失败，请重试')
      } finally {
        uploading.value = false
      }
    }

    // 删除文档
    const deleteDocument = async (docId) => {
      if (!confirm('确定要删除这个文档吗？')) return

      try {
        const response = await fetch(`/api/ai/rag/user/documents/${docId}`, {
          method: 'DELETE',
          headers: {
            'Authorization': `Bearer ${authStore.token}`,
            'X-User-Id': authStore.user.id.toString()
          }
        })

        const result = await response.json()
        if (result.code === 200) {
          alert('删除成功')
          await loadRoles()
          if (selectedRole.value) {
            await loadRoleDocuments(selectedRole.value.role_name)
          }
        } else {
          alert(`删除失败: ${result.msg}`)
        }
      } catch (error) {
        console.error('删除失败:', error)
        alert('删除失败，请重试')
      }
    }

    // 测试对话相关方法
    const testRole = (role) => {
      testingRole.value = role
      testMessages.value = [
        {
          id: 1,
          role: 'assistant',
          content: `Hi! I'm ${role.role_name}. Let's have a conversation in English!`,
          timestamp: new Date()
        }
      ]
      showTestModal.value = true
    }

    const closeTestModal = () => {
      showTestModal.value = false
      testingRole.value = null
      testMessages.value = []
      testInput.value = ''
    }

    const sendTestMessage = async () => {
      if (!testInput.value.trim() || testSending.value) return

      const userMessage = {
        id: Date.now(),
        role: 'user',
        content: testInput.value,
        timestamp: new Date()
      }
      testMessages.value.push(userMessage)

      const messageText = testInput.value
      testInput.value = ''
      testSending.value = true

      try {
        const response = await fetch('/api/ai/chat', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${authStore.token}`,
            'X-User-Id': authStore.user.id.toString()
          },
          body: JSON.stringify({
            session_id: `test_${testingRole.value.role_name}_${Date.now()}`,
            user_id: authStore.user.id.toString(),
            message: messageText,
            role_name: testingRole.value.role_name,
            role_prompt: `You are ${testingRole.value.role_name}. Please stay in character and use the knowledge from uploaded documents about this role.`
          })
        })

        const result = await response.json()
        if (result.code === 200) {
          const assistantMessage = {
            id: Date.now() + 1,
            role: 'assistant',
            content: result.data.reply,
            timestamp: new Date()
          }
          testMessages.value.push(assistantMessage)
          
          // 滚动到底部
          await nextTick()
          const chatContainer = document.querySelector('.chat-messages')
          if (chatContainer) {
            chatContainer.scrollTop = chatContainer.scrollHeight
          }
        } else {
          alert(`对话失败: ${result.msg}`)
        }
      } catch (error) {
        console.error('发送消息失败:', error)
        alert('发送失败，请重试')
      } finally {
        testSending.value = false
      }
    }

    // 工具方法
    const getRoleEmoji = (roleName) => {
      const emojiMap = {
        '明日香': '🧡',
        'Emily': '👩‍🏫',
        'Professor': '👨‍🎓',
        'Teacher': '👩‍🏫',
        'Assistant': '🤖'
      }
      
      for (const [key, emoji] of Object.entries(emojiMap)) {
        if (roleName.includes(key)) return emoji
      }
      return '🎭'
    }

    const formatFileSize = (bytes) => {
      if (!bytes) return '0 B'
      const k = 1024
      const sizes = ['B', 'KB', 'MB', 'GB']
      const i = Math.floor(Math.log(bytes) / Math.log(k))
      return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
    }

    const formatTime = (date) => {
      return new Date(date).toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
      })
    }

    // 生命周期
    onMounted(() => {
      loadRoles()
    })

    return {
      // 数据
      loading,
      roles,
      selectedRole,
      roleDocuments,
      
      // 上传相关
      showUploadModal,
      uploadRoleName,
      uploading,
      uploadForm,
      
      // 测试对话相关
      showTestModal,
      testingRole,
      testMessages,
      testInput,
      testSending,
      
      // 方法
      selectRole,
      uploadForRole,
      closeUploadModal,
      handleFileSelect,
      handleDrop,
      uploadDocument,
      deleteDocument,
      testRole,
      closeTestModal,
      sendTestMessage,
      getRoleEmoji,
      formatFileSize,
      formatTime
    }
  }
}
</script>

<style scoped>
.role-library {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.header {
  text-align: center;
  margin-bottom: 40px;
}

.header h1 {
  font-size: 2.5rem;
  margin-bottom: 10px;
  color: #333;
}

.header p {
  font-size: 1.1rem;
  color: #666;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-header h2 {
  font-size: 1.5rem;
  color: #333;
}

.loading {
  text-align: center;
  padding: 40px;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #f3f3f3;
  border-top: 4px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin: 0 auto 20px;
}

@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 60px 20px;
  background: #f8f9fa;
  border-radius: 12px;
  margin-bottom: 40px;
}

.empty-icon {
  font-size: 4rem;
  margin-bottom: 20px;
}

.empty-state h3 {
  font-size: 1.5rem;
  margin-bottom: 10px;
  color: #333;
}

.empty-state p {
  color: #666;
  margin-bottom: 30px;
}

.roles-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
  margin-bottom: 40px;
}

.role-card {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
  cursor: pointer;
  transition: all 0.3s ease;
}

.role-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.15);
}

.role-avatar {
  font-size: 3rem;
  text-align: center;
  margin-bottom: 15px;
}

.role-info {
  text-align: center;
  margin-bottom: 20px;
}

.role-info h3 {
  font-size: 1.3rem;
  margin-bottom: 8px;
  color: #333;
}

.role-info p {
  color: #666;
  font-size: 0.9rem;
  margin: 2px 0;
}

.role-actions {
  display: flex;
  gap: 10px;
}

.role-actions button {
  flex: 1;
  padding: 8px 12px;
  font-size: 0.9rem;
}

.role-details {
  background: white;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.documents-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.document-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 8px;
  gap: 15px;
}

.doc-icon {
  font-size: 2rem;
}

.doc-info {
  flex: 1;
}

.doc-info h4 {
  margin: 0 0 5px 0;
  color: #333;
}

.doc-info p {
  margin: 0 0 5px 0;
  color: #666;
  font-size: 0.9rem;
}

.doc-info small {
  color: #999;
  font-size: 0.8rem;
}

.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0,0,0,0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal {
  background: white;
  border-radius: 12px;
  width: 90%;
  max-width: 500px;
  max-height: 90vh;
  overflow-y: auto;
}

.modal-large {
  max-width: 800px;
  height: 80vh;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  color: #333;
}

.btn-close {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 20px;
}

.form-group {
  margin-bottom: 20px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
  color: #333;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 1rem;
}

.form-group small {
  display: block;
  margin-top: 5px;
  color: #666;
  font-size: 0.85rem;
}

.file-upload-area {
  border: 2px dashed #ddd;
  border-radius: 8px;
  padding: 30px;
  text-align: center;
  transition: border-color 0.3s ease;
}

.file-upload-area:hover {
  border-color: #007bff;
}

.upload-placeholder {
  color: #666;
}

.upload-icon {
  font-size: 3rem;
  margin-bottom: 15px;
}

.link-btn {
  background: none;
  border: none;
  color: #007bff;
  cursor: pointer;
  text-decoration: underline;
}

.file-selected {
  display: flex;
  align-items: center;
  gap: 15px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 6px;
}

.file-icon {
  font-size: 2rem;
}

.file-info {
  flex: 1;
  text-align: left;
}

.file-info p {
  margin: 0 0 5px 0;
  font-weight: 500;
}

.file-info small {
  color: #666;
}

.btn-remove {
  background: #dc3545;
  color: white;
  border: none;
  border-radius: 50%;
  width: 30px;
  height: 30px;
  cursor: pointer;
  font-size: 1.2rem;
}

.form-actions {
  display: flex;
  gap: 10px;
  justify-content: flex-end;
}

.chat-container {
  height: 60vh;
  display: flex;
  flex-direction: column;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 20px;
}

.message {
  margin-bottom: 15px;
  display: flex;
  flex-direction: column;
}

.message.user {
  align-items: flex-end;
}

.message.assistant {
  align-items: flex-start;
}

.message-content {
  max-width: 70%;
  padding: 12px 16px;
  border-radius: 18px;
  word-wrap: break-word;
}

.message.user .message-content {
  background: #007bff;
  color: white;
}

.message.assistant .message-content {
  background: white;
  color: #333;
  border: 1px solid #ddd;
}

.message-time {
  font-size: 0.75rem;
  color: #999;
  margin-top: 5px;
}

.chat-input {
  border-top: 1px solid #eee;
  padding-top: 20px;
}

.input-group {
  display: flex;
  gap: 10px;
}

.input-group input {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 25px;
  font-size: 1rem;
}

.input-group button {
  padding: 12px 24px;
  border-radius: 25px;
}

/* 按钮样式 */
.btn-primary {
  background: #007bff;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.3s ease;
}

.btn-primary:hover:not(:disabled) {
  background: #0056b3;
}

.btn-primary:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.btn-secondary {
  background: #6c757d;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
}

.btn-secondary:hover {
  background: #545b62;
}

.btn-outline {
  background: transparent;
  color: #007bff;
  border: 1px solid #007bff;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 1rem;
}

.btn-outline:hover {
  background: #007bff;
  color: white;
}

.btn-danger-outline {
  background: transparent;
  color: #dc3545;
  border: 1px solid #dc3545;
  padding: 8px 16px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-danger-outline:hover {
  background: #dc3545;
  color: white;
}

.icon-plus::before {
  content: '+';
  margin-right: 5px;
}
</style>