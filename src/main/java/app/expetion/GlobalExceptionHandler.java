package app.expetion;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private boolean isApiRequest(HttpServletRequest request) {
        return request.getRequestURI() != null && request.getRequestURI().startsWith("/api/");
    }

    @ExceptionHandler(DomainException.class)
    public Object handleDomainException(DomainException e, HttpServletRequest request) {
        log.warn("Domain exception: {}", e.getMessage());
        
        if (isApiRequest(request)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        }
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.setStatus(HttpStatus.NOT_FOUND);
        return modelAndView;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Object handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        log.warn("Validation exception: {}", e.getMessage());
        
        if (isApiRequest(request)) {
            Map<String, String> errors = new HashMap<>();
            e.getBindingResult().getAllErrors().forEach(error -> {
                String fieldName = ((FieldError) error).getField();
                String errorMessage = error.getDefaultMessage();
                errors.put(fieldName, errorMessage);
            });
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
        }
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.setStatus(HttpStatus.BAD_REQUEST);
        return modelAndView;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Object handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        log.warn("HTTP message not readable: {}", e.getMessage());
        
        if (isApiRequest(request)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Invalid request body");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.setStatus(HttpStatus.BAD_REQUEST);
        return modelAndView;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ModelAndView handleAccessDeniedException(AccessDeniedException e) {
        log.warn("Access denied: {}", e.getMessage());
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.setStatus(HttpStatus.FORBIDDEN);
        return modelAndView;
    }

    @ExceptionHandler(Exception.class)
    public Object handleGenericException(Exception e, HttpServletRequest request) {
        log.error("Unhandled exception occurred", e);
        
        if (isApiRequest(request)) {
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "An unexpected error occurred");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
        
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("error");
        modelAndView.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        return modelAndView;
    }
}
