/*
 *   This is part of the source code of Patient Reported Information Multidimensional Exploration (PRIME)
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package data_collection;

import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ForumThread implements Serializable {
    final public String title;
    final public String url;
    public int nMessages;
    public String startDate;
    public List<Message> messages;
    final public Map<String,String> otherData;

    public ForumThread(String title, String url) {
        this.title = title;
        this.url = url;
        this.otherData = new HashMap<>();
    }


    @Override
    public String toString() {

        StringBuilder builder = new StringBuilder("Thread :  "+ title + " startDate: " + startDate + "\n");

        for(Message message: messages)
        {
            builder.append(message).append("\n");
        }

        builder.append("\n");

        return builder.toString();
    }

    public JSONObject getJSON() throws JSONException {
        JSONObject threadJSON = new JSONObject();
        threadJSON.put("Title",title);
        threadJSON.put("ThreadURL",url);

        Collections.sort(messages);

        JSONArray messagesJSON = new JSONArray();

        if(messages.size()> 0) {
            threadJSON.put("StartDate",messages.get(0).getDate());
            threadJSON.put("EndDate",messages.get(messages.size()-1).getDate());
            threadJSON.put("Category",messages.get(0).getData("Category"));

            for (Message message : messages) {
                if(message.hasData("Category")){
                    threadJSON.put("Category",message.getData("Category"));
                    break;
                }
            }

            int i = 0;
            if ( messages.get(0).getIndex() == 0) {
                    messagesJSON.put(i++, title);
                }

            for (Message message : messages) {
                messagesJSON.put(i++, message.getText());
            }
        }
        threadJSON.put("Posts",messagesJSON);

        return threadJSON;
    }
}
