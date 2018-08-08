<template>
  <div>
    <slot v-bind="this"></slot>
    <el-pagination
      @current-change="onCurrentChange"
      @size-change="onPageSize"
      :current-page="currentPage"
      :page-sizes="[10, 25, 50, 100, 200, '所有']"
      :page-size="pageSize"
      layout="total, sizes, prev, pager, next, jumper"
      :total="totalRecords">
    </el-pagination>
  </div>
</template>
<script>
export default {
  props: ['getData', 'data', 'total', 'orderBy', 'filters'],
  data() {
    return {
      pageSize: 25,
      currentPage: 1,
      orderby: this.orderBy,
      orderKey: undefined,
      order: undefined,
      pageData: [],
      filtered: [],
    };
  },
  computed: {
    offset() {
      return (this.currentPage - 1) * this.pageSize;
    },
    params() {
      const paramsResult = Object.create(null);
      paramsResult.size = this.pageSize;
      paramsResult.page = this.currentPage;
      paramsResult.orderKey = this.orderKey;
      paramsResult.order = this.order;
      if (this.filters) {
        Object.entries(this.filters).forEach((item) => {
          if (item[1] !== '' && item[1] !== undefined) {
            paramsResult[item[0]] = item[1];
          }
        });
      }
      return paramsResult;
    },
    totalRecords() {
      if (this.data) {
        return this.filtered.length;
      }
      return this.total;
    },
  },
  watch: {
    $route: 'getDataByParams',
    data: 'buildCurrentPage',
  },
  created() {
    this.getDataByParams();
  },
  methods: {
    search() {
      this.getDataByParams();
    },
    getDataByParams() {
      if (this.data) {
        this.buildCurrentPage();
      } else if (this.getData) {
        this.getData(this.params);
      }
    },
    buildCurrentPage() {
      if (this.data) {
        let filtered = this.data.slice();
        if (this.filters) {
          Object.entries(this.filters).forEach((item) => {
            const key = item[0];
            const value = item[1];
            if (value || (typeof value === 'boolean')) {
              filtered = filtered.filter((e) => {
                if (typeof e[key] === 'string') {
                  return e[key].toLowerCase().indexOf(value.toLowerCase().trim()) > -1;
                }
                return e[key] === value;
              });
            }
          });
        }
        if (this.orderby) {
          const key = this.orderby.replace(/^-/, '');
          let order = 1;
          if (this.orderby[0] === '-') {
            order = -1;
          }
          filtered.sort((a, b) => {
            if (a[key] === b[key]) {
              return 0;
            } else if (a[key] > b[key]) {
              return order;
            }
            return -order;
          });
        }
        this.filtered = filtered;
        if (filtered.length > this.pageSize) {
          filtered = filtered.filter((e, index) => (
            (index < (this.offset + this.pageSize)) && (index >= this.offset)
          ));
        }
        this.pageData = filtered;
      }
    },
    onSortChange(sort) {
      if (sort.order === null) {
        this.orderby = this.orderBy;
      } else if (sort.order === 'descending') {
        this.orderby = `-${sort.prop}`;
      } else {
        this.orderby = `${sort.prop}`;
      }
      // this.orderKey = sort.prop;
      this.order = sort.order;
      this.getDataByParams();
    },
    onCurrentChange(page) {
      this.currentPage = page;
      this.getDataByParams();
    },
    onPageSize(pageSize) {
      if (JSON.stringify(pageSize) === 'null') {
        this.pageSize = this.totalRecords;
      } else {
        this.pageSize = pageSize;
      }
      this.getDataByParams();
    },
  },
};
</script>

