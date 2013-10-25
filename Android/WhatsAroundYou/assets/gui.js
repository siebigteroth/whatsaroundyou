function initWebview() {
    $('body').append('<div id="top"></div>');
    $('#top').append('<p>All map tiles stored by the WhatsAroundYou application, were created by '+
                     '<a href="#" onclick="javascript:android.openURL(\'http://stamen.com/\');">Stamen Design</a>, under '+
                     '<a href="#" onclick="javascript:android.openURL(\'http://creativecommons.org/licenses/by/3.0\');">CC BY 3.0</a>.<br>'+
                     'Map data by '+
                     '<a href="#" onclick="javascript:android.openURL(\'http://openstreetmap.org/\');">OpenStreetMap</a>, under '+
                     '<a href="#" onclick="javascript:android.openURL(\'http://creativecommons.org/licenses/by-sa/3.0\');">CC BY SA</a>.</p>');
    
    //start/ stop button
    $('body').append('<div class="medium primary btn" id="startstop-service">'+
                     '<a href="#">'+
                     '<i class="icon-clock"></i>'+
                     '<i class="state icon-switch"></i>'+
                     '<i class="icon-mobile"></i> '+
                     '<span class="text">'+lng('start_service')+'</span>'+
                     '</a></div>');
    $('#startstop-service').click(function(){
        if($(this).find('.state').hasClass('icon-switch')) { //start service
            $(this).find('.state').removeClass('icon-switch');
            $(this).find('.state').addClass('icon-flash');
            $(this).find('.text').html(lng('stop_service'));
            android.startService();
        }
        else { //stop service
            $(this).find('.state').removeClass('icon-flash');
            $(this).find('.state').addClass('icon-switch');
            $(this).find('.text').html(lng('start_service'));
            android.stopService();
        }
    });
}

function connectionFailed() {
    $('#startstop-service').removeClass('icon-flash');
    $('#startstop-service').addClass('icon-switch');
    $('#startstop-service').find('.text').html(lng('start_service'));
}

function loadImage(imageData)
{
    $('#top > img').remove();
    var image = '<img src="data:image/png;base64,'+imageData+'"/>';
    $('#top').append('<img src="'+image+'"/>');
}

function switchOptionsMenu() {
    //unused
}