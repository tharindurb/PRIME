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

import java.util.HashMap;
import java.util.Map;

public class Message implements Comparable<Message> {
    private final String text;
    private final String date;
    private final int index;
    private final String authorURL;
    private final String authorID;
    private Map<String,Object> data;

    public Message(String text, String date, int index, String authorURL, String authorID) {
        this.text = text;
        this.date = date;
        this.index = index;
        this.authorURL = authorURL;
        this.authorID = authorID;
    }

    @Override
    public String toString() {
        return date + " : "  + text;
    }

    public String getText() {

        return text;
    }

    public String getDate() {
        return date;
    }

    public int getIndex() {
        return index;
    }

    public String getAuthorURL() {
        return authorURL;
    }

    public String getAuthorID() {
        return authorID;
    }

    public void addData(String key, Object value){
        if(data == null){
            data = new HashMap<>();
        }
        data.put(key,value);
    }

    public  Object getData(String key){
        if(data != null  && data.containsKey(key)) {
            return data.get(key);
        }else {
            return null;
        }
    }

    public  boolean hasData(String key){
        if(data != null) {
            return data.containsKey(key);
        }else{
            return false;
        }
    }

    @Override
    public int compareTo(Message o) {
        return this.index - o.index;
    }

}
