<template>
    <div class="panel" :class="panelType">
        <div class="panel-content-title">
            <slot name="title"></slot>
        </div>
        <div class="panel-content-body">
            <slot name="content"></slot>
        </div>
    </div>
</template>

<script>
export default {
  props: ['type'],
  data() {
    return {
    };
  },
  methods: {
  },
  computed: {
    panelType() {
      const obj = {
        [`panel-${this.type}-info`]: true,
      };
      return obj;
    },
  },
};
</script>

<style lang="sass" scoped>
.panel {
    background-color: #fff;
    border: 1px solid transparent;
    border-radius: 4px;
    -webkit-box-shadow: 0 1px 1px rgba(0,0,0,.05);
    box-shadow: 0 1px 1px rgba(0,0,0,.05);
}
.panel-default-info {
    background-color: #3d3d3d;
    padding-top: 10px;
    padding-bottom: 5px;
    border: 1px solid #262626;
    opacity: .8;
}
.panel-danger-info {
    background-color: #c00;
    padding-top: 10px;
    padding-bottom: 5px;
    text-decoration: none;
    opacity: .8;
    &:hover {
      background-color: red;
    }
}
.panel-warning-info {
    background-color: #f80;
    padding-top: 10px;
    padding-bottom: 5px;
    text-decoration: none;
    opacity: .8;
}
.panel-success-info {
    background-color: #67c23a;
    padding-top: 10px;
    padding-bottom: 5px;
    text-decoration: none;
    opacity: .8;
}
.panel-content-title {
    text-align: center;
    color: #fff;
    font-size: 15px;
}
.panel-content-body {
    text-align: center;
    color: #fff;
    font-size: 22px;
    padding-top: 5px;
}
</style>
