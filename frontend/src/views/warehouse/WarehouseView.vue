<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">仓库信息用于库存台账、库位配置与跨仓调拨。</span>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增仓库</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="仓库列表" description="维护仓库编码、校区归属和负责人。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="warehouseCode" label="仓库编码" min-width="140" />
        <el-table-column prop="warehouseName" label="仓库名称" min-width="160" />
        <el-table-column prop="campusId" label="校区ID" width="100" />
        <el-table-column prop="campus" label="校区名称" min-width="140" />
        <el-table-column prop="address" label="地址" min-width="220" show-overflow-tooltip />
        <el-table-column prop="manager" label="负责人" width="140" />
        <el-table-column prop="contactPhone" label="联系电话" width="160" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <StatusBadge :label="row.status === 'DISABLED' ? '停用' : '正常'" :tone="row.status === 'DISABLED' ? 'danger' : 'success'" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该仓库？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="WH" title="暂无仓库" description="先创建仓库，再继续配置库位、库存和调拨流程。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" :title="form.id ? '编辑仓库' : '新增仓库'" eyebrow="Warehouse Editor" subtitle="仓库基础信息将直接用于库存和调拨模块。" width="700">
      <el-form :model="form" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="仓库编码">
          <el-input v-model="form.warehouseCode" />
        </el-form-item>
        <el-form-item label="仓库名称">
          <el-input v-model="form.warehouseName" />
        </el-form-item>
        <el-form-item label="所属校区ID">
          <el-input-number v-model="form.campusId" :min="1" />
        </el-form-item>
        <el-form-item label="校区名称">
          <el-input v-model="form.campus" />
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.manager" />
        </el-form-item>
        <el-form-item label="仓库状态">
          <el-select v-model="form.status">
            <el-option label="正常" value="NORMAL" />
            <el-option label="停用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="地址" style="grid-column: 1 / -1;">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" />
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
import { Location, OfficeBuilding, Phone, User } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import StatusBadge from '../../components/ui/StatusBadge.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const visible = ref(false)
const form = reactive({ id: null, warehouseCode: '', warehouseName: '', campusId: null, campus: '', address: '', manager: '', contactPhone: '', status: 'NORMAL', remark: '' })

const metrics = computed(() => [
  { label: '仓库总数', value: list.value.length, helper: '当前已登记仓库', icon: OfficeBuilding, tone: 'accent' },
  { label: '地址覆盖', value: list.value.filter(item => item.address).length, helper: '已填写仓库地址', icon: Location, tone: 'teal' },
  { label: '负责人', value: list.value.filter(item => item.manager).length, helper: '已配置负责人', icon: User, tone: 'neutral' },
  { label: '联系电话', value: list.value.filter(item => item.contactPhone).length, helper: '已配置联系号码', icon: Phone, tone: 'warning' }
])

const load = async () => {
  list.value = await apiGet('/api/warehouse/list')
}

const openCreate = () => {
  Object.assign(form, { id: null, warehouseCode: '', warehouseName: '', campusId: null, campus: '', address: '', manager: '', contactPhone: '', status: 'NORMAL', remark: '' })
  visible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const save = async () => {
  await apiPost('/api/warehouse', form)
  visible.value = false
  await load()
}

const remove = async (id) => {
  await apiDelete(`/api/warehouse/${id}`)
  await load()
}

onMounted(load)
</script>
