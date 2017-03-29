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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import junit.framework.TestCase;

import org.apache.maven.it.Verifier;
import org.apache.maven.it.util.ResourceExtractor;

/**
 * Integration test for determining if compass compilation works based on a
 * simple war project.
 * 
 * @author Mark C. Prins <mprins@users.sf.net>
 */
public class MavenCompassIntegrationTest extends TestCase {
    private Verifier verifier;
    private File testDir;
    private final String GROUPID = "org.jasig.maven.sass-maven-plugin";
    private final String ARTIFACTID = "maven-compass-test";
    private final String VERSION = "1.0";
    private final String PACKAGING = "war";

    /**
     * setUp the Maven project and verifier, execute the 'compile' goal.
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        this.testDir = ResourceExtractor.simpleExtractResources(
                this.getClass(), "/maven-compass-test");

        this.verifier = new Verifier(this.testDir.getAbsolutePath());

        this.verifier.deleteArtifact(this.GROUPID,
                this.ARTIFACTID, this.VERSION, this.PACKAGING);
        this.verifier.executeGoal("compile");
    }

    /**
     * test for error free execution.
     * 
     * @throws Exception
     */
    public void testErrorFree() throws Exception {
        this.verifier.resetStreams();
        this.verifier.verifyErrorFreeLog();
    }

    /**
     * test for equal-ness of result.
     * 
     * @throws Exception
     */
    public void testCompareResults() throws Exception {
        final String expected = readFileAsString(this.testDir.getAbsolutePath()
                + File.separator + "expected.css");

        final String compiled = this.verifier.getBasedir() + File.separator
                + "target" + File.separator + this.ARTIFACTID + "-"
                + this.VERSION + File.separator + "css" + File.separator
                + "compiled.css";

        this.verifier.assertFilePresent(compiled);

        final String actual = readFileAsString(compiled);
        assertEquals(expected, actual);
    }

    /**
     * execute the 'clean' goal.
     * 
     * @see junit.framework.TestCase#tearDown()
     */
    @Override
    protected void tearDown() throws Exception {
        this.verifier.executeGoal("clean");
    }

    /**
     * read a file in one block.
     * 
     * @param filePath
     *            file to read
     * @return a string with the file contents
     * @throws IOException
     */
    private static String readFileAsString(String filePath) throws IOException {
        final byte[] buffer = new byte[(int) new File(filePath).length()];
        BufferedInputStream f = null;
        try {
            f = new BufferedInputStream(new FileInputStream(filePath));
            f.read(buffer);
        } finally {
            if (f != null) {
                try {
                    f.close();
                } catch (final IOException ignored) {
                }
            }
        }
        return new String(buffer);
    }
}
