package com.meituan.android.aspectj;

import com.android.annotations.NonNull;
import com.android.build.gradle.AppExtension;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.android.build.transform.api.CombinedTransform;
import com.android.build.transform.api.Context;
import com.android.build.transform.api.ScopedContent;
import com.android.build.transform.api.Transform;
import com.android.build.transform.api.TransformException;
import com.android.build.transform.api.TransformInput;
import com.android.build.transform.api.TransformOutput;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import org.aspectj.bridge.IMessage;
import org.aspectj.bridge.MessageHandler;
import org.aspectj.tools.ajc.Main;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.android.build.transform.api.ScopedContent.ContentType.CLASSES;

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
    public static final Set<ScopedContent.Scope> SCOPE_EMPTY = ImmutableSet.of();

    public static final Set<ScopedContent.Scope> SCOPE_FULL_PROJECT = Sets.immutableEnumSet(
            ScopedContent.Scope.PROJECT,
            ScopedContent.Scope.PROJECT_LOCAL_DEPS,
            ScopedContent.Scope.SUB_PROJECTS,
            ScopedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,
            ScopedContent.Scope.EXTERNAL_LIBRARIES);

    private Project project;

    public AspectJTransform(Project project) {
        this.project = project;
    }

    @Override
    public void transform(Context context, Collection<TransformInput> inputs, Collection<TransformInput> referencedInputs, TransformOutput output, boolean isIncremental) throws IOException, TransformException, InterruptedException {
        // calculate bytecode paths
        List<File> files = Lists.newArrayList();
        for (TransformInput input : inputs) {
            switch (input.getFormat()) {
                case JAR:
                case SINGLE_FOLDER:
                    for (File file : input.getFiles()) {
                        files.add(file);
                    }
                    break;
                case MULTI_FOLDER:
                    for (File file : input.getFiles()) {
                        File[] children = file.listFiles();
                        if (children != null) {
                            for (File child : children) {
                                files.add(child);
                            }
                        }
                    }
                    break;
                default:
                    throw new RuntimeException("Unsupported ScopedContent.Format value: " + input.getFormat().name());
            }
        }
        final String inpath = Joiner.on(File.pathSeparator).join(files);

        // calculate bootclass path
        List<File> bootFiles = ((AppExtension)project.getExtensions().getByName("android")).getBootClasspath();
        final String bootpath = Joiner.on(File.pathSeparator).join(bootFiles);

        // assemble compile options
        String[] args = {
                "-1.7",
                "-showWeaveInfo",
                "-encoding", "UTF-8",
                "-inpath", inpath,
                "-d", output.getOutFile().getAbsolutePath(),
                "-bootclasspath", bootpath};

        // run compile
        MessageHandler handler = new MessageHandler(true);
        new Main().run(args, handler);

        // print compile msg
        Logger logger = project.getLogger();
        for (IMessage message : handler.getMessages(null, true)) {
            // verbose case for debug
//            System.out.println(message.getMessage());
            if (IMessage.ERROR.isSameOrLessThan(message.getKind())) {
                if (null != message.getThrown()) {
                    logger.error(message.getMessage(), message.getThrown());
                } else {
                    logger.error(message.getMessage());
                }
            } else if (IMessage.WARNING.isSameOrLessThan(message.getKind())) {
                logger.warn(message.getMessage());
            } else if (IMessage.DEBUG.isSameOrLessThan(message.getKind())){
                logger.debug(message.getMessage());
            } else {
                // level up weave info log for debug
//                logger.quiet(message.getMessage());
                logger.info(message.getMessage());
            }
        }
    }

    @NonNull
    @Override
    public String getName() {
        return "aspectJ";
    }

    @NonNull
    @Override
    public Set<ScopedContent.ContentType> getInputTypes() {
        return Sets.immutableEnumSet(CLASSES);
    }

    @NonNull
    @Override
    public Set<ScopedContent.ContentType> getOutputTypes() {
        return Sets.immutableEnumSet(CLASSES);
    }

    @NonNull
    @Override
    public Set<ScopedContent.Scope> getScopes() {
        return SCOPE_FULL_PROJECT;
    }

    @NonNull
    @Override
    public ScopedContent.Format getOutputFormat() {
        return ScopedContent.Format.SINGLE_FOLDER;
    }

    @NonNull
    @Override
    public Set<ScopedContent.Scope> getReferencedScopes() {
        return SCOPE_EMPTY;
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileInputs() {
        return ImmutableList.of();
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFileOutputs() {
        return ImmutableList.of();
    }

    @NonNull
    @Override
    public Collection<File> getSecondaryFolderOutputs() {
        return ImmutableList.of();
    }

    @NonNull
    @Override
    public Map<String, Object> getParameterInputs() {
        return ImmutableMap.of();
    }

    @Override
    public boolean isIncremental() {
        // can't be incremental
        // because java bytecode and aspect bytecode are woven across each other
        // (actually these bytecodes are the same, cz they're both written in java literally)
        return false;
    }
}
