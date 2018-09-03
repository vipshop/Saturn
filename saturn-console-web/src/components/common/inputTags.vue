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
        if (this.title === '日期段') {
          const dateParten = /^(0[1-9]|1[0-2])\/(0[1-9]|[1-2][0-9]|3[0-1])-(0[1-9]|1[0-2])\/(0[1-9]|[1-2][0-9]|3[0-1])$/;
          if (dateParten.test(inputValue)) {
            this.dynamicTags.push(inputValue);
          } else {
            this.$message.errorMessage('请输入正确的日期格式，例如03/10-03/20');
          }
        } else {
          const timeParten = /^(0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])-(0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])$/;
          if (timeParten.test(inputValue)) {
            this.dynamicTags.push(inputValue);
          } else {
            this.$message.errorMessage('请输入正确的时间格式，例如10:00-11:00');
          }
        }
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
