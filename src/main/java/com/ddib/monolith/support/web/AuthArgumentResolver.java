package com.ddib.monolith.support.web;

import com.ddib.monolith.support.exception.CommonErrorCode;
import com.ddib.monolith.support.exception.CustomException;
import com.ddib.monolith.support.security.AuthAttributes;
import com.ddib.monolith.support.security.UserId;
import com.ddib.monolith.support.security.UserName;
import com.ddib.monolith.support.security.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class AuthArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return (parameter.hasParameterAnnotation(UserId.class) && parameter.getParameterType().equals(Long.class))
                || (parameter.hasParameterAnnotation(UserName.class) && parameter.getParameterType().equals(String.class))
                || (parameter.hasParameterAnnotation(UserRole.class) && parameter.getParameterType().equals(String.class));
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer mavContainer,
            NativeWebRequest webRequest,
            WebDataBinderFactory binderFactory
    ) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new CustomException(CommonErrorCode.UNAUTHORIZED);
        }

        if (parameter.hasParameterAnnotation(UserId.class)) {
            return request.getAttribute(AuthAttributes.USER_ID);
        }
        if (parameter.hasParameterAnnotation(UserName.class)) {
            return request.getAttribute(AuthAttributes.USER_NAME);
        }
        if (parameter.hasParameterAnnotation(UserRole.class)) {
            return request.getAttribute(AuthAttributes.USER_ROLE);
        }
        return null;
    }
}

