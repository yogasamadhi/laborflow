package com.company.labor.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.labor.common.result.Result;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
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

    @Test
    void catchAllReturnsGenericMessageWithoutLeakingStackTrace() {
        // A deliberately noisy exception whose message must never reach the client.
        RuntimeException noisy = new RuntimeException("NullPointerException at com.internal.DataSource:42");

        Result<Void> result = handler.handleException(noisy);

        assertThat(result.getMessage()).isEqualTo("服务繁忙，请稍后重试");
        assertThat(result.getMessage()).doesNotContain("DataSource");
    }

    @Test
    void dataIntegrityReturnsGenericMessageWithoutRawCause() {
        // The raw SQL/constraint name must be logged but not returned.
        DataIntegrityViolationException ex =
                new DataIntegrityViolationException("violates uk_salary_monthly_month (SELECT ... )");

        Result<Void> result = handler.handleDataIntegrity(ex);

        assertThat(result.getMessage()).isEqualTo("数据已存在或不符合约束，请检查后重试");
        assertThat(result.getMessage()).doesNotContain("uk_salary_monthly_month");
    }
}
