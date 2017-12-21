<template>
    <li>
        <div class="tm-info"> 
        <div class="tm-item">
            <slot name="title"></slot>
        </div> 
        <div class="tm-icon">
            <i class="el-icon-time"></i>
        </div> 
        <time class="tm-datetime"> 
            <slot name="date"></slot>
        </time> 
        </div>
        <div class="tm-box"> 
            <slot name="content"></slot>
        </div>
    </li>
</template>
