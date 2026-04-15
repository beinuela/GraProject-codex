<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <el-select v-model="categoryFilter" clearable placeholder="按分类筛选" style="width: 220px" @change="load">
          <el-option v-for="category in categories" :key="category.id" :label="category.categoryName" :value="category.id" />
        </el-select>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增物资</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="物资档案" description="维护物资编码、单位、安全库存与分类归属。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="materialName" label="物资名称" min-width="180" />
        <el-table-column prop="materialCode" label="物资编码" min-width="160" />
        <el-table-column prop="categoryId" label="分类ID" width="100" />
        <el-table-column prop="unit" label="单位" width="100" />
        <el-table-column prop="safetyStock" label="安全库存" width="110" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该物资？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="MT" title="暂无物资档案" description="创建物资后，库存、申领和调拨流程才能关联。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" :title="form.id ? '编辑物资' : '新增物资'" eyebrow="Material Editor" subtitle="保持原有字段与接口，统一视觉和录入体验。" width="680">
      <el-form :model="form" label-position="top" class="form-grid form-grid--2">
        <el-form-item label="物资名称">
          <el-input v-model="form.materialName" />
        </el-form-item>
        <el-form-item label="物资编码">
          <el-input v-model="form.materialCode" />
        </el-form-item>
        <el-form-item label="物资分类">
          <el-select v-model="form.categoryId" placeholder="请选择分类">
            <el-option v-for="category in categories" :key="category.id" :label="category.categoryName" :value="category.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="单位">
          <el-input v-model="form.unit" />
        </el-form-item>
        <el-form-item label="安全库存">
          <el-input-number v-model="form.safetyStock" :min="0" />
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
import { Box, CollectionTag, Tickets, Warning } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const categories = ref([])
const categoryFilter = ref(null)
const visible = ref(false)
const form = reactive({ id: null, materialName: '', materialCode: '', categoryId: null, unit: '', safetyStock: 0, remark: '' })

const metrics = computed(() => [
  { label: '物资总数', value: list.value.length, helper: '当前筛选范围内的物资档案', icon: Box, tone: 'accent' },
  { label: '分类覆盖', value: new Set(list.value.map(item => item.categoryId).filter(Boolean)).size, helper: '已使用分类数量', icon: CollectionTag, tone: 'teal' },
  { label: '编码完整', value: list.value.filter(item => item.materialCode).length, helper: '已设置物资编码', icon: Tickets, tone: 'neutral' },
  { label: '安全库存', value: list.value.filter(item => Number(item.safetyStock) > 0).length, helper: '已配置阈值的物资', icon: Warning, tone: 'warning' }
])

const loadCategory = async () => {
  categories.value = await apiGet('/api/material/category')
}

const load = async () => {
  const params = categoryFilter.value ? `?categoryId=${categoryFilter.value}` : ''
  list.value = await apiGet(`/api/material/info${params}`)
}

const openCreate = () => {
  Object.assign(form, { id: null, materialName: '', materialCode: '', categoryId: null, unit: '', safetyStock: 0, remark: '' })
  visible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const save = async () => {
  await apiPost('/api/material/info', form)
  visible.value = false
  await load()
}

const remove = async (id) => {
  await apiDelete(`/api/material/info/${id}`)
  await load()
}

onMounted(async () => {
  await loadCategory()
  await load()
})
</script>
