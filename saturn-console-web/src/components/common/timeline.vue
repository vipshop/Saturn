<template>
    <div class="tm-body">
        <ol class="tm-items">
            <slot></slot>
        </ol>
    </div>
</template>

<style type="text/css">
.tm-body {
    background: #373A3C none repeat scroll 0 0;
    border-radius: 4px;
    margin-left: 5px;
    margin-right: 5px;
}
.tm-body::after {
    bottom: 0;
    content: "";
    display: block;
    height: 65%;
    left: 146px;
    position: absolute;
    width: 3px;
    z-index: 0;
}
.tm-items {
    list-style: outside none none;
    margin: 0;
    padding: 0;
    position: relative;
    font-size:	14px;
}
.tm-items::before {
    background-color: #eee;
    bottom: 0;
    content: " ";
    left: 127px;
    position: absolute;
    top: 0;
    width: 3px;
}
.tm-items > li {
    margin: 30px 0;
    min-height: 65px;
    padding: 0 0 0 190px;
    position: relative;
    z-index: 1;
}
.tm-items > li .tm-icon {
    background-color: #373A3C;
    border-color: #08c;
    border-radius: 126px;
    color: #ccc;
    font-size: 22px;
    height: 25px;
    left: 66px;
    line-height: 25px;
    margin-left: 50px;
    margin-top: -17px;
    padding-top: 5px;
    position: absolute;
    text-align: center;
    top: 50%;
    width: 25px;
    z-index: 2;
}
.tm-items > li .tm-datetime {
    height: 48px;
    left: 0;
    margin-left: 3px;
    margin-top: -17px;
    padding-top: 5px;
    position: absolute;
    text-align: right;
    top: 50%;
    width: 100px;
    z-index: 3;
}
.tm-items > li .tm-item {
    color: #ccc;
    font-weight: 700;
    height: 48px;
    left: 150px;
    margin-top: -17px;
    padding-top: 5px;
    position: absolute;
    text-align: left;
    top: 50%;
    width: 100px;
    z-index: 3;
}
.tm-items > li .tm-timeline-box {
    left: 140px;
    line-height: 29px;
    margin-left: -30px;
    margin-top: -20px;
    position: absolute;
    text-align: center;
    top: 50%;
    width: 120px;
    z-index: 2;
}
.tm-items > li .tm-timeline-box .tm-timeline {
    background-color: #428bca;
    padding-left: 2px;
    text-align: left;
}
.tm-items > li .tm-ratio {
    line-height: 29px;
    text-align: left;
}
.tm-items > li .tm-box {
    background: #222 none repeat scroll 0 0;
    border: 1px solid #222;
    border-radius: 6px;
    margin-left: 80px;
    margin-right: 5px;
    min-height: 65px;
    padding: 10px 20px;
    position: relative;
}
.tm-items > li .tm-box::after {
    -moz-border-bottom-colors: none;
    -moz-border-left-colors: none;
    -moz-border-right-colors: none;
    -moz-border-top-colors: none;
    border-color: transparent #222 transparent transparent;
    border-image: none;
    border-style: solid;
    border-width: 8px;
    content: " ";
    height: 0;
    margin-top: -8px;
    pointer-events: none;
    position: absolute;
    right: 100%;
    top: 50%;
    width: 0;
    z-index: 2;
}
.tm-items > li .tm-box .tm-meta {
    margin: 20px 0 0;
}
</style>
