package com.hand.demo.infra.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.choerodon.core.exception.CommonException;
import org.hzero.boot.apaas.common.userinfo.domain.UserVO;
import org.hzero.boot.apaas.common.userinfo.infra.feign.IamRemoteService;
import org.hzero.boot.interfaces.sdk.dto.RequestPayloadDTO;
import org.hzero.boot.interfaces.sdk.dto.ResponsePayloadDTO;
import org.hzero.boot.interfaces.sdk.invoke.InterfaceInvokeSdk;
import org.hzero.core.util.TokenUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

/**
 * Utils
 */

@Component
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final InterfaceInvokeSdk interfaceInvokeSdk;
    private final IamRemoteService iamRemoteService;

    @Autowired
    public Utils(InterfaceInvokeSdk interfaceInvokeSdk, IamRemoteService iamRemoteService) {
        this.interfaceInvokeSdk = interfaceInvokeSdk;
        this.iamRemoteService = iamRemoteService;
    }

    /**
     * Calls the WMS API to push a count order.
     *
     * @param namespace     the namespace of the interface
     * @param serverCode    the server code of the interface
     * @param interfaceCode the specific interface code to invoke
     * @param jsonString    the JSON payload to send in the request
     * @return a map containing the response body from the interface
     * @throws CommonException if an error occurs during the API call or response parsing
     */
    public Map<String, Object> callWmsApiPushCountOrder(String namespace, String serverCode, String interfaceCode, String jsonString) {
        // Prepare headers with authorization token
        Map<String, String> headerParamMap = new HashMap<>();
        headerParamMap.put("Authorization", "bearer " + TokenUtils.getToken());

        // Construct the request payload
        RequestPayloadDTO requestPayload = new RequestPayloadDTO();
        requestPayload.setPayload(jsonString);
        requestPayload.setMediaType("application/json");
        requestPayload.setHeaderParamMap(headerParamMap);

        // Log the outgoing request
        logger.info("Calling WMS API with payload: {}", jsonString);

        // Send the request to the interface

        // TODO: Use the actual service when deployed
        // Uncomment the following line to enable real API calls
        // ResponsePayloadDTO response = interfaceInvokeSdk.invoke(namespace, serverCode, interfaceCode, requestPayload);

        // Dummy response for testing purposes
        ResponsePayloadDTO response = new ResponsePayloadDTO();
        // Success example: {code=WMS-2024 12 20 16:47:56114, returnStatus=S, returnMsg=Success sync}
        // response.setBody("{\"code\":\"WMS-2024 12 20 16:47:56114\", \"returnStatus\":\"S\", \"returnMsg\":\"Success sync\"}");
        response.setBody("{\"code\":\"WMS-2024 12 20 16:47:56114\", \"returnStatus\":\"E\", \"returnMsg\":\"Error sync\"}");
        // Log the response received
        logger.info("Interface response: {} {}", response.getMessage(), response.getBody());

        // Validate response body
        Object body = response.getBody();
        if (body == null) {
            throw new CommonException("Response body from the interface is null");
        }

        // Parse the response body into a map
        try {
            return objectMapper.readValue(body.toString(), new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new CommonException("Failed to parse response body from the interface", e);
        }
    }

    public UserVO getUserVO() {
        ResponseEntity<String> stringResponse = iamRemoteService.selectSelf();
        ObjectMapper objectMapper = new ObjectMapper();
        // Fix object mapper error
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Set a custom date format that matches the API response
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        objectMapper.setDateFormat(dateFormat);
        UserVO userVO;
        try {
            logger.info(stringResponse.getBody());
            userVO = objectMapper.readValue(stringResponse.getBody(), UserVO.class);
        } catch (JsonProcessingException e) {
            throw new CommonException("Failed to parse response body to UserVO", e);
        } catch (Exception e) {
            throw new CommonException("Unexpected error occurred", e);
        }
        return userVO;
    }
}
