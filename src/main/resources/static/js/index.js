var keyPrefix = "quick-out-";
$(function () {
    loadDatabase();
    loadData();

    $("#searchButton").click(function () {
        loadData();
    });

    $("#all").click(function () {
        if (this.checked) {
            $("input[name='checboxName']").prop("checked", true);
        } else {
            undefined
            $("input[name='checboxName']").prop("checked", false);
        }
    });

    initItemAll();


    $("#controllerOut").val(".controller")
    $("#serviceOut").val(".service")
    $("#reqOut").val(".req")
    $("#entityOut").val(".entity")
    $("#mapperOut").val(".mapper")
    $("#mapperXmlOut").val(".mapper.xml")

});

$("#databaseConfig").on("click", function () {
    loadDatabaseShowTable();
    $('#myModal').modal('show')
})

$("#packageOut").bind('input propertychange', function () {
    // pathChange(this.value);
});


function pathChange(value) {
    if (value.endWith(".")) {
        value = value.substring(0, value.length - 1);
    }
    $("#controllerOut").val(value + ".controller")
    $("#serviceOut").val(value + ".service")
    $("#reqOut").val(value + ".req")
    $("#entityOut").val(value + ".entity")
    $("#mapperOut").val(value + ".mapper")
    $("#mapperXmlOut").val(value + ".mapper.xml")
}

$("#clearButton").on("click", function () {
    $("#tableName").val('');
    loadData();
});


function initItemAll() {
    initItem("out");
    initItem("packageOut")
    initItem("controllerOut");
    initItem("serviceOut");
    initItem("reqOut");
    initItem("entityOut");
    initItem("mapperOut");
    initItem("mapperXmlOut");
    initItem("author");
    initItem("email");
}

function initItem(id) {
    $("#" + id).val(localStorage.getItem(keyPrefix + id))
}

function loadData() {
    $.ajax({
        url: ctx + "list",
        type: "post",
        timeout:3000,
        data: {"tableName": $("#tableName").val(), "databaseName": $("#databaseId").val()},
        dataType: "json",
        beforeSend: function () {
            $("#searchButton").button('loading');
        },
        complete: function () {
            $("#searchButton").button('complete');
        },
        error: function () {
            $("#searchButton").button('complete');
        },
        success: function (data) {
            if (data.code == 0) {
                var rows = data.data.rows;
                if (rows) {
                    var h = "";
                    for (var i = 0; i < rows.length; i++) {
                        var li = rows[i];
                        h += "<tr>";
                        h += "<td>";
                        h += "<input type='checkbox' name='checboxName' value='" + li.tableName + "'>"
                        h += "</td>";
                        h += "<td>";
                        h += i + 1;
                        h += "</td>";
                        h += "<td>";
                        h += li.tableName;
                        h += "</td>";
                        h += "<td>";
                        h += li.comments
                        h += "</td>";
                        h += "<td>";
                        h += li.createTime
                        h += "</td>";
                        h += "</tr>";
                    }
                    $("#tableBody").html(h);
                }
            } else {
                Toast(data.msg);
            }
        }
    });
}

function generateCode() {

    var obj = document.getElementsByName("checboxName");
    var check_val = [];
    for (k in obj) {
        if (obj[k].checked) {
            check_val.push(obj[k].value);
        }
    }
    if (check_val.length <= 0) {
        Toast("??????????????????");
        return false;
    }

    var data = $("#formDiv").serialize() + "&generateTableNames=" + check_val.join(",") + "&" + $("#left").serialize()+"&databaseName="+$("#databaseId").val();
    var outType = $("#outType").val();
    if (outType=="zip"){
        var url = ctx + "generateZip";
        $.download(url,data,'post' );
    }else{

        var r = confirm("????????????????????????????????????????????????????")
        if (r) {
            $.ajax({
                url: ctx + "generate",
                type: "post",
                timeout: 2000,
                data: data,
                dataType: "json",
                beforeSend: function () {
                    $("#generate").button('loading');
                },
                complete: function () {
                    $("#generate").button('complete');
                },
                error: function () {
                    $("#generate").button('complete');
                },
                success: function (data) {
                    if (data.code == 0) {
                        Toast("????????????");
                    } else {
                        alert(data.msg);
                    }
                }
            });
        }

    }
}

jQuery.download = function(url, data, method){ // ??????url???data
    if( url && data ){
        // data ??? string ?????? array/object
        data = typeof data == 'string' ? data : jQuery.param(data); // ?????????????????? form??? input
        var inputs = '';
        jQuery.each(data.split('&'), function(){
            var pair = this.split('=');
            inputs+='<input type="hidden" name="'+ pair[0] +'" value="'+ pair[1] +'" />';
        }); // request????????????
        jQuery('<form action="'+ url +'" target="_blank" method="'+ (method||'post') +'">'+inputs+'</form>').appendTo('body').submit().remove();
    };
};


$("#cacheButton").on("click", function () {
    setItem("out");
    setItem("packageOut");
    setItem("controllerOut");
    setItem("serviceOut");
    setItem("reqOut");
    setItem("entityOut");
    setItem("mapperOut");
    setItem("mapperXmlOut");
    setItem("author");
    setItem("email");
});

