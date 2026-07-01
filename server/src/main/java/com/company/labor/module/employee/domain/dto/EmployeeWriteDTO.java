package com.company.labor.module.employee.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Data;

public final class EmployeeWriteDTO {
    private EmployeeWriteDTO() {
    }

    @Data
    public static class EmployeeRequest {
        @NotBlank(message = "员工姓名必填")
        private String name;
        @Pattern(regexp = "^$|^1\\d{10}$", message = "手机号格式不正确")
        private String phone;
        @Pattern(regexp = "^$|^\\d{15}$|^\\d{17}[0-9Xx]$", message = "身份证号格式不正确")
        private String idCard;
        @Pattern(regexp = "^$|male|female", message = "性别只能为 male 或 female")
        private String gender;
        private LocalDate birthDate;
        private Long departmentId;
        private Long positionId;
        @Pattern(regexp = "company_employee|project_staff|temporary_worker|team_leader", message = "员工类型不合法")
        private String employeeType;
        private LocalDate entryDate;
        private LocalDate leaveDate;
        private String bankAccount;
        private String bankName;
        private String emergencyContact;
        @Pattern(regexp = "^$|^1\\d{10}$", message = "紧急联系人电话格式不正确")
        private String emergencyPhone;
        @Pattern(regexp = "active|inactive|left", message = "员工状态不合法")
        private String status;
        private String remark;
    }

    @Data
    public static class DepartmentRequest {
        @NotBlank(message = "部门名称必填")
        private String name;
        private Long parentId;
        @Min(value = 0, message = "排序不能小于 0")
        private Integer sortOrder;
        @Pattern(regexp = "active|inactive", message = "部门状态不合法")
        private String status;
    }

    @Data
    public static class PositionRequest {
        @NotBlank(message = "岗位名称必填")
        private String name;
        private Long departmentId;
        @Min(value = 0, message = "排序不能小于 0")
        private Integer sortOrder;
        @Pattern(regexp = "active|inactive", message = "岗位状态不合法")
        private String status;
    }
}
