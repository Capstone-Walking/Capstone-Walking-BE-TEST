package com.walking.api.web.support;

import com.walking.api.web.support.ApiResponse.FailureBody;
import com.walking.api.web.support.ApiResponse.Success;
import com.walking.api.web.support.ApiResponse.SuccessBody;
import lombok.experimental.UtilityClass;
import org.springframework.http.HttpStatus;

@UtilityClass
public class ApiResponseGenerator {

	public static ApiResponse<Success> success(final HttpStatus status) {
		return new ApiResponse<>(
				new Success(MessageCode.SUCCESS.getValue(), MessageCode.SUCCESS.getCode()), status);
	}

	public static ApiResponse<Success> success(final HttpStatus status, MessageCode code) {
		return new ApiResponse<>(new Success(code.getValue(), code.getCode()), status);
	}

	public static <D> ApiResponse<SuccessBody<D>> success(final D data, final HttpStatus status) {
		return new ApiResponse<>(
				new SuccessBody<>(data, MessageCode.SUCCESS.getValue(), MessageCode.SUCCESS.getCode()),
				status);
	}

	public static <D> ApiResponse<SuccessBody<D>> success(
			final D data, final HttpStatus status, MessageCode code) {
		return new ApiResponse<>(new SuccessBody<>(data, code.getValue(), code.getCode()), status);
	}

	public static <D> ApiResponse<Page<D>> success(
			final org.springframework.data.domain.Page<D> data, final HttpStatus status) {
		return new ApiResponse<>(new Page<>(data), status);
	}

	public static ApiResponse<Void> fail(final HttpStatus status) {
		return new ApiResponse<>(status);
	}

	public static ApiResponse<FailureBody> fail(final FailureBody body, final HttpStatus status) {
		return new ApiResponse<>(body, status);
	}

	public static ApiResponse<FailureBody> fail(
			final String code, final String message, final HttpStatus status) {
		return new ApiResponse<>(new FailureBody(code, message), status);
	}

	public static ApiResponse<FailureBody> fail(final String message, final HttpStatus status) {
		return new ApiResponse<>(new FailureBody(message), status);
	}
}
