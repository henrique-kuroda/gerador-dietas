package com.gerador.dietas.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.gerador.dietas.llm.LlmException.Kind;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Service
public class GeminiLlmService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(GeminiLlmService.class);
    private static final long[] BACKOFF_MS = {500L, 1500L, 3000L};

    private final RestClient restClient;
    private final GeminiProperties props;

    public GeminiLlmService(RestClient llmRestClient, GeminiProperties props) {
        this.restClient = llmRestClient;
        this.props = props;
    }

    @Override
    public String generateJson(String prompt) {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new LlmException(Kind.CONFIGURATION, "GEMINI_API_KEY não configurada");
        }

        LlmException last = null;
        for (int attempt = 0; attempt < BACKOFF_MS.length; attempt++) {
            try {
                return callGemini(prompt);
            } catch (LlmException ex) {
                last = ex;
                if (ex.getKind() != Kind.UNAVAILABLE && ex.getKind() != Kind.RATE_LIMITED) {
                    throw ex;
                }
                long wait = BACKOFF_MS[attempt];
                log.warn("Falha transitória na Gemini (tentativa {}/{}, kind={}): {}. Retentando em {}ms.",
                        attempt + 1, BACKOFF_MS.length, ex.getKind(), ex.getMessage(), wait);
                if (attempt < BACKOFF_MS.length - 1) {
                    sleep(wait);
                }
            }
        }
        throw last;
    }

    private String callGemini(String prompt) {
        URI uri = URI.create(
                props.baseUrl() + "/v1beta/models/" + props.model() + ":generateContent?key=" + props.apiKey());

        Map<String, Object> body = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "temperature", 0.7
                )
        );

        try {
            GeminiResponse response = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        int status = resp.getStatusCode().value();
                        String reason = "HTTP " + status;
                        if (status == 429) {
                            throw new LlmException(Kind.RATE_LIMITED, "Gemini retornou 429 (rate limit)");
                        }
                        if (status >= 500) {
                            throw new LlmException(Kind.UNAVAILABLE, "Gemini indisponível: " + reason);
                        }
                        throw new LlmException(Kind.CONFIGURATION,
                                "Gemini rejeitou a requisição (" + reason + "). Verifique chave/modelo.");
                    })
                    .body(GeminiResponse.class);

            String text = extractText(response);
            if (text == null || text.isBlank()) {
                throw new LlmException(Kind.INVALID_RESPONSE, "Gemini retornou resposta vazia");
            }
            return text;

        } catch (ResourceAccessException ex) {
            throw new LlmException(Kind.UNAVAILABLE, "Falha de rede/timeout ao chamar Gemini", ex);
        }
    }

    private String extractText(GeminiResponse response) {
        if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        GeminiResponse.Candidate first = response.candidates().get(0);
        if (first == null || first.content() == null || first.content().parts() == null
                || first.content().parts().isEmpty()) {
            return null;
        }
        return first.content().parts().get(0).text();
    }

    private void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new LlmException(Kind.UNAVAILABLE, "Interrompido durante backoff", ie);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record GeminiResponse(List<Candidate> candidates) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Candidate(Content content) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Content(List<Part> parts) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Part(String text) {
        }
    }
}
