<template>
  <div class="page-card">
    <h2 class="page-title">库位管理</h2>
    <el-space style="margin-bottom:12px">
      <el-select v-model="warehouseFilter" placeholder="按仓库筛选" clearable style="width:180px" @change="load">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
      </el-select>
      <el-button type="primary" @click="openCreate">新增库位</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="locationCode" label="库位编码" />
      <el-table-column prop="locationName" label="库位名称" />
      <el-table-column prop="warehouseName" label="所属仓库" />
      <el-table-column prop="capacity" label="容量" />
      <el-table-column prop="usedCapacity" label="已用容量" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="170">
        <template #default="scope">
          <el-space>
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="remove(scope.row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="库位信息" width="520">
      <el-form :model="form" label-width="100px">
        <el-form-item label="所属仓库">
          <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width:100%">
            <el-option v-for="w in warehouses" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="库位编码"><el-input v-model="form.locationCode" /></el-form-item>
        <el-form-item label="库位名称"><el-input v-model="form.locationName" /></el-form-item>
        <el-form-item label="容量"><el-input-number v-model="form.capacity" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="已用容量"><el-input-number v-model="form.usedCapacity" :min="0" style="width:100%" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { apiDelete, apiGet, apiPost } from '../../api'

const list = ref([])
const warehouses = ref([])
const warehouseFilter = ref(null)
const visible = ref(false)
const form = reactive({ id: null, warehouseId: null, locationCode: '', locationName: '', capacity: 0, usedCapacity: 0, remark: '' })

const loadWarehouses = async () => { warehouses.value = await apiGet('/api/warehouse/list') }
const load = async () => {
  const params = warehouseFilter.value ? `?warehouseId=${warehouseFilter.value}` : ''
  list.value = await apiGet(`/api/location${params}`)
}
const openCreate = () => { Object.assign(form, { id: null, warehouseId: null, locationCode: '', locationName: '', capacity: 0, usedCapacity: 0, remark: '' }); visible.value = true }
const openEdit = (row) => { Object.assign(form, row); visible.value = true }
const save = async () => { await apiPost('/api/location', form); visible.value = false; await load() }
const remove = async (id) => { await apiDelete(`/api/location/${id}`); await load() }

onMounted(async () => { await loadWarehouses(); await load() })
</script>
