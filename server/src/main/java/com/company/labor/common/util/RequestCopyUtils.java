package com.company.labor.common.util;

import com.company.labor.common.exception.BusinessException;
import org.springframework.beans.BeanUtils;

public final class RequestCopyUtils {
    private RequestCopyUtils() {
    }

    public static <T> T copy(Object source, Class<T> targetType) {
        try {
            T target = targetType.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (ReflectiveOperationException ex) {
            throw new BusinessException("请求参数转换失败");
        }
    }
}
