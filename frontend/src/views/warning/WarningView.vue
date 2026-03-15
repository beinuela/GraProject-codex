<template>
  <div class="page-card">
    <h2 class="page-title">预警管理</h2>
    <el-space style="margin-bottom:12px">
      <el-select v-model="filterType" placeholder="预警类型" clearable style="width:150px" @change="load">
        <el-option value="STOCK_LOW" label="库存不足" />
        <el-option value="STOCK_BACKLOG" label="库存积压" />
        <el-option value="EXPIRING_SOON" label="即将过期" />
        <el-option value="EXPIRED" label="已过期" />
        <el-option value="ABNORMAL_USAGE" label="异常消耗" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="处理状态" clearable style="width:130px" @change="load">
        <el-option value="UNHANDLED" label="未处理" />
        <el-option value="HANDLED" label="已处理" />
      </el-select>
      <el-button @click="load">刷新</el-button>
      <el-button type="warning" @click="triggerScan">手动扫描</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="warningType" label="预警类型" width="120">
        <template #default="scope">
          <el-tag :type="typeTag(scope.row.warningType)" size="small">{{ typeLabel(scope.row.warningType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="materialId" label="物资ID" width="90" />
      <el-table-column prop="warehouseId" label="仓库ID" width="90" />
      <el-table-column prop="content" label="预警内容" show-overflow-tooltip />
      <el-table-column prop="handleStatus" label="状态" width="90">
        <template #default="scope">
          <el-tag :type="scope.row.handleStatus === 'HANDLED' ? 'success' : 'danger'" size="small">
            {{ scope.row.handleStatus === 'HANDLED' ? '已处理' : '未处理' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="120">
        <template #default="scope">
          <el-button size="small" type="primary" v-if="scope.row.handleStatus==='UNHANDLED'" @click="handleWarning(scope.row.id)">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { apiGet, apiPost } from '../../api'

const list = ref([])
const filterType = ref('')
const filterStatus = ref('')

const typeLabel = (t) => ({ STOCK_LOW: '库存不足', STOCK_BACKLOG: '库存积压', EXPIRING_SOON: '即将过期', EXPIRED: '已过期', ABNORMAL_USAGE: '异常消耗' }[t] || t)
const typeTag = (t) => ({ STOCK_LOW: 'danger', STOCK_BACKLOG: 'warning', EXPIRING_SOON: 'warning', EXPIRED: 'danger', ABNORMAL_USAGE: 'info' }[t] || 'info')

const load = async () => {
  const params = new URLSearchParams()
  if (filterType.value) params.append('type', filterType.value)
  if (filterStatus.value) params.append('status', filterStatus.value)
  const qs = params.toString() ? `?${params.toString()}` : ''
  list.value = await apiGet(`/api/warning/list${qs}`)
}
const triggerScan = async () => { await apiPost('/api/warning/scan'); await load() }
const handleWarning = async (id) => {
  const { value } = await ElMessageBox.prompt('请输入处理备注', '处理预警', { confirmButtonText: '确定', cancelButtonText: '取消' })
  await apiPost(`/api/warning/${id}/handle`, { remark: value })
  await load()
}

onMounted(load)
</script>
