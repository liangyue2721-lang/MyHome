package com.make.finance.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.make.finance.mapper.UserAccountsMapper;
import com.make.finance.domain.UserAccounts;
import com.make.finance.service.IUserAccountsService;

/**
 * 用户账户银行卡信息Service业务层处理
 *
 * @author erqi
 * @date 2025-06-03
 */
@Service
public class UserAccountsServiceImpl implements IUserAccountsService {

    @Autowired
    private UserAccountsMapper userAccountsMapper;

    /**
     * 查询用户账户银行卡信息
     *
     * @param id 用户账户银行卡信息主键
     * @return 用户账户银行卡信息
     */
    @Override
    public UserAccounts selectUserAccountsById(Long id) {
        return userAccountsMapper.selectUserAccountsById(id);
    }

    /**
     * 查询用户账户银行卡信息列表
     *
     * @param userAccounts 用户账户银行卡信息
     * @return 用户账户银行卡信息
     */
    @Override
    public List<UserAccounts> selectUserAccountsList(UserAccounts userAccounts) {
        return userAccountsMapper.selectUserAccountsList(userAccounts);
    }

    /**
     * 新增用户账户银行卡信息
     *
     * @param userAccounts 用户账户银行卡信息
     * @return 结果
     */
    @Override
    public int insertUserAccounts(UserAccounts userAccounts) {
        return userAccountsMapper.insertUserAccounts(userAccounts);
    }

    /**
     * 修改用户账户银行卡信息
     *
     * @param userAccounts 用户账户银行卡信息
     * @return 结果
     */
    @Override
    public int updateUserAccounts(UserAccounts userAccounts) {
        return userAccountsMapper.updateUserAccounts(userAccounts);
    }

    /**
     * 批量删除用户账户银行卡信息
     *
     * @param ids 需要删除的用户账户银行卡信息主键
     * @return 结果
     */
    @Override
    public int deleteUserAccountsByIds(Long[] ids) {
        return userAccountsMapper.deleteUserAccountsByIds(ids);
    }

    /**
     * 删除用户账户银行卡信息信息
     *
     * @param id 用户账户银行卡信息主键
     * @return 结果
     */
    @Override
    public int deleteUserAccountsById(Long id) {
        return userAccountsMapper.deleteUserAccountsById(id);
    }
}
