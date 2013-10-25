var language;
var lng_short;
function lng(key) {
    for(var k in language)
        if(k==key)
            return language[k];
    return '';
}

function initLanguage(lang) {
    lng_short=lang;
    switch(lang) {
        case 'de': //German
            language = {
                start_service:'verbinde mit Smartwatch',
                stop_service:'trenne Verbindung'
            };
            break;
        default: //en; English
            language = {
                start_service:'connect',
                stop_service:'disconnect'
            };
            lng_short='en';
            break;
    }
}
