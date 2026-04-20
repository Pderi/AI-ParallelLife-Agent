package com.qin.qaiagentproject.tools;

import com.qin.qaiagentproject.config.TerminalToolProperties;
import com.qin.qaiagentproject.exception.BusinessException;
import com.qin.qaiagentproject.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TerminalOperationToolTest {

    @Test
    void shouldRejectWhenToolDisabled() {
        TerminalToolProperties properties = new TerminalToolProperties();
        properties.setEnabled(false);
        properties.setAllowedCommands(List.of("echo"));
        TerminalOperationTool tool = new TerminalOperationTool(properties);

        BusinessException ex = assertThrows(BusinessException.class, () -> tool.executeTerminalCommand("echo hello"));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
        assertEquals("Terminal tool is disabled by configuration.", ex.getMessage());
    }

    @Test
    void shouldRejectUnsafeCharacters() {
        TerminalToolProperties properties = new TerminalToolProperties();
        properties.setEnabled(true);
        properties.setAllowedCommands(List.of("echo"));
        TerminalOperationTool tool = new TerminalOperationTool(properties);

        BusinessException ex = assertThrows(BusinessException.class, () -> tool.executeTerminalCommand("echo hello & whoami"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals("Command contains unsafe characters and was rejected.", ex.getMessage());
    }

    @Test
    void shouldRejectCommandNotInAllowList() {
        TerminalToolProperties properties = new TerminalToolProperties();
        properties.setEnabled(true);
        properties.setAllowedCommands(List.of("echo"));
        TerminalOperationTool tool = new TerminalOperationTool(properties);

        BusinessException ex = assertThrows(BusinessException.class, () -> tool.executeTerminalCommand("dir"));
        assertEquals(ErrorCode.FORBIDDEN.getCode(), ex.getCode());
    }
}
