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
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

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

    public void execute() throws MojoExecutionException, MojoFailureException {
        final Log log = this.getLog();
        
        final Set<Future<Object>> watchedThreads = Collections.newSetFromMap(new ConcurrentHashMap<Future<Object>, Boolean>());
        
        //Add shutdown hook that cleans up watch threads
        final Runnable shutdownRunnable = new Runnable() {
            @Override
            public void run() {
                log.info("Shutting down " + watchedThreads.size() + " SASS Watch Threads");
                
                while (!watchedThreads.isEmpty()) {
                    for (final Iterator<Future<Object>> futureItr = watchedThreads.iterator(); futureItr.hasNext(); ) {
                        final Future<Object> future = futureItr.next();
                        if (future.isDone()) {
                            futureItr.remove();
                        }
                        else {
                            future.cancel(true);
                        }
                    }
                    
                    //Don't make this a hard spin loop, yield
                    Thread.yield();
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(new Thread(shutdownRunnable, "SASS Watch Shutdown Hook"));
        
        final Set<String> sassDirectories = this.findSassDirs();
        for (final String sassSubDir : sassDirectories) {
            final File sassDir = newCanonicalFile(sassSourceDirectory, sassSubDir);
            final File sassDestDir = newCanonicalFile(new File(outputDirectory, sassSubDir), relativeOutputDirectory);

            final String sassSourceDirStr = sassDir.toString();
            final String cssDestDirStr = sassDestDir.toString();
            final int index = StringUtils.differenceAt(sassSourceDirStr, cssDestDirStr);
            
            //Generate the SASS Script
            final String sassScript = this.buildSassScript(sassSourceDirStr, cssDestDirStr);
            log.debug("SASS Ruby Script:\n" + sassScript);        
            
            final Runnable watchRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (log.isDebugEnabled()) {
                            log.debug("Started watching SASS Template: " + sassDir + " => " + sassDestDir);
                        }
                        else {
                            log.info("Started watching SASS Template: " + sassSourceDirStr.substring(index) + " => " + cssDestDirStr.substring(index));
                        }
                        
                        final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
                        final ScriptEngine jruby = scriptEngineManager.getEngineByName("jruby");
                        jruby.eval(sassScript);
                        
                        log.info("Stopped watching SASS Template: " + sassSourceDirStr.substring(index) + " => " + cssDestDirStr.substring(index));
                    }
                    catch (Exception e) {
                        log.error("Error while watching SASS Template: " + sassSourceDirStr.substring(index) + " => " + cssDestDirStr.substring(index), e);
                    }                    
                }
            };
            
            final FutureTask<Object> future = new FutureTask<Object>(watchRunnable, null);
            final Thread watchThread = new Thread(future, "SASS Watch " + sassSourceDirStr.substring(index));
            watchedThreads.add(future);
            watchThread.start();
        }
        
        
        //Wait for all watcher threads to complete
        while (!watchedThreads.isEmpty()) {
            for (final Iterator<Future<Object>> futureItr = watchedThreads.iterator(); futureItr.hasNext(); ) {
                final Future<Object> future = futureItr.next();
                try {
                    future.get(1, TimeUnit.SECONDS);
                    futureItr.remove(); //remove futures that have completed
                }
                catch (InterruptedException e) {
                    //Stop what we were doing, we were interrupted
                    return;
                }
                catch (ExecutionException e) {
                    //ignore
                }
                catch (TimeoutException e) {
                    //ignore
                }
            }
        }
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
