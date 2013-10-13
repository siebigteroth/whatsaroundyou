function initWebview() {
    $('body').append('<div class="small info btn icon-left icon-play" id="startstop-service"><a href="#">'+lng('start_service')+'</a></div>');
    $('#startstop-service').click(function(){
        if($(this).hasClass('icon-play')) { //start service
            $(this).removeClass('icon-play');
            $(this).addClass('icon-stop');
            $(this).find('a').html(lng('stop_service'));
            android.startService();
        }
        else { //stop service
            $(this).removeClass('icon-stop');
            $(this).addClass('icon-play');
            $(this).find('a').html(lng('start_service'));
            android.stopService();
        }
    });
}

function connectionFailed() {
    $('#startstop-service').removeClass('icon-stop');
    $('#startstop-service').addClass('icon-play');
    $('#startstop-service').find('a').html(lng('start_service'));
}

function switchOptionsMenu() {
    $('body').append('<br>Hallo Welt!');
    console.log('OptionsMenu');
}