package com.qin.qaiagentproject.tools;

import com.qin.qaiagentproject.exception.BusinessException;
import com.qin.qaiagentproject.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileOperationToolTest {

    private final FileOperationTool fileOperationTool = new FileOperationTool();

    @Test
    void shouldRejectInvalidFileName() {
        BusinessException ex = assertThrows(BusinessException.class, () -> fileOperationTool.readFile("../a.txt"));
        assertEquals(ErrorCode.BAD_REQUEST.getCode(), ex.getCode());
        assertEquals("Invalid file name.", ex.getMessage());
    }

    @Test
    void shouldThrowNotFoundWhenFileMissing() {
        String fileName = "missing-file-" + System.nanoTime() + ".txt";
        BusinessException ex = assertThrows(BusinessException.class, () -> fileOperationTool.readFile(fileName));
        assertEquals(ErrorCode.NOT_FOUND.getCode(), ex.getCode());
        assertTrue(ex.getMessage().startsWith("File not found: "));
    }
}
