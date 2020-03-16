package cn.bo.project.admin.modules.system.controller;


import cn.bo.project.admin.modules.system.entity.SysLog;
import cn.bo.project.admin.modules.system.service.ISysLogService;
import cn.bo.project.base.api.ResultBean;
import cn.bo.project.base.core.query.QueryGenerator;
import cn.bo.project.base.utils.oConvertUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @Author zhangbo
 * @Date 2020/1/4 13:07
 * @Description 系统日志控制器
 * @PackageName cn.bo.project.admin.modules.system.controller
 **/
@Slf4j
@Api(tags="日志API")
@RestController
@RequestMapping("/sys/log")
public class SysLogController {
	
	@Autowired
	private ISysLogService sysLogService;
	
	/**
	 * @功能：查询日志记录
	 * @param syslog
	 * @param pageNo
	 * @param pageSize
	 * @param req
	 * @return
	 */
	@ApiOperation("系统日志列表")
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResultBean<IPage<SysLog>> queryPageList(SysLog syslog, @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
											   @RequestParam(name="pageSize", defaultValue="10") Integer pageSize, HttpServletRequest req) {
		ResultBean<IPage<SysLog>> result = new ResultBean<IPage<SysLog>>();
		QueryWrapper<SysLog> queryWrapper = QueryGenerator.initQueryWrapper(syslog, req.getParameterMap());
		Page<SysLog> page = new Page<SysLog>(pageNo, pageSize);
		//日志关键词
		String keyWord = req.getParameter("keyWord");
		if(oConvertUtils.isNotEmpty(keyWord)) {
			queryWrapper.like("log_content",keyWord);
		}
		//TODO 过滤逻辑处理
		//TODO begin、end逻辑处理
		//TODO 一个强大的功能，前端传一个字段字符串，后台只返回这些字符串对应的字段
		//创建时间/创建人的赋值
		IPage<SysLog> pageList = sysLogService.page(page, queryWrapper);
		log.info("查询当前页："+pageList.getCurrent());
		log.info("查询当前页数量："+pageList.getSize());
		log.info("查询结果数量："+pageList.getRecords().size());
		log.info("数据总数："+pageList.getTotal());
		result.setSuccess(true);
		result.setData(pageList);
		return result;
	}
	
	/**
	 * @功能：删除单个日志记录
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "/delete", method = RequestMethod.DELETE)
	public ResultBean<SysLog> delete(@RequestParam(name="id",required=true) String id) {
		ResultBean<SysLog> result = new ResultBean<SysLog>();
		SysLog sysLog = sysLogService.getById(id);
		if(sysLog==null) {
			result.error500("未找到对应实体");
		}else {
			boolean ok = sysLogService.removeById(id);
			if(ok) {
				result.success("删除成功!");
			}
		}
		return result;
	}
	
}