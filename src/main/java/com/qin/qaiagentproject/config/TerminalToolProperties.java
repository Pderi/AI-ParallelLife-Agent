package com.qin.qaiagentproject.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "tool.terminal")
public class TerminalToolProperties {

    /**
     * 默认关闭终端工具，按需显式开启。
     */
    private boolean enabled = false;

    /**
     * 允许执行的命令白名单（仅校验命令名，不包含参数）。
     */
    private List<String> allowedCommands = new ArrayList<>();
}
