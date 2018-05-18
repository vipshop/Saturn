<template>
<div class="fav-container">
  <el-popover trigger="click" ref="popover" title="我的收藏" @show="show = true" @hide="show = false" popper-class="my-fav" placement="bottom" width="400">
    <el-form :model="form">
      <el-table :data="favorites" :show-header="true" v-if="show" class="fav-table">
        <el-table-column property="title" label="名称" show-overflow-tooltip>
          <template slot-scope="scope">
            <el-form-item v-if="scope.row.path === editPath" style="margin-bottom: 0;">
              <el-input v-model="form.title" :placeholder="scope.row.title"></el-input>
            </el-form-item>
            <router-link :to="{ path: scope.row.path}" v-else>
              {{scope.row.title}}
            </router-link>
          </template>
        </el-table-column>
        <!-- <el-table-column property="date" label="日期" show-overflow-tooltip>
          <template slot-scope="scope">
            {{scope.row.date | formatDate}}
          </template>
        </el-table-column> -->
        <el-table-column
          property="group"
          label="分组"
          show-overflow-tooltip
          :filters="groupsList"
          :filter-method="filterGroup"
          filter-placement="bottom-end">
          <template slot-scope="scope">
            <el-form-item v-if="scope.row.path === editPath" style="margin-bottom: 0;">
              <el-select allow-create filterable size="small" v-model="form.group" style="width: 100%">
                  <el-option v-for="item in groupsList" :label="item.text" :value="item.value" :key="item.value"></el-option>
              </el-select>
            </el-form-item>
            <span v-else>{{scope.row.group || '暂无分组'}}</span>
          </template>
        </el-table-column>
        <el-table-column width="80px" label="操作">
          <template slot-scope="scope">
            <div v-if="scope.row.path === editPath">
              <el-tooltip class="item" effect="dark" content="保存" placement="top" key="floppy">
                <el-button type="text" @click="submitForm(scope.row.path)"><i class="fa fa-floppy-o" aria-hidden="true"></i></el-button>
              </el-tooltip>
              <el-tooltip class="item" effect="dark" content="取消" placement="top" key="undo">
                <el-button type="text" @click.stop="resetFields"><i class="fa fa-undo" aria-hidden="true"></i></el-button>
              </el-tooltip>
            </div>
            <div v-else>
              <el-tooltip class="item" effect="dark" content="修改" placement="top" key="edit">
                <el-button type="text" icon="el-icon-edit" @click="edit(scope.row, scope.row.path)"></el-button>
              </el-tooltip>
              <el-tooltip class="item" effect="dark" content="删除" placement="top" key="delete">
                <el-button type="text" :key="scope.row.path" icon="el-icon-delete text-danger" v-favorites:favorites.del="scope.row.path" @favorites-del="showMessage"></el-button>
              </el-tooltip>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-form>
  </el-popover>
  <el-popover
    trigger="click"
    ref="popover1"
    title="添加收藏"
    placement="bottom"
    v-model="visible"
    popper-class="my-fav"
    width="350">
    <el-form :model="form">
      <el-form-item label="名称" label-width="40px" style="margin: 9px 0;">
        <el-input v-model="form.title" size="small" auto-complete="off" placeholder="输入名称"></el-input>
      </el-form-item>
      <el-form-item label="分组" label-width="40px" style="margin-bottom: 9px;">
          <el-select clearable allow-create filterable size="small" v-model="form.group" style="width: 100%">
              <el-option v-for="item in groupsList" :label="item.text" :value="item.value" :key="item.value"></el-option>
          </el-select>
      </el-form-item>
      <div style="text-align: right; margin: 0">
        <el-button size="mini" type="default"  @click="resetFields">取 消</el-button>
        <el-button type="primary" size="mini" @click="submitForm()">确 定</el-button>
      </div>
    </el-form>
  </el-popover>
  <el-tooltip class="item" effect="dark" content="添加收藏" placement="top-start">
    <el-button type="text" v-popover:popover1 v-favorites:favorites.add="unit" @favorites-add="showMessage">
      <i :class="['fa', isFav ? 'fa-heart' : 'fa-heart-o']" aria-hidden="true"></i>
    </el-button>
  </el-tooltip>
  <el-tooltip class="item" effect="dark" content="查看收藏" placement="top-start">
    <el-button type="text" v-popover:popover v-favorites:favorites>
      <i :class="['fa', hasFav ? 'fa-star' : 'fa-star-o']" aria-hidden="true"></i>
    </el-button>
  </el-tooltip> 
</div>
</template>
<script>
export default {
  name: 'Favorites',
  props: ['unit'],
  data() {
    return {
      show: false,
      visible: false,
      favorites: this.$favorites(),
      editPath: '',
      form: {
        title: '',
        group: '',
      },
    };
  },
  watch: {
    $route: function init() {
      this.favorites = this.$favorites();
    },
  },
  computed: {
    isFav() {
      return this.favorites.some(item => item.path === location.hash.substring(1));
    },
    hasFav() {
      return this.favorites.length;
    },
    groupsList() {
      const resultArr = [];
      const groupArrays = [];
      this.favorites.forEach((ele) => {
        groupArrays.push(ele.group);
      });
      const groupArr = Array.from(new Set(groupArrays));
      groupArr.forEach((ele2) => {
        if (ele2) {
          const param = {
            text: ele2,
            value: ele2,
          };
          resultArr.push(param);
        }
      });
      return resultArr;
    },
  },
  methods: {
    filterGroup(value, row) {
      return row.group === value;
    },
    resetFields() {
      this.form.title = '';
      this.form.group = '';
      this.favorites = this.$favorites();
      this.visible = false;
      this.$nextTick(() => { this.editPath = ''; });
    },
    submitForm(path) {
      this.$favorites(path, this.form.title, this.form.group);
      this.favorites = this.$favorites();
      this.resetFields();
      this.editPath = '';
    },
    edit(data, editPath) {
      this.form.title = data.title;
      this.form.group = data.group;
      this.editPath = editPath;
    },
    showMessage(status, op, data) {
      if (op === 'add') {
        this.edit(data);
        const messageText = status ? '该资源收藏成功！' : '该资源已收藏！';
        const messageType = status ? 'success' : 'error';
        this.$message.customMessage(messageType, messageText);
      } else if (op === 'del') {
        this.$message.customMessage('success', '该收藏资源已移除成功!');
      }
    },
  },
};
</script>
<style lang="sass">
.fav-container {
  display: inline-block;
  margin: 0 10px;
  height: 40px;
  line-height: 40px;
}
.fav-table {
  max-height: 350px;
  overflow: auto;
}
.my-fav {
  &.el-popover {
      padding: 8px;
  }
  .el-popover__title {
      margin-bottom: 5px;
      padding: 5px 0;
      font-weight: 800;
      font-size: 15px;
  }
  .el-table {
    td, th {
      height: 30px;
    }
  }
  .el-table__column-filter-trigger {
    i {
      color: #0000ff;
      font-size: 14px;
      font-weight: bolder;
    }
  }
}
</style>
