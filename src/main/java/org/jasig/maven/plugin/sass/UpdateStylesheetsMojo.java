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

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

/**
 * Mojo that compiles SASS Templates into CSS files. Uses JRuby to execute a generated script that calls the SASS GEM
 *
 * @goal update-stylesheets
 */
public class UpdateStylesheetsMojo extends AbstractSassMojo {

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = this.getLog();

        final String sassScript = buildSassScript();
        log.debug("SASS Ruby Script:\n" + sassScript);

        //Execute the SASS Compilation Ruby Script
        log.info("Compiling SASS Templates");
        final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        final ScriptEngine jruby = scriptEngineManager.getEngineByName("jruby");
        try {
            jruby.eval(sassScript);
            final CompilationErrors compilationErrors = (CompilationErrors) jruby.getBindings(ScriptContext.ENGINE_SCOPE).get("compilation_errors");
            if (compilationErrors.hasErrors()) {
                for (CompilationErrors.CompilationError error: compilationErrors) {
                    log.error("Compilation of template " + error.filename + " failed: " + error.message);
                }
                if (failOnError) {
                    throw new MojoFailureException("SASS compilation encountered errors (see above for details).");
                }
            }
        }
        catch (final ScriptException e) {
            throw new MojoExecutionException("Failed to execute SASS ruby script:\n" + sassScript, e);
        }
    }

    protected String buildSassScript() throws MojoExecutionException {

        final StringBuilder sassScript = new StringBuilder();
        buildBasicSASSScript(sassScript);
        sassScript.append("Sass::Plugin.update_stylesheets");

        return sassScript.toString();
    }
}
