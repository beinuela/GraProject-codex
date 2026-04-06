<template>
  <div class="page-card">
    <h2 class="page-title">系统配置</h2>
    <el-space style="margin-bottom:12px">
      <el-button type="primary" @click="openCreate">新增配置</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="configKey" label="配置键" />
      <el-table-column prop="configValue" label="配置值" show-overflow-tooltip />
      <el-table-column prop="configGroup" label="分组" width="120" />
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

    <el-dialog append-to-body v-model="visible" title="系统配置" width="520">
      <el-form :model="form" label-width="90px">
        <el-form-item label="配置键"><el-input v-model="form.configKey" /></el-form-item>
        <el-form-item label="配置值"><el-input v-model="form.configValue" type="textarea" /></el-form-item>
        <el-form-item label="分组"><el-input v-model="form.configGroup" /></el-form-item>
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
const visible = ref(false)
const filterGroup = ref('')
const form = reactive({ id: null, configKey: '', configValue: '', configGroup: '', remark: '' })

const load = async () => { list.value = await apiGet('/api/config') }
const openCreate = () => { Object.assign(form, { id: null, configKey: '', configValue: '', configGroup: '', remark: '' }); visible.value = true }
const openEdit = (row) => { Object.assign(form, row); visible.value = true }
const save = async () => { await apiPost('/api/config', form); visible.value = false; await load() }
const remove = async (id) => { await apiDelete(`/api/config/${id}`); await load() }

onMounted(load)
</script>
