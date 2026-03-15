<template>
  <div class="page-card">
    <h2 class="page-title">供应商管理</h2>
    <el-space style="margin-bottom:12px">
      <el-input v-model="keyword" placeholder="搜索供应商名称" style="width:220px" clearable />
      <el-button type="primary" @click="openCreate">新增</el-button>
      <el-button type="success" @click="load">刷新</el-button>
    </el-space>

    <el-table :data="filteredList" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="supplierName" label="供应商名称" />
      <el-table-column prop="contactPerson" label="联系人" />
      <el-table-column prop="contactPhone" label="联系电话" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="supplyScope" label="供应范围" show-overflow-tooltip />
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

    <el-dialog v-model="visible" title="供应商信息" width="560">
      <el-form :model="form" label-width="100px">
        <el-form-item label="供应商名称"><el-input v-model="form.supplierName" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contactPerson" /></el-form-item>
        <el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="供应范围"><el-input v-model="form.supplyScope" type="textarea" /></el-form-item>
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
import { computed, onMounted, reactive, ref } from 'vue'
import { apiDelete, apiGet, apiPost } from '../../api'

const list = ref([])
const keyword = ref('')
const visible = ref(false)
const form = reactive({ id: null, supplierName: '', contactPerson: '', contactPhone: '', email: '', supplyScope: '', remark: '' })

const filteredList = computed(() => {
  if (!keyword.value) return list.value
  return list.value.filter(r => r.supplierName && r.supplierName.includes(keyword.value))
})

const load = async () => { list.value = await apiGet('/api/supplier') }
const openCreate = () => { Object.assign(form, { id: null, supplierName: '', contactPerson: '', contactPhone: '', email: '', supplyScope: '', remark: '' }); visible.value = true }
const openEdit = (row) => { Object.assign(form, row); visible.value = true }
const save = async () => { await apiPost('/api/supplier', form); visible.value = false; await load() }
const remove = async (id) => { await apiDelete(`/api/supplier/${id}`); await load() }

onMounted(load)
</script>
