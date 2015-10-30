package com.meituan.android.aspectj;

import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * <p>Gradle plugin for AspectJ weaving in android applications.</p>
 *
 * <p>This plugin does an intermediate work on full-project byte codes. These byte codes are woven
 * with all aspects. This means byte codes in dependencies, sub projects, etc., will all be woven.
 * And every aspect in such places will also be woven into all byte codes. This case is much more
 * like you mix them up since plugin treats them in an undifferentiated way.</p>
 *
 * <h3>Usage</h3>
 * <p>apply this after you apply android application plugin in your build script.</p>
 *
 * <h3>Prerequisite</h3>
 * <ul>
 *
 * <li>Add aspect runtime dependency, as <code>compile 'org.aspectj:aspectjrt:1.8.6'</code></li>
 *
 * <li>Write your aspects in Aspect Annotation (@Aspect) syntax</li>
 *
 * <li>Apply android gradle plugin 1.4.0-beta2 or higher.</li>
 *
 * </ul>
 *
 * <p>Created by Xiz on 9/21, 2015.</p>
 */
class AspectJPlugin implements Plugin<Project> {
    protected Project project;

    @Override
    public void apply(Project project) {
        this.project = project;

        checkAndroidPlugin()
        configureProject()
        createExtension()
    }

    protected void configureProject() {
        project.android.registerTransform(new AspectJTransform(project))
        project.dependencies {
            compile 'org.aspectj:aspectjrt:1.8.7'
        }
    }

    protected void checkAndroidPlugin() {
        if (!project.plugins.hasPlugin(AppPlugin)) {
            throw new GradleException("The android 'application' plugin is required.")
        }
    }

    protected void createExtension() {
        project.extensions.create('aspectj', AspectJExtension)
    }
}
