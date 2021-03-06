/*
 * Copyright (C) 2015-2020 yunjiweidian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */
package com.yunji.titan.manager.impl.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.yunji.titan.manager.bo.LinkBO;
import com.yunji.titan.manager.common.ILinkFileResolver;
import com.yunji.titan.manager.common.LinkFileResolverFactory;
import com.yunji.titan.manager.common.LinkFileType;
import com.yunji.titan.manager.dao.LinkDao;
import com.yunji.titan.manager.dao.SceneDao;
import com.yunji.titan.manager.entity.Link;
import com.yunji.titan.manager.entity.LinkVariable;
import com.yunji.titan.manager.entity.Scene;
import com.yunji.titan.manager.service.LinkService;

/**
 * @desc 链路表Service实现类
 *
 * @author liuliang
 *
 */
@Service
public class LinkServiceImpl implements LinkService{

	@Resource
	private LinkDao linkDao;
	
	@Resource
	private SceneDao sceneDao;
	
	@Autowired
	private LinkFileResolverFactory linkFileResolverFactory;
	
	/**
     * @desc 查询链路总数量
     *
     * @author liuliang
     *
     * @return int 链路总数量
     * @throws Exception
     */
	@Override
	public int getLinkCount() throws Exception {
		return linkDao.queryLinkCount();
	}
	
	/**
	 * @desc 查询符合条件的记录总数
	 *
	 * @author liuliang
	 *
	 * @param linkName 链路名
	 * @return int 符合条件的记录总数
	 * @throws Exception 
	 */
	@Override
	public int getLinkCount(String linkName) throws Exception {
		if(StringUtils.isBlank(linkName)){
			return this.getLinkCount();
		}else{
			return linkDao.queryLinkCount(linkName);
		}
	}

	/**
	 * @desc 分页查询所有链路列表
	 *
	 * @author liuliang
	 *
	 * @param pageIndex 当前页
	 * @param pageSize 每页条数
	 * @return List<LinkBO> 链路BO集合
	 * @throws Exception
	 */
	@Override
	public List<LinkBO> getLinkList(String linkName,int pageIndex, int pageSize) throws Exception{
		//1、查询
		List<Link> linkList = null;
		if(StringUtils.isBlank(linkName)){
			linkList = linkDao.queryLinkByPage(pageIndex, pageSize);
		}else{
			linkList = linkDao.queryLinkByPage(linkName,pageIndex, pageSize);
		}
		//2、转换
		List<LinkBO> linkBOList = new ArrayList<LinkBO>();
		if((null != linkList) && (0 < linkList.size())){
			LinkBO linkBO = null;
			for(Link link:linkList){
				linkBO = new LinkBO();
				BeanUtils.copyProperties(link, linkBO);
				linkBOList.add(linkBO);
			}
		}
		//3、返回
		return linkBOList;
	}

	/**
	 * @desc 增加链路记录
	 *
	 * @author liuliang
	 *
	 * @param linkBO 链路参数BO
	 * @return int 新增链路id
	 * @throws Exception
	 */
	@Override
	public int addLink(LinkBO linkBO) throws Exception {
		int linkId = linkDao.addLink(linkBO);
		SaveLinkVariable(linkBO, linkId);
		return linkId;
	}

	private void SaveLinkVariable(LinkBO linkBO, int linkId) throws Exception {
		linkDao.removeLinkVariableByLinkId(String.valueOf(linkId));
		
		String varName = linkBO.getVarName();
		String varExpression = linkBO.getVarExpression();
		if(StringUtils.isNotBlank(varName) && StringUtils.isNotBlank(varExpression)){
			String[] varNames = varName.split(",");
			String[] varExpressions = varExpression.split(",");
			for (int i = 0; i < Math.min(varNames.length, varExpressions.length); i++) {
				// to do insert linkVar
				LinkVariable linkVariable = new LinkVariable();
				linkVariable.setLinkId((long)linkId);
				linkVariable.setStresstestUrl(linkBO.getStresstestUrl());
				linkVariable.setVarName(varNames[i]);
				linkVariable.setVarExpression(varExpressions[i]);
				linkDao.addLinkVariable(linkVariable);
			}
		}
	}

	/**
	 * @desc 更新链路记录
	 *
	 * @author liuliang
	 *
	 * @param linkBO 链路参数BO
	 * @return int 受影响的记录数
	 * @throws Exception
	 */
	@Override
	public int updateLink(LinkBO linkBO) throws Exception {
		SaveLinkVariable(linkBO,linkBO.getLinkId().intValue());
		return linkDao.updateLink(linkBO);
	}

	/**
	 * @desc 删除链路记录
	 *
	 * @author liuliang
	 *
	 * @param idList 链路ID(多个ID以英文","隔开)
	 * @return int 受影响的记录数
	 * @throws Exception
	 */
	@Override
	public int removeLink(String idList) throws Exception {
		linkDao.removeLinkVariableByLinkId(String.valueOf(idList));
		return linkDao.removeLink(idList);
	}

	/**
	 * @desc 根据ID查询链路详情
	 *
	 * @author liuliang
	 *
	 * @param linkId 链路ID
	 * @return Link 链路实体
	 * @throws Exception
	 */
	@Override
	public Link getLink(long linkId) throws Exception {
		Link link = linkDao.getLink(linkId);
		List<LinkVariable> linkVariables = linkDao.getLinkVariableByIds(String.valueOf(linkId));
		String varNames = linkVariables.stream().map(l ->l.getVarName()).collect(Collectors.joining(","));
		String varExpressions = linkVariables.stream().map(l ->l.getVarExpression()).collect(Collectors.joining(","));
		link.setVarName(varNames);
		link.setVarExpression(varExpressions);
		return link;
	}

