<template>
  <div class="page-card">
    <h2 class="page-title">用户管理</h2>
    <el-space style="margin-bottom:12px">
      <el-button type="primary" @click="openCreate">新增用户</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="真实姓名" />
      <el-table-column prop="roleId" label="角色ID" width="90" />
      <el-table-column prop="deptId" label="部门ID" width="90" />
      <el-table-column prop="status" label="状态" width="80">
        <template #default="scope">
          <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 1 ? '启用' : '禁用' }}
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

    <el-dialog v-model="visible" :title="form.id ? '编辑用户' : '新增用户'" width="520">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" :placeholder="form.id ? '留空不修改' : '请输入密码'" /></el-form-item>
        <el-form-item label="真实姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色ID"><el-input-number v-model="form.roleId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="部门ID"><el-input-number v-model="form.deptId" :min="1" style="width:100%" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
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
const form = reactive({ id: null, username: '', password: '', realName: '', roleId: null, deptId: null, status: 1 })

const load = async () => { list.value = await apiGet('/api/rbac/users') }
const openCreate = () => {
  Object.assign(form, { id: null, username: '', password: '', realName: '', roleId: null, deptId: null, status: 1 })
  visible.value = true
}
const openEdit = (row) => { Object.assign(form, { ...row, password: '' }); visible.value = true }
const save = async () => {
  await apiPost('/api/rbac/users', form)
  visible.value = false
  await load()
}
const remove = async (id) => { await apiDelete(`/api/rbac/users/${id}`); await load() }

onMounted(load)
</script>
