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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import com.google.common.collect.ImmutableMap;

/**
 * Base for batching SASS Mojos
 * 
 */
public abstract class AbstractSassMojo extends AbstractMojo {

    /**
     * Sources for compilation with their destination directory containing SASS files
     *
     * @parameter
     * @required
     */
    protected List<Resource> resources;
    
    /**
     * @parameter expression="${encoding}" default-value="${project.build.directory}
     * @required
     */
    protected File buildDirectory;

    /**
     * Fail the build if errors occur during compilation of sass/scss templates.
     *
     * @parameter default-value="true"
     */
    protected boolean failOnError;
    
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
    protected Map<String, String> sassOptions = new HashMap<String, String>(ImmutableMap.of(
            "unix_newlines", "true", 
            "cache", "true",
            "always_update", "true",
            "style", ":expanded"));

    
    protected void buildBasicSASSScript(final StringBuilder sassScript) throws MojoExecutionException {
        final Log log = this.getLog();
        
        sassScript.append("require 'rubygems'\n");
        sassScript.append("require 'sass/plugin'\n");
        sassScript.append("require 'java'\n");
        
        sassScript.append("Sass::Plugin.options.merge!(\n");
        
        //If not explicitly set place the cache location in the target dir
        if (!this.sassOptions.containsKey("cache_location")) {
            final File sassCacheDir = new File(this.buildDirectory, "sass_cache");
            final String sassCacheDirStr = sassCacheDir.toString();
            this.sassOptions.put("cache_location", "'" + escapePath(sassCacheDirStr) + "'");
        }
        
        //Add the plugin configuration options
        for (final Iterator<Entry<String, String>> entryItr = this.sassOptions.entrySet().iterator(); entryItr.hasNext();) {
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
        
        // set up compilation error reporting
        sassScript.append("java_import ");
        sassScript.append(CompilationErrors.class.getName());
        sassScript.append("\n");
        sassScript.append("$compilation_errors = CompilationErrors.new\n");
        sassScript.append("Sass::Plugin.on_compilation_error {|error, template, css| $compilation_errors.add(template, error.message) }\n");

        //Add the SASS template locations
        for (Resource source : resources) {
        	for (Entry<String, String> entry : source.getDirectoriesAndDestinations().entrySet()) {
        		log.info("Queing SASS Template for compile: " + entry.getKey() + " => " + entry.getValue());

    		    sassScript.append("Sass::Plugin.add_template_location('")
    		    	.append(entry.getKey())
    		    	.append("', '")
    		    	.append(entry.getValue())
    		    	.append("')\n");
        	}
		}
    }

    /**
     * Handles the usage of Windows style paths like c:\foo\bar\scss with
     * the SASS mojos provided by this plugin.
     *
     * @param originalPath the original path in native system style
     * @return the converted pathname
     */
    protected String escapePath(final String originalPath) {
        if(originalPath == null || originalPath.isEmpty()) {
            throw new IllegalArgumentException("No path given.");
        }
        return originalPath.replace("\\", "/");
    }

}
