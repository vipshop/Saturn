<template>
    <div class="panel-container">
        <div class="panel-header">
            <span class="pull-left">{{title}}</span>
            <a class="pull-right" @click="togglePanel">
                <i v-if="isExpand" class="fa fa-minus"></i>
                <i v-if="!isExpand" class="fa fa-plus"></i>
            </a>
        </div>
        <div class="panel-body" v-show="isExpand">
            <slot name="chart"></slot>
        </div>
    </div>
</template>

<script>
export default {
  props: ['title'],
  data() {
    return {
      isExpand: true,
    };
  },
  methods: {
    togglePanel() {
      this.isExpand = !this.isExpand;
    },
  },
};
</script>
<style lang="sass" scoped>
.panel-container {
    border: 1px solid #e5e5e5;
    margin-bottom: 20px;
    > .panel-header {
        color: #333;
        background-color: #e9edf1;
        border: 1px solid #e5e5e5;
        height: 35px;
        line-height: 35px;
        padding: 0 10px;
        i {
            height: 35px;
            line-height: 35px;
        }
    }
    > .panel-body {
        padding: 15px;
        background-color: #fff;
    }
}
</style>
