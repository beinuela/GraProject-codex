<template>
  <div class="page-card">
    <h2 class="page-title">出库管理（FEFO）</h2>
    <el-form :model="form" inline>
      <el-form-item label="关联申请ID"><el-input-number v-model="form.applyOrderId" :min="1" /></el-form-item>
      <el-form-item label="仓库">
        <el-select v-model="form.warehouseId" style="width:180px">
          <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.remark" style="width:260px" /></el-form-item>
    </el-form>

    <el-table :data="form.items" border style="margin-top:8px">
      <el-table-column label="物资" min-width="180">
        <template #default="scope">
          <el-select v-model="scope.row.materialId" filterable style="width:100%">
            <el-option v-for="m in materials" :key="m.id" :label="m.materialName" :value="m.id" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="数量" width="120">
        <template #default="scope"><el-input-number v-model="scope.row.quantity" :min="1" style="width:100%" /></template>
      </el-table-column>
      <el-table-column label="推荐批次" min-width="220">
        <template #default="scope">
          <el-button size="small" @click="checkRecommend(scope.row)">查看推荐</el-button>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="scope"><el-button link type="danger" @click="remove(scope.$index)">删除</el-button></template>
      </el-table-column>
    </el-table>

    <el-space style="margin-top:12px">
      <el-button @click="add">新增明细</el-button>
      <el-button type="primary" @click="submit">提交出库</el-button>
    </el-space>

    <el-dialog v-model="showRec" title="近效期优先推荐批次" width="700">
      <el-table :data="recommendBatches" border>
        <el-table-column prop="batchNo" label="批次号" />
        <el-table-column prop="remainQty" label="剩余数量" width="120" />
        <el-table-column prop="expireDate" label="过期日期" width="140" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { apiGet, apiPost } from '../../api'

const warehouses = ref([])
const materials = ref([])
const showRec = ref(false)
const recommendBatches = ref([])

const form = reactive({
  applyOrderId: null,
  warehouseId: null,
  remark: '',
  items: [{ materialId: null, quantity: 1 }]
})

const loadBase = async () => {
  warehouses.value = await apiGet('/api/warehouse/list')
  materials.value = await apiGet('/api/material/info')
}

const add = () => form.items.push({ materialId: null, quantity: 1 })
const remove = (i) => form.items.splice(i, 1)

const checkRecommend = async (row) => {
  if (!row.materialId || !form.warehouseId) {
    ElMessage.warning('请先选择仓库和物资')
    return
  }
  recommendBatches.value = await apiGet('/api/inventory/recommend-outbound', { materialId: row.materialId, warehouseId: form.warehouseId })
  showRec.value = true
}

const submit = async () => {
  if (!form.warehouseId || form.items.length === 0) {
    ElMessage.warning('请填写仓库和明细')
    return
  }
  await apiPost('/api/inventory/stock-out', form)
  ElMessage.success('出库完成')
  form.items = [{ materialId: null, quantity: 1 }]
}

onMounted(loadBase)
</script>
