package com.qin.qaiagentproject.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.qin.qaiagentproject.exception.BusinessException;
import com.qin.qaiagentproject.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://www.searchapi.io/api/v1/search";

    private final String apiKey;

    public WebSearchTool(String apiKey) {
        this.apiKey = apiKey;
    }

    @Tool(description = "Search for information from Baidu Search Engine")
    public String searchWeb(
            @ToolParam(description = "Search query keyword") String query) {
        if (query == null || query.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "Search query must not be empty.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Search API key is not configured.");
        }
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query.trim());
        paramMap.put("api_key", apiKey);
        paramMap.put("engine", "baidu");
        try {
            String response = HttpUtil.get(SEARCH_API_URL, paramMap);
            // 取出返回结果的前 5 条
            JSONObject jsonObject = JSONUtil.parseObj(response);
            // 提取 organic_results 部分
            JSONArray organicResults = jsonObject.getJSONArray("organic_results");
            if (organicResults == null || organicResults.isEmpty()) {
                return "No search results found.";
            }
            int maxSize = Math.min(organicResults.size(), 5);
            List<Object> objects = organicResults.subList(0, maxSize);
            // 拼接搜索结果为字符串
            String result = objects.stream().map(obj -> {
                JSONObject tmpJSONObject = (JSONObject) obj;
                return tmpJSONObject.toString();
            }).collect(Collectors.joining(","));
            return result;
        } catch (RuntimeException e) {
            log.error("Error searching with query: {}", query, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Failed to search web.");
        }
    }
}
