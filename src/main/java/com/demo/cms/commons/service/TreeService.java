/**
 * Copyright &copy; 2017 demo.com All rights reserved.
 */
package com.demo.cms.commons.service;

import com.demo.cms.commons.dao.TreeDao;
import com.demo.cms.commons.utils.StringUtils;
import com.demo.cms.commons.entity.TreeEntity;
import com.demo.cms.commons.utils.Reflections;
import com.demo.cms.modules.sys.entity.User;
import com.demo.cms.modules.sys.utils.UserUtils;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * Service基类
 * @author demo.com
 * @version 2014-05-16
 */

public abstract class TreeService<D extends TreeDao<T>, T extends TreeEntity<T>> extends FrameworkService<D, T> {

	@Transactional(readOnly = false)
	public void save(T entity) {

		@SuppressWarnings("unchecked")
		Class<T> entityClass = Reflections.getClassGenricType(getClass(), 1);

		// 如果没有设置父节点，则代表为跟节点，有则获取父节点实体
		if (entity.getParent() == null || null==entity.getParent().getId()){
			entity.setParent(null);
		}else{
			entity.setParent(super.get(entity.getParent().getId()));
		}
		if (entity.getParent() == null){
			T parentEntity = null;
			try {
				parentEntity = entityClass.getConstructor(Long.class).newInstance(0L);
			} catch (Exception e) {
				throw new ServiceException(e);
			}
			entity.setParent(parentEntity);
			entity.getParent().setParentIds(StringUtils.EMPTY);
		}

		// 获取修改前的parentIds，用于更新子节点的parentIds
		String oldParentIds = entity.getParentIds();

		// 设置新的父节点串
		entity.setParentIds(entity.getParent().getParentIds()+entity.getParent().getId()+",");

		// 保存或更新实体
		super.save(entity);

		// 更新子节点 parentIds
		T o = null;
		try {
			o = entityClass.newInstance();
		} catch (Exception e) {
			throw new ServiceException(e);
		}
		o.setParentIds("%,"+entity.getId()+",%");
		List<T> list = dao.findByParentIdsLike(o);
		for (T e : list){
			if (e.getParentIds() != null && oldParentIds != null){
				e.setParentIds(e.getParentIds().replace(oldParentIds, entity.getParentIds()));
				preUpdateChild(entity, e);
				dao.updateParentIds(e);
			}
		}

	}

	/**
	 * 预留接口，用户更新子节前调用
	 * @param childEntity
	 */
	protected void preUpdateChild(T entity, T childEntity) {
		Date now = new Date();
		User user = UserUtils.getUser();
		if(null==childEntity.getUpdateTime()) childEntity.setUpdateTime(now);
		if(null==childEntity.getUpdateUser() || null==childEntity.getUpdateUser().getId())childEntity.setUpdateUser(user.getId()==null?new User(1L):user);
	}

}
