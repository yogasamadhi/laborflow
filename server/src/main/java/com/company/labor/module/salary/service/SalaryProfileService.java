package com.company.labor.module.salary.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.company.labor.module.salary.domain.dto.SalaryProfileSaveDTO;
import com.company.labor.module.salary.domain.entity.SalaryProfileDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileItemDO;
import com.company.labor.module.salary.domain.vo.SalaryProfileListVO;
import java.util.List;

public interface SalaryProfileService extends IService<SalaryProfileDO> {
    SalaryProfileDO saveProfile(Long id, SalaryProfileSaveDTO dto);
    List<SalaryProfileItemDO> items(Long profileId);
    Page<SalaryProfileListVO> search(long page, long pageSize, Long employeeId, Long departmentId, String keyword);
}
