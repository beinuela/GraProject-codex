<template>
  <div class="page-card">
    <h2 class="page-title">物资分类管理</h2>
    <el-space style="margin-bottom:12px">
      <el-button type="primary" @click="openCreate">新增分类</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="categoryName" label="分类名称" />
      <el-table-column prop="remark" label="备注" />
      <el-table-column label="操作" width="170">
        <template #default="scope">
          <el-space>
            <el-button size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-popconfirm title="确定删除？" @confirm="remove(scope.row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </el-space>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="visible" title="分类信息" width="460">
      <el-form :model="form" label-width="90px">
        <el-form-item label="分类名称"><el-input v-model="form.categoryName" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
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
const visible = ref(false)
const form = reactive({ id: null, categoryName: '', remark: '' })

const load = async () => { list.value = await apiGet('/api/material/category') }
const openCreate = () => { Object.assign(form, { id: null, categoryName: '', remark: '' }); visible.value = true }
const openEdit = (row) => { Object.assign(form, row); visible.value = true }
const save = async () => { await apiPost('/api/material/category', form); visible.value = false; await load() }
const remove = async (id) => { await apiDelete(`/api/material/category/${id}`); await load() }

onMounted(load)
</script>
