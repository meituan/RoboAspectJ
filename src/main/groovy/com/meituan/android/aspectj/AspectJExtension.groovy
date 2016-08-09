package com.meituan.android.aspectj

import com.android.build.gradle.internal.CompileOptions
import org.gradle.api.Action
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ExcludeRuleContainer
import org.gradle.api.internal.artifacts.DefaultExcludeRuleContainer

/**
 * Created by Xiz on Oct 28, 2015.
 */
class AspectJExtension {
    private ExcludeRuleContainer excludeRuleContainer = new DefaultExcludeRuleContainer()

    private CompileOptions compileOptions = new CompileOptions()

    // when rxjava in use, ajc requires jre rt.java as its classpath. Or an error will be issued.
    private boolean javartNeeded = false

    private boolean verbose = false

    private boolean enable = true

    private boolean disableWhenDebug = false

    private XLintLevel xlintLevel = XLintLevel.ERROR

    public void exclude(Map<String, String> excludeProperties) {
        excludeRuleContainer.add(excludeProperties)
    }

    public Set<ExcludeRule> getExcludeRules() {
        return excludeRuleContainer.getRules()
    }

    public void compileOptions(Action<CompileOptions> action) {
        action.execute(compileOptions)
    }

    void setCompileOptions(CompileOptions compileOptions) {
        this.compileOptions = compileOptions
    }

    public CompileOptions getCompileOptions() {
        return compileOptions
    }

    boolean getJavartNeeded() {
        return javartNeeded
    }

    void setJavartNeeded(boolean javartNeeded) {
        this.javartNeeded = javartNeeded
    }

    boolean getVerbose() {
        return verbose
    }

    void setVerbose(boolean verbose) {
        this.verbose = verbose
    }

    boolean getEnable() {
        return enable
    }

    void setEnable(boolean enabled) {
        this.enable = enabled
    }

    boolean getDisableWhenDebug() {
        return disableWhenDebug
    }

    void setDisableWhenDebug(boolean disableWhenDebug) {
        this.disableWhenDebug = disableWhenDebug
    }

    public void xlintLevel(XLintLevel xlintLevel) {
        this.xlintLevel = xlintLevel
    }

    XLintLevel getXlintLevel() {
        return xlintLevel
    }

    public enum XLintLevel {
        IGNORE,
        WARNING,
        ERROR
    }
}