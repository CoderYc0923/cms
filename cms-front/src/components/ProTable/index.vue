<template>
  <div class="__pro_table">
    <!-- 搜索表单 -->
    <SearchForm
      v-if="props.search !== null"
      class="__pro_table-search_form __pro_table-child"
      :columns="effectColumns.filter((c) => !c.hideInSearch)"
      :labelColSpan="labelColSpan"
      :wrapperColSpan="wrapperColSpan"
      ref="searchFormRef"
      :loading="loading"
      @request="handleSearch"
      @formChange="formChange"
      @modeChange="modeChange"
    />
    <!-- 工具栏 -->
    <div class="__pro_table-tools __pro_table-child">
      <slot name="tools"></slot>
    </div>
    <!-- 表格 -->
    <a-table
      class="__pro_table-table __pro_table-child ant-table-striped"
      :columns="effectColumns.filter((c) => !c.hideInTable)"
      :dataSource="dataSource"
      :pagination="false"
      :rowKey="rowKey"
      :scroll="scroll"
      :loading="loading"
      :rowClassName="
        (_record, index) => (index % 2 === 1 ? 'table-striped' : null)
      "
      v-bind="attrs"
    >
      <template #bodyCell="{ column, record }">
        <!-- 使用插槽来实现自定义 单元格样式 -->
        <slot name="bodyCell" :record="record" :column="column"></slot>
      </template>
      <template v-if="slots.expandedRowRender" #expandedRowRender="{ record }">
        <slot name="expandedRowRender" :record="record"></slot>
      </template>
    </a-table>
    <!-- 分页 -->
    <div class="__pro_table-pagination">
      <a-pagination
        v-if="props.pagination !== null"
        v-model:current="pagin.pageNo"
        :total="pagin.totalCount"
        :pageSize="pagin.pageSize"
        :showTotal="
          (total, range) =>
            `当前显示${range[0]}-${range[1]}条记录，共 ${total} 条记录`
        "
        @change="handleChangePagin"
      />
    </div>
  </div>
</template>

<script setup>
import SearchForm from "@/components/SearchForm/index.vue";
import { clearEmptyPro } from "@/utils/util";
import { reactive, ref, useAttrs, watch } from "vue";

const props = defineProps({
  columns: {
    type: Array,
    default: () => [],
  },
  search: {
    default: () => ({}),
  },
  pagination: {
    default: () => ({}),
  },
  request: {
    type: Function,
    default: () => {},
  },
  rowKey: {
    type: String,
    default: undefined,
  },
  labelColSpan: {
    type: Number,
    default: 6,
  },
  wrapperColSpan: {
    type: Number,
    default: 17,
  },
  scroll: {
    type: Object,
    default: { x: 1300 },
  },
});

const emit = defineEmits(["formChange", "modeChange"]);

const attrs = useAttrs();
const slots = useSlots();
const searchFormRef = ref();
let loading = ref(false);
let dataSource = ref([]);
let pagin = reactive({
  pageNo: 1,
  pageSize: 20,
  totalCount: 0,
});

const effectColumns = ref([]);

// 搜索
const handleSearch = async (params, args = {}) => {
  try {
    const { reset } = args;
    loading.value = true;
    let paramsEffect = {};
    // search === null 表示没有搜索项 有查询条件则将搜索项参数保存
    if (props.search !== null) Object.assign(paramsEffect, { ...params });
    // 是否重置分页参数
    if (reset) {
      pagin.pageNo = 1;
      pagin.pageSize = 20;
      pagin.totalCount = 0;
    }
    // pagination === null 表示没有分页 有分页则将分页参数保存
    if (props.pagination !== null) Object.assign(paramsEffect, { ...pagin });
    clearEmptyPro(paramsEffect);
    const { data, total } = await props.request({
      ...paramsEffect,
      totalCount: undefined,
    });
    dataSource.value = data;
    // 历史bug修复：当total为null时，分页器展示出问题，应设置为0 ——2024.06.21
    props.pagination !== null && (pagin = { ...pagin, totalCount: total ?? 0 });
  } catch (error) {
    console.error(error);
  } finally {
    loading.value = false;
  }
};

// 没有搜索表单
props.search === null && handleSearch();

// 刷新
const reload = () => {
  if (props.search !== null) {
    searchFormRef.value.handleSearch({ reset: false });
  } else {
    handleSearch();
  }
};

// 重置 搜索项 分页 后刷新
const resetAndReload = () => {
  if (props.search !== null) {
    searchFormRef.value.resetSearchForm();
  } else {
    handleSearch({}, { reset: true });
  }
};

// 翻页
const handleChangePagin = (pageNo, pageSize) => {
  pagin = {
    ...pagin,
    pageNo,
    pageSize,
  };
  reload();
};

const formChange = (params) => {
  emit("formChange", params);
};

const modeChange = (mode) => {
  emit("modeChange", mode);
};

watch(
  () => props.columns,
  (value) => {
    effectColumns.value = value;
  },
  {
    deep: true,
    immediate: true,
  }
);

defineExpose({
  reload,
  resetAndReload,
  searchFormRef,
});
</script>

<style lang="less" scoped>
.__pro_table {
  background: #fff;
  padding: 24px;

  &-tools {
    :deep(& button) {
      margin-right: 16px;
    }
  }

  &-pagination {
    .ant-pagination {
      text-align: right !important;
    }
  }
  &-child {
    margin-bottom: 24px;
  }
  .ant-table-striped :deep(.table-striped) td {
    background-color: #fafafa;
  }
}
</style>
