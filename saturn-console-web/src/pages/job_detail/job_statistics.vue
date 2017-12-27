<template>
    <div class="page-content">
        <div>
            <el-row :gutter="20">
                <el-col :span="8">
                    <Panel type="default">
                        <div slot="title">平均运行时长(s)</div>
                        <div slot="content">1.2</div>
                    </Panel>
                </el-col>
                <el-col :span="8">
                    <Panel type="warning">
                        <div slot="title">总执行次数</div>
                        <div slot="content">2000</div>
                    </Panel>
                </el-col>
                <el-col :span="8">
                    <Panel type="danger">
                        <div slot="title">失败率</div>
                        <div slot="content">1%</div>
                    </Panel>
                </el-col>
            </el-row>
        </div>
        <div class="page-table">
            <div class="page-table-header">
                <div class="page-table-header-title"><i class="fa fa-list"></i>失败调用
                    <el-button type="text" @click="handleRefresh()"><i class="fa fa-refresh"></i></el-button>
                </div>
            </div>
            <el-table stripe :data="tableData" style="width: 100%">
                <el-table-column prop="traceId" label="traceId"></el-table-column>
                <el-table-column prop="time" label="时间"></el-table-column>
                <el-table-column prop="shards" label="分片"></el-table-column>
                <el-table-column prop="executor" label="Executor"></el-table-column>
            </el-table>
        </div>
    </div>
</template>

<script>
export default {
  data() {
    return {
      tableData: [{
        traceId: '111',
        time: '2017-08-01 10:20:02',
        shards: '1',
        executor: '123',
      }, {
        traceId: '222',
        time: '2017-08-01 10:20:02',
        shards: '1',
        executor: '123',
      }, {
        traceId: '333',
        time: '2017-08-01 10:20:02',
        shards: '1',
        executor: '123',
      }],
    };
  },
};
</script>
