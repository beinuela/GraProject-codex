<template>
  <div class="page-card">
    <h2 class="page-title">事件列表</h2>
    <el-space style="margin-bottom:12px">
      <el-select v-model="filterStatus" placeholder="状态筛选" clearable style="width:130px" @change="load">
        <el-option value="OPEN" label="待处理" />
        <el-option value="IN_PROGRESS" label="处理中" />
        <el-option value="CLOSED" label="已关闭" />
      </el-select>
      <el-button type="primary" @click="openCreate">上报事件</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="eventTitle" label="事件标题" />
      <el-table-column prop="eventType" label="事件类型" width="120" />
      <el-table-column prop="eventLevel" label="等级" width="80" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="scope">
          <el-tag :type="{ OPEN:'danger', IN_PROGRESS:'warning', CLOSED:'success' }[scope.row.status]" size="small">
            {{ { OPEN:'待处理', IN_PROGRESS:'处理中', CLOSED:'已关闭' }[scope.row.status] || scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="eventTime" label="事件时间" width="170" />
      <el-table-column label="操作" width="200">
        <template #default="scope">
          <el-space>
            <el-button size="small" type="primary" v-if="scope.row.status==='OPEN'" @click="handleEvent(scope.row.id)">处理</el-button>
            <el-button size="small" type="success" v-if="scope.row.status==='IN_PROGRESS'" @click="closeEvent(scope.row.id)">关闭</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog append-to-body v-model="createVisible" title="上报事件列表" width="560">
      <el-form :model="form" label-width="90px">
        <el-form-item label="事件标题"><el-input v-model="form.eventTitle" /></el-form-item>
        <el-form-item label="事件类型">
          <el-select v-model="form.eventType" style="width:100%">
            <el-option value="NATURAL_DISASTER" label="自然灾害" />
            <el-option value="ACCIDENT" label="事故灾难" />
            <el-option value="PUBLIC_HEALTH" label="公共卫生" />
            <el-option value="SOCIAL_SECURITY" label="社会安全" />
            <el-option value="OTHER" label="其他" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件等级">
          <el-select v-model="form.eventLevel" style="width:100%">
            <el-option :value="1" label="一级（特别重大）" />
            <el-option :value="2" label="二级（重大）" />
            <el-option :value="3" label="三级（较大）" />
            <el-option :value="4" label="四级（一般）" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件描述"><el-input v-model="form.eventDescription" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="发生地点"><el-input v-model="form.location" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEvent">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { apiGet, apiPost } from '../../api'

const list = ref([])
const filterStatus = ref('')
const createVisible = ref(false)
const form = reactive({ eventTitle: '', eventType: '', eventLevel: 4, eventDescription: '', location: '' })

const load = async () => {
  const params = new URLSearchParams()
  if (filterStatus.value) params.append('status', filterStatus.value)
  const qs = params.toString() ? `?${params.toString()}` : ''
  list.value = await apiGet(`/api/event${qs}`)
}
const openCreate = () => {
  Object.assign(form, { eventTitle: '', eventType: '', eventLevel: 4, eventDescription: '', location: '' })
  createVisible.value = true
}
const saveEvent = async () => {
  await apiPost('/api/event', form)
  ElMessage.success('事件上报成功')
  createVisible.value = false
  await load()
}
const handleEvent = async (id) => {
  await apiPost(`/api/event/${id}/handle`)
  ElMessage.success('已开始处理')
  await load()
}
const closeEvent = async (id) => {
  const { value } = await ElMessageBox.prompt('请输入处理结果', '关闭事件', { confirmButtonText: '确定', cancelButtonText: '取消' })
  await apiPost(`/api/event/${id}/close`, { handleResult: value })
  ElMessage.success('事件已关闭')
  await load()
}

onMounted(load)
</script>
