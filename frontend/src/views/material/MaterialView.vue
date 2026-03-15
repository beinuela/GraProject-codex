<template>
  <div class="page-card">
    <h2 class="page-title">物资信息</h2>
    <el-space style="margin-bottom:12px">
      <el-select v-model="categoryFilter" placeholder="按分类筛选" clearable style="width:180px" @change="load">
        <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="openCreate">新增物资</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="materialName" label="物资名称" />
      <el-table-column prop="materialCode" label="物资编码" />
      <el-table-column prop="categoryId" label="分类ID" width="90" />
      <el-table-column prop="unit" label="单位" width="80" />
      <el-table-column prop="safetyStock" label="安全库存" width="100" />
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
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

    <el-dialog v-model="visible" title="物资信息" width="560">
      <el-form :model="form" label-width="90px">
        <el-form-item label="物资名称"><el-input v-model="form.materialName" /></el-form-item>
        <el-form-item label="物资编码"><el-input v-model="form.materialCode" /></el-form-item>
        <el-form-item label="物资分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.categoryName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位"><el-input v-model="form.unit" /></el-form-item>
        <el-form-item label="安全库存"><el-input-number v-model="form.safetyStock" :min="0" style="width:100%" /></el-form-item>
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
const categories = ref([])
const categoryFilter = ref(null)
const visible = ref(false)
const form = reactive({ id: null, materialName: '', materialCode: '', categoryId: null, unit: '', safetyStock: 0, remark: '' })

const loadCategory = async () => { categories.value = await apiGet('/api/material/category') }
const load = async () => {
  const params = categoryFilter.value ? `?categoryId=${categoryFilter.value}` : ''
  list.value = await apiGet(`/api/material/info${params}`)
}
const openCreate = () => { Object.assign(form, { id: null, materialName: '', materialCode: '', categoryId: null, unit: '', safetyStock: 0, remark: '' }); visible.value = true }
const openEdit = (row) => { Object.assign(form, row); visible.value = true }
const save = async () => { await apiPost('/api/material/info', form); visible.value = false; await load() }
const remove = async (id) => { await apiDelete(`/api/material/info/${id}`); await load() }

onMounted(async () => { await loadCategory(); await load() })
</script>
