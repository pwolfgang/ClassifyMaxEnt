/*
 * Copyright (c) 2018, Temple University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * * All advertising materials features or use of this software must display 
 *   the following  acknowledgement
 *   This product includes software developed by Temple University
 * * Neither the name of the copyright holder nor the names of its 
 *   contributors may be used to endorse or promote products derived 
 *   from this software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package edu.temple.cla.papolicy.wolfgang.texttools.classifymaxent;

import edu.stanford.nlp.classify.Classifier;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.temple.cla.papolicy.wolfgang.texttools.util.CommonFrontEnd;
import edu.temple.cla.papolicy.wolfgang.texttools.util.Util;
import edu.temple.cla.papolicy.wolfgang.texttools.util.WordCounter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import picocli.CommandLine;

/**
 * This is a front end consistent with the other TextTools for a Maximum Entropy
 * classifier that uses the
 * <a href = "https://nlp.stanford.edu/software/classifier.shtml">
 * Stanford Classifier</a>.
 *
 * @author Paul Wolfgang
 */
public class Main implements Callable<Void> {

    @CommandLine.Option(names = "--output_table_name",
            description = "Table where results are written")
    private String outputTableName;

    @CommandLine.Option(names = "--output_code_col", required = true,
            description = "Column where the result is set")
    private String outputCodeCol;

    @CommandLine.Option(names = "--model",
            description = "Directory where model files are located")
    private String modelDir = "Model_Dir";

    private final String[] args;

    /**
     * Constructor.
     *
     * @param args The command-line arguments.
     */
    public Main(String[] args) {
        this.args = args;
    }

    /**
     * The main entry point.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        Main main = new Main(args);
        CommandLine commandLine = new CommandLine(main);
        commandLine.setUnmatchedArgumentsAllowed(true).parse(args);
        try {
            main.call();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Code for the main method. This is called after the command-line arguments
     * have been set into the fields. Most command-line arguments are processed
     * by the CommonFrontEnd.
     *
     * @return
     */
    @Override
    public Void call() {
        List<Map<String, Object>> cases = new ArrayList();
        CommonFrontEnd commonFrontEnd = new CommonFrontEnd();
        CommandLine commandLine = new CommandLine(commonFrontEnd);
        commandLine.setUnmatchedArgumentsAllowed(true);
        commandLine.parse(args);
        commonFrontEnd.loadData(cases);
        File modelParent = new File(modelDir);
        @SuppressWarnings("unchecked") 
        Classifier<String, String> classifier = 
                (Classifier<String, String>) Util.readFile(modelParent, "classifier.bin");
        cases.forEach(classificationCase -> {
            WordCounter count = (WordCounter)classificationCase.get("counts");
            Datum<String, String> datum = new BasicDatum<>(count.getWords(), null);
            String cat = classifier.classOf(datum);
            int catInt;
            if (cat.equalsIgnoreCase("true")) {
                catInt = 1;
            } else if (cat.equalsIgnoreCase("false")) {
                catInt = 0;
            } else {
                catInt = Integer.parseInt(cat);
            }
            classificationCase.put("newCode", catInt);
        });
        String outputTable = outputTableName != null ? outputTableName : commonFrontEnd.getTableName();
        if (outputCodeCol != null) {
            System.err.println("Inserting result into database");
            commonFrontEnd.outputToDatabase(outputTable,
                    outputCodeCol, cases, "newCode");
        }
        System.err.println("SUCESSFUL COMPLETION");

        return null;
    }

 }
