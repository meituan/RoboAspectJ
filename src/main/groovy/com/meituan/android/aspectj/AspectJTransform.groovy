package com.meituan.android.aspectj

import com.android.annotations.NonNull
import com.android.build.transform.api.*
import com.google.common.base.Joiner
import com.google.common.base.Strings
import com.google.common.collect.*
import org.apache.commons.io.FileUtils
import org.aspectj.bridge.IMessage
import org.aspectj.bridge.MessageHandler
import org.aspectj.tools.ajc.Main
import org.gradle.api.Project
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.logging.Logger

import static com.android.build.transform.api.ScopedContent.ContentType.CLASSES

/**
 * <p>The main work of this {@link com.android.build.transform.api.Transform} implementation is to
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
public class AspectJTransform extends Transform implements CombinedTransform {
    public static final Set<ScopedContent.Scope> SCOPE_EMPTY = ImmutableSet.of()

    public static final Set<ScopedContent.Scope> SCOPE_FULL_PROJECT = Sets.immutableEnumSet(
            ScopedContent.Scope.PROJECT,
            ScopedContent.Scope.PROJECT_LOCAL_DEPS,
            ScopedContent.Scope.SUB_PROJECTS,
            ScopedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
            ScopedContent.Scope.EXTERNAL_LIBRARIES)

    private Project project
    private ImmutableSet<String> excludedFilePaths


    public AspectJTransform(Project project) {
        this.project = project
    }

    @Override
    public void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutput output, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        // resolve libs to be excluded
        ImmutableSet.Builder<String> builder = new ImmutableSet.Builder()
        String javaRtPath
        project.android.applicationVariants.all {
            String javaRt = Joiner.on(File.separator).join(['jre', 'lib', 'rt.jar'])
            for (String classpath : javaCompiler.classpath.asPath.split(File.pathSeparator)) {
                if (classpath.contains(javaRt)) {
                    javaRtPath = classpath
                }
            }

            variantData.variantConfiguration.allPackagedJars.each {
                for (ExcludeRule rule : project.aspectj.excludeRules) {
                    String excludeLib = Joiner.on(File.separator).join([rule.group, rule.module])
                    if (it.absolutePath.contains(excludeLib)) {
                        builder.add(it.name + '-' + it.path.hashCode())
                    }
                }
            }
        }
        excludedFilePaths = builder.build();

        // categorize bytecode files
        List<File> files = Lists.newArrayList();
        List<File> excludedFiles = Lists.newArrayList();
        for (TransformInput input : inputs) {
            switch (input.getFormat()) {
                case ScopedContent.Format.JAR:
                case ScopedContent.Format.SINGLE_FOLDER:
                    for (File file : input.files) {
                        if (isFileExcluded(file)) {
                            excludedFiles.add(file)
                        } else {
                            files.add(file)

                        }
                    }
                    break;
                case ScopedContent.Format.MULTI_FOLDER:
                    for (File file : input.files) {
                        File[] children = file.listFiles();
                        if (children != null) {
                            for (File child : children) {
                                if (isFileExcluded(file)) {
                                    excludedFiles.add(file)
                                } else {
                                    files.add(file)
                                }
                            }
                        }
                    }
                    break;
                default:
                    throw new RuntimeException("Unsupported ScopedContent.Format value: " + input.format.name);
            }
        }

        // copy excluded files for other transforms' usage later
        for (File file : excludedFiles) {
            FileUtils.copyDirectory(file, output.outFile);
        }

        //evaluate class paths
        final String inpath = Joiner.on(File.pathSeparator).join(files)
        final String classpath = Joiner.on(File.pathSeparator).join(project.aspectj.javartNeeded ? [*excludedFiles, javaRtPath] : excludedFiles)
        final String bootpath = Joiner.on(File.pathSeparator).join(project.android.bootClasspath)

        // assemble compile options
        def args = [
                "-source", project.aspectj.compileOptions.sourceCompatibility.name,
                "-target", project.aspectj.compileOptions.targetCompatibility.name,
                "-showWeaveInfo",
                "-encoding", project.aspectj.compileOptions.encoding,
                "-inpath", inpath,
                "-d", output.outFile.absolutePath,
                "-bootclasspath", bootpath];

        // append classpath argument if any
        if (!Strings.isNullOrEmpty(classpath)) {
            args << '-classpath'
            args << classpath
        }

        // run compilation
        MessageHandler handler = new MessageHandler(true);
        new Main().run(args as String[], handler);

        // log compile
        Logger logger = project.getLogger();
        for (IMessage message : handler.getMessages(null, true)) {
            // level up weave info log for debug
//            logger.quiet(message.getMessage());
            if (IMessage.ERROR.isSameOrLessThan(message.getKind())) {
                if (null != message.getThrown()) {
                    logger.error(message.getMessage(), message.getThrown());
                } else {
                    logger.error(message.getMessage());
                }
            } else if (IMessage.WARNING.isSameOrLessThan(message.getKind())) {
                logger.warn(message.getMessage());
            } else if (IMessage.DEBUG.isSameOrLessThan(message.getKind())) {
                logger.debug(message.getMessage());
            } else {
                logger.info(message.getMessage());
            }
        }
    }

    @NonNull
    @Override
    public String getName() {
        return "aspectJ"
    }

    @NonNull
    @Override
    public Set<ScopedContent.ContentType> getInputTypes() {
        return Sets.immutableEnumSet(CLASSES)
    }

    @NonNull
    @Override
    public Set<ScopedContent.ContentType> getOutputTypes() {
        return Sets.immutableEnumSet(CLASSES)
    }

    @NonNull
    @Override
    public Set<ScopedContent.Scope> getScopes() {
        return SCOPE_FULL_PROJECT
    }

    @NonNull
    @Override
    public ScopedContent.Format getOutputFormat() {
        return ScopedContent.Format.SINGLE_FOLDER
    }

    @NonNull
    @Override
    public Set<ScopedContent.Scope> getReferencedScopes() {
        return SCOPE_EMPTY
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileInputs() {
        return ImmutableList.of()
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileOutputs() {
        return ImmutableList.of()
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFolderOutputs() {
        return ImmutableList.of()
    }

    @NonNull
    @Override
    public Map<String, Object> getParameterInputs() {
        return ImmutableMap.of()
    }

    @Override
    public boolean isIncremental() {
        // can't be incremental
        // because java bytecode and aspect bytecode are woven across each other
        return false
    }

    protected boolean isFileExcluded(File file) {
        return excludedFilePaths.contains(file.name)
    }
}
