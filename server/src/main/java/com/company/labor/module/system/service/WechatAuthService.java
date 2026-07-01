package com.company.labor.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.company.labor.module.system.domain.entity.WechatUserBindingDO;
import com.company.labor.module.system.domain.vo.WechatSessionVO;
import com.company.labor.module.system.mapper.WechatUserBindingMapper;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WechatAuthService {
    private static final String COL_OPENID = "openid";
    private static final String COL_DELETED = "deleted";

    private final WechatMiniappClient miniappClient;
    private final WechatUserBindingMapper bindingMapper;

    public WechatSessionVO exchangeSession(String code) {
        WechatMiniappClient.WechatCodeSession session = miniappClient.exchangeSession(code);
        LocalDateTime now = LocalDateTime.now();
        WechatUserBindingDO binding = bindingMapper.selectOne(new QueryWrapper<WechatUserBindingDO>()
                .eq(COL_OPENID, session.openid())
                .eq(COL_DELETED, 0));
        if (binding == null) {
            binding = createPendingBinding(session, now);
            bindingMapper.insert(binding);
        } else {
            updateSession(binding, session, now);
            bindingMapper.updateById(binding);
        }
        boolean bound = binding.getUserId() != null && "active".equals(binding.getStatus());
        return new WechatSessionVO(
                binding.getOpenid(),
                binding.getUnionid(),
                true,
                bound,
                binding.getUserId(),
                binding.getEmployeeId(),
                binding.getNickname());
    }

    private WechatUserBindingDO createPendingBinding(WechatMiniappClient.WechatCodeSession session, LocalDateTime now) {
        WechatUserBindingDO binding = new WechatUserBindingDO();
        binding.setOpenid(session.openid());
        binding.setUnionid(session.unionid());
        binding.setStatus("pending");
        binding.setLastSessionAt(now);
        binding.setDeleted(0);
        binding.setCreatedAt(now);
        binding.setUpdatedAt(now);
        return binding;
    }

    private void updateSession(WechatUserBindingDO binding, WechatMiniappClient.WechatCodeSession session, LocalDateTime now) {
        if (StringUtils.hasText(session.unionid())) {
            binding.setUnionid(session.unionid());
        }
        binding.setLastSessionAt(now);
        binding.setUpdatedAt(now);
    }
}
