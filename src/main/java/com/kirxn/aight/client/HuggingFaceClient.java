package com.kirxn.aight.client;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class HuggingFaceClient {
    private static final String API_URL = "https://api-inference.huggingface.co/models/";

    private final String modelName;
    private final String accessToken;
    private final int maxLength;
    private final Double temperature;
    private final int maxRetries;
    private final long retryDelay;

    @Builder.Default
    private final OkHttpClient client = new OkHttpClient();

    public String call(String inputs) throws IOException {
        //Check if inputs are not empty
        if (inputs.isEmpty()) {
            throw new IllegalArgumentException("Input string cannot be empty");
        }
        //Create JSON payload using Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> payload = new HashMap<>();
        payload.put("inputs", inputs);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_length",maxLength);
        if (temperature != null) {
            parameters.put("temperature", temperature);
        }
        payload.put("parameters", parameters);

        String requestParams = objectMapper.writeValueAsString(payload);
        //Create the complete url
        String url = API_URL + modelName;
        //Building the request and sending it
        RequestBody requestBody = RequestBody.create(requestParams, MediaType.parse("application/json"));
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization","Bearer " + accessToken)
                .post(requestBody)
                .build();

        int retries = 0;
        while (true) {
            try (Response response = client.newCall(request).execute()) {
                if(response.isSuccessful()) {
                    return response.body().string();
                } else if (retries< maxRetries) {
                    retries++;
                    try {
                        Thread.sleep(retryDelay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    throw new IOException("Unexpected response code: " + response.code());
                }
            }
        }
    }
}
