<template>
    <el-dialog :title="title" :visible.sync="isVisible" :before-close="closeDialog" custom-class="view-content">
        <div class="view-content-body">
            <pre>{{content}}</pre>
        </div>
    </el-dialog>
</template>

<script>
export default {
  props: ['title', 'content'],
  data() {
    return {
      isVisible: true,
    };
  },
  methods: {
    closeDialog() {
      this.$emit('close-dialog');
    },
  },
};
</script>
<style lang="sass">
.view-content {
    .el-dialog__body {
        padding: 0px;
    }
    .view-content-body {
        height: 350px;
        overflow-y: auto;
    }
}
</style>
