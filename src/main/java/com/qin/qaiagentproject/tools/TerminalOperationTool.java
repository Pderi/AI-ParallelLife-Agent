package com.qin.qaiagentproject.tools;

import cn.hutool.core.util.RuntimeUtil;
import com.qin.qaiagentproject.config.TerminalToolProperties;
import com.qin.qaiagentproject.exception.BusinessException;
import com.qin.qaiagentproject.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
public class TerminalOperationTool {

    private static final String DANGEROUS_CHARS_REGEX = ".*[&|;><`$()].*";

    private final TerminalToolProperties terminalToolProperties;

    @Tool(description = "Execute a command in the terminal")
    public String executeTerminalCommand(@ToolParam(description = "Command to execute in the terminal") String command) {
        validateToolEnabled();
        String normalizedCommand = normalizeAndValidateCommand(command);
        return executeWhitelistedCommand(normalizedCommand);
    }

    private void validateToolEnabled() {
        if (!terminalToolProperties.isEnabled()) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "Terminal tool is disabled by configuration.");
        }
    }

    private String normalizeAndValidateCommand(String command) {
        if (command == null || command.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Command must not be empty.");
        }
        String trimmedCommand = command.trim();
        if (trimmedCommand.matches(DANGEROUS_CHARS_REGEX)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Command contains unsafe characters and was rejected.");
        }

        String[] parts = trimmedCommand.split("\\s+");
        String commandName = parts[0].toLowerCase(Locale.ROOT);
        Set<String> allowedCommands = terminalToolProperties.getAllowedCommands().stream()
            .map(item -> item.toLowerCase(Locale.ROOT))
            .collect(Collectors.toSet());
        if (allowedCommands.isEmpty()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "No allowed terminal commands are configured.");
        }
        if (!allowedCommands.contains(commandName)) {
            throw new BusinessException(
                ErrorCode.FORBIDDEN,
                "Command is not allowed. Allowed commands: " + String.join(", ", allowedCommands)
            );
        }
        return trimmedCommand;
    }

    private String executeWhitelistedCommand(String command) {
        try {
            return RuntimeUtil.execForStr(StandardCharsets.UTF_8, "cmd.exe", "/c", command);
        } catch (RuntimeException e) {
            log.error("Error executing terminal command: {}", command, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to execute command.");
        }
    }
}