function setItem(id) {
    var value = $("#" + id).val();
    if (!isEmpty(value)) {
        localStorage.setItem(keyPrefix + id, value);
    }
}

function jinyong(obj) {
    var dis = $(obj).parent().parent().find(".p-center input")[0];
    console.log($(dis))
    if (!$(dis).attr("disabled")) {
        $(dis).attr("disabled", "disabled")
        $(obj).html("<span style='color:#07c160;'>??????</span>")
    } else {
        $(dis).removeAttr("disabled")
        $(obj).html("??????")
    }
}

function loadDatabase() {
    $.ajax({
        url: ctx + "load",
        type: "post",
        dataType: "json",
        async: false,
        success: function (data) {
            if (data.code == 0) {
                var rows = data.data;
                if (rows) {
                    var html = "";
                    for (var i = 0; i < rows.length; i++) {
                        var row = rows[i];
                        html += "<option id='" + row.name + "'>" + row.name + "</option>";
                    }
                    $("#databaseId").html(html);
                }
            } else {
                alert(data.msg);
            }
        }
    });
}

function loadDatabaseShowTable() {
    $.ajax({
        url: ctx + "load",
        type: "post",
        dataType: "json",
        async: false,
        success: function (data) {
            if (data.code == 0) {
                var rows = data.data;
                if (rows) {
                    var html = "";
                    for (var i = 0; i < rows.length; i++) {
                        var row = rows[i];

                        var url= row.url;
                        if (url){
                            url = row.url.replaceAll("?","<br/>").replaceAll("&","<br/>")
                        }

                        html += "<tr>";
                        html += "<td class='name'>" + row.name + "</td>";
                        html += "<td class='driverClassName'>" + row.driverClassName + "</td>";
                        html += "<td class='url' value='"+row.url+"' style='width:200px;'>" + url + "</td>";
                        html += "<td class='username'>" + row.username + "</td>";
                        html += "<td class='password'>" + row.password + "</td>";
                        html += "<td> <a type=\"button\" class=\"btn btn-xs\"  onclick=\"deleteDatabase('" + row.name + "')\" >??????</a> <a type=\"button\" class=\"btn btn-xs\"  onclick=\"updateDatabase(this)\" >??????</a></td>";
                        html += "</tr>";
                    }
                    $("#databaseTableBody").html(html);
                }
            } else {
                alert(data.msg);
            }
        }
    });
}

function deleteDatabase(name) {
    var r = confirm("???????????????????")
    if (r) {
        $.ajax({
            url: ctx + "remove",
            type: "post",
            data: {"name": name},
            dataType: "json",
            async: false,
            success: function (data) {
                if (data.code == 0) {
                    loadDatabaseShowTable();
                    loadDatabase();
                } else {
                    alert(data.msg);
                }
            }
        });
    }
}

function addDatabase() {
    $.ajax({
        url: ctx + "reload",
        type: "post",
        data: $("#addForm").serialize(),
        dataType: "json",
        async: false,
        success: function (data) {
            if (data.code == 0) {
                loadDatabase();
                loadDatabaseShowTable();
                $("#add").modal("hide");
            } else {
                alert(data.msg);
            }
        }
    });
}

function updateDatabase(obj) {
    var parent = $(obj).parent().parent();
    $("#name").val(parent.find(".name").html());
    $("#driverClassName").val(parent.find(".driverClassName").html());
    $("#url").val(HTMLDecode(parent.find(".url").attr("value")));
    $("#password").val(parent.find(".password").html());
    $("#username").val(parent.find(".username").html());
    $("#add").modal("show");
}

function HTMLDecode(text) {
    var temp = document.createElement("div");
    temp.innerHTML = text;
    var output = temp.innerText || temp.textContent;
    temp = null;
    return output;
}

function isEmpty(outValue) {
    if (outValue != null && outValue != '' && outValue != 'undefined' && outValue != undefined) {
        return false;
    }
    return true;
}

String.prototype.endWith = function (endStr) {
    var d = this.length - endStr.length;
    return (d >= 0 && this.lastIndexOf(endStr) == d);
}

function Toast(msg,duration){
    duration=isNaN(duration)?3000:duration;
    var m = document.createElement('div');
    m.innerHTML = msg;
    m.style.cssText="max-width:60%;min-width: 150px;padding:0 14px;height: 40px;color: rgb(255, 255, 255);line-height: 40px;text-align: center;border-radius: 4px;position: fixed;top: 50%;left: 50%;transform: translate(-50%, -50%);z-index: 999999;background: rgba(0, 0, 0,.7);font-size: 16px;";
    document.body.appendChild(m);
    setTimeout(function() {
        var d = 0.5;
        m.style.webkitTransition = '-webkit-transform ' + d + 's ease-in, opacity ' + d + 's ease-in';
        m.style.opacity = '0';
        setTimeout(function() { document.body.removeChild(m) }, d * 1000);
    }, duration);
}