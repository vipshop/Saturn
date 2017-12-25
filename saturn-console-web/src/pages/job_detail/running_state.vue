<template>
    <div class="page-content">
        <div class="page-table">
            <div class="page-table-header">
                <div class="page-table-header-title"><i class="fa fa-list"></i>运行状态
                    <el-button type="text" @click="handleRefresh()"><i class="fa fa-refresh"></i></el-button>
                </div>
                <div class="page-table-header-separator"></div>
                <div>
                    <span class="refresh-text">自动刷新：</span>
                    <el-select v-model="autoRefreshTime" placeholder="请选择" size="mini" style="width: 120px;">
                        <el-option value="30" label="30秒"></el-option>
                    </el-select>
                </div>
            </div>
            <el-table stripe :data="tableData" style="width: 100%">
                <el-table-column prop="shard" label="分片项"></el-table-column>
                <el-table-column label="状态">
                    <template slot-scope="scope"> 
                        <el-tag :type="scope.row.status ? 'success' : 'danger'" close-transition>{{scope.row.status}}</el-tag>
                    </template>
                </el-table-column>
                <el-table-column prop="executor" label="Executor"></el-table-column>
                <el-table-column prop="executeTime" label="最近执行时间"></el-table-column>
                <el-table-column prop="nextTime" label="下次开始时间"></el-table-column>
                <el-table-column label="操作">
                    <template slot-scope="scope">
                        <el-tooltip content="分片返回信息" placement="top">
                            <el-button type="text" @click=""><i class="fa fa-reply-all"></i></el-button>
                        </el-tooltip>
                        <el-tooltip content="查看日志" placement="top">
                            <el-button type="text" @click=""><i class="fa fa-file-text-o"></i></el-button>
                        </el-tooltip>
                    </template>
                </el-table-column>
            </el-table>
        </div>
    </div>
</template>
<script>
export default {
  data() {
    return {
      autoRefreshTime: '30',
      tableData: [{
        shard: '0',
        status: true,
        executor: '123',
        executeTime: '2017-01-12',
        nextTime: '2017-01-12',
      }, {
        shard: '1',
        status: false,
        executor: '123',
        executeTime: '2017-01-12',
        nextTime: '2017-01-12',
      }, {
        shard: '2',
        status: true,
        executor: '123',
        executeTime: '2017-01-12',
        nextTime: '2017-01-12',
      }],
    };
  },
  methods: {
  },
};
</script>
<style lang="sass" scoped>
.refresh-text {
    font-weight: bold;
    font-size: 13px;
}
</style>
