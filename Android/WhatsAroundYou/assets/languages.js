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
                stop_service:'trenne Verbindung',
                search: 'Suche nach Adresse',
                tracks: 'Routen:',
                no_tracks: 'Keine Routen gefunden. Suchen Sie nach einer Adresse, um eine Route hinzuzufügen.',
                search_results: 'Suchergebnisse:',
                back: 'zurück'
            };
            break;
        default: //en; English
            language = {
                start_service:'connect',
                stop_service:'disconnect',
                search: 'search for address',
                tracks: 'tracks:',
                no_tracks: 'no tracks found. search for an address to add one.',
                search_results: 'search results:',
                back: 'back'
            };
            lng_short='en';
            break;
    }
}
