<template>
  <div class="page">
    <van-nav-bar title="新建对话" left-arrow @click-left="$router.back()" />

    <!-- 选择主题 -->
    <div class="card">
      <h4 style="margin-bottom: 12px">选择主题</h4>
      <van-loading v-if="loadingThemes" size="24px" style="padding: 16px" />
      <van-empty v-else-if="themes.length === 0" description="暂无主题数据" image-size="60" />
      <van-grid v-else :column-num="2" :border="false">
        <van-grid-item
          v-for="t in themes" :key="t.id"
          :text="t.name"
          :class="{ 'theme-active': selectedTheme === t.id }"
          @click="selectedTheme = t.id"
        />
      </van-grid>
    </div>

    <!-- 选择角色 -->
    <div class="card">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px">
        <h4 style="margin: 0">选择AI角色</h4>
        <van-button size="mini" type="primary" plain @click="$router.push('/roles')">
          我的角色库
        </van-button>
      </div>
      <van-loading v-if="loadingRoles" size="24px" style="padding: 16px" />
      <van-empty v-else-if="allRoles.length === 0" description="暂无角色数据" image-size="60" />
      <van-radio-group v-else v-model="selectedRole">
        <van-cell-group inset>
          <!-- 系统预设角色 -->
          <van-cell
            v-for="r in systemRoles" :key="`system_${r.id}`"
            :title="r.name"
            :label="r.description"
            clickable
            @click="selectedRole = `system_${r.id}`"
          >
            <template #right-icon><van-radio :name="`system_${r.id}`" /></template>
          </van-cell>
          
          <!-- 用户自定义角色 -->
          <van-cell
            v-for="r in userRoles" :key="`user_${r.role_name}`"
            :title="`🎭 ${r.role_name}`"
            :label="`我的角色 • ${r.document_count}个文档`"
            clickable
            @click="selectedRole = `user_${r.role_name}`"
          >
            <template #right-icon><van-radio :name="`user_${r.role_name}`" /></template>
          </van-cell>
        </van-cell-group>
      </van-radio-group>
    </div>

    <!-- 选择模型 -->
    <div class="card">
      <h4 style="margin-bottom: 12px">选择AI模型</h4>
      <van-loading v-if="loadingModels" size="24px" style="padding: 16px" />
      <van-empty v-else-if="models.length === 0" description="暂无模型数据" image-size="60" />
      <van-radio-group v-else v-model="selectedModel">
        <van-cell-group inset>
          <van-cell
            v-for="m in models" :key="m.id"
            :title="m.name"
            :label="m.provider"
            clickable
            @click="selectedModel = m.id"
          >
            <template #right-icon><van-radio :name="m.id" /></template>
          </van-cell>
        </van-cell-group>
      </van-radio-group>
    </div>

    <div style="padding: 0 16px 24px">
      <van-button type="primary" block round :loading="creating" @click="handleCreate">
        开始对话
      </van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { getThemes, getRoles, createSession } from '@/api/practice'
import { getModels } from '@/api/ai'
import { getUserRoles } from '@/api/rag'

const router = useRouter()
const themes = ref([])
const systemRoles = ref([])  // 系统预设角色
const userRoles = ref([])    // 用户自定义角色
const models = ref([])
const selectedTheme = ref(null)
const selectedRole = ref(null)
const selectedModel = ref(null)
const creating = ref(false)
const loadingThemes = ref(true)
const loadingRoles = ref(true)
const loadingModels = ref(true)

// 合并所有角色
const allRoles = computed(() => [...systemRoles.value, ...userRoles.value])

// 统一解析分页或数组响应
const parseList = (d) => {
  if (!d) return []
  if (Array.isArray(d)) return d
  return d.records || d.content || d.list || d.data || []
}

const handleCreate = async () => {
  if (!selectedTheme.value) { showToast('请选择主题'); return }
  if (!selectedRole.value) { showToast('请选择AI角色'); return }
  
  creating.value = true
  try {
    // 解析选中的角色
    let roleData = {}
    if (selectedRole.value.startsWith('system_')) {
      // 系统预设角色
      const roleId = selectedRole.value.replace('system_', '')
      const role = systemRoles.value.find(r => r.id == roleId)
      roleData = {
        roleId: roleId,
        roleName: role?.name,
        rolePrompt: role?.description
      }
    } else if (selectedRole.value.startsWith('user_')) {
      // 用户自定义角色
      const roleName = selectedRole.value.replace('user_', '')
      roleData = {
        roleName: roleName,
        rolePrompt: `You are ${roleName}. Please stay in character and use the knowledge from uploaded documents about this role.`
      }
    }

    const session = await createSession({
      themeId: selectedTheme.value,
      roleId: roleData.roleId || undefined,
      modelId: selectedModel.value || undefined,
      ...roleData
    })
    const sessionId = session?.id || session
    router.replace(`/chat/${sessionId}`)
  } catch (e) {
    showToast('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

onMounted(async () => {
  // 并行加载
  const [themesRes, rolesRes, modelsRes, userRolesRes] = await Promise.allSettled([
    getThemes(),
    getRoles(),
    getModels(),
    getUserRoles()
  ])

  if (themesRes.status === 'fulfilled') {
    themes.value = parseList(themesRes.value)
  } else {
    console.error('加载主题失败:', themesRes.reason)
    showToast('加载主题失败')
  }
  loadingThemes.value = false

  if (rolesRes.status === 'fulfilled') {
    systemRoles.value = parseList(rolesRes.value)
  } else {
    console.error('加载系统角色失败:', rolesRes.reason)
  }

  if (userRolesRes.status === 'fulfilled') {
    userRoles.value = parseList(userRolesRes.value)
  } else {
    console.error('加载用户角色失败:', userRolesRes.reason)
  }

  // 默认选中第一个角色
  if (systemRoles.value.length > 0) {
    selectedRole.value = `system_${systemRoles.value[0].id}`
  } else if (userRoles.value.length > 0) {
    selectedRole.value = `user_${userRoles.value[0].role_name}`
  }
  loadingRoles.value = false

  if (modelsRes.status === 'fulfilled') {
    models.value = parseList(modelsRes.value)
    if (models.value.length > 0) selectedModel.value = models.value[0].id
  } else {
    console.error('加载模型失败:', modelsRes.reason)
  }
  loadingModels.value = false
})
</script>

<style scoped>
.theme-active :deep(.van-grid-item__content) {
  background: #e8f4ff;
  border-radius: 8px;
  color: #1989fa;
  font-weight: 600;
}
</style>
