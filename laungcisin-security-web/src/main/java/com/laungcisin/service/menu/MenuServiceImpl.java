package com.laungcisin.service.menu;

import com.laungcisin.bean.ZtreeBean;
import com.laungcisin.security.rbac.mybatis.entity.SysMenu;
import com.laungcisin.security.rbac.mybatis.mapper.SysMenuMapper;
import com.laungcisin.utils.PageData;
import com.laungcisin.utils.menu.MenuRequestParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

@Service("menuService")
public class MenuServiceImpl implements MenuService {
    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private SysMenuMapper sysMenuMapper;

    @Override
    public List<ZtreeBean> getAllMenuByUserId(Long userId) {
        logger.info("MenuServiceImpl.getAllMenuByUserId()查询数据");
        List<SysMenu> menuList = sysMenuMapper.getAllMenuByUserId(userId);
        return this.buildTree(menuList);
    }

    @Override
    public List<ZtreeBean> getMenuByRelRoleId(Long roleId) {
        logger.info("MenuServiceImpl.getAllMenuByUserId()查询数据");
        List<SysMenu> menuList = sysMenuMapper.getMenuByRelRoleId(roleId);
        return this.buildTree(menuList);
    }

    @Override
    public PageData getMenuList(MenuRequestParam param) {
        PageData pageData = new PageData();
        pageData.setCount(sysMenuMapper.getAllMenuCount());
        List<SysMenu> list = sysMenuMapper.getMenuPageData(param.getPage(), param.getLimit(), param.getMenuName());
        pageData.setData(list);

        return pageData;
    }

    private List<ZtreeBean> buildTree(List<SysMenu> menuList) {
        if (!CollectionUtils.isEmpty(menuList)) {
            List<ZtreeBean> ztreeBeans = new ArrayList<>();
            for (SysMenu menu : menuList) {
                ZtreeBean tree = new ZtreeBean();
                tree.setpId(menu.getParentId() + "");
                tree.setName(menu.getName());
                tree.setChkDisabled("false");
                tree.setOpen("");
                tree.setId(menu.getMenuId() + "");
                ztreeBeans.add(tree);
            }

            return ztreeBeans;
        }

        return new ArrayList<>();
    }
}
