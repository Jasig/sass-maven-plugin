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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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

        // build sass script
        final StringBuilder sassBuilder = new StringBuilder();
        buildBasicSASSScript(sassBuilder);
        sassBuilder.append("Sass::Plugin.update_stylesheets");
        final String sassScript = sassBuilder.toString();

        // ...and execute
        executeSassScript(sassScript);
    }
}
