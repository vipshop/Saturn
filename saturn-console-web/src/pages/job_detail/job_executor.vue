<template>
    <div class="page-content">
        <el-table border :data="tableData" style="width: 100%">
            <el-table-column prop="executor" label="Executor"></el-table-column>
            <el-table-column prop="ip" label="IP"></el-table-column>
            <el-table-column prop="shards" label="分片项"></el-table-column>
            <el-table-column prop="version" label="作业版本"></el-table-column>
        </el-table>
    </div>
</template>
<script>
export default {
  data() {
    return {
      tableData: [{
        executor: 'gd6-executor-01',
        ip: '192.168.1.1',
        shards: '1,2,3',
        version: '1.0.1',
      }, {
        executor: 'gd6-executor-02',
        ip: '192.168.1.1',
        shards: '1,2,3',
        version: '1.0.1',
      }, {
        executor: 'gd6-executor-03',
        ip: '192.168.1.1',
        shards: '1,2,3',
        version: '1.0.1',
      }],
    };
  },
  methods: {
  },
};
</script>
