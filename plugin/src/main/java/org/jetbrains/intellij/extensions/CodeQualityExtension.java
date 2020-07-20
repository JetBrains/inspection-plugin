package org.jetbrains.intellij.extensions;

import org.gradle.api.tasks.*;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.Collection;

@SuppressWarnings("unused")
public class CodeQualityExtension {

    /**
     * Whether or not this task will ignore failures and continue running the build.
     */
    private Boolean ignoreFailures;

    /**
     * The source sets to be analyzed as part of the <tt>check</tt> and <tt>build</tt> tasks.
     */
    private Collection<SourceSet> sourceSets;

    /**
     * The directory where reports will be generated.
     */
    private File reportsDir;

    public Boolean isIgnoreFailures() {
        return ignoreFailures;
    }

    @Input
    @Optional
    public Boolean getIgnoreFailures() {
        return ignoreFailures;
    }

    public void setIgnoreFailures(Boolean ignoreFailures) {
        this.ignoreFailures = ignoreFailures;
    }

    @NotNull
    @Internal
    public Collection<SourceSet> getSourceSets() {
        return sourceSets;
    }

    public void setSourceSets(@NotNull Collection<SourceSet> sourceSets) {
        this.sourceSets = sourceSets;
    }

    @OutputDirectory
    public File getReportsDir() {
        return reportsDir;
    }

    public void setReportsDir(@NotNull File reportsDir) {
        this.reportsDir = reportsDir;
    }
}