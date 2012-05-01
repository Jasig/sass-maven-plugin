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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

import com.google.common.collect.ImmutableMap;

/**
 * @goal watch
 */
public class WatchMojo extends AbstractMojo {
    /**
     * Directory containing SASS files
     * 
     * @parameter expression="${watch.sassSource}"
     * @required
     */
    private File sassSourceDirectory;
    
    /**
     * Defines output directory 
     *
     * @parameter expression="${watch.output}"
     * @required
     */
    private File outputDirectory;
    
    /**
     * @parameter expression="${encoding}" default-value="${project.build.directory}
     * @required
     */
    private File buildDirectory;
    
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
        sassScript.append("require 'rubygems'\n");
        sassScript.append("require 'sass/plugin'\n");
        sassScript.append("Sass::Plugin.options.merge!(\n");
        
        //If not explicitly set place the cache location in the target dir
        if (!sassOptions.containsKey("cache_location")) {
            final File sassCacheDir = newCanonicalFile(buildDirectory, "sass_cache");
            sassOptions.put("cache_location", "'" + sassCacheDir.toString() + "'");
        }
        
        //If not explicitly set write the css output location
        sassOptions.put("css_location", "'" + outputDirectory.toString() + "'");
        sassOptions.put("template_location", "'" + sassSourceDirectory.toString() + "'");
        
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
        sassScript.append("Sass::Plugin.watch");
        
        log.debug("SASS Ruby Script:\n" + sassScript);
        
        //Execute the SASS Compliation Ruby Script
        final String sassSourceDirStr = this.sassSourceDirectory.toString();
        final String outputDirStr = this.outputDirectory.toString();
        final int index = StringUtils.differenceAt(sassSourceDirStr, outputDirStr);
        
        log.info("Watching SASS Templates in " + sassSourceDirStr.substring(index) + " and writing CSS to " + outputDirStr.substring(index));
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
}
