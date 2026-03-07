<template>
  <div class="knowledge-base">
    <div class="header">
      <h2>知识库管理</h2>
      <el-button type="primary" @click="showUpload = true">上传文档</el-button>
    </div>

    <!-- 文档列表 -->
    <el-table :data="documents" v-loading="loading" style="width: 100%">
      <el-table-column prop="filename" label="文件名" />
      <el-table-column label="描述">
        <template #default="{ row }">{{ row.metadata?.description || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="handleDelete(row.doc_id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 上传对话框 -->
    <el-dialog v-model="showUpload" title="上传文档" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="文件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".txt,.md,.pdf"
            :on-change="onFileChange"
          >
            <el-button>选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 .txt .md .pdf，单文件最大 10MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" placeholder="可选，文档用途说明" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showUpload = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="handleUpload">上传</el-button>
      </template>
    </el-dialog>

    <!-- RAG 测试 -->
    <div class="rag-test" style="margin-top: 24px">
      <h3>检索测试</h3>
      <el-input v-model="testQuery" placeholder="输入问题测试检索效果" style="width: 400px" />
      <el-button style="margin-left: 8px" @click="handleSearch">检索</el-button>
      <div v-if="searchResults.length" style="margin-top: 12px">
        <el-card v-for="(r, i) in searchResults" :key="i" style="margin-bottom: 8px">
          <div><strong>{{ r.filename }}</strong> (相似度: {{ (r.score * 100).toFixed(1) }}%)</div>
          <div style="margin-top: 6px; color: #666; font-size: 13px">{{ r.content }}</div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listDocuments, uploadDocument, deleteDocument, searchDocuments } from '@/api/rag'

const documents = ref([])
const loading = ref(false)
const showUpload = ref(false)
const uploading = ref(false)
const form = ref({ description: '' })
const selectedFile = ref(null)
const testQuery = ref('')
const searchResults = ref([])

async function fetchDocuments() {
  loading.value = true
  try {
    const res = await listDocuments()
    documents.value = res.data || []
  } finally {
    loading.value = false
  }
}

function onFileChange(file) {
  selectedFile.value = file.raw
}

async function handleUpload() {
  if (!selectedFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', selectedFile.value)
    fd.append('description', form.value.description)
    const res = await uploadDocument(fd)
    ElMessage.success(`上传成功，共 ${res.data.chunks} 个分块`)
    showUpload.value = false
    form.value.description = ''
    selectedFile.value = null
    fetchDocuments()
  } catch (e) {
    ElMessage.error('上传失败')
  } finally {
    uploading.value = false
  }
}

async function handleDelete(docId) {
  await ElMessageBox.confirm('确认删除该文档？', '提示', { type: 'warning' })
  await deleteDocument(docId)
  ElMessage.success('删除成功')
  fetchDocuments()
}

async function handleSearch() {
  if (!testQuery.value.trim()) return
  const res = await searchDocuments(testQuery.value)
  searchResults.value = res.data || []
}

onMounted(fetchDocuments)
</script>
