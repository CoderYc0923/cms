<template>
  <a-anchor :affix="false" :items="items" :get-container="container" @click="handleClick"></a-anchor>
</template>

<script setup>
const props = defineProps({
  anchorItems: {
    type: Array,
    default: () => []
  },
  container: {
    type: Function,
    default: () => window
  }
});

const [items, setItems] = useState([]);

const handleInitItems = anchorItems => {
  setItems(anchorItems);
};

const handleClick = (e, link) => {
  e.preventDefault();

  const targetId = link.href.replace("#", "");
  const targetElement = document.getElementById(targetId);

  if (targetElement) {
    targetElement.scrollIntoView({ behavior: "smooth" });
  }
};

watch(
  () => props.anchorItems,
  anchorItems => {
    if (anchorItems) {
      handleInitItems(anchorItems);
    }
  },
  {
    immediate: true
  }
);
</script>

<style lang="less" scoped>
</style>
