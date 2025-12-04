package com.logix.common.util;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.Map;

/**
 * JSON工具类
 * 提供统一的JSON序列化和反序列化功能
 *
 * @author Kanade
 * @since 2025/09/24
 */
@Slf4j
public final class JsonUtils {

    private JsonUtils() {
    }

    private static final ObjectMapper MAPPER = createMapper();

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // 支持Java 8时间类型
        mapper.registerModule(new JavaTimeModule());
        // 日期格式化为ISO-8601格式而非时间戳
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // 忽略未知属性，提高兼容性
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        // 忽略null值字段
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;
    }

    /**
     * 对象转JSON字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.debug("JSON序列化失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * JSON字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.readValue(json, clazz);
        } catch (Exception e) {
            log.debug("JSON反序列化失败: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * 对象转map
     */
    public static Map<String, Object> toMap(Object object) {
        if (object == null) {
            return Collections.emptyMap();
        }
        return MAPPER.convertValue(object, new TypeReference<Map<String, Object>>() {});
    }

    /**
     * 获取ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return MAPPER;
    }

}