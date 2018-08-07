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

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.sort.SortParseElement;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;

public class DataCollectionUtils
{


    public static JSONArray getAuthoredPosts(String authorID, Client client, String index, String type) throws JSONException {
        JSONArray authoredPosts = new JSONArray();

        SearchResponse scrollResp = client.prepareSearch(index)
                .setTypes(type)
                .addSort(SortParseElement.DOC_FIELD_NAME, SortOrder.ASC)
                .setScroll(new TimeValue(600000))
                .setQuery(QueryBuilders.matchPhraseQuery("AuthorID", authorID))
                .setSize(50).execute().actionGet();
        while (true) {

            for (SearchHit hit : scrollResp.getHits().getHits()) {
                JSONObject hitJSON = new JSONObject(hit.getSourceAsString());
                authoredPosts.put(hitJSON);
            }
            scrollResp = client.prepareSearchScroll(scrollResp.getScrollId()).setScroll(new TimeValue(600000)).execute().actionGet();

            //Break condition: No hits are returned
            if (scrollResp.getHits().getHits().length == 0) {
                break;
            }
        }

        return authoredPosts;
    }

    public static String removePunctuations(String text) {
        return text.replaceAll("[\"*:?\\.;<>(),!+-/|]", " ").replace("\\", " ");
    }

    public static String getIndexMapping() {
        return new String("{\n" +
                "\"properties\": {\n" +
                "\"AuthorID\": {\n" +
                "  \"type\": \"string\",\n" +
                "  \"index\": \"not_analyzed\"\n" +
                "},\n" +
                "\"AuthorURL\": {\n" +
                "  \"type\": \"string\",\n" +
                "  \"index\": \"not_analyzed\"\n" +
                "},\n" +
                "\"Content\": {\n" +
                "  \"type\": \"string\"\n" +
                "},\n" +
                "\"MessageIndex\": {\n" +
                "  \"type\": \"integer\"\n" +
                "},\n" +
                "\"PostDate\": {\n" +
                "  \"type\": \"date\",\n" +
                "  \"format\": \"dd/MM/yyyy\"\n" +
                "},\n" +
                "\"ThreadURL\": {\n" +
                "  \"type\": \"string\",\n" +
                "  \"index\": \"not_analyzed\"\n" +
                "},\n" +
                "\"Title\": {\n" +
                "  \"type\": \"string\"\n" +
                "}\n" +
                "}\n" +
                "}");
    }

}
