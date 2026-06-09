package com.gangku.be.external.ai;

import com.gangku.be.exception.CustomException;
import com.gangku.be.exception.constant.CommonErrorCode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/** AI future 결과를 언랩하는 유틸리티. static 메서드로 제공하여 Mock 환경에서도 실제 언랩 로직이 동작한다. */
public final class AiResponses {

    private AiResponses() {}

    /**
     * CompletableFuture 결과를 블로킹으로 기다린 뒤 반환한다. ExecutionException 내부의 CustomException은
     * 그대로 재던지고, 그 외 예외는 AI_SERVICE_ERROR로 변환한다.
     */
    public static <T> T await(CompletableFuture<T> future) {
        try {
            return future.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new CustomException(CommonErrorCode.AI_SERVICE_ERROR);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof CustomException customException) {
                throw customException;
            }
            throw new CustomException(CommonErrorCode.AI_SERVICE_ERROR);
        }
    }
}
