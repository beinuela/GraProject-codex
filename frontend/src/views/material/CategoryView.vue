<template>
  <PageScaffold :metrics="metrics">
    <FilterActionBar>
      <template #filters>
        <span class="table-note">分类编码用于承接物资档案的归类与统计分析。</span>
      </template>
      <template #actions>
        <el-button type="primary" @click="openCreate">新增分类</el-button>
        <el-button @click="load">刷新</el-button>
      </template>
    </FilterActionBar>

    <TableShell title="分类列表" description="维护分类名称、编码和备注。" :badge="`${list.length} 条`">
      <el-table :data="list" class="list-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="categoryName" label="分类名称" min-width="180" />
        <el-table-column prop="categoryCode" label="分类编码" min-width="160" />
        <el-table-column prop="remark" label="备注" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="inline-actions">
              <el-button size="small" @click="openEdit(row)">编辑</el-button>
              <el-popconfirm title="确认删除该分类？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button size="small" type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
        <template #empty>
          <EmptyState glyph="CT" title="暂无分类数据" description="先创建分类，物资档案才能归入统一目录。" />
        </template>
      </el-table>
    </TableShell>

    <DialogShell v-model="visible" :title="form.id ? '编辑分类' : '新增分类'" eyebrow="Category Editor" subtitle="分类字段与保存接口保持不变。" width="560">
      <el-form :model="form" label-position="top">
        <el-form-item label="分类名称">
          <el-input v-model="form.categoryName" />
        </el-form-item>
        <el-form-item label="分类编码">
          <el-input v-model="form.categoryCode" />
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
import { CollectionTag, Memo, Tickets } from '@element-plus/icons-vue'
import { apiDelete, apiGet, apiPost } from '../../api'
import DialogShell from '../../components/ui/DialogShell.vue'
import EmptyState from '../../components/ui/EmptyState.vue'
import FilterActionBar from '../../components/ui/FilterActionBar.vue'
import PageScaffold from '../../components/ui/PageScaffold.vue'
import TableShell from '../../components/ui/TableShell.vue'

const list = ref([])
const visible = ref(false)
const form = reactive({ id: null, categoryName: '', categoryCode: '', remark: '' })

const metrics = computed(() => [
  { label: '分类总数', value: list.value.length, helper: '当前维护的分类数', icon: CollectionTag, tone: 'accent' },
  { label: '已编码分类', value: list.value.filter(item => item.categoryCode).length, helper: '已设置编码规则', icon: Tickets, tone: 'teal' },
  { label: '备注覆盖', value: list.value.filter(item => item.remark).length, helper: '已填写说明', icon: Memo, tone: 'neutral' }
])

const load = async () => {
  list.value = await apiGet('/api/material/category')
}

const openCreate = () => {
  Object.assign(form, { id: null, categoryName: '', categoryCode: '', remark: '' })
  visible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  visible.value = true
}

const save = async () => {
  await apiPost('/api/material/category', form)
  visible.value = false
  await load()
}

const remove = async (id) => {
  await apiDelete(`/api/material/category/${id}`)
  await load()
}

onMounted(load)
</script>
