<template>
  <div class="space-manage">
    <div class="space-manage__toolbar">
      <div class="space-manage__title">空间管理</div>
      <div class="space-manage__actions">
        <a-select
          v-model:value="statusFilter"
          style="width: 140px"
          allow-clear
          placeholder="全部状态"
          @change="loadList"
        >
          <a-select-option :value="1">启用</a-select-option>
          <a-select-option :value="0">禁用</a-select-option>
        </a-select>
        <a-button type="primary" @click="openCreate">新建空间</a-button>
      </div>
    </div>

    <a-table
      row-key="id"
      :columns="columns"
      :data-source="list"
      :loading="loading"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 1 ? 'success' : 'default'">
            {{ record.status === 1 ? '启用' : '禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a @click="openEdit(record)">编辑</a>
            <a @click="goWorkspace(record)">进入</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="modalVisible"
      :title="isEdit ? '编辑空间' : '新建空间'"
      :confirm-loading="submitting"
      destroy-on-close
      @ok="handleSubmit"
    >
      <a-form layout="vertical" :model="form">
        <a-form-item label="名称" required>
          <a-input
            v-model:value="form.name"
            :maxlength="10"
            placeholder="最多 10 个字符"
          />
        </a-form-item>
        <a-form-item v-if="!isEdit" label="Slug" required>
          <a-input
            v-model:value="form.slug"
            :maxlength="10"
            placeholder="英文标识，创建后不可改"
          />
        </a-form-item>
        <a-form-item v-else label="Slug">
          <a-input :value="form.slug" disabled />
        </a-form-item>
        <a-form-item label="描述">
          <a-textarea
            v-model:value="form.description"
            :maxlength="255"
            :rows="3"
            placeholder="可选"
          />
        </a-form-item>
        <a-form-item label="排序" required>
          <a-input-number
            v-model:value="form.sort"
            :min="0"
            style="width: 100%"
          />
        </a-form-item>
        <a-form-item v-if="isEdit" label="状态" required>
          <a-radio-group v-model:value="form.status">
            <a-radio :value="1">启用</a-radio>
            <a-radio :value="0">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { message } from 'ant-design-vue'
import { createSpace, updateSpace, getSpaceList } from '@/service/space'
import { useSpacesStore } from '@/stores/spaces'

const router = useRouter()
const spacesStore = useSpacesStore()

const columns = [
  { title: 'ID', dataIndex: 'id', key: 'id', width: 80 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: 'Slug', dataIndex: 'slug', key: 'slug' },
  { title: '描述', dataIndex: 'description', key: 'description', ellipsis: true },
  { title: '排序', dataIndex: 'sort', key: 'sort', width: 80 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 140 }
]

const [list, setList] = useState([])
const [loading, setLoading] = useState(false)
const [statusFilter, setStatusFilter] = useState(undefined)
const [modalVisible, setModalVisible] = useState(false)
const [isEdit, setIsEdit] = useState(false)
const [submitting, setSubmitting] = useState(false)
const [editingId, setEditingId] = useState(null)

const defaultForm = () => ({
  name: '',
  slug: '',
  description: '',
  sort: 0,
  status: 1
})

const [form, setForm] = useState(defaultForm())

const loadList = async () => {
  setLoading(true)
  try {
    const res = await getSpaceList(statusFilter.value)
    if (res.code === 0 || res.code === 200) {
      setList(res.data || [])
    }
  } catch (error) {
    message.error(error?.message || '加载空间列表失败')
  } finally {
    setLoading(false)
  }
}

const openCreate = () => {
  setIsEdit(false)
  setEditingId(null)
  setForm(defaultForm())
  setModalVisible(true)
}

const openEdit = record => {
  setIsEdit(true)
  setEditingId(record.id)
  setForm({
    name: record.name || '',
    slug: record.slug || '',
    description: record.description || '',
    sort: record.sort ?? 0,
    status: record.status ?? 1
  })
  setModalVisible(true)
}

const goWorkspace = record => {
  if (!record?.slug) {
    return
  }
  router.push(`/${record.slug}`)
}

const validateForm = () => {
  if (!form.value.name?.trim()) {
    message.warning('请输入名称')
    return false
  }
  if (!isEdit.value && !form.value.slug?.trim()) {
    message.warning('请输入 slug')
    return false
  }
  if (form.value.sort == null || form.value.sort < 0) {
    message.warning('请输入合法排序')
    return false
  }
  if (isEdit.value && (form.value.status !== 0 && form.value.status !== 1)) {
    message.warning('请选择状态')
    return false
  }
  return true
}

const handleSubmit = async () => {
  if (!validateForm()) {
    return
  }
  setSubmitting(true)
  try {
    if (isEdit.value) {
      await updateSpace(editingId.value, {
        name: form.value.name.trim(),
        description: form.value.description?.trim() || '',
        sort: form.value.sort,
        status: form.value.status
      })
      message.success('更新成功')
    } else {
      await createSpace({
        name: form.value.name.trim(),
        slug: form.value.slug.trim(),
        description: form.value.description?.trim() || '',
        sort: form.value.sort
      })
      message.success('创建成功')
    }
    setModalVisible(false)
    await loadList()
    // 刷新顶栏启用空间列表
    await spacesStore.fetchList(1)
  } catch (error) {
    message.error(error?.message || '保存失败')
  } finally {
    setSubmitting(false)
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped lang="less">
.space-manage {
  height: 100%;
  padding: 24px 32px;
  overflow: auto;
  background: var(--color-bg-surface);

  &__toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 0;
    padding-bottom: 16px;
    border-bottom: 1px solid var(--color-border);
  }

  &__title {
    font-size: var(--text-title-md);
    font-weight: 600;
    color: var(--color-text-primary);
  }

  &__actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  :deep(.ant-table) {
    margin-top: 8px;
  }

  :deep(.ant-table-thead > tr > th) {
    font-size: var(--text-label);
    font-weight: 500;
    color: var(--color-text-secondary);
    background: transparent;
    border-bottom: 1px solid var(--color-border);
  }

  :deep(.ant-table-tbody > tr > td) {
    border-bottom: 1px solid var(--color-border);
  }

  :deep(.ant-table-tbody > tr:hover > td) {
    background: var(--color-bg-hover);
  }

  :deep(.ant-table-cell) a {
    color: var(--color-primary);

    &:hover {
      color: var(--color-primary-hover);
    }
  }
}
</style>
