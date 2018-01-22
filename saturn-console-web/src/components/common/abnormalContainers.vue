<template>
    <FilterPageList :data="abnormalContainersList" :total="total" :order-by="orderBy" :filters="filters">
        <template slot-scope="scope">
            <el-form :inline="true" class="table-filter">
                <input type="text" v-show="false"/>
                <el-form-item label="">
                    <el-input placeholder="搜索" v-model="filters.taskId" @keyup.enter.native="scope.search"></el-input>
                </el-form-item>
                <el-form-item>
                    <el-button type="primary" icon="el-icon-search" @click="scope.search">查询</el-button>
                </el-form-item>
            </el-form>
            <div class="page-table">
                <div class="page-table-header">
                    <div class="page-table-header-title"><i class="fa fa-list"></i>异常容器
                        <el-button type="text" @click="refreshList"><i class="fa fa-refresh"></i></el-button>
                    </div>
                </div>
                <el-table stripe border @sort-change="scope.onSortChange" :data="scope.pageData" style="width: 100%">
                    <el-table-column prop="taskId" label="容器名"></el-table-column>
                    <el-table-column prop="domainName" label="所属域" sortable>
                        <template slot-scope="scope">
                            <router-link tag="a" :to="{ name: 'job_overview', params: { domain: scope.row.domainName } }">
                                <el-button type="text">
                                    {{scope.row.domainName}}
                                </el-button>
                            </router-link>
                        </template>
                    </el-table-column>
                    <el-table-column prop="degree" label="域等级" sortable>
                        <template slot-scope="scope">
                            {{$map.degreeMap[scope.row.degree]}}
                        </template>
                    </el-table-column>
                    <el-table-column prop="cause" label="异常原因"></el-table-column>
                </el-table>
            </div>
        </template>
    </FilterPageList>
</template>
<script>
export default {
  props: ['abnormalContainersList'],
  data() {
    return {
      filters: {
        taskId: '',
      },
      orderBy: 'taskId',
      total: this.abnormalContainersList.length,
    };
  },
  methods: {
    refreshList() {
      this.$emit('refresh-list');
    },
  },
};
</script>
