<template>
  <div class="page-card">
    <h2 class="page-title">用户管理</h2>
    <el-space wrap style="margin-bottom: 12px">
      <el-button type="primary" @click="openCreate">新增用户</el-button>
      <el-button @click="load">刷新</el-button>
    </el-space>

    <el-table :data="list" border>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="realName" label="姓名" />
      <el-table-column prop="deptId" label="部门ID" />
      <el-table-column prop="roleId" label="角色ID" />
      <el-table-column prop="status" label="状态" width="90" />
      <el-table-column label="操作" width="180">
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

    <el-dialog v-model="visible" title="用户信息" width="560">
      <el-form :model="form" label-width="90px">
        <el-form-item label="用户名"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="密码"><el-input v-model="form.password" type="password" show-password /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="部门"><el-select v-model="form.deptId" style="width:100%"><el-option v-for="d in depts" :key="d.id" :label="d.deptName" :value="d.id" /></el-select></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.roleId" style="width:100%"><el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" /></el-select></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" /></el-form-item>
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
const roles = ref([])
const depts = ref([])
const visible = ref(false)
const form = reactive({ id: null, username: '', password: '', realName: '', deptId: null, roleId: null, status: 1 })

const load = async () => {
  list.value = await apiGet('/api/rbac/users')
  roles.value = await apiGet('/api/rbac/roles')
  depts.value = await apiGet('/api/rbac/depts')
}

const openCreate = () => {
  Object.assign(form, { id: null, username: '', password: '', realName: '', deptId: null, roleId: null, status: 1 })
  visible.value = true
}

const openEdit = (row) => {
  Object.assign(form, { ...row, password: '' })
  visible.value = true
}

const save = async () => {
  await apiPost('/api/rbac/users', form)
  visible.value = false
  await load()
}

const remove = async (id) => {
  await apiDelete(`/api/rbac/users/${id}`)
  await load()
}

onMounted(load)
</script>
