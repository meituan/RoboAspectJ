package com.meituan.android.aspectj

import com.android.annotations.NonNull
import com.android.build.api.transform.*
import com.google.common.base.Joiner
import com.google.common.base.Strings
import com.google.common.collect.Lists
import com.google.common.collect.Sets
import org.apache.commons.io.FileUtils
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.logging.Logger

/**
 * <p>The main work of this {@link com.android.build.api.transform.Transform} implementation is to
 * do the AspectJ binary weaving at build time.</p>
 *
 * <p>Tranform system in android plugin offers us a good timing to manipulate byte codes. So we
 * interpose weaving in the way of turning class files to dex files, specifically speaking, right
 * after the system extracts byte codes from project, sub projects, dependencies, etc.. Note
 * that aspect and real java codes are both literally written in Java, we don't have to separate
 * them apart. So ajc aspect path and inpath should be identical. </p>
 *
 * <p>Created by Xiz on 9/21, 2015.</p>
 */
public class AspectJTransform extends Transform {
    private static final Set<QualifiedContent.ContentType> COTNENT_CLASS = Sets.immutableEnumSet(QualifiedContent.DefaultContentType.CLASSES)
    private static final Set<QualifiedContent.Scope> SCOPE_FULL_PROJECT = Sets.immutableEnumSet(
            QualifiedContent.Scope.PROJECT,
            QualifiedContent.Scope.PROJECT_LOCAL_DEPS,
            QualifiedContent.Scope.SUB_PROJECTS,
            QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
            QualifiedContent.Scope.EXTERNAL_LIBRARIES)

    private Project project

    public AspectJTransform(Project project) {
        this.project = project
    }

    @Override
    void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutputProvider outputProvider, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        Logger logger = project.getLogger()
        File output = null;

        // grab java runtime jar
        String javaRtPath = null
        project.android.applicationVariants.all {
            String javaRt = Joiner.on(File.separator).join(['jre', 'lib', 'rt.jar'])
            for (String classpath : javaCompiler.classpath.asPath.split(File.pathSeparator)) {
                if (classpath.contains(javaRt)) {
                    javaRtPath = classpath
                }
            }
        }

        // categorize bytecode files
        List<File> files = Lists.newArrayList()
        List<File> excludeFiles = Lists.newArrayList()
        for (TransformInput input : inputs) {
            for (DirectoryInput folder : input.directoryInputs) {
                if (isFileExcluded(folder.file)) {
                    excludeFiles.add(folder.file)
                    output = outputProvider.getContentLocation(folder.name, outputTypes, scopes, Format.DIRECTORY)
                    FileUtils.copyDirectoryToDirectory(folder.file, output)
                } else {
                    files.add(folder.file)

                }
            }

            for (JarInput jar : input.jarInputs) {
                if (isFileExcluded(jar.file)) {
                    excludeFiles.add(jar.file)
                    output = outputProvider.getContentLocation(jar.name.replace(".jar", ""), outputTypes, scopes, Format.JAR)
                    FileUtils.copyFile(jar.file, output)
                } else {
                    files.add(jar.file)

                }
            }
        }

        // copy excluded files for other transforms' usage later
        output = outputProvider.getContentLocation(name, outputTypes, scopes, Format.DIRECTORY);

        //evaluate class paths
        final String inpath = Joiner.on(File.pathSeparator).join(files)
        final String classpath = Joiner.on(File.pathSeparator).join(
                project.aspectj.javartNeeded && !Strings.isNullOrEmpty(javaRtPath) ?
                        [*excludeFiles.collect { it.absolutePath }, javaRtPath] :
                        excludeFiles.collect { it.absolutePath })
        final String bootpath = Joiner.on(File.pathSeparator).join(project.android.bootClasspath)

        // assemble compile options
        def args = [
                "-source", project.aspectj.compileOptions.sourceCompatibility.name,
                "-target", project.aspectj.compileOptions.targetCompatibility.name,
                "-showWeaveInfo",
                "-encoding", project.aspectj.compileOptions.encoding,
                "-inpath", inpath,
                "-d", output.absolutePath,
                "-bootclasspath", bootpath]

        // append classpath argument if any
        if (!Strings.isNullOrEmpty(classpath)) {
            args << '-classpath'
            args << classpath
        }

        // run compilation
        MessageHandler handler = new MessageHandler(true)
        new Main().run(args as String[], handler)

        // log compile
        for (IMessage message : handler.getMessages(null, true)) {
            // level up weave info log for debug
//            logger.quiet(message.getMessage())
            if (IMessage.ERROR.isSameOrLessThan(message.getKind())) {
                logger.error(message.getMessage(), message.getThrown())
                throw new GradleException(message.message, message.thrown)
            } else if (IMessage.WARNING.isSameOrLessThan(message.getKind())) {
                logger.warn(message.getMessage())
            } else if (IMessage.DEBUG.isSameOrLessThan(message.getKind())) {
                logger.info(message.getMessage())
            } else {
                logger.debug(message.getMessage())
            }
        }
    }

    @NonNull
    @Override
    public String getName() {
        "aspectJ"
    }

    @NonNull
    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        COTNENT_CLASS
    }

    @NonNull
    @Override
    public Set<QualifiedContent.Scope> getScopes() {
        SCOPE_FULL_PROJECT
    }

    @Override
    public boolean isIncremental() {
        // can't be incremental
        // because java bytecode and aspect bytecode are woven across each other
        false
    }

    protected boolean isFileExcluded(File file) {
        for (ExcludeRule rule : project.aspectj.excludeRules) {
            if (file.absolutePath.contains(Joiner.on(File.separator).join([rule.group, rule.module]))) {
                return true
            }
        }
        return false
    }

}