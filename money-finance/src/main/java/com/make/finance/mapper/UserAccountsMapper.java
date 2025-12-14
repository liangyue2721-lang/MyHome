package com.make.finance.mapper;

import java.util.List;

import com.make.finance.domain.UserAccounts;

/**
 * 用户账户银行卡信息Mapper接口
 *
 * @author erqi
 * @date 2025-06-03
 */
public interface UserAccountsMapper {

    /**
     * 查询用户账户银行卡信息
     *
     * @param id 用户账户银行卡信息主键
     * @return 用户账户银行卡信息
     */
    public UserAccounts selectUserAccountsById(Long id);

    /**
     * 查询用户账户银行卡信息列表
     *
     * @param userAccounts 用户账户银行卡信息
     * @return 用户账户银行卡信息集合
     */
    public List<UserAccounts> selectUserAccountsList(UserAccounts userAccounts);

    /**
     * 新增用户账户银行卡信息
     *
     * @param userAccounts 用户账户银行卡信息
     * @return 结果
     */
    public int insertUserAccounts(UserAccounts userAccounts);

    /**
     * 修改用户账户银行卡信息
     *
     * @param userAccounts 用户账户银行卡信息
     * @return 结果
     */
    public int updateUserAccounts(UserAccounts userAccounts);

    /**
     * 删除用户账户银行卡信息
     *
     * @param id 用户账户银行卡信息主键
     * @return 结果
     */
    public int deleteUserAccountsById(Long id);

    /**
     * 批量删除用户账户银行卡信息
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteUserAccountsByIds(Long[] ids);
}
