package com.campus.material.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.campus.material.modules.rbac.entity.SysRole;
import com.campus.material.modules.rbac.entity.SysUser;
import com.campus.material.modules.rbac.mapper.SysRoleMapper;
import com.campus.material.modules.rbac.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;

    public CustomUserDetailsService(SysUserMapper userMapper, SysRoleMapper roleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        SysUser user = userMapper.selectOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        if (user == null) {
            throw new UsernameNotFoundException("user not found");
        }
        SysRole role = roleMapper.selectById(user.getRoleId());
        String roleCode = role == null ? "DEPT_USER" : role.getRoleCode();
        return new LoginUser(user.getId(), user.getUsername(), user.getPassword(), roleCode, user.getStatus());
    }
}
