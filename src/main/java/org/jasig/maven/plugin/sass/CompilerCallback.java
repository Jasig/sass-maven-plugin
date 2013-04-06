package org.jasig.maven.plugin.sass;

import org.apache.maven.plugin.logging.Log;

/**
 * Callback to bind <a href="http://sass-lang.com/docs/yardoc/Sass/Plugin/Compiler.html">Sass::Plugin::Compiler</a>
 */
public class CompilerCallback {
    private boolean compileError;
    private Log log;

    public CompilerCallback(Log log) {
        this.log = log;
    }

    /**

     * @see <a href="http://sass-lang.com/docs/yardoc/Sass/Plugin/Compiler.html#on_compilation_error-instance_method">on_compilation_error</a>
     */
    public void compilationError(String error, String template, String css) {
        log.error("Compilation of template " + template + " failed: " + error);
        compileError = true;
    }

    /**
     * @see <a href="http://sass-lang.com/docs/yardoc/Sass/Plugin/Compiler.html#on_updated_stylesheet-instance_method">on_updated_stylesheet</a>
     */
    public void updatedStylesheeet(String template, String css) {
        log.info("    >> " + template + " => " + css);
    }

    /**
     * @see <a href="http://sass-lang.com/docs/yardoc/Sass/Plugin/Compiler.html#on_template_modified-instance_method">on_template_modified</a>
     */
    public void templateModified(String template) {
        log.info("Change File detected " + template);
    }

    /**
     * @see <a href="http://sass-lang.com/docs/yardoc/Sass/Plugin/Compiler.html#on_template_created-instance_method">on_template_created</a>
     */
    public void templateCreated(String template) {
        log.info("New File detected " + template);
    }

    /**
     * @see <a href="http://sass-lang.com/docs/yardoc/Sass/Plugin/Compiler.html#on_template_deleted-instance_method">on_template_deleted</a>
     */
    public void templateDeleted(String template) {
        log.info("Delete File detected " + template);
    }

    public boolean hadError() {
        return compileError;
    }
}
