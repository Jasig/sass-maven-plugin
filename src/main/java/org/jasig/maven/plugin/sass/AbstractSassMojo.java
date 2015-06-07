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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Base for batching SASS Mojos.
 * 
 */
public abstract class AbstractSassMojo extends AbstractMojo {

    /**
     * Sources for compilation with their destination directory containing SASS files. Allows
     * for multiple resource sources and destinations. If specified it precludes the direct
     * specification of sassSourceDirectory/relativeOutputDirectory/destination parameters.
     * <br/>
     * Example configuration
     * <pre>
     *      &lt;resource>
     *          &lt;source>
     *              &lt;directory>${basedir}/src/main/webapp&lt;/directory>
     *              &lt;includes>
     *                  &lt;include>**&#47;scss&lt;/include>
     *              &lt;/includes>
     *          &lt;/source>
     *          &lt;relativeOutputDirectory>..&lt;/relativeOutputDirectory>
     *          &lt;destination>${project.build.directory}/${project.build.finalName}&lt;/destination>
     *      &lt;/resource>
     * </pre>
     *
     * @parameter
     */
    protected List<Resource> resources;

    /**
     * Defines paths where jruby will look for gems. E.g. a maven build could download
     * gems into ${project.build.directory}/rubygems and a gemPath pointed to this
     * directory. Finally, individual gems can be loaded via the &lt;gems> configuration.
     *
     * @parameter default-value="${project.build.directory}/rubygems"
     */
    protected String[] gemPaths = new String[0];

    /**
     * Defines gems to be loaded before Sass/Compass. This is useful to add gems
     * with custom Sass functions or stylesheets. Gems that hook into Compass
     * are transparently added to Sass' load_path.
     *
     * @parameter
     */
    protected String[] gems = new String[0];

    /**
     * Build directory for the plugin.
     *
     * @parameter expression="${buildDirectory}" default-value="${project.build.directory}"
     */
    protected File buildDirectory;

    /**
     * Fail the build if errors occur during compilation of sass/scss templates.
     *
     * @parameter default-value="true"
     */
    protected boolean failOnError;

    /**
     * Defines options for Sass::Plugin.options. See
     * {@link http://sass-lang.com/docs/yardoc/file.SASS_REFERENCE.html#options }
     * If the value is a string it must by quoted in the maven configuration:
     * &lt;cache_location>'/tmp/sass'&lt;/cache_location> <br/>
     * If no options are set the default configuration set is used which is:
     * 
     * <pre>
     * &lt;unix_newlines>true&lt;/unix_newlines>
     * &lt;cache>true&lt;/cache>
     * &lt;always_update>true&lt;/always_update>
     * &lt;cache_location>${project.build.directory}/sass_cache&lt;/cache_location>
     * &lt;style>:expanded&lt;/style>
     * </pre>
     * 
     * @parameter
     */
    protected Map<String, String> sassOptions = new HashMap<String, String>(ImmutableMap.of(
            "unix_newlines", "true",
            "cache", "true",
            "always_update", "true",
            "style", ":expanded"));

    /**
     * Enable the use of Compass style library mixins, this emulates the
     * {@code --compass} commandline option of Sass.
     *
     * @parameter default-value="false"
     */
    protected boolean useCompass;

    /**
     * Directory containing SASS files, defaults to the Maven Web application sources directory (src/main/webapp)
     *
     * @parameter default-value="${basedir}/src/main/webapp" 
     * @required
     */
    protected File sassSourceDirectory;
    
    /**
     * Defines files in the source directories to include
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
     * Defines an additional path section when calculating the destination for the SCSS file. Allows,
     * for example "/media/skins/universality/coal/scss/portal.scss" to end up at "/media/skins/universality/coal/portal.css"
     * by specifying ".."  
     *
     * @parameter default-value=".."
     */
    protected String relativeOutputDirectory;
   
    /**
     * Where to put the compiled CSS files
     *
     * @parameter expression="${encoding}" default-value="${project.build.directory}/${project.build.finalName}
     */
    protected File destination;

    /**
     * Execute the SASS Compilation Ruby Script
     */
    protected void executeSassScript(String sassScript) throws MojoExecutionException, MojoFailureException {
        final Log log = this.getLog();
        System.setProperty("org.jruby.embed.localcontext.scope", "threadsafe");

        log.debug("Execute SASS Ruby Script:\n" + sassScript);

        final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
        final ScriptEngine jruby = scriptEngineManager.getEngineByName("jruby");
        try {
            CompilerCallback compilerCallback = new CompilerCallback(log);
            jruby.getBindings(ScriptContext.ENGINE_SCOPE).put("compiler_callback", compilerCallback);
            jruby.eval(sassScript);
            if (failOnError && compilerCallback.hadError()) {
                throw new MojoFailureException("SASS compilation encountered errors (see above for details).");
            }
        }
        catch (final ScriptException e) {
            throw new MojoExecutionException("Failed to execute SASS ruby script:\n" + sassScript, e);
        }
    }

