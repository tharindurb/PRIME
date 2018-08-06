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
package nlptasks;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class SentenceSplitter {
    final SentenceDetectorME sdetector;
    private static SentenceSplitter sentenceSplitter;
    private SentenceSplitter() throws IOException {
        InputStream inputStream = new FileInputStream("./models/en-sent.bin");
        this.sdetector = new SentenceDetectorME(new SentenceModel(inputStream));
        inputStream.close();
    }

    public static SentenceSplitter getSentenceSplitter(){
        if(sentenceSplitter == null){
            try {
                sentenceSplitter = new SentenceSplitter();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sentenceSplitter;
    }

    public synchronized String[] sentDetect(String text) {
        return sdetector.sentDetect(text);
    }
}
