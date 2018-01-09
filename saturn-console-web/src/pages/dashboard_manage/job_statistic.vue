<template>
    <div>作业{{zkCluster}}</div>
</template>
<script>
export default {
  data() {
    return {
      zkCluster: this.$route.query.zkCluster,
    };
  },
  methods: {
  },
};
</script>
