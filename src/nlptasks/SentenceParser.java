/*
 *   This is part of the source code of Patient Empowered Analytics Platform (PEAP)
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
import opennlp.tools.cmdline.parser.ParserTool;
import opennlp.tools.parser.Parse;
import opennlp.tools.parser.Parser;
import opennlp.tools.parser.ParserFactory;
import opennlp.tools.parser.ParserModel;
import opennlp.tools.sentdetect.SentenceDetectorME;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SentenceParser {
    private final Parser parser;
    private static SentenceParser sentenceParser;

    private SentenceParser() throws IOException {
        InputStream modelIn = new FileInputStream("./models/en-parser-chunking.bin");
        ParserModel model = new ParserModel(modelIn);
        parser = ParserFactory.create(model);
        modelIn.close();
    }

    public static SentenceParser getSentenceParser(){
        if(sentenceParser == null){
            try {
                sentenceParser = new SentenceParser();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sentenceParser;
    }

    public String getParsedTree(String text){
        StringBuffer sb = new StringBuffer();

        for (String sentence : SentenceSplitter.getSentenceSplitter().sentDetect(text)) {
            Parse topParses[] = ParserTool.parseLine(sentence, parser, 1);

            for (Parse p : topParses) {
                p.show(sb);
            }

            sb.append(" ");
        }

        return sb.toString();
    }
}
