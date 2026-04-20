package com.qin.qaiagentproject.tools;

import cn.hutool.core.io.FileUtil;
import com.qin.qaiagentproject.constant.FileConstant;
import com.qin.qaiagentproject.exception.BusinessException;
import com.qin.qaiagentproject.exception.ErrorCode;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Pattern;

public class FileOperationTool {

    private final String FILE_DIR = FileConstant.FILE_SAVE_DIR + "/file";
    private static final Pattern SAFE_FILE_NAME = Pattern.compile("^[a-zA-Z0-9._-]+$");

    @Tool(description = "Read content from a file")
    public String readFile(@ToolParam(description = "Name of the file to read") String fileName) {
        try {
            File targetFile = resolveSafeFile(fileName);
            if (!targetFile.exists() || !targetFile.isFile()) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "File not found: " + fileName);
            }
            return FileUtil.readUtf8String(targetFile);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to read file: " + e.getMessage());
        }
    }

    @Tool(description = "Write content to a file")
    public String writeFile(
        @ToolParam(description = "Name of the file to write") String fileName,
        @ToolParam(description = "Content to write to the file") String content) {
        try {
            File targetFile = resolveSafeFile(fileName);
            // 创建目录
            FileUtil.mkdir(FILE_DIR);
            FileUtil.writeUtf8String(content, targetFile);
            return "File written successfully to: " + targetFile.getPath();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to write file: " + e.getMessage());
        }
    }

    private File resolveSafeFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "File name must not be empty.");
        }
        if (!SAFE_FILE_NAME.matcher(fileName).matches()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid file name.");
        }
        Path basePath = Paths.get(FILE_DIR).toAbsolutePath().normalize();
        Path targetPath = basePath.resolve(fileName).normalize();
        if (!targetPath.startsWith(basePath)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Invalid file path.");
        }
        return targetPath.toFile();
    }
}
