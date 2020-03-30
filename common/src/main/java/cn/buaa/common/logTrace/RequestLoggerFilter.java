package cn.buaa.common.logTrace;

import cn.buaa.common.entity.RequestLoggerEntity;
import cn.buaa.common.idGenerator.ILogTraceIdGenerator;
import cn.buaa.common.idGenerator.impl.RandomIdGeneratorImp;
import cn.buaa.common.utils.ThreadLocalUtils;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONException;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpMethod;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;

/**
 * @author hubert
 */
public class RequestLoggerFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(RequestLoggerFilter.class);

    private ILogTraceIdGenerator randomIdGeneratorImp;

    @Override
    public void init(FilterConfig filterConfig) {
        ServletContext servletContext = filterConfig.getServletContext();
        ApplicationContext applicationContext = WebApplicationContextUtils.getWebApplicationContext(servletContext);
        randomIdGeneratorImp = applicationContext.getBean(RandomIdGeneratorImp.class);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        MultiReadHttpServletRequestWrapper requestWrapper = new MultiReadHttpServletRequestWrapper((HttpServletRequest) servletRequest);
        LoggerHttpServletResponseWrapper responseWrapper = new LoggerHttpServletResponseWrapper((HttpServletResponse) servletResponse);
        handleRequest(requestWrapper);
        filterChain.doFilter(requestWrapper, responseWrapper);
        byte[] bytes = handleResponse(responseWrapper);
        servletResponse.getOutputStream().write(bytes);
        ThreadLocalUtils.remove();
    }

    @Override
    public void destroy() {
    }

    /**
     * handle request: generator requestId and set it to the current thread's ThreadLocal。
     *
     * @param request request data
     */
    private void handleRequest(MultiReadHttpServletRequestWrapper request) {
        RequestLoggerEntity loggerEntity = buildLoggerEntity(request);
        String requestId = buildRequestId(loggerEntity.getParams(), loggerEntity.getBody());
        ThreadLocalUtils.setRequestId(requestId);
        LOGGER.info("requestId:{},requestInfo:{}", requestId, JSON.toJSONString(loggerEntity));
    }

    private RequestLoggerEntity buildLoggerEntity(MultiReadHttpServletRequestWrapper request) {
        RequestLoggerEntity entity = new RequestLoggerEntity();
        entity.setParams(request.getParameterMap());
        entity.setMethodPath(request.getRequestURI());
        entity.setRequestAddress(request.getRemoteAddr());
        String body = null;
        if (request.getMethod().equalsIgnoreCase(HttpMethod.POST.name())) {
            try {
                body = buildRequestBody(request.getReader());
            } catch (IOException e) {
                LOGGER.warn("get request info error");
            }
        }
        entity.setBody(body);
        return entity;
    }

    /**
     * handle the response data。
     * print the response data,also at this you can handle response data
     * eg : add the request id to response data;
     * format response data in standard format;
     *
     * @param responseWrapper response
     * @return response data
     */
    private byte[] handleResponse(LoggerHttpServletResponseWrapper responseWrapper) {
        byte[] bytes = null;
        String requestId = ThreadLocalUtils.getRequestId();
        try {
            bytes = responseWrapper.getResponseData();
            String responseData = new String(bytes);
            JSONObject jo = JSON.parseObject(responseData);
            if (!jo.containsKey("requestId")) {
                jo.put("requestId", requestId);
            }
            bytes = jo.toJSONString().getBytes(responseWrapper.getCharacterEncoding());
        } catch (IOException e) {
            LOGGER.warn("get response data error, requestId:{}", requestId);
            return null;
        } catch (JSONException e) {
            LOGGER.warn("json parse response data error, requestId:{}", requestId);
        }
        LOGGER.info("requestId:{},response data:{}", requestId, null == bytes ? StringUtils.EMPTY : new String(bytes));
        return bytes;
    }

    /**
     * generator request id. every request will have a unique id.
     * if the request already have request id,the id will be return
     * or generator {@ThreadLocalUtils}
     *
     * @param params requests params
     * @param body   request body,use to find request id
     * @return request id(String)
     */
    private String buildRequestId(Map<String, String[]> params, String body) {

        String result = null == params.get("requestId") ? null : params.get("requestId")[0];
        if (StringUtils.isNotBlank(result)) {
            return result;
        }
        try {
            JSONObject jsonObject = StringUtils.isNotBlank(body) ? JSON.parseObject(body) : null;
            if (null != jsonObject && jsonObject.containsKey("requestId")) {
                result = jsonObject.getString("requestId");
            }
        } catch (Exception e) {
            LOGGER.warn("parse Request body to JSONObject error", e);
        }
        result = StringUtils.isBlank(result) ? randomIdGeneratorImp.generate() : result;

        return result;
    }

    /**
     * transform body bytes to String
     *
     * @param bufferedReader buffer to save body bytes
     * @return body data as String
     */
    private String buildRequestBody(BufferedReader bufferedReader) {
        String result;
        StringBuilder stringBuilder = new StringBuilder();
        try {
            while ((result = bufferedReader.readLine()) != null) {
                stringBuilder.append(result);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return stringBuilder.toString();
    }
}