	/**
	 * @desc 根据链路ID查询链路列表
	 *
	 * @author liuliang
	 *
	 * @param ids 链路ID (多个ID以英文","隔开)
	 * @return List<Link> 链路实体集合
	 */
	@Override
	public List<Link> getLinkListByIds(String ids) throws Exception{
		return linkDao.getLinkListByIds(ids);
	}

	/**
	 * @desc 根据链路ID查询链路变量定义列表
	 *
	 * @author liuliang
	 *
	 * @param ids 链路ID (多个ID以英文","隔开)
	 * @return List<Link> 链路实体集合
	 */
	@Override
	public List<LinkVariable> getLinkVariableListByIds(String ids) throws Exception{
		return linkDao.getLinkVariableByIds(ids);
	}

	/**
	 * @desc 删除链路并更新链路相关的场景
	 *
	 * @author liuliang
	 *
	 * @param linkId 链路ID
	 * @param sceneCount 包含该链路ID的场景数
	 * @return int 受影响的记录数
	 * @throws Exception
	 */
	@Override
	@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class, isolation = Isolation.DEFAULT, timeout = 5)
	public int removeLinkAndUpdateScene(long linkId, int sceneCount) throws Exception {
		//1、删除链路
		int removedLinkNum = linkDao.removeLink(String.valueOf(linkId));
		linkDao.removeLinkVariableByLinkId(String.valueOf(linkId));
		//2、更新链路相关场景
		//2.1、查询linkId关联的所有场景
		List<Scene> sceneList = sceneDao.getSceneListByLinkId(linkId, 0, sceneCount);  
		//2.2、逐条处理
		if((null != sceneList) && (0 < sceneList.size())){
			String containLinkid = "";
			String linkRelation = "";
			for(Scene scene:sceneList){
				containLinkid = scene.getContainLinkid();
				linkRelation = scene.getLinkRelation();
				if(containLinkid.equals(String.valueOf(linkId))){
					//2.2.1、该场景只包含待删除的一个链路,直接删除场景
					sceneDao.removeScene(String.valueOf(scene.getSceneId()));
				}else{
					//2.2.2、该场景包含多个链路,更新场景
					//a、数据处理
					containLinkid = this.replaceLinkId(linkId, containLinkid);
					linkRelation = this.replaceLinkId(linkId, linkRelation);
					//b、更新
					scene.setContainLinkid(containLinkid);
					scene.setLinkRelation(linkRelation);
					sceneDao.updateScene(scene);
				}
			}
		}
		//3、返回处理结果
		return removedLinkNum;
	}
	
	/**
	 * @desc 去除字符串中包含的的linkId
	 *
	 * @author liuliang
	 *
	 * @param linkId 链路ID
	 * @param waitReplaceStr 待处理的字符串
	 * @return String 处理后的字符串
	 */
	private String replaceLinkId(long linkId,String waitReplaceStr){
		if(StringUtils.isBlank(waitReplaceStr)){
			return waitReplaceStr;
		}
		waitReplaceStr = waitReplaceStr.replace(String.valueOf(linkId), "").replace(",,", ",");
		final String checkKey = ",";
		if(waitReplaceStr.startsWith(checkKey)){
			waitReplaceStr = waitReplaceStr.substring(1);
		}
		if(waitReplaceStr.endsWith(checkKey)){
			waitReplaceStr = waitReplaceStr.substring(0,waitReplaceStr.length()-1);
		}
		return waitReplaceStr;
	}

	/**
	 * @desc 根据url查询链路列表
	 *
	 * @param url
	 * @return List<Link> 链路实体集合
	 */
	@Override
	public List<Link> getLinkListByUrl(String url) throws Exception{
		return linkDao.getLinkListByUrl(url);
	}
	/**
	 * 通过上传的文件添加链路
	 * @param file
	 * @return 成功添加的linkid
	 * @throws Exception
	 */
	@Override
	public List<Long> addByFile(File file) throws Exception {
		List<Long> result = new ArrayList<Long>();
		List<LinkBO> links = new ArrayList<LinkBO>();
		
	    if(!isFileValid(file)) throw new Exception("文件格式错误，请校验！");
		
		ILinkFileResolver resolver = linkFileResolverFactory.create(file);
		
		try {
			links = resolver.resolve(file);
		} catch (Exception e) {
			throw new Exception("解析失败，请联系管理员！");
		}
		
		for(LinkBO link : links){
			List<Link> dbLink = linkDao.getLinkListByUrl(link.getStresstestUrl());
			if (dbLink.size() > 0) {
				link.setLinkId(dbLink.get(0).getLinkId());
				linkDao.updateLink(link);
				result.add(dbLink.get(0).getLinkId());
			} else {
				int linkId = addLink(link);
				result.add(Integer.valueOf(linkId).longValue());
			}
		}
		
		return result;
	}

	private boolean isFileValid(File file) {
		try {
			String suffix = file.getName().substring(file.getName().lastIndexOf(".") + 1).toUpperCase();
			LinkFileType.valueOf(suffix);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

}
