<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">角色编码用于后端鉴权与前端菜单装配。</span>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增角色</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="角色台账" description="维护角色编码、名称和职责说明。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="roleCode" label="角色编码" min-width="170" />
        <el-table-column prop="roleName" label="角色名称" min-width="150" />
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该角色？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="RL" title="暂无角色数据" description="创建角色后可用于账号授权与菜单控制。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" :title="form.id ? '编辑角色' : '新增角色'" eyebrow="Role Editor" subtitle="保持与当前后端角色编码体系一致。" width="560">
      <el-form :model="form" label-position="top">
        <el-form-item label="角色编码">
          <el-input v-model="form.roleCode" placeholder="例如 ADMIN" />
        </el-form-item>
        <el-form-item label="角色名称">
          <el-input v-model="form.roleName" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" />
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
import { Key, Tickets, UserFilled } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const visible = ref(false)
const form = reactive({ id: null, roleCode: '', roleName: '', description: '' })

const metrics = computed(() => [
  { label: '角色数量', value: list.value.length, helper: '当前角色定义总数', icon: UserFilled, tone: 'accent' },
  { label: '已编码角色', value: list.value.filter(item => item.roleCode).length, helper: '已具备鉴权编码', icon: Key, tone: 'teal' },
  { label: '已说明角色', value: list.value.filter(item => item.description).length, helper: '已填写职责说明', icon: Tickets, tone: 'neutral' }
])

const load = async () => {
  list.value = await apiGet('/api/rbac/roles')
}

const openCreate = () => {
  Object.assign(form, { id: null, roleCode: '', roleName: '', description: '' })
  visible.value = true
}

const openEdit = (row) => {
  Object.assign(form, { ...row })
  visible.value = true
}

const save = async () => {
  await apiPost('/api/rbac/roles', form)
  visible.value = false
  await load()
}

const remove = async (id) => {
  await apiDelete(`/api/rbac/roles/${id}`)
  await load()
}

onMounted(load)
</script>
