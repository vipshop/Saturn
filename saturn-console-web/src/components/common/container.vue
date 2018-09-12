<template>
    <el-container class="my-container">
        <el-header height="50px">
            <VHeader></VHeader>
        </el-header>
        <router-view></router-view>
    </el-container>
</template>

<style lang="sass" scoped>
.my-container {
    height: 100%;
}
</style>
