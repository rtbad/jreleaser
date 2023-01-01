/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2023 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.assemblers;

import org.jreleaser.bundle.RB;
import org.jreleaser.model.Constants;
import org.jreleaser.model.internal.JReleaserContext;
import org.jreleaser.model.internal.assemble.Assembler;
import org.jreleaser.model.internal.common.FileSet;
import org.jreleaser.model.spi.assemble.AssemblerProcessingException;
import org.jreleaser.model.spi.assemble.AssemblerProcessor;
import org.jreleaser.sdk.command.Command;
import org.jreleaser.sdk.command.CommandException;
import org.jreleaser.sdk.command.CommandExecutor;
import org.jreleaser.util.FileUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.jreleaser.mustache.MustacheUtils.applyTemplates;
import static org.jreleaser.util.FileUtils.createDirectoriesWithFullAccess;
import static org.jreleaser.util.FileUtils.grantFullAccess;
import static org.jreleaser.util.PlatformUtils.isWindows;
import static org.jreleaser.util.StringUtils.isNotBlank;
import static org.jreleaser.util.StringUtils.quote;

/**
 * @author Andres Almiray
 * @since 0.2.0
 */
public abstract class AbstractAssemblerProcessor<A extends org.jreleaser.model.api.assemble.Assembler, S extends Assembler<A>> implements AssemblerProcessor<A, S> {
    protected final JReleaserContext context;
    protected S assembler;

    protected AbstractAssemblerProcessor(JReleaserContext context) {
        this.context = context;
    }

    @Override
    public S getAssembler() {
        return assembler;
    }

    @Override
    public void setAssembler(S assembler) {
        this.assembler = assembler;
    }

    @Override
    public void assemble(Map<String, Object> props) throws AssemblerProcessingException {
        try {
            context.getLogger().debug(RB.$("packager.create.properties"), assembler.getType(), assembler.getName());
            Map<String, Object> newProps = fillProps(props);

            Path assembleDirectory = (Path) props.get(Constants.KEY_DISTRIBUTION_ASSEMBLE_DIRECTORY);
            Files.createDirectories(assembleDirectory);

            doAssemble(newProps);
        } catch (IllegalArgumentException | IOException e) {
            throw new AssemblerProcessingException(e);
        }
    }

    protected abstract void doAssemble(Map<String, Object> props) throws AssemblerProcessingException;

    protected void writeFile(String content, Path outputFile) throws AssemblerProcessingException {
        try {
            createDirectoriesWithFullAccess(outputFile.getParent());
            Files.write(outputFile, content.getBytes(UTF_8), CREATE, WRITE, TRUNCATE_EXISTING);
            grantFullAccess(outputFile);
        } catch (IOException e) {
            throw new AssemblerProcessingException(RB.$("ERROR_unexpected_error_writing_file", outputFile.toAbsolutePath()), e);
        }
    }

    protected void writeFile(byte[] content, Path outputFile) throws AssemblerProcessingException {
        try {
            createDirectoriesWithFullAccess(outputFile.getParent());
            Files.write(outputFile, content, CREATE, WRITE, TRUNCATE_EXISTING);
            grantFullAccess(outputFile);
        } catch (IOException e) {
            throw new AssemblerProcessingException(RB.$("ERROR_unexpected_error_writing_file", outputFile.toAbsolutePath()), e);
        }
    }

    protected Map<String, Object> fillProps(Map<String, Object> props) {
        Map<String, Object> newProps = new LinkedHashMap<>(props);
        context.getLogger().debug(RB.$("packager.fill.git.properties"));
        context.getModel().getRelease().getReleaser().fillProps(newProps, context.getModel());
        context.getLogger().debug(RB.$("assembler.fill.assembler.properties"));
        fillAssemblerProperties(newProps);
        applyTemplates(props, props);
        return newProps;
    }

    protected void fillAssemblerProperties(Map<String, Object> props) {
        props.putAll(assembler.props());
    }

    protected void executeCommand(Path directory, Command command) throws AssemblerProcessingException {
        try {
            int exitValue = new CommandExecutor(context.getLogger())
                .executeCommand(directory, command);
            if (exitValue != 0) {
                throw new CommandException(RB.$("ERROR_command_execution_exit_value", exitValue));
            }
        } catch (CommandException e) {
            throw new AssemblerProcessingException(RB.$("ERROR_unexpected_error"), e);
        }
    }

    protected void executeCommand(Command command) throws AssemblerProcessingException {
        try {
            int exitValue = new CommandExecutor(context.getLogger())
                .executeCommand(command);
            if (exitValue != 0) {
                throw new CommandException(RB.$("ERROR_command_execution_exit_value", exitValue));
            }
        } catch (CommandException e) {
            throw new AssemblerProcessingException(RB.$("ERROR_unexpected_error"), e);
        }
    }

    protected void executeCommandCapturing(Command command, OutputStream out) throws AssemblerProcessingException {
        try {
            int exitValue = new CommandExecutor(context.getLogger())
                .executeCommandCapturing(command, out);
            if (exitValue != 0) {
                context.getLogger().error(out.toString().trim());
                throw new CommandException(RB.$("ERROR_command_execution_exit_value", exitValue));
            }
        } catch (CommandException e) {
            throw new AssemblerProcessingException(RB.$("ERROR_unexpected_error"), e);
        }
    }

    protected void executeCommandCapturing(Path directory, Command command, OutputStream out) throws AssemblerProcessingException {
        try {
            int exitValue = new CommandExecutor(context.getLogger())
                .executeCommandCapturing(directory, command, out);
            if (exitValue != 0) {
                context.getLogger().error(out.toString().trim());
                throw new CommandException(RB.$("ERROR_command_execution_exit_value", exitValue));
            }
        } catch (CommandException e) {
            throw new AssemblerProcessingException(RB.$("ERROR_unexpected_error"), e);
        }
    }

    protected void copyFileSets(JReleaserContext context, Path destination) throws AssemblerProcessingException {
        try {
            for (FileSet fileSet : assembler.getFileSets()) {
                Path src = context.getBasedir().resolve(fileSet.getResolvedInput(context));
                Path dest = destination;

                String output = fileSet.getResolvedOutput(context);
                if (isNotBlank(output)) {
                    dest = destination.resolve(output);
                }

                Set<Path> paths = fileSet.getResolvedPaths(context);
                FileUtils.copyFiles(context.getLogger(), src, dest, paths);
            }
        } catch (IOException e) {
            throw new AssemblerProcessingException(RB.$("ERROR_assembler_copying_files"), e);
        }
    }

    protected String maybeQuote(String str) {
        return isWindows() ? quote(str) : str;
    }
}
