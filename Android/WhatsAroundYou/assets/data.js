var db = window.openDatabase('db', '1.0', 'db', 1024*1024);
if(db==null)
    window.stop();
    
var tracks = new Array();
    
function sql(statement,data_array,callback,on_error) {
    if(data_array==null)
        data_array=[];
    if(on_error==null)
        on_error=function (x,error) {
            console.error(error);
        };
    db.transaction(function(x) {
        x.executeSql(statement,data_array,callback,on_error);
    });
}

//create tables
sql('CREATE TABLE IF NOT EXISTS settings (id unique, value)');