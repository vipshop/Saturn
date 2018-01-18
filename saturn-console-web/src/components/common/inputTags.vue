<template>
    <div class="input-tags">
        <el-tag :key="tag" v-for="tag in dynamicTags" closable :disable-transitions="false" @close="handleClose(tag)">{{tag}}</el-tag>
        <el-input
        class="input-new-tag"
        v-if="inputVisible"
        v-model="inputValue"
        ref="saveTagInput"
        size="small"
        @keyup.enter.native="handleInputConfirm"
        @blur="handleInputConfirm">
        </el-input>
        <el-button v-else class="button-new-tag" size="small" @click="showInput">+ {{title}}</el-button>
    </div>
</template>
<script>
export default {
  props: ['dynamicTags', 'title'],
  data() {
    return {
      inputVisible: false,
      inputValue: '',
    };
  },
  methods: {
    handleClose(tag) {
      this.dynamicTags.splice(this.dynamicTags.indexOf(tag), 1);
    },
    showInput() {
      this.inputVisible = true;
      this.$nextTick(() => {
        this.$refs.saveTagInput.$refs.input.focus();
      });
    },
    handleInputConfirm() {
      const inputValue = this.inputValue;
      if (inputValue) {
        this.dynamicTags.push(inputValue);
      }
      this.inputVisible = false;
      this.inputValue = '';
    },
  },
};
</script>
<style lang="sass" scoped>
.input-tags {
    padding: 10px;
    border: 1px solid #b7c0c9;
    width: auto;
    min-height: 100px;
    height: 100px;
    overflow-y: auto;
}
.el-tag {
  margin-right: 5px;
  margin-bottom: 5px;
}
.button-new-tag {
  height: 24px;
  line-height: 24px;
  padding-top: 0;
  padding-bottom: 0;
}
.input-new-tag {
  width: 90px;
  margin-left: 10px;
  vertical-align: bottom;
}
</style>