    protected void buildBasicSASSScript(final StringBuilder sassScript) throws MojoExecutionException {
        final Log log = this.getLog();

        sassScript.append("require 'rubygems'\n");

        if (gemPaths.length > 0) {
            sassScript.append("env = { 'GEM_PATH' => [\n");
            for (final String gemPath : gemPaths) {
                sassScript.append("    '").append(gemPath).append("',\n");
            }

            final String gemPath = System.getenv("GEM_PATH");
            if (gemPath != null) {
                for (final String p : gemPath.split(File.pathSeparator)) {
                    sassScript.append("    '").append(p).append("',\n");
                }
            }
            sassScript.setLength(sassScript.length() - 2); // remove trailing comma
            sassScript.append("\n");
            sassScript.append("] }\n");
            sassScript.append("Gem.paths = env\n");
        }

        for (final String gem : gems) {
            sassScript.append("require '").append(gem).append("'\n");
        }

        sassScript.append("require 'sass/plugin'\n");
        sassScript.append("require 'java'\n");

        if (this.useCompass) {
            log.info("Running with Compass enabled.");
            sassScript.append("require 'compass'\n");
            sassScript.append("require 'compass/exec'\n");
            sassScript.append("Compass.add_project_configuration \n");
            this.sassOptions.put("load_paths", "Compass.configuration.sass_load_paths");
            // manually specify these paths
            sassScript.append("Compass::Frameworks.register_directory('jar:'+ File.join(Compass.base_directory, 'frameworks/compass'))\n");
            sassScript.append("Compass::Frameworks.register_directory('jar:'+ File.join(Compass.base_directory, 'frameworks/blueprint'))\n");
        }

        // Get all template locations from resources and set option 'template_location' and
        // 'css_location' (to override default "./public/stylesheets/sass", "./public/stylesheets")
        // remaining locations are added later with 'add_template_location'
        final Iterator<Entry<String, String>> templateLocations = getTemplateLocations();
        if (templateLocations.hasNext()) {
            Entry<String, String> location = templateLocations.next();
            sassOptions.put("template_location", "'" + location.getKey() + "'");
            sassOptions.put("css_location", "'" + location.getValue() + "'");
        }

        //If not explicitly set place the cache location in the target dir
        if (!this.sassOptions.containsKey("cache_location")) {
            final File sassCacheDir = new File(this.buildDirectory, "sass_cache");
            final String sassCacheDirStr = sassCacheDir.toString();
            this.sassOptions.put("cache_location", "'" + FilenameUtils.separatorsToUnix(sassCacheDirStr) + "'");
        }

        //Add the plugin configuration options
        sassScript.append("Sass::Plugin.options.merge!(\n");
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

        // add remaining template locations with 'add_template_location' (need to be done after options.merge)
        while (templateLocations.hasNext()) {
            Entry<String, String> location = templateLocations.next();
            sassScript.append("Sass::Plugin.add_template_location('")
                .append(location.getKey())
                .append("', '")
                .append(location.getValue())
                .append("')\n");
        }

        // set up sass compiler callback for reporting
        sassScript.append("Sass::Plugin.on_compilation_error {|error, template, css| $compiler_callback.compilationError(error.message, template, css) }\n");
        sassScript.append("Sass::Plugin.on_updated_stylesheet {|template, css| $compiler_callback.updatedStylesheeet(template, css) }\n");
        sassScript.append("Sass::Plugin.on_template_modified {|template| $compiler_callback.templateModified(template) }\n");
        sassScript.append("Sass::Plugin.on_template_created {|template| $compiler_callback.templateCreated(template) }\n");
        sassScript.append("Sass::Plugin.on_template_deleted {|template| $compiler_callback.templateDeleted(template) }\n");

        // make ruby give use some debugging info when requested
        if (log.isDebugEnabled()) {
            sassScript.append("require 'pp'\npp Sass::Plugin.options\n");
            if (useCompass) {
                sassScript.append("pp Compass::configuration\n");
            }
        }
    }

    private Iterator<Entry<String, String>> getTemplateLocations() {
        final Log log = getLog();
        
        List<Resource> r = this.resources;
        
        //If no resources specified
        if (r == null) {
            final Resource resource = new Resource();
            resource.source = new FileSet();
            resource.source.setDirectory(this.sassSourceDirectory.toString());
            if (this.includes != null) {
                resource.source.setIncludes(Arrays.asList(this.includes));
            }
            if (this.excludes != null) {
                resource.source.setExcludes(Arrays.asList(this.excludes));
            }
            resource.relativeOutputDirectory = this.relativeOutputDirectory;
            resource.destination = this.destination;
            r = ImmutableList.of(resource);
        }
        
        List<Entry<String, String>> locations = new ArrayList<Entry<String, String>>();
        for (final Resource source : r) {

            if(!source.destination.exists()) {
               log.warn("Source "+source.destination+ " does not exist");
               continue;
            }

            for (final Entry<String, String> entry : source.getDirectoriesAndDestinations().entrySet()) {
                log.info("Queueing SASS Template for compile: " + entry.getKey() + " => " + entry.getValue());
                locations.add(entry);
            }
        }
        return locations.iterator();
    }

}
