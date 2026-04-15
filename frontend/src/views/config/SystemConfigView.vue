<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <el-input v-model="groupFilter" clearable placeholder="按分组筛选" style="width: 220px" />
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增配置</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="系统配置" description="维护配置键、配置值和所属分组。" :badge="`${filteredList.length} 条`">
      <el-table :data="filteredList" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="configKey" label="配置键" min-width="180" />
        <el-table-column prop="configValue" label="配置值" min-width="220" show-overflow-tooltip />
        <el-table-column prop="configGroup" label="分组" width="140" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该配置？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="CF" title="暂无系统配置" description="创建配置项后，可集中管理业务参数与开关。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" :title="form.id ? '编辑配置' : '新增配置'" eyebrow="Config Editor" subtitle="统一维护配置键值和配置分组。" width="640">
      <el-form :model="form" label-position="top">
        <el-form-item label="配置键">
          <el-input v-model="form.configKey" />
        </el-form-item>
        <el-form-item label="配置值">
          <el-input v-model="form.configValue" type="textarea" />
        </el-form-item>
        <el-form-item label="分组">
          <el-input v-model="form.configGroup" />
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
import { CollectionTag, Setting, Tickets } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const visible = ref(false)
const groupFilter = ref('')
const form = reactive({ id: null, configKey: '', configValue: '', configGroup: '', remark: '' })

const filteredList = computed(() => {
  if (!groupFilter.value) return list.value
  return list.value.filter(item => item.configGroup && item.configGroup.includes(groupFilter.value))
})

const metrics = computed(() => [
  { label: '配置总数', value: list.value.length, helper: '系统内已维护的配置项', icon: Setting, tone: 'accent' },
  { label: '分组数量', value: new Set(list.value.map(item => item.configGroup).filter(Boolean)).size, helper: '当前配置分组数', icon: CollectionTag, tone: 'teal' },
  { label: '筛选命中', value: filteredList.value.length, helper: '当前分组筛选结果', icon: Tickets, tone: 'neutral' }
])

const load = async () => {
  list.value = await apiGet('/api/config')
}

const openCreate = () => {
  Object.assign(form, { id: null, configKey: '', configValue: '', configGroup: '', remark: '' })
  visible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const save = async () => {
  await apiPost('/api/config', form)
  visible.value = false
  await load()
}

const remove = async (id) => {
  await apiDelete(`/api/config/${id}`)
  await load()
}

onMounted(load)
</script>
