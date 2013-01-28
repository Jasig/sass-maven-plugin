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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility class for capturing sass/scss compilation errors in ruby
 * and transporting them into the java world for reporting in maven.
 */
public class CompilationErrors implements Iterable<CompilationErrors.CompilationError> {
    final List<CompilationError> errors = new ArrayList<CompilationError>();

    public void add(String file, String message) {
        errors.add(new CompilationError(file, message));
    }

    public boolean hasErrors() {
        return errors.size() > 0;
    }

    @Override
    public Iterator<CompilationError> iterator() {
        return errors.iterator();
    }

    public class CompilationError {
        final String filename;
        final String message;

        CompilationError(String filename, String message) {
            this.filename = filename;
            this.message = message;
        }
    }
}