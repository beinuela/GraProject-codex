<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">维护校区名称、地址和负责人，为仓库和事件地点提供基础映射。</span>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增校区</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="校区台账" description="统一维护校区地址、负责人和联系电话。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="campusName" label="校区名称" min-width="160" />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="manager" label="负责人" width="140" />
        <el-table-column prop="contactPhone" label="联系电话" width="160" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该校区？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="CP" title="暂无校区数据" description="先创建一个校区节点，再继续维护仓库和事件信息。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" :title="form.id ? '编辑校区' : '新增校区'" eyebrow="Campus Editor" subtitle="校区字段保持原样，便于与现有接口直接对接。" width="620">
      <el-form :model="form" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="校区名称">
          <el-input v-model="form.campusName" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.manager" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="备注" style="grid-column: 1 / -1;">
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
import { Location, OfficeBuilding, Phone, User } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const visible = ref(false)
const form = reactive({ id: null, campusName: '', address: '', manager: '', contactPhone: '', remark: '' })

const metrics = computed(() => [
  { label: '校区总数', value: list.value.length, helper: '已建校区节点', icon: OfficeBuilding, tone: 'accent' },
  { label: '地址覆盖', value: list.value.filter(item => item.address).length, helper: '已填写详细地址', icon: Location, tone: 'teal' },
  { label: '负责人', value: list.value.filter(item => item.manager).length, helper: '已指定负责人', icon: User, tone: 'neutral' },
  { label: '联系电话', value: list.value.filter(item => item.contactPhone).length, helper: '已配置联络方式', icon: Phone, tone: 'warning' }
])

const load = async () => {
  list.value = await apiGet('/api/campus')
}

const openCreate = () => {
  Object.assign(form, { id: null, campusName: '', address: '', manager: '', contactPhone: '', remark: '' })
  visible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const save = async () => {
  await apiPost('/api/campus', form)
  visible.value = false
  await load()
}

const remove = async (id) => {
  await apiDelete(`/api/campus/${id}`)
  await load()
}

onMounted(load)
</script>
