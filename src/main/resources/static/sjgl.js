(function() {



    $("#fly").click(function() {
        alert("抱歉，不支持！");
    });


    $("#fly1").click(function() {
        alert("抱歉，不支持！");
    });
    $("#fly11").click(function() {
        alert("账号或密码错误！");
    });



})();

function check() {
    if (document.getElementById("username").value == "") {
        alert("没有输入用户名！");
        return false;
    } else if (document.getElementById("password").value == "") {
        alert("没有输入密码！");
        return false;
    } else {
        alert("提交成功！")
        return true;
    }
}