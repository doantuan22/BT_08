package com.example.bt_08.exception;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.validation.BindException;
import org.springframework.web.bind.annotation.ControllerAdvice;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice
public class GraphQLExceptionAdvice {

    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(ResourceNotFoundException ex, DataFetchingEnvironment env) {
        return error(ErrorType.NOT_FOUND, ex.getMessage(), null, env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleBusiness(BusinessException ex, DataFetchingEnvironment env) {
        return error(ErrorType.BAD_REQUEST, ex.getMessage(), null, env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleConstraintViolation(ConstraintViolationException ex, DataFetchingEnvironment env) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            fieldErrors.merge(lastNode(v.getPropertyPath().toString()), v.getMessage(), (a, b) -> a + ", " + b);
        }
        String message = fieldErrors.values().stream().collect(Collectors.joining("; "));
        return error(ErrorType.BAD_REQUEST, message, fieldErrors, env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleBind(BindException ex, DataFetchingEnvironment env) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getFieldErrors().stream()
                .map(fe -> lastNode(fe.getField()))
                .filter(name -> !name.isBlank() && !name.equals("$"))
                .forEach(name -> fieldErrors.putIfAbsent(name, "Giá trị không hợp lệ"));
        String message = fieldErrors.isEmpty()
                ? "Tham số không hợp lệ: sai kiểu dữ liệu (ví dụ id phải là số nguyên)"
                : "Tham số không hợp lệ: " + String.join(", ", fieldErrors.keySet());
        return error(ErrorType.BAD_REQUEST, message, fieldErrors, env);
    }

    @GraphQlExceptionHandler
    public GraphQLError handleDataIntegrity(DataIntegrityViolationException ex, DataFetchingEnvironment env) {
        return error(ErrorType.BAD_REQUEST, "Dữ liệu vi phạm ràng buộc trong cơ sở dữ liệu (trùng tên hoặc đang được tham chiếu)", null, env);
    }

    private static GraphQLError error(ErrorType type, String message, Map<String, String> fieldErrors,
            DataFetchingEnvironment env) {
        GraphqlErrorBuilder<?> builder = GraphqlErrorBuilder.newError(env).errorType(type).message(message);
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            builder.extensions(Map.of("fieldErrors", fieldErrors));
        }
        return builder.build();
    }

    private static String lastNode(String path) {
        int i = path.lastIndexOf('.');
        return i >= 0 ? path.substring(i + 1) : path;
    }
}
