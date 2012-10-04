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

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.DirectoryScanner;

import com.google.common.collect.ImmutableMap;

/**
 * Base for batching SASS Mojos
 * 
 */
public abstract class AbstractSassMojo extends AbstractMojo {
    /**
     * Directory containing SASS files, defaults to the Maven Web application sources directory (src/main/webapp)
     *
     * @parameter default-value="${basedir}/src/main/webapp" 
     * @required
     */
    protected File sassSourceDirectory;
    
    /**
     * Defines output directory relative to 
     *
     * @parameter default-value=".."
     */
    protected String relativeOutputDirectory;
    
    /**
     * Defines files in the source directories to include (none by default), recommended to be
     * set in favor of skinConfigurationFile
     * 
     * Defaults to: "**&#47;scss"
     *
     * @parameter
     */
    protected String[] includes = new String[] { "**/scss" };
 
    /**
     * Defines which of the included files in the source directories to exclude (none by default).
     *
     * @parameter
     */
    protected String[] excludes;
    
    /**
     * @parameter expression="${encoding}" default-value="${project.build.directory}
     * @required
     */
    protected File buildDirectory;
    
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

    
    protected void buildSassOptions(final StringBuilder sassScript) throws MojoExecutionException {
        sassScript.append("require 'rubygems'\n");
        sassScript.append("require 'sass/plugin'\n");
        sassScript.append("Sass::Plugin.options.merge!(\n");
        
        //If not explicitly set place the cache location in the target dir
        if (!sassOptions.containsKey("cache_location")) {
            final String sassCacheDir = newCanonicalFile(buildDirectory, "sass_cache");
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
    }
    
    protected String newCanonicalFile(File parent, String child) throws MojoExecutionException {
        final File f = new File(parent, child);
        try {
            return f.getCanonicalPath().replace('\\', '/');
        }
        catch (IOException e) {
            throw new MojoExecutionException("Failed to create canonical File for: " + f, e);
        }
    }

    protected Set<String> findSassDirs() {
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
