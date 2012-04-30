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
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.collect.ImmutableMap;

/**
 * Mojo that compiles SASS Templates into CSS files. Uses JRuby to execute a generated script that calls the SASS GEM
 * 
 * @goal update-stylesheets
 */
public class SassCompilerMojo extends AbstractMojo {
    /**
     * Directory containing SASS files, defaults to the Maven Web application sources directory (src/main/webapp)
     *
     * @parameter default-value="${basedir}/src/main/webapp" 
     * @required
     */
    private File sassSourceDirectory;
    
    /**
     * Defines output directory relative to 
     *
     * @parameter default-value=".."
     */
    private String relativeOutputDirectory;
    
    /**
     * Defines files in the source directories to include (none by default), recommended to be
     * set in favor of skinConfigurationFile
     * 
     * Defaults to: "**&#47;scss"
     *
     * @parameter
     */
    private String[] includes = new String[] { "**/scss" };
 
    /**
     * Defines which of the included files in the source directories to exclude (none by default).
     *
     * @parameter
     */
    private String[] excludes;
    
    /**
     * @parameter expression="${encoding}" default-value="${project.build.directory}
     * @required
     */
    private File buildDirectory;
    
    /**
     * @parameter expression="${encoding}" default-value="${project.build.directory}/${project.build.finalName}
     * @required
     */
    private File baseOutputDirectory;
    
    /**
     * Defines options for Sass::Plugin.options. See http://sass-lang.com/docs/yardoc/file.SASS_REFERENCE.html#options
     * If the value is a string it must by quoted in the maven configuration:
     * &lt;cache_location>'/tmp/sass'&lt;/cache_location>
     * <br/>
     * If no options are set the default configuration set is used which is:
     * &lt;unix_newlines>true&lt;/unix_newlines>
     * &lt;cache>true&lt;/cache>
     * &lt;always_update>true&lt;/always_update>
     * &lt;cache_location>${project.build.directory}/sass_cache&lt;/cache_location>
     * &lt;style>:expanded&lt;/style>
     *
     * @parameter
     */
    private Map<String, String> sassOptions = new HashMap<String, String>(ImmutableMap.of(
            "unix_newlines", "true", 
            "cache", "true",
            "always_update", "true",
            "style", ":expanded"));

    
    public void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = this.getLog();
        
        final StringBuilder sassScript = new StringBuilder();
        sassScript.append("begin\n");
        sassScript.append("require 'rubygems'\n");
        sassScript.append("rescue LoadError\n");
        sassScript.append("puts 'Missing RubyGems'; exit\n");
        sassScript.append("end\n");
        sassScript.append("begin\n");
        sassScript.append("require 'sass/plugin'\n");
        sassScript.append("rescue LoadError\n");
        sassScript.append("puts 'Missing Sass gem'; exit\n");
        sassScript.append("end")
        sassScript.append("Sass::Plugin.options.merge!(\n");
        
        //If not explicitly set place the cache location in the target dir
        if (!sassOptions.containsKey("cache_location")) {
            final File sassCacheDir = newCanonicalFile(buildDirectory, "sass_cache");
            sassOptions.put("cache_location", "'" + sassCacheDir.toString() + "'");
        }
        
        //Add the plugin configuration options
        for (final Iterator<Entry<String, String>> entryItr = sassOptions.entrySet().iterator(); entryItr.hasNext();) {
            final Entry<String, String> optEntry = entryItr.next();
            final String opt = optEntry.getKey();
            final String value = optEntry.getValue();
            sassScript.append("    :").append(opt).append(" => ").append(value);
            if (entryItr.hasNext()) {
                sassScript.append(",");
            }
            sassScript.append("\n");
        }
        sassScript.append(")\n");

        //Add the SASS Template locations
        final Set<String> sassDirectories = this.findSassFiles();
        for (final String sassSubDir : sassDirectories) {
            final File sassDir = newCanonicalFile(sassSourceDirectory, sassSubDir);
            final File sassDestDir = newCanonicalFile(new File(baseOutputDirectory, sassSubDir), relativeOutputDirectory);

            final String sassDirStr = sassDir.toString();
            final String sassDestDirStr = sassDestDir.toString();
            final int index = StringUtils.differenceAt(sassDirStr, sassDestDirStr);
            log.info("Queing SASS Template for compile: " + sassDirStr.substring(index) + " => " + sassDestDirStr.substring(index));
            
            sassScript.append("Sass::Plugin.add_template_location('").append(sassDir).append("', '")
                    .append(sassDestDir).append("')\n");
        }
        sassScript.append("Sass::Plugin.update_stylesheets");
        
        log.debug("SASS Ruby Script:\n" + sassScript);
        
        //Execute the SASS Compliation Ruby Script
        log.info("Compiling " + sassDirectories.size() + " SASS Templates");
        final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        final ScriptEngine jruby = scriptEngineManager.getEngineByName("jruby");
        try {
            jruby.eval(sassScript.toString());
        }
        catch (ScriptException e) {
            throw new MojoExecutionException("Failed to execute SASS ruby script:\n" + sassScript, e);
        }
    }
    
    private File newCanonicalFile(File parent, String child) throws MojoExecutionException {
        final File f = new File(parent, child);
        try {
            return f.getCanonicalFile();
        }
        catch (IOException e) {
            throw new MojoExecutionException("Failed to create canonical File for: " + f, e);
        }
    }

    private Set<String> findSassFiles() {
        final Log log = this.getLog();
        log.debug("Looking in " + sassSourceDirectory + " for dirs that match " + Arrays.toString(includes) + " but not " + Arrays.toString(excludes));
        
        final DirectoryScanner directoryScanner = new DirectoryScanner();
        directoryScanner.setIncludes(includes);
        directoryScanner.setExcludes(excludes);
        directoryScanner.setBasedir(sassSourceDirectory);
        directoryScanner.scan();
        
        final Set<String> sassDirectories = new LinkedHashSet<String>();
        for (final String dirName : directoryScanner.getIncludedDirectories()) {
            sassDirectories.add(dirName);
        }  
        
        return sassDirectories;
    }
}
