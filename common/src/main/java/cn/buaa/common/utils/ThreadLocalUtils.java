package cn.buaa.common.utils;

import com.alibaba.fastjson.JSON;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.Map;

public class ThreadLocalUtils {

    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL = new ThreadLocal<>();

    private static final String REQUEST_ID_KEY = "requestId";

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadLocalUtils.class);

    public static void setValue(@NotBlank String key, Object o) {
        Map<String, Object> map = getValues();
        if (MapUtils.isEmpty(map)) {
            map = new HashMap<>();
        }
        map.put(key, o);
        THREAD_LOCAL.set(map);
    }

    public static void remove() {
        THREAD_LOCAL.remove();
    }


    public static Object getValue(@NotBlank String key) {
        return getValueByType(Object.class, key);
    }

    public static <T> T getValueByType(@NotNull Class<T> clazz, @NotBlank(message = "key can't be blank") String key) {
        Map<String, Object> result = THREAD_LOCAL.get();
        if (null == result || !result.containsKey(key)) {
            return null;
        }
        try {
            return clazz.cast(result.get(key));
        } catch (Exception e) {
            LOGGER.warn("error occurred at acquire threadLocal value, class cast exception value:{},clazz:{}", JSON.toJSONString(result), clazz);
        }
        return null;
    }

    public static Map<String, Object> getValues() {
        return THREAD_LOCAL.get();
    }

    public static String getRequestId() {
        String result = getValueByType(String.class, REQUEST_ID_KEY);
        if (StringUtils.isBlank(result)) {
            return StringUtils.EMPTY;
        }
        return result;
    }

    public static void setRequestId(String requestId) {
        setValue(REQUEST_ID_KEY, requestId);
    }


}
