function initWebview() {
    $('body').append('<div id="top"></div>');
    $('body').append('<div id="tracks"></div>');
    $('body').append('<div id="results"></div>');
    
    //tracks
    android.reloadTracks();
    
    //search
    $('#top').append('<div class="field">'+
                     '<input class="wide text input" type="text" placeholder="'+lng('search')+'" id="search"/>'+
                     '<div class="medium primary btn">'+
                     '<a href="#"><i class="icon-search"></i></a>'+
                     '</div>'+
                     '</div>');
    $('#search').click(function(){
        $('#tracks').hide();
        $('#results').show('fast');
        $('#results').empty();
        $('#results').append('<div class="medium primary btn icon-left-circled">'+
         '<a href="#">'+lng('back')+'</a>'+
         '</div>');
        $('#results').append('<div>'+lng('search_results')+'<ul></ul></div>');
        android.searchFor($('#top input').val());
    });
    
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

function listTracks() {
    $('#tracks').empty();
    $('#tracks').append('<ul></ul>');
    var headertext;
    if(tracks.length==0)
        headertext=lng('no_tracks');
    else
    {
        for(var i=0; i<tracks.length; i++)
        {
            var track = tracks[i];
            $('#tracks > ul').append('<li onclick="javascript:loadTrack('+track+');">'+track+'</li>');
        }
        headertext=lng('tracks');
    }  
    $('#tracks').prepend(headertext);
}

function loadTrack(filename)
{
    console.log('loadTrack: '+filename);
    android.loadTrack(filename);
}

function unloadTrack()
{
    
}

function addSearchResult(index,name)
{
    $('#results > div > ul').append('<li onclick="javascript:loadResult('+index+');">'+name+'</li>');
}

function loadResult(index)
{
    console.log('loadResult: '+index);
}

function switchOptionsMenu() {
    $('body').append('<br>Hallo Welt!');
    console.log('OptionsMenu');
}