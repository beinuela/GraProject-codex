<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">部门是用户归属和业务统计的基础维度。</span>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增部门</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="部门台账" description="维护部门名称、编码和备注信息。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="deptName" label="部门名称" min-width="180" />
        <el-table-column prop="deptCode" label="部门编码" min-width="160" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该部门？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="DP" title="暂无部门信息" description="创建部门后，用户可以绑定到对应组织单元。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" :title="form.id ? '编辑部门' : '新增部门'" eyebrow="Department Editor" subtitle="维护部门名称、编码和备注。" width="560">
      <el-form :model="form" label-position="top">
        <el-form-item label="部门名称">
          <el-input v-model="form.deptName" />
        </el-form-item>
        <el-form-item label="部门编码">
          <el-input v-model="form.deptCode" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
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
import { DataBoard, Memo, Tickets } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const visible = ref(false)
const form = reactive({ id: null, deptName: '', deptCode: '', remark: '' })

const metrics = computed(() => [
  { label: '部门数量', value: list.value.length, helper: '当前组织单元总数', icon: DataBoard, tone: 'accent' },
  { label: '已编码部门', value: list.value.filter(item => item.deptCode).length, helper: '已设置编码规则', icon: Tickets, tone: 'teal' },
  { label: '备注覆盖', value: list.value.filter(item => item.remark).length, helper: '已填写说明的部门', icon: Memo, tone: 'neutral' }
])

const load = async () => {
  list.value = await apiGet('/api/rbac/depts')
}

const openCreate = () => {
  Object.assign(form, { id: null, deptName: '', deptCode: '', remark: '' })
  visible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const save = async () => {
  await apiPost('/api/rbac/depts', form)
  visible.value = false
  await load()
}

const remove = async (id) => {
  await apiDelete(`/api/rbac/depts/${id}`)
  await load()
}

onMounted(load)
</script>
