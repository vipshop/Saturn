<template>
    <div>
        <el-row :gutter="10">
            <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                <Chart-container title="域重要等级分布">
                    <div slot="chart">
                        <Pie id="1"></Pie>
                    </div>
                </Chart-container>
            </el-col>
            <el-col :xs="24" :sm="24" :md="24" :lg="24" :xl="12">
                <Chart-container title="失败最高的Top10域">
                    <div slot="chart">
                        <Column id="2"></Column>
                    </div>
                </Chart-container>
            </el-col>
        </el-row>
        
    </div>
</template>

<style lang="sass" scoped>
.test {
    width: 100%;
    @media screen and (min-width: 1600px) {
        width: 50%;
        float: left;
    }
}
</style>
