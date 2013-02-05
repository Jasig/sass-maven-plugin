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
import java.util.LinkedHashMap;

import org.apache.maven.model.FileSet;
import org.codehaus.plexus.util.DirectoryScanner;
import org.codehaus.plexus.util.StringUtils;

public class Resource {

	 /**
     * Directories containing SASS files
     *
     * @parameter
     * @required
     */
	protected FileSet source;
	
	/**
     * Where to put the compiled CSS files
     *
     * @parameter expression="${encoding}" default-value="${project.build.directory}/${project.build.finalName}
     * @required
     */
	protected File destination;
	
	public LinkedHashMap<String, String> getDirectoriesAndDestinations() {
		
		// Scan for directories
		DirectoryScanner scanner = new DirectoryScanner();
    	scanner.setBasedir(source.getDirectory());
    	scanner.setIncludes(source.getIncludes().toArray(new String[source.getIncludes().size()]));
    	scanner.setExcludes(source.getExcludes().toArray(new String[source.getExcludes().size()]));
    	scanner.scan();
    	
    	LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
    	result.put(source.getDirectory(), destination.toString());
    	for (String included : scanner.getIncludedDirectories()) {
    		if (!included.isEmpty()) {
	    		String subdir = StringUtils.difference(source.getDirectory(), included);
	    		result.put(source.getDirectory() + "/" + included, destination.toString() + "/" + subdir);
    		}
    	}
        return result;
	}
}
