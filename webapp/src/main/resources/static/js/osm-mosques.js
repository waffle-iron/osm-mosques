/*
 * All of our cool Javascript stuff here
 */

function attemptGeocode(ditibKey)
{
    var gcUrlBase = "../rest/ditib/geocode/";
    var mapUrlBase = "../";

    // http://localhost:8888/rest/ditib/geocode/84030-20-5
    var gcUrl = gcUrlBase + ditibKey;
    console.log(gcUrl);
    $.post(gcUrl, '', function (data)
    {
        console.log(data);
        if (data["geocoded"] == true)
        {
            var lat = data["lat"];
            var lon = data["lon"];
            var jumpUrl = mapUrlBase + "#zoom=17&amp;lat=" + lat + "&amp;lon=" + lon;

            window.open(jumpUrl, 'map');
        }
        else
        {
            alert("Failed to geocode place");
        }
    },
    'json');
}
