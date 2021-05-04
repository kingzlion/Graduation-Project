$(function() {


    var ac = new BMap.Autocomplete({
        "input": startAddress,
    });



    ac.addEventListener("onconfirm", function(e) {
        //创建一个监听者 用来监听文本框是否完成自动填充  if则执行
        //onconfirm文本框
        var startAddress = $('#startAddress').val();

    });




    //            在这里    结束

});