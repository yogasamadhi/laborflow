package com.company.labor.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.labor.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void validationErrorsIncludeReadableFieldMessages() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "dto");
        bindingResult.addError(new FieldError("dto", "salaryMonth", "工资月份格式必须为 yyyy-MM"));
        bindingResult.addError(new FieldError("dto", "amount", "金额不能小于 0"));

        Result<Void> result = handler.handleMethodArgumentNotValid(new MethodArgumentNotValidException(null, bindingResult));

        assertThat(result.getMessage())
                .contains("salaryMonth：工资月份格式必须为 yyyy-MM")
                .contains("amount：金额不能小于 0");
    }
}
