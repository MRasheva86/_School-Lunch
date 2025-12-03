package app.expetion;

import feign.FeignException;

public class ClientErrorException extends RuntimeException {

    private final FeignException feignException;
    
    public ClientErrorException(FeignException feignException) {
        super(feignException);
        this.feignException = feignException;
    }
    
    public FeignException getFeignException() {
        return feignException;
    }
}

