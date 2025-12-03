package app.web.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ErrorMessageExtractor {
    
    private final ObjectMapper objectMapper;
    
    public ErrorMessageExtractor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String extractErrorMessage(FeignException e) {

        try {
            String responseBody = e.contentUTF8();

            if (responseBody != null && !responseBody.trim().isEmpty()) {
                try {
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    
                    if (jsonNode.has("message")) {
                        String message = jsonNode.get("message").asText();
                        if (message != null && !message.trim().isEmpty()) {
                            return cleanErrorMessage(message);
                        }
                    }

                    if (jsonNode.has("error")) {
                        String error = jsonNode.get("error").asText();
                        if (error != null && !error.trim().isEmpty()) {
                            return cleanErrorMessage(error);
                        }
                    }

                    if (jsonNode.has("errorMessage")) {
                        String errorMessage = jsonNode.get("errorMessage").asText();
                        if (errorMessage != null && !errorMessage.trim().isEmpty()) {
                            return cleanErrorMessage(errorMessage);
                        }
                    }

                    if (jsonNode.has("detail")) {
                        String detail = jsonNode.get("detail").asText();
                        if (detail != null && !detail.trim().isEmpty()) {
                            return cleanErrorMessage(detail);
                        }
                    }

                    if (jsonNode.has("error") && jsonNode.get("error").isObject()) {
                        JsonNode errorObj = jsonNode.get("error");
                        if (errorObj.has("message")) {
                            String message = errorObj.get("message").asText();
                            if (message != null && !message.trim().isEmpty()) {
                                return cleanErrorMessage(message);
                            }
                        }
                    }

                    if (jsonNode.has("errors") && jsonNode.get("errors").isArray()) {
                        JsonNode errors = jsonNode.get("errors");
                        if (errors.size() > 0) {
                            JsonNode firstError = errors.get(0);
                            if (firstError.has("defaultMessage")) {
                                return cleanErrorMessage(firstError.get("defaultMessage").asText());
                            }
                            if (firstError.has("message")) {
                                return cleanErrorMessage(firstError.get("message").asText());
                            }
                        }
                    }
                    if (jsonNode.isTextual()) {
                        String text = jsonNode.asText();
                        if (text != null && !text.trim().isEmpty()) {
                            return cleanErrorMessage(text);
                        }
                    }

                    if (jsonNode.isObject()) {
                        java.util.Iterator<String> fieldNames = jsonNode.fieldNames();

                        while (fieldNames.hasNext()) {
                            String fieldName = fieldNames.next();
                            JsonNode fieldValue = jsonNode.get(fieldName);

                            if (fieldValue.isTextual()) {
                                String text = fieldValue.asText();

                                if (text != null && !text.trim().isEmpty() &&
                                    !text.matches("(?i)^(DomainExeption|Domain Exception|Exception)\\s*:?\\s*$")) {
                                    return cleanErrorMessage(text);
                                }
                            }
                        }
                    }
                } catch (Exception jsonException) {
                    return cleanErrorMessage(responseBody);
                }
            }
        } catch (Exception ex) {
        }
        return "Error occurred: " + e.status();
    }

    private String cleanErrorMessage(String message) {
        if (message == null || message.trim().isEmpty()) {
            return message;
        }
        
        String cleaned = message.trim();
        
        if (cleaned.matches("(?i)^(DomainExeption|Domain Exception)\\s*:?\\s*$")) {
            return "An error occurred. Please try again.";
        }
        
        Pattern pattern = java.util.regex.Pattern.compile(
            "(?i).*?(?:DomainExeption|Domain Exception)\\s*:?\\s*(.+)$", 
            java.util.regex.Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(cleaned);

        if (matcher.find() && matcher.group(1) != null && !matcher.group(1).trim().isEmpty()) {
            cleaned = matcher.group(1).trim();
        }
        
        Pattern pattern2 = java.util.regex.Pattern.compile(
            ".*?[A-Z][a-zA-Z0-9]*Exception\\s*:?\\s*(.+)$", 
            java.util.regex.Pattern.DOTALL
        );
        Matcher matcher2 = pattern2.matcher(cleaned);

        if (matcher2.find() && matcher2.group(1) != null && !matcher2.group(1).trim().isEmpty()) {
            String extracted = matcher2.group(1).trim();
            if (!extracted.equals(cleaned) && !extracted.isEmpty()) {
                cleaned = extracted;
            }
        }
        
        cleaned = cleaned.replaceAll("(?i)\\bDomainExeption\\s*:?\\s*", "");
        cleaned = cleaned.replaceAll("(?i)\\bDomain Exception\\s*:?\\s*", "");
        cleaned = cleaned.replaceFirst("^[a-zA-Z0-9.]+Exception\\s*:?\\s*", "");
        
        cleaned = cleaned.replaceAll("^[\"']|[\"']$", "");
        cleaned = cleaned.replaceFirst("^\\s*[:\\-]?\\s*", "").trim();
        
        if (cleaned.isEmpty() || cleaned.matches("^\\s*$") || cleaned.matches("(?i)^(DomainExeption|Domain Exception)\\s*:?\\s*$")) {

            int lastColon = message.lastIndexOf(':');

            if (lastColon > 0 && lastColon < message.length() - 1) {
                String afterColon = message.substring(lastColon + 1).trim();

                if (!afterColon.isEmpty() && !afterColon.matches("(?i)^(DomainExeption|Domain Exception)\\s*$")) {
                    return afterColon;
                }
            }

            return "An error occurred. Please try again.";
        }
        
        return cleaned;
    }
}

