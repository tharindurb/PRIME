package clinical_info;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONObject;
import util.Utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class TimelineInfo {
    private final static List<TimeBin> timelineMap = getTimelineInfo();

    public static List<JSONObject> sortPostsByDate(JSONArray posts) throws Exception {
        List<JSONObject> sortedPosts = new ArrayList<>();

        for (int i = 0; i < posts.length(); i++) {
            JSONObject post = posts.getJSONObject(i);
            sortedPosts.add(post);
        }

        sortedPosts.sort((o1, o2) -> {
            try {
                Date o1Date = Utils.dateFormat.parse(o1.getString("PostDate"));
                Date o2Date = Utils.dateFormat.parse(o2.getString("PostDate"));
                return o1Date.compareTo(o2Date);
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        });

        return sortedPosts;
    }

    public static List<TimeBin> getTimelineInfo() {
        List<TimeBin> timelineMap = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader("./models/surgeryKeywords/surgeryTimeline.txt"));
            String line;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    String[] firstSplit = line.split(":");
                    String label = firstSplit[0];
                    String dateRange = firstSplit[1];
                    String[] timeRange = dateRange.split(",");

                    TimeBin timeBin = new TimeBin(label, Integer.parseInt(timeRange[0]), Integer.parseInt(timeRange[1]));
                    timelineMap.add(timeBin);
                }
            }
            reader.close();
            Collections.sort(timelineMap);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return timelineMap;
    }

    public static Map<String, List<JSONObject>> getTimeLineBinnedPosts(JSONArray posts, String pivotDate) {
        Map<String, List<JSONObject>> timelineBinnedPosts = new HashMap<>();
        for (TimeBin timeBin : timelineMap) timelineBinnedPosts.put(timeBin.label, new ArrayList<>());

        try {
            int gLower = timelineMap.get(0).lowerLimit;
            int gUpper = timelineMap.get(timelineMap.size() - 1).upperLimit;

            List<JSONObject> sortedPosts = sortPostsByDate(posts);

            int startIndex = 0;

            for (JSONObject post : sortedPosts) {
                String postDate = post.getString("PostDate");
                int dateDiff = Utils.getDateDifferece(pivotDate, postDate);

                if (dateDiff < gLower || dateDiff > gUpper) continue;

                boolean done = false;
                while (!done) {
                    TimeBin timeBin = timelineMap.get(startIndex);
                    if(dateDiff < timeBin.lowerLimit){
                        done = true;
                    }else if (timeBin.lowerLimit <= dateDiff && dateDiff <= timeBin.upperLimit) {
                        timelineBinnedPosts.get(timeBin.label).add(post);
                        done = true;
                    } else {
                        ++startIndex;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return timelineBinnedPosts;
    }

}

class TimeBin implements Comparable<TimeBin> {
    String label;
    Integer lowerLimit;
    Integer upperLimit;

    public TimeBin(String label, Integer lowerLimit, Integer upperLimit) {
        this.label = label;
        this.lowerLimit = lowerLimit;
        this.upperLimit = upperLimit;
    }

    @Override
    public int compareTo(TimeBin o) {
        return lowerLimit.compareTo(o.lowerLimit);
    }
}
