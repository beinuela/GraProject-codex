<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">维护账号、角色编号与部门归属，不改变现有权限接口。</span>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增用户</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="用户列表" description="系统账号、真实姓名、角色与部门关联。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="150" />
        <el-table-column prop="realName" label="真实姓名" min-width="140" />
        <el-table-column prop="roleId" label="角色ID" width="100" />
        <el-table-column prop="deptId" label="部门ID" width="100" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <StatusBadge :label="row.status === 1 ? '启用' : '禁用'" :tone="row.status === 1 ? 'success' : 'danger'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-button size="small" type="warning" @click="resetPassword(row)">重置密码</el-button>
              <el-popconfirm title="确认删除该用户？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="US" title="暂无用户数据" description="先创建一个系统账号以开始分配权限。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell
      v-model="visible"
      :title="form.id ? '编辑用户' : '新增用户'"
      eyebrow="User Editor"
      subtitle="保留现有字段结构与保存接口。"
      width="640"
    >
      <el-form :model="form" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="用户名">
          <el-input v-model="form.username" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" :placeholder="form.id ? '留空则不修改' : '请输入密码'" />
        </el-form-item>
        <el-form-item label="真实姓名">
          <el-input v-model="form.realName" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option :value="1" label="启用" />
            <el-option :value="0" label="禁用" />
          </el-select>
        </el-form-item>
        <el-form-item label="角色ID">
          <el-input-number v-model="form.roleId" :min="1" />
        </el-form-item>
        <el-form-item label="部门ID">
          <el-input-number v-model="form.deptId" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </DialogShell>
  </PageScaffold>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Lock, OfficeBuilding, User, UserFilled } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const visible = ref(false)
const form = reactive({ id: null, username: '', password: '', realName: '', roleId: null, deptId: null, status: 1 })

const metrics = computed(() => [
  { label: '账号总数', value: list.value.length, helper: '系统内可维护用户', icon: UserFilled, tone: 'accent' },
  { label: '启用账号', value: list.value.filter(item => item.status === 1).length, helper: '当前处于启用状态', icon: User, tone: 'success' },
  { label: '禁用账号', value: list.value.filter(item => item.status !== 1).length, helper: '已被停用的账号', icon: Lock, tone: 'warning' },
  { label: '覆盖部门', value: new Set(list.value.map(item => item.deptId).filter(Boolean)).size, helper: '已关联部门数量', icon: OfficeBuilding, tone: 'teal' }
])

const load = async () => {
  list.value = await apiGet('/api/rbac/users')
}

const openCreate = () => {
  Object.assign(form, { id: null, username: '', password: '', realName: '', roleId: null, deptId: null, status: 1 })
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

const resetPassword = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt(`为账号 ${row.username} 设置新密码`, '重置密码', {
      confirmButtonText: '确认重置',
      cancelButtonText: '取消',
      inputType: 'password',
      inputPattern: /^.{6,}$/,
      inputErrorMessage: '密码长度至少6位'
    })
    await apiPost(`/api/rbac/users/${row.id}/reset-password`, { password: value })
    ElMessage.success('密码已重置')
  } catch (_) {
    // 用户取消时不提示错误。
  }
}

onMounted(load)
</script>
