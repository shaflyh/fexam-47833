package com.hand.demo.app.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.choerodon.core.exception.CommonException;
import org.hzero.boot.interfaces.sdk.dto.RequestPayloadDTO;
import org.hzero.boot.interfaces.sdk.dto.ResponsePayloadDTO;
import org.hzero.core.util.TokenUtils;
import org.hzero.boot.interfaces.sdk.invoke.InterfaceInvokeSdk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class WmsApiService {

    private static final Logger logger = LoggerFactory.getLogger(WmsApiService.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String MEDIA_TYPE_JSON = "application/json";

    private final InterfaceInvokeSdk interfaceInvokeSdk;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a WmsApiService with required dependencies.
     *
     * @param interfaceInvokeSdk the SDK for invoking interfaces
     * @param objectMapper       the JSON object mapper
     */
    public WmsApiService(InterfaceInvokeSdk interfaceInvokeSdk, ObjectMapper objectMapper) {
        this.interfaceInvokeSdk = interfaceInvokeSdk;
        this.objectMapper = objectMapper;
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
        Map<String, String> headers = createAuthorizationHeader();

        RequestPayloadDTO requestPayload = buildRequestPayload(jsonString, headers);

        logger.info("Calling WMS API with payload: {}", jsonString);

        ResponsePayloadDTO response = interfaceInvokeSdk.invoke(namespace, serverCode, interfaceCode, requestPayload);

        String payload = response.getPayload();
        logger.info("Interface response payload: {}", payload);

        return parseResponsePayload(payload);
    }

    /**
     * Creates the authorization header map.
     *
     * @return a map containing the authorization header
     * @throws CommonException if the token is missing or invalid
     */
    private Map<String, String> createAuthorizationHeader() {
        String token = TokenUtils.getToken();
        if (token == null || token.isEmpty()) {
            throw new CommonException("Authorization token is missing");
        }

        Map<String, String> headers = new HashMap<>();
        headers.put(AUTHORIZATION_HEADER, BEARER_PREFIX + token);
        return headers;
    }

    /**
     * Builds the request payload object.
     *
     * @param jsonString the JSON payload as a string
     * @param headers    the headers map
     * @return a populated RequestPayloadDTO object
     */
    private RequestPayloadDTO buildRequestPayload(String jsonString, Map<String, String> headers) {
        RequestPayloadDTO requestPayload = new RequestPayloadDTO();
        requestPayload.setPayload(jsonString);
        requestPayload.setMediaType(MEDIA_TYPE_JSON);
        requestPayload.setHeaderParamMap(headers);
        return requestPayload;
    }

    /**
     * Parses the response payload from JSON string to a Map.
     *
     * @param payload the JSON response as a string
     * @return a map representing the response payload
     * @throws CommonException if the payload is null or cannot be parsed
     */
    private Map<String, Object> parseResponsePayload(String payload) {
        if (payload == null) {
            throw new CommonException("Response payload from the interface is null");
        }

        try {
            return objectMapper.readValue(payload, new TypeReference<Map<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new CommonException("Failed to parse response body from the interface : " + payload, e);
        }
    }

    /**
     * Validates the response payload for failure indications.
     *
     * @param responsePayload the response payload map
     * @throws CommonException if the response indicates a failure
     */
    private void validateResponse(Map<String, Object> responsePayload) {
        Object failedObj = responsePayload.get("failed");
        if (Boolean.TRUE.equals(failedObj)) {
            String message = (String) responsePayload.getOrDefault("message", "Unknown error from WMS API");
            logger.error("WMS API returned failure: {}", message);
            throw new CommonException(message);
        }
    }

    /**
     * Validates the response payload for failure indications.
     *
     * @param responsePayload the response payload map
     * @throws CommonException if the response indicates a failure
     */
    public String validateResponses(Map<String, Object> responsePayload) {
        Object failedObj = responsePayload.get("failed");
        if (Boolean.TRUE.equals(failedObj)) {
            String message = (String) responsePayload.getOrDefault("message", "Unknown error from WMS API");
            logger.error("WMS API returned failure: {}", message);
            return message;
        }
        return null;
    }
}
