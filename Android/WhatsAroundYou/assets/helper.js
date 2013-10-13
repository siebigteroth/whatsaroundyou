//error handling
function androidErrorHandler(msg, file, line) {
    console.error('"'+msg+'" ('+file+':'+line+')');
    return false;
}
window.onerror = androidErrorHandler;

$(document).ready(function(){
    android.webviewLoaded();
});