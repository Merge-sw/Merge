package com.merge.backend.assessment.judge0;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Component
public class Judge0Client {

    private static final int STATUS_IN_QUEUE = 1;
    private static final int STATUS_PROCESSING = 2;

    private final RestTemplate restTemplate;
    private final Judge0Properties props;

    public Judge0Client(RestTemplateBuilder builder, Judge0Properties props) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(15))
                .build();
        this.props = props;
    }

    public Judge0Outcome execute(String sourceCode) {
        String token = submit(sourceCode);
        return poll(token);
    }

    private String submit(String sourceCode) {
        Judge0SubmissionRequest body = new Judge0SubmissionRequest(sourceCode);
        Judge0SubmissionToken response = restTemplate.postForObject(
                props.getBaseUrl() + "/submissions?base64_encoded=false&wait=false",
                body,
                Judge0SubmissionToken.class);

        if (response == null || response.getToken() == null) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Judge0 did not return a submission token");
        }
        return response.getToken();
    }

    private Judge0Outcome poll(String token) {
        String url = props.getBaseUrl() + "/submissions/" + token + "?base64_encoded=false";
        int attempts = 0;

        while (attempts < props.getMaxPollAttempts()) {
            Judge0StatusResult result = restTemplate.getForObject(url, Judge0StatusResult.class);

            if (result == null || result.getStatus() == null) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Judge0 returned an empty result");
            }

            int statusId = result.getStatus().getId();

            if (statusId != STATUS_IN_QUEUE && statusId != STATUS_PROCESSING) {
                return new Judge0Outcome(statusId, result.errorOutput());
            }

            attempts++;
            try {
                Thread.sleep(props.getPollIntervalMs());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Interrupted while polling Judge0");
            }
        }

        // Exhausted poll attempts — treat as time limit exceeded
        return new Judge0Outcome(5, null);
    }
}
