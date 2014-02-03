/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.maven.plugin.sass;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.zip.CRC32;

/**
 * Mojo that compiles SASS Templates into CSS files. Uses JRuby to execute a generated script that calls the SASS GEM
 *
 * @goal update-stylesheets
 * @phase process-sources
 */
public class UpdateStylesheetsMojo extends AbstractSassMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Compiling SASS Templates");


        boolean execute = performExecutionCheck();

        if (!execute) {
            getLog().info("Skipping SASS Templates, no changes");
            return;
        }

        // build sass script
        final StringBuilder sassBuilder = new StringBuilder();
        buildBasicSASSScript(sassBuilder);
        sassBuilder.append("Sass::Plugin.update_stylesheets");
        final String sassScript = sassBuilder.toString();

        // ...and execute
        executeSassScript(sassScript);
    }

    private boolean performExecutionCheck() throws MojoFailureException {
        boolean execute;

        try {
            File cacheFile = constructChecksumFile();
            Map<String, String> cachedChecksumMap = parseChecksumFile(cacheFile);
            Map<String, String> currentChecksumMap = createCurrentChecksumMap();

            if (currentChecksumMap.equals(cachedChecksumMap)) {
                execute = false;
            } else {
                execute = true;
            }

            writeChecksumFile(cacheFile, currentChecksumMap);

        } catch (IOException e) {
            throw new MojoFailureException(e.getMessage(), e);
        }
        return execute;
    }

    private Map<String, String> createCurrentChecksumMap() throws IOException {
        Map<String, String> currentChecksumMap = new HashMap<String, String>();
        Iterator<Map.Entry<String, String>> locations = getTemplateLocations();

        while (locations.hasNext()) {
            Map.Entry<String, String> entry = locations.next();
            File sourceDir = new File(entry.getKey());

            if (!sourceDir.exists())  {
                continue;
            }

            Iterator<File> files = FileUtils.iterateFiles(sourceDir, new String[] { "scss" }, true);

            while (files.hasNext())  {
                File file = files.next();
                String fileEntry = file.getCanonicalPath();
                String currentChecksum = getChecksum(file);

                currentChecksumMap.put(fileEntry, currentChecksum);
            }
        }
        return currentChecksumMap;
    }

    private Map<String, String> parseChecksumFile(File cacheFile) throws IOException {
        Map<String, String> checksumMap = new HashMap<String, String>();

        if (cacheFile.exists()) {
            List<String> lines = Files.readLines(cacheFile, Charset.defaultCharset());

            for (String line : lines) {
                if (!line.contains(" ")) {
                    continue;
                }

                int index = line.indexOf(' ');

                String checksum = line.substring(0, index);
                String filename = line.substring(index + 1);
                checksumMap.put(filename, checksum);
            }
        }
        return checksumMap;
    }

    private void writeChecksumFile(File cacheFile, Map<String, String> checksumMap) throws FileNotFoundException {
        BufferedWriter writer = Files.newWriter(cacheFile, Charset.defaultCharset());
        PrintWriter pw = new PrintWriter(writer);
        for (Map.Entry<String, String> entry : checksumMap.entrySet()) {
            pw.println(createChecksumLine(entry));
        }
        pw.close();
    }

    private String createChecksumLine(Map.Entry<String, String> entry) {
        return entry.getValue() + " " + entry.getKey();
    }

    private File constructChecksumFile() {
        File cacheDirectory = new File(buildDirectory, SASS_CACHE);
        if (!cacheDirectory.exists()) {
            cacheDirectory.mkdirs();
        }
        return new File(cacheDirectory, "checksums");
    }

    private String getChecksum(File file) throws IOException {
        long checksum = Files.getChecksum(file, new CRC32());
        return Long.toHexString(checksum);
    }

}
