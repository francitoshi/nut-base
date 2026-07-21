/*
 * Copyright (C) 2026 francitoshi@gmail.com
 * SPDX-License-Identifier: GPL-3.0-or-later
 * See LICENSE file in the project root for full license text.
 */
package io.nut.base.io;

import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Recursively walks a file/directory tree starting from one or more root paths.
 * <p>
 * It can be used in two ways:
 * <ul>
 * <li>By extending the class and implementing
 * {@link #process(Path, BasicFileAttributes)}.</li>
 * <li>By using the static factories {@link #of(FileConsumer)} /
 *       {@link #of(FileConsumer, BiPredicate)} to pass a lambda instead of creating a
 * subclass.</li>
 * </ul>
 * <p>
 * Walk options (only files, only directories, skip symbolic links, follow
 * symbolic links, skip hidden entries, max depth) are supplied once, at
 * construction time, either as an {@link Options} snapshot, as an
 * {@link EnumSet} of {@link Option} flags plus a max depth, or as a plain
 * varargs list of {@link Option} flags (unlimited depth). There is no way to
 * change them afterwards: a {@code PathWalker} instance is immutable for its
 * entire lifetime, so it can safely be shared and reused across threads and
 * across multiple {@link #walk(Path...)} calls without any synchronization.
 * <p>
 * When walking multiple root paths, exact duplicates and any path that is
 * already contained within another supplied root are automatically excluded, so
 * each file is only ever visited once even if the caller passes overlapping
 * paths.
 * <p>
 * Symbolic links are not followed by default. When {@link Option#FollowSymlinks}
 * is enabled, the walker still guarantees each real file/directory is processed
 * only once per {@code walk} call: every visited entry is tracked by its real
 * identity (file key, or resolved real path as a fallback), so two different
 * symlinked paths that resolve to the same target are not processed twice, and
 * directory cycles created through symlinks are simply skipped instead of
 * looping forever.
 */
public abstract class PathWalker 
{
    /** Boolean-style walk options, grouped as flags in an {@link EnumSet}. */
    public enum Option
    {
        /** Only pass files (not directories) to {@link #process(Path, BasicFileAttributes)}. */
        OnlyFiles,
        /** Only pass directories (not files) to {@link #process(Path, BasicFileAttributes)}. Mutually exclusive with {@link #OnlyFiles}. */
        OnlyDirectories,
        /** Exclude symbolic links from {@link #process(Path, BasicFileAttributes)}, whether or not they are followed. */
        SkipSymlinks,
        /** Follow symbolic links while recursing into directories, instead of treating them as opaque leaf entries. */
        FollowSymlinks,
        /** Exclude hidden files and directories (per {@link Files#isHidden(Path)}) from the walk. */
        SkipHidden
    }
    /**
     * Immutable snapshot of the walk options, created once for a
     * {@link PathWalker} and never changed for its entire lifetime.
     */
    public static final class Options
    {
        private final EnumSet<Option> options;
        private final int maxDepth;

        public Options(EnumSet<Option> set, int maxDepth)
        {
            Objects.requireNonNull(set, "set");
            if (set.contains(Option.OnlyFiles) && set.contains(Option.OnlyDirectories))
            {
                throw new IllegalStateException("OnlyFiles and OnlyDirectories are mutually exclusive");
            }
            if (maxDepth < 0)
            {
                throw new IllegalArgumentException("maxDepth must be >= 0");
            }
            // Defensive copy: an immutable snapshot must not change even if the
            // caller keeps mutating the EnumSet it passed in.
            this.options = EnumSet.copyOf(set);
            this.maxDepth = maxDepth;
        }

        public boolean isOnlyFiles()
        {
            return options.contains(Option.OnlyFiles);
        }

        public boolean isOnlyDirectories()
        {
            return options.contains(Option.OnlyDirectories);
        }

        public boolean isSkipSymlinks()
        {
            return options.contains(Option.SkipSymlinks);
        }

        public boolean isFollowSymlinks()
        {
            return options.contains(Option.FollowSymlinks);
        }

        public boolean isSkipHidden()
        {
            return options.contains(Option.SkipHidden);
        }

        public int getMaxDepth()
        {
            return maxDepth;
        }

        @Override
        public String toString()
        {
            return "Options{options=" + options + ", maxDepth=" + maxDepth + '}';
        }
    }

    private final Options options;

    /**
     * Creates a walker from an already-built {@link Options} snapshot, typically
     * one obtained from {@link #getOptions()} on another {@code PathWalker}, so
     * several walkers can share the exact same configuration.
     */
    public PathWalker(Options options)
    {
        this.options = Objects.requireNonNull(options, "options");
    }

    /**
     * Creates a walker configured from the given flags and max depth.
     */
    public PathWalker(EnumSet<Option> opt, int maxDepth)
    {
        this.options = new Options(Objects.requireNonNull(opt, "opt"), maxDepth);
    }

    /**
     * Creates a walker configured from the given flags, with unlimited depth.
     */
    public PathWalker(Option... opt)
    {
        Objects.requireNonNull(opt, "opt");
        EnumSet<Option> set = EnumSet.noneOf(Option.class);
        for(Option item : opt)
        {
            set.add(Objects.requireNonNull(item, "opt element"));
        }
        this.options = new Options(set, Integer.MAX_VALUE);
    }

    /** Returns the immutable options this walker was configured with. */
    public Options getOptions()
    {
        return options;
    }

    // ---------------------------------------------------------------
    // Extension point: additional overridable filter
    // ---------------------------------------------------------------
    /**
     * Additional filter applied to every candidate file/directory (after the
     * onlyFiles/onlyDirectories/skipHidden/skipSymlinks options have been
     * applied). Accepts everything by default. Override to add custom logic
     * (extension, size, name pattern, etc.).
     */
    protected boolean filter(Path path, BasicFileAttributes attrs)
    {
        return true;
    }

    // ---------------------------------------------------------------
    // Extension point: consumption of each visited entry
    // ---------------------------------------------------------------
    /**
     * Invoked for every file or directory that passes the options and the
     * filter.
     */
    protected abstract void process(Path path, BasicFileAttributes attrs) throws IOException;

    /**
     * Invoked when a file/directory cannot be visited (e.g. due to
     * permissions). Ignored and the walk continues by default; override for
     * different behavior (logging, aborting, etc.).
     */
    protected FileVisitResult onVisitFailed(Path path, IOException exc) throws IOException
    {
        return FileVisitResult.CONTINUE;
    }

    // ---------------------------------------------------------------
    // Launching the walk
    // ---------------------------------------------------------------
    /**
     * Launches the recursive walk starting from a single root path. Convenience
     * overload of {@link #walk(Path...)}.
     */
    public final void walk(Path start) throws IOException
    {
        walk(new Path[] { start });
    }

    /**
     * Launches the recursive walk starting from several root paths.
     * <p>
     * Exact duplicates and roots that are already nested inside another
     * supplied root are automatically discarded before walking, so no file is
     * visited twice. At this point the options are frozen (if they weren't
     * already).
     */
    public final void walk(Path... starts) throws IOException
    {
        walk(Arrays.asList(starts));
    }

    /**
     * Launches the recursive walk starting from a collection of root paths. See
     * {@link #walk(Path...)} for details on duplicate/nested-path handling.
     */
    public final void walk(Collection<Path> starts) throws IOException
    {
        Objects.requireNonNull(starts, "starts");
        if (starts.isEmpty())
        {
            throw new IllegalArgumentException("At least one start path is required");
        }
        for (Path p : starts)
        {
            Objects.requireNonNull(p, "start path cannot be null");
        }

        List<Path> roots = deduplicateAndRemoveNested(starts);

        // FOLLOW_LINKS is only enabled when explicitly requested; the JDK itself then
        // detects same-branch cycles (a link pointing back to one of its own ancestors)
        // and reports them as FileSystemLoopException through visitFileFailed.
        EnumSet<FileVisitOption> visitOptions = options.isFollowSymlinks()
                ? EnumSet.of(FileVisitOption.FOLLOW_LINKS)
                : EnumSet.noneOf(FileVisitOption.class);

        // Shared across every root in this walk() call: tracks the real identity
        // (file key, or resolved real path as a fallback) of every entry already
        // processed while following symlinks, so the same real file/directory is
        // never visited twice even when reached through different symlinked paths.
        // Only allocated when actually needed, since it costs nothing to skip when
        // followSymlinks() is disabled (the default).
        final Set<Object> visitedRealPaths = options.isFollowSymlinks() ? new HashSet<>() : null;

        for (Path root : roots)
        {
            Files.walkFileTree(root, visitOptions, options.getMaxDepth(), new SimpleFileVisitor<Path>()
            {

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
                {
                    // Real-identity tracking is only meaningful when links are followed:
                    // without FOLLOW_LINKS, a symlink is never traversed as a directory,
                    // so no branch can reach the same real directory twice, and the
                    // root-level containment cleanup already rules out nested roots.
                    if (options.isFollowSymlinks() && !visitedRealPaths.add(realIdentity(dir, attrs)))
                    {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (options.isSkipHidden() && !dir.equals(root) && isHidden(dir))
                    {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    if (!options.isOnlyFiles() && filter(dir, attrs))
                    {
                        process(dir, attrs);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
                {
                    boolean isSymlink = attrs.isSymbolicLink();
                    if (options.isSkipSymlinks() && isSymlink)
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    // Same reasoning as preVisitDirectory: only worth the fileKey()/
                    // toRealPath() cost when links are actually being followed.
                    if (options.isFollowSymlinks() && !visitedRealPaths.add(realIdentity(file, attrs)))
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    if (options.isSkipHidden() && isHidden(file))
                    {
                        return FileVisitResult.CONTINUE;
                    }
                    if (!options.isOnlyDirectories() && filter(file, attrs))
                    {
                        process(file, attrs);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException
                {
                    return onVisitFailed(file, exc);
                }

                private boolean isHidden(Path path)
                {
                    try
                    {
                        return Files.isHidden(path);
                    }
                    catch (IOException e)
                    {
                        // If it cannot be determined, do not treat it as hidden.
                        return false;
                    }
                }
            });
        }
    }

    /**
     * Returns a value that uniquely identifies the real file/directory a path
     * resolves to, used to detect the same target reached through different
     * (possibly symlinked) paths.
     * <p>
     * Prefers {@link BasicFileAttributes#fileKey()} (typically device+inode on
     * POSIX systems), which is cheap and available without extra I/O. Falls
     * back to resolving the real path (following all links) when no file key is
     * available, e.g. on file systems or platforms that don't support it.
     */
    private static Object realIdentity(Path path, BasicFileAttributes attrs) throws IOException
    {
        Object key = attrs.fileKey();
        if (key != null)
        {
            return key;
        }
        return path.toRealPath();
    }

    /**
     * Normalizes the given paths to their absolute, normalized form, removes
     * exact duplicates, and discards any path that is already contained within
     * another path in the collection (i.e. keeps only the "top-level" roots).
     */
    private static List<Path> deduplicateAndRemoveNested(Collection<Path> starts)
    {
        // Normalize to absolute paths for reliable containment comparisons.
        List<Path> normalized = new ArrayList<>();
        for (Path p : starts)
        {
            Path abs = p.toAbsolutePath().normalize();
            if (!normalized.contains(abs))
            {
                normalized.add(abs);
            }
        }

        // Sort by path depth (fewer components first) so ancestors are evaluated
        // before their potential descendants.
        normalized.sort(Comparator.comparingInt(Path::getNameCount));

        List<Path> roots = new ArrayList<>();
        for (Path candidate : normalized)
        {
            boolean containedInExistingRoot = false;
            for (Path existingRoot : roots)
            {
                if (candidate.startsWith(existingRoot))
                {
                    containedInExistingRoot = true;
                    break;
                }
            }
            if (!containedInExistingRoot)
            {
                roots.add(candidate);
            }
        }
        return roots;
    }

    // ---------------------------------------------------------------
    // Lambda support (no need to create a subclass)
    // ---------------------------------------------------------------
    @FunctionalInterface
    public interface FileConsumer
    {
        void accept(Path path, BasicFileAttributes attrs) throws IOException;
    }

    /**
     * Creates a walker from a lambda/consumer, with no additional filter.
     */
    public static PathWalker of(FileConsumer consumer)
    {
        Objects.requireNonNull(consumer, "consumer");
        return new PathWalker()
        {
            @Override
            protected void process(Path path, BasicFileAttributes attrs) throws IOException
            {
                consumer.accept(path, attrs);
            }
        };
    }

    /**
     * Creates a walker from a lambda/consumer plus a lambda filter.
     */
    public static PathWalker of(FileConsumer consumer, BiPredicate<Path, BasicFileAttributes> filter)
    {
        Objects.requireNonNull(consumer, "consumer");
        Objects.requireNonNull(filter, "filter");
        return new PathWalker()
        {
            @Override
            protected void process(Path path, BasicFileAttributes attrs) throws IOException
            {
                consumer.accept(path, attrs);
            }

            @Override
            protected boolean filter(Path path, BasicFileAttributes attrs)
            {
                return filter.test(path, attrs);
            }
        };
    }
}
