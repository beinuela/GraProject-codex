<template>
  <div class="page-card">
    <h2 class="page-title">预警中心</h2>
    <el-space wrap style="margin-bottom:12px">
      <el-select v-model="filters.type" clearable placeholder="预警类型" style="width:200px">
        <el-option label="库存不足" value="STOCK_LOW" />
        <el-option label="库存积压" value="STOCK_BACKLOG" />
        <el-option label="临期" value="EXPIRING_SOON" />
        <el-option label="过期" value="EXPIRED" />
        <el-option label="异常领用" value="ABNORMAL_USAGE" />
      </el-select>
      <el-select v-model="filters.status" clearable placeholder="处理状态" style="width:160px">
        <el-option label="未处理" value="UNHANDLED" />
        <el-option label="已处理" value="HANDLED" />
      </el-select>
      <el-button @click="load">查询</el-button>
      <el-button type="warning" @click="scan">触发扫描</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="预警ID" width="90" />
      <el-table-column prop="warningType" label="类型" width="140" />
      <el-table-column prop="materialId" label="物资ID" width="90" />
      <el-table-column prop="warehouseId" label="仓库ID" width="90" />
      <el-table-column prop="content" label="内容" min-width="220" />
      <el-table-column prop="handleStatus" label="状态" width="100" />
      <el-table-column label="操作" width="140">
        <template #default="scope">
          <el-button size="small" type="primary" @click="handle(scope.row.id)" :disabled="scope.row.handleStatus === 'HANDLED'">处理</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { apiGet, apiPost } from '../../api'

const list = ref([])
const filters = reactive({ type: '', status: '' })

const load = async () => { list.value = await apiGet('/api/warning/list', filters) }
const scan = async () => { await apiPost('/api/warning/scan', {}); await load() }
const handle = async (id) => {
  const { value } = await ElMessageBox.prompt('处理备注', '处理预警', { inputValue: '已处理' })
  await apiPost(`/api/warning/${id}/handle`, {}, { remark: value })
  await load()
}

onMounted(load)
</script>
