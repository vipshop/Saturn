<template>
    <el-dialog :title="title" :visible.sync="isVisible" :before-close="closeDialog">
        <fieldset class="fieldset-border">
            <div class="view-content">
                <pre>{{content}}</pre>
            </div>
        </fieldset>
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
<style lang="sass" scoped>
.fieldset-border {
    border: 1px solid #ccc;
}
.view-content {
    height: 350px;
    overflow-y: auto;
}
</style>
