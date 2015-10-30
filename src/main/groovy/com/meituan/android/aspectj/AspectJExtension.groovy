package com.meituan.android.aspectj

import com.android.build.gradle.internal.CompileOptions
import org.gradle.api.Action;

/**
 * Created by Xiz on Oct 28, 2015.
 */
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ExcludeRuleContainer
import org.gradle.api.internal.artifacts.DefaultExcludeRuleContainer

class AspectJExtension {
    private ExcludeRuleContainer excludeRuleContainer = new DefaultExcludeRuleContainer();

    private CompileOptions compileOptions = new CompileOptions();

    private boolean rxJavaEnabled;

    public void exclude(Map<String, String> excludeProperties) {
        excludeRuleContainer.add(excludeProperties);
    }

    public Set<ExcludeRule> getExcludeRules() {
        return excludeRuleContainer.getRules();
    }

    private void setExcludeRuleContainer(ExcludeRuleContainer excludeRuleContainer) {
        this.excludeRuleContainer = excludeRuleContainer;
    }

    public void compileOptions(Action<CompileOptions> action) {
        action.execute(compileOptions);
    }

    public CompileOptions getCompileOptions() {
        return compileOptions;
    }

    boolean getRxJavaEnabled() {
        return rxJavaEnabled
    }

    void setRxJavaEnabled(boolean rxJavaEnabled) {
        this.rxJavaEnabled = rxJavaEnabled
    }
}