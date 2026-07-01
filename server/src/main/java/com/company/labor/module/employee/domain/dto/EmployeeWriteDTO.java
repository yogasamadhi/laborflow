package com.company.labor.module.employee.domain.dto;

import java.time.LocalDate;
import lombok.Data;

public final class EmployeeWriteDTO {
    private EmployeeWriteDTO() {
    }

    @Data
    public static class EmployeeRequest {
        private String name;
        private String phone;
        private String idCard;
        private String gender;
        private LocalDate birthDate;
        private Long departmentId;
        private Long positionId;
        private String employeeType;
        private LocalDate entryDate;
        private LocalDate leaveDate;
        private String bankAccount;
        private String bankName;
        private String emergencyContact;
        private String emergencyPhone;
        private String status;
        private String remark;
    }

    @Data
    public static class DepartmentRequest {
        private String name;
        private Long parentId;
        private Integer sortOrder;
        private String status;
    }

    @Data
    public static class PositionRequest {
        private String name;
        private Long departmentId;
        private Integer sortOrder;
        private String status;
    }
}
