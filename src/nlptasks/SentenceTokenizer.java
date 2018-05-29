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

import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class SentenceTokenizer {
    private static SentenceTokenizer sentenceTokenizer;
    private final Tokenizer tokenizer;

    private SentenceTokenizer() throws IOException {
        InputStream modelIn = new FileInputStream("./models/en-token.bin");
        TokenizerModel model = new TokenizerModel(modelIn);
        tokenizer = new TokenizerME(model);
        modelIn.close();
    }

    public static SentenceTokenizer getSentenceTokenizer() {
        if (sentenceTokenizer == null) {
            try {
                sentenceTokenizer = new SentenceTokenizer();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sentenceTokenizer;
    }

    public synchronized String[] getTokens(String text) {
        return tokenizer.tokenize(text);
    }
}