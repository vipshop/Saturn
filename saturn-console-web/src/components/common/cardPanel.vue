<template>
    <div class="card-panel" :class="panelType">
        <div class="card-panel-heading">
            <el-row>
                <el-col :span="6">
                    <i class="fa-5x" :class="icon"></i>
                </el-col>
                <el-col :span="18" class="text-right">
                    <div class="huge">{{num}}</div>
                    <div class="title-size">{{title}}</div>
                </el-col>
            </el-row>
        </div>
        <a @click="viewDetail">
            <div class="card-panel-footer">
                <span class="pull-left">查看</span>
                <span class="pull-right">
                    <i class="fa fa-arrow-circle-right"></i>
                </span>
            </div>
        </a>
    </div>
</template>
<script>
export default {
  props: ['type', 'title', 'icon', 'num', 'toUrl', 'urlQuery'],
  data() {
    return {
    };
  },
  methods: {
    viewDetail() {
      this.$router.push({ name: this.toUrl, query: { zkCluster: this.urlQuery } });
    },
  },
  computed: {
    panelType() {
      const obj = {
        [`card-panel-${this.type}`]: true,
      };
      return obj;
    },
  },
};
</script>
<style lang="sass" scoped>
.card-panel {
    margin-bottom: 20px;
    background-color: #fff;
    border: 1px solid transparent;
    border-radius: 4px;
    -webkit-box-shadow: 0 1px 1px rgba(0,0,0,.05);
    box-shadow: 0 1px 1px rgba(0,0,0,.05);
}
.card-panel-primary {
    border-color: #337ab7;
    >.card-panel-heading {
        color: #fff;
        background-color: #337ab7;
        border-color: #337ab7;
    }
}
.card-panel-green {
    border-color: #5cb85c;
    >.card-panel-heading {
        background-color: #5cb85c;
        border-color: #5cb85c;
        color: #fff;
    }
}
.card-panel-yellow {
    border-color: #f0ad4e;
    >.card-panel-heading {
        background-color: #f0ad4e;
        border-color: #f0ad4e;
        color: #fff;
    }
}
.card-panel-heading {
    padding: 10px 15px;
    border-bottom: 1px solid transparent;
    border-top-left-radius: 3px;
    border-top-right-radius: 3px;
}
.card-panel-footer {
    height: 20px;
    padding: 10px 15px;
    background-color: #f5f5f5;
    border-top: 1px solid #ddd;
    border-bottom-right-radius: 3px;
    border-bottom-left-radius: 3px;
}
.fa-5x {
    font-size: 5em;
}
.huge {
    font-size: 40px;
    line-height: normal;
}
.title-size {
    font-size: 14px;
}
</style>
