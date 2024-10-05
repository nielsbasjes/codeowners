/*
 * CodeOwners Tools
 * Copyright (C) 2023-2024 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an AS IS BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.basjes.gitignore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.emptyList;

public final class Utils {
    private Utils() {
    }

    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    /**
     * Automatically find all non-ignored directories and files starting in the projects root.
     *
     * @return List all non-ignored directories and files.
     */
    public static List<Path> findAllNonIgnored(GitIgnoreFileSet gitIgnoreFileSet) {
        return findAllNonIgnored(gitIgnoreFileSet, gitIgnoreFileSet.getProjectBaseDir().toPath());
    }

    public static List<Path> findAllNonIgnored(GitIgnoreFileSet gitIgnoreFileSet, Path baseDir) {
        return findAllNonIgnored(gitIgnoreFileSet, baseDir, 128);
    }

    /**
     * Recursively find all directories and files that are not ignored.
     * @param current The current directory
     * @param maxRecursionDepth A limiter to avoid going infinitely deep.
     * @return List of the found Paths.
     */
    private static List<Path> findAllNonIgnored(GitIgnoreFileSet gitIgnoreFileSet, Path current, int maxRecursionDepth) {
        List<Path> found = new ArrayList<>();
        List<Path> subDirs = new ArrayList<>();

        if (!Files.isDirectory(current)) {
            LOG.debug("Locate GI: Not DIR  {}", current);
            return emptyList(); // It must be a directory
        }

        String dirPath = current.toFile().getPath();
        if (!dirPath.endsWith(File.separator)) {
            dirPath = dirPath + File.separatorChar;
        }

        if (gitIgnoreFileSet.ignoreFile(dirPath)) {
            LOG.debug("Locate GI: Ignored  {}", current);
            return emptyList(); // Is ignored
        }

        LOG.debug("Locate GI: Scan     {}", current);

        found.add(current);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(current)) {
            for (Path path : stream) {
                if (gitIgnoreFileSet.keepFile(path.toString())) {
                    if (Files.isDirectory(path)) {
                        subDirs.add(path);
                    } else {
                        found.add(path);
                    }
                }
            }
        }
        catch (IOException e) {
            LOG.error("Unable to list the content of {} due to {}", current, e.toString());
            return emptyList();
        }

        int nextMaxRecursionDepth = maxRecursionDepth-1;
        if (nextMaxRecursionDepth > 0) {
            for (Path subDir : subDirs) {
                found.addAll(findAllNonIgnored(gitIgnoreFileSet, subDir, nextMaxRecursionDepth));
            }
        }
        Collections.sort(found);
        return found;
    }


}
