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

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

/**
 * @goal watch
 */
public class WatchMojo extends AbstractSassMojo {
    
    /**
     * Defines output directory 
     *
     * @parameter expression="${watch.output}"
     * @required
     */
    private File outputDirectory;
    
    /**
     * Specifies the skin name to watch, must match part of the skin string
     *
     * @parameter expression="${watch.skin}"
     * @required
     */
    private String skin;

    public void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = this.getLog();
        
        final String sassSubDir = this.findSassDir(this.skin);
            
        final File sassDir = newCanonicalFile(sassSourceDirectory, sassSubDir);
        final File sassDestDir = newCanonicalFile(new File(outputDirectory, sassSubDir), relativeOutputDirectory);

        final String sassSourceDirStr = sassDir.toString();
        final String cssDestDirStr = sassDestDir.toString();
        final int index = StringUtils.differenceAt(sassSourceDirStr, cssDestDirStr);
        
        //Generate the SASS Script
        final String sassScript = this.buildSassScript(sassSourceDirStr, cssDestDirStr);
        log.debug("SASS Ruby Script:\n" + sassScript);     
        
        if (log.isDebugEnabled()) {
            log.debug("Started watching SASS Template: " + sassDir + " => " + sassDestDir);
        }
        else {
            log.info("Started watching SASS Template: " + sassSourceDirStr.substring(index) + " => " + cssDestDirStr.substring(index));
        }
        
        final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        final ScriptEngine jruby = scriptEngineManager.getEngineByName("jruby");
        try {
            jruby.eval(sassScript);
        }
        catch (ScriptException e) {
            throw new MojoExecutionException("Failed to execute SASS Watch Script:\n" + sassScript, e);
        }
    }

    protected String findSassDir(String skin) throws MojoFailureException {
        final List<String> matches = new LinkedList<String>();
        
        final Set<String> sassDirectories = this.findSassDirs();
        for (final String sassSubDir : sassDirectories) {
            if (sassSubDir.contains(skin)) {
                matches.add(sassSubDir);
            }
        }
        
        if (matches.size() == 1) {
            return matches.get(0);
        }
        
        if (matches.isEmpty()) {
            final StringBuilder msg = new StringBuilder();
            msg.append("None of the SASS template directories match skin name: ").append(skin).append("\n");
            msg.append("\tSASS template directories:\n");
            for (final String sassSubDir : sassDirectories) {
                msg.append("\t\t").append(sassSubDir).append("\n");
            }
            
            throw new MojoFailureException(msg.toString());
        }
        

        final StringBuilder msg = new StringBuilder();
        msg.append("Multiple SASS template directories match skin name: ").append(skin).append("\n");
        msg.append("\tMatching SASS template directories:\n");
        for (final String sassSubDir : matches) {
            msg.append("\t\t").append(sassSubDir).append("\n");
        }
        
        throw new MojoFailureException(msg.toString());
    }

    protected String buildSassScript(String sassSourceDir, String cssDestDir) throws MojoExecutionException {
        final StringBuilder sassScript = new StringBuilder();
        
        //Set write the css output location
        sassOptions.put("template_location", "'" + sassSourceDir + "'");
        sassOptions.put("css_location", "'" + cssDestDir + "'");
        
        this.buildSassOptions(sassScript);
        sassScript.append("Sass::Plugin.watch");
        
        return sassScript.toString();
    }
}
