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

import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;
import us.monoid.json.JSONObject;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class ESBulkFeeder {

    private final Client transportClient;
    private BulkRequestBuilder bulkBuilder;
    private final String index;
    private final String type;
    private final int bulkLimit;
    private int docCount;

    public ESBulkFeeder(Client client, String index, String type, int bulkLimit) {
        this.transportClient =  client;
        this.index = index;
        this.type = type;
        this.bulkLimit = bulkLimit;
        this.bulkBuilder = transportClient.prepareBulk();
        this.docCount = 0;
    }

    public ESBulkFeeder(String index, String type, int bulkLimit) {
        this.transportClient =  getESTransportClient();
        this.index = index;
        this.type = type;
        this.bulkLimit = bulkLimit;
        this.bulkBuilder = transportClient.prepareBulk();
        this.docCount = 0;
    }

    public boolean feedToES(String id, JSONObject tweet)
    {
        bulkBuilder.add(transportClient.prepareIndex(index, type, id).setSource(tweet.toString()));

        ++docCount;

        if (docCount >= bulkLimit) {
            BulkResponse bulkRes = bulkBuilder.execute().actionGet();
            if (bulkRes.hasFailures()) {
                System.out.println("##### Bulk Request failure with error: " + bulkRes.buildFailureMessage());
                return false;
            }
            bulkBuilder = transportClient.prepareBulk();
            docCount = 0;
        }

        return true;
    }

    public boolean flushBulk()
    {
      if(bulkBuilder.numberOfActions()> 0) {
          BulkResponse bulkRes = bulkBuilder.execute().actionGet();
          if (bulkRes.hasFailures()) {
              System.out.println("##### Bulk Request failure with error: " + bulkRes.buildFailureMessage());
              return false;
          }
          docCount = 0;
      }
        return true;
    }

    private static Client getESTransportClient() {
        try {
            return TransportClient.builder().build()
                    .addTransportAddress(new InetSocketTransportAddress(InetAddress.getByName("localhost"), 9300));
        } catch (UnknownHostException e) {
            return null;
        }
    }

    public Client getClient(){
        return transportClient;
    }
}
