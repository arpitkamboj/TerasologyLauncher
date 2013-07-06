/*
 * Copyright (c) 2013 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.launcher.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.launcher.gui.SplashScreenWindow;
import org.terasology.launcher.util.DirectoryUtils;
import org.terasology.launcher.util.FileUtils;
import org.terasology.launcher.util.OperatingSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The SelfUpdater class is responsible for copying the updated files to the right location.
 * <p/>
 * The update method will prepare a new process to run that will copy the files and restart the launcher.
 */
public final class SelfUpdater {

    private static final Logger logger = LoggerFactory.getLogger(SelfUpdater.class);

    private SelfUpdater() {
    }

    /**
     * Starts the update process after downloading the needed files.
     */
    public static void runUpdate(final SplashScreenWindow splash, final OperatingSystem os,
                                 final File downloadDirectory, final File launcherDirectory) {
        final String separator = File.separator;
        final String javaBin = System.getProperty("java.home") + separator + "bin" + separator + "java";
        final File tempLauncherDirectory = new File(downloadDirectory, "TerasologyLauncher");
        final File classpath = new File(tempLauncherDirectory, "lib");

        final List<String> arguments = new ArrayList<>();
        // Set 'java' executable as programme to run
        arguments.add(javaBin);
        // Build and set the classpath
        arguments.add("-cp");
        if (os.isWindows()) {
            arguments.add("\"" + classpath.getPath() + separator + "*" + "\"");
        } else {
            final StringBuilder classpathBuilder = new StringBuilder();
            final File[] files = classpath.listFiles();
            if ((files != null) && (files.length > 0)) {
                for (File f : files) {
                    classpathBuilder.append(f.toURI()).append(File.pathSeparator);
                }
            }
            classpathBuilder.deleteCharAt(classpathBuilder.length() - 1);
            arguments.add(classpathBuilder.toString());
        }
        // Specify class with main method to run
        arguments.add(SelfUpdater.class.getCanonicalName());
        // Arguments for update locations
        arguments.add(launcherDirectory.getPath());
        arguments.add(tempLauncherDirectory.getPath());

        logger.info("Running launcher self update with: {}", arguments);
        logger.info("Current launcher path: {}", launcherDirectory.getPath());
        logger.info("New files temporarily located in: {}", tempLauncherDirectory.getPath());

        final ProcessBuilder pb = new ProcessBuilder();
        pb.command(arguments);

        try {
            pb.start();
        } catch (IOException e) {
            logger.error("Failed to run self update process!", e);
        }
        System.exit(0);
    }

    public static void main(final String[] args) {
        logger.info("Running self updater.");

        final String launcherDirectoryArg = args[0];
        final String tempLauncherDirectoryArg = args[1];
        final File launcherDirectory = new File(launcherDirectoryArg);
        final File tempLauncherDirectory = new File(tempLauncherDirectoryArg);

        try {
            // Check both directories
            DirectoryUtils.checkDirectory(launcherDirectory);
            DirectoryUtils.checkDirectory(tempLauncherDirectory);

            logger.info("Delete launcher directory: {}", launcherDirectory);
            FileUtils.delete(launcherDirectory);

            logger.info("Copy new files: {}", tempLauncherDirectory);
            FileUtils.copyFolder(tempLauncherDirectory, launcherDirectory);
        } catch (IOException e) {
            logger.error("Auto updating the launcher failed!", e);
            System.exit(1);
        }

        // Start new launcher
        final String separator = System.getProperty("file.separator");
        final String javaPath = System.getProperty("java.home") + separator + "bin" + separator + "java";

        final List<String> arguments = new ArrayList<>();
        arguments.add(javaPath);
        arguments.add("-jar");
        arguments.add(launcherDirectory.getPath() + separator + "lib" + separator + "TerasologyLauncher.jar");

        final ProcessBuilder pb = new ProcessBuilder();
        pb.command(arguments);

        logger.info("Start new launcher: {}", arguments);
        try {
            pb.start();
        } catch (IOException e) {
            logger.error("Failed to restart launcher process after update.", e);
            System.exit(1);
        }
        System.exit(0);
    }
}
