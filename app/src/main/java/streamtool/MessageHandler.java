package streamtool;

import org.json.JSONException;
import org.json.JSONObject;

import common.Message;

public class MessageHandler extends Message {
    public void receive(String msg) {
        // TODO deal with feed info and update status when they're ready

        // get the json if it's valid
        JSONObject o;
        try {
            o = new JSONObject(msg);
        } catch (JSONException e) {
            return;
        }

        // get the data part
        JSONObject d = o.optJSONObject("d", new JSONObject());

        // get the id, if set
        String id = d.optString("requestId","0");

        // set the feed status if it's ready
        if (id.contains("feed")) {
            JSONObject responseData = d.optJSONObject("responseData");
            if (responseData != null) {
                // mediaState shows that the media source is receiving something, it changes when it connects to streamlink
                // mediaCursor changes to above 0 when the stream actually starts playing
                String mediaState = responseData.optString("mediaState", "0");
                Long mediaCursor = responseData.optLong("mediaCursor", 0L);

                if (mediaState.equals("OBS_MEDIA_STATE_PLAYING") && mediaCursor > 0L) {
                    String p = id.substring(4);
                    int i = -1;
                    try {
                        i = Integer.parseInt(p);
                    } catch (NumberFormatException e) {}
                    if (i >= 0 && i < FeedManager.feeds.size()) {
                        FeedManager.feeds.get(i).status = Status.STANDBY;
                    }
                }
            }
        }

    }
}
