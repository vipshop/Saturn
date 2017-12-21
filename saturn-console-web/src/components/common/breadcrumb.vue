<template>
    <div class="user_breadcrum">
        <el-breadcrumb separator="/">
            <template v-for='item in items'>
                <el-breadcrumb-item :key="item.name" :to="{ name: item.name }">
                    {{item.title}}
                </el-breadcrumb-item>
            </template>
        </el-breadcrumb>
    </div>
</template>
<script>
export default {
  props: ['items'],
};
</script>
<style lang="sass" scoped>
</style>
