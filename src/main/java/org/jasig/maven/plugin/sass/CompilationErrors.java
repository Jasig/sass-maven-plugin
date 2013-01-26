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