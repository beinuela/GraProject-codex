<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <el-input v-model="keyword" clearable placeholder="搜索供应商名称" style="width: 240px" />
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增供应商</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="供应商列表" description="统一维护联系人、邮箱和供应范围。" :badge="`${filteredList.length} 条`">
      <el-table :data="filteredList" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="supplierName" label="供应商名称" min-width="180" />
        <el-table-column prop="contactPerson" label="联系人" width="140" />
        <el-table-column prop="contactPhone" label="联系电话" width="160" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="supplyScope" label="供应范围" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该供应商？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="SP" title="暂无供应商" description="录入供应商后可为采购与补货提供基础资料。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" :title="form.id ? '编辑供应商' : '新增供应商'" eyebrow="Supplier Editor" subtitle="供应商字段与原接口完全兼容。" width="720">
      <el-form :model="form" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="供应商名称">
          <el-input v-model="form.supplierName" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model="form.contactPerson" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" />
        </el-form-item>
        <el-form-item label="供应范围" style="grid-column: 1 / -1;">
          <el-input v-model="form.supplyScope" type="textarea" />
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
import { MessageBox, Phone, Promotion, User } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const keyword = ref('')
const visible = ref(false)
const form = reactive({ id: null, supplierName: '', contactPerson: '', contactPhone: '', email: '', supplyScope: '', remark: '' })

const filteredList = computed(() => {
  if (!keyword.value) return list.value
  return list.value.filter(item => item.supplierName && item.supplierName.includes(keyword.value))
})

const metrics = computed(() => [
  { label: '供应商总数', value: list.value.length, helper: '当前供应商台账总量', icon: Promotion, tone: 'accent' },
  { label: '搜索结果', value: filteredList.value.length, helper: '当前筛选命中数量', icon: MessageBox, tone: 'neutral' },
  { label: '联系人', value: list.value.filter(item => item.contactPerson).length, helper: '已配置联系人', icon: User, tone: 'teal' },
  { label: '联系电话', value: list.value.filter(item => item.contactPhone).length, helper: '已配置联络方式', icon: Phone, tone: 'warning' }
])

const load = async () => {
  list.value = await apiGet('/api/supplier')
}

const openCreate = () => {
  Object.assign(form, { id: null, supplierName: '', contactPerson: '', contactPhone: '', email: '', supplyScope: '', remark: '' })
  visible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const save = async () => {
  await apiPost('/api/supplier', form)
  visible.value = false
  await load()
}

const remove = async (id) => {
  await apiDelete(`/api/supplier/${id}`)
  await load()
}

onMounted(load)
</script>
