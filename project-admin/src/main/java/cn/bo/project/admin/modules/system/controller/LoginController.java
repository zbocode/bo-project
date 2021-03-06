package cn.bo.project.admin.modules.system.controller;

import cn.bo.project.admin.modules.shiro.model.DefContants;
import cn.bo.project.admin.modules.system.entity.SysMenu;
import cn.bo.project.admin.modules.system.entity.SysUser;
import cn.bo.project.admin.modules.system.model.RouterModel;
import cn.bo.project.admin.modules.system.model.SysLoginModel;
import cn.bo.project.admin.modules.system.service.*;
import cn.bo.project.base.constant.CacheConstant;
import cn.bo.project.base.constant.CommonConstant;
import cn.bo.project.base.core.api.ISysBaseAPI;
import cn.bo.project.base.core.model.LoginUser;
import cn.bo.project.base.response.ResponseData;
import cn.bo.project.base.utils.*;
import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.log4j.Log4j2;
import org.apache.shiro.SecurityUtils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;


@Log4j2
@RestController
@Api(tags="登录API")
@RequestMapping("/sys")
public class LoginController {
	@Autowired
	private ISysUserService sysUserService;
	@Autowired
	private ISysBaseAPI sysBaseAPI;
	@Autowired
    private RedisUtil redisUtil;
	@Autowired
	private ISysMenuService menuService;
	@Autowired
	private SysPermissionService permissionService;

	@ApiOperation("登录接口")
	@RequestMapping(value = "/login", method = RequestMethod.POST)
	public ResponseData login(@RequestBody SysLoginModel sysLoginModel){
		String username = sysLoginModel.getUsername();
		String password = sysLoginModel.getPassword();

		String code = String.valueOf(redisUtil.get(CommonConstant.CAPTCHA_CODE_KEY+sysLoginModel.getCodekey()));
		redisUtil.del(CommonConstant.CAPTCHA_CODE_KEY+sysLoginModel.getCodekey());
		if (code==null){
			return ResponseData.error("验证码过期");
		}
		if (!code.equals(sysLoginModel.getCode())){
			return ResponseData.error("验证码错误");
		}
		SysUser sysUser = sysUserService.selectUserByUserName(username);
		sysUserService.checkUserIsEffective(sysUser);
		String userpassword = PasswordUtil.encrypt(username, password, sysUser.getSalt());
		String syspassword = sysUser.getPassword();
		if (!syspassword.equals(userpassword)) {
			return ResponseData.error("用户名或密码错误");
		}
		sysBaseAPI.addLog("用户: " + username + ",登录成功！", CommonConstant.LOG_TYPE_1, null);
		return ResponseData.success(userInfo(sysUser).getData());
	}
	
	/**
	 * 退出登录
	 * @param request
	 * @param response
	 * @return
	 */
	@ApiOperation("退出登录接口")
	@RequestMapping(value = "/logout",method = RequestMethod.POST)
	public ResponseData logout(HttpServletRequest request, HttpServletResponse response) {
		//用户退出逻辑
	    String token = request.getHeader(DefContants.X_ACCESS_TOKEN);
		if(oConvertUtils.isEmpty(token)) {
			return ResponseData.error("退出登录失败！");
		}
		String username = JwtUtil.getUsername(token);
		LoginUser sysUser = sysBaseAPI.getUserByName(username);
	    if(sysUser!=null) {
	    	sysBaseAPI.addLog("用户名: "+sysUser.getRealName()+",退出成功！", CommonConstant.LOG_TYPE_1, null);
	    	log.info(" 用户名:  "+sysUser.getRealName()+",退出成功！ ");
	    	//清空用户登录Token缓存
	    	redisUtil.del(CommonConstant.PREFIX_USER_TOKEN + token);
	    	//清空用户登录Shiro权限缓存
			redisUtil.del(CommonConstant.PREFIX_USER_SHIRO_CACHE + sysUser.getId());
			//清空用户的缓存信息（包括部门信息），例如sys:cache:user::<username>
			redisUtil.del(String.format("%s::%s", CacheConstant.SYS_USERS_CACHE, sysUser.getUserName()));
			//调用shiro的logout
			SecurityUtils.getSubject().logout();
	    	return ResponseData.success("退出登录成功！");
	    }else {
	    	return ResponseData.error("Token无效!");
	    }
	}
	

	/**
	 * 用户信息
	 * @param sysUser
	 * @return
	 */
	private ResponseData userInfo(SysUser sysUser) {
		ResponseData responseData = new ResponseData();
		String syspassword = sysUser.getPassword();
		String username = sysUser.getUserName();
		// 生成token
		String token = JwtUtil.sign(username, syspassword);
        // 设置token缓存有效时间
		redisUtil.set(CommonConstant.PREFIX_USER_TOKEN + token, token);
		redisUtil.expire(CommonConstant.PREFIX_USER_TOKEN + token, JwtUtil.EXPIRE_TIME*2 / 1000);

		JSONObject obj = new JSONObject();
		obj.put("token", token);
		obj.put("userInfo", sysUser);
		responseData.setData(obj);
		return responseData;
	}

	/**
	 * 获取验证码
	 * @return
	 */
	@ApiOperation("获取验证码接口")
	@RequestMapping(value = "/captchaImage", method = RequestMethod.GET)
	private ResponseData getCodeImage() throws IOException {
		Map<String,String> map = new HashMap<String,String>(3);
		// 生成随机字串
		String verifyCode = RandomUtil.randomString(4);
		// 唯一标识
		String codekey = IdUtil.simpleUUID();
		redisUtil.set(CommonConstant.CAPTCHA_CODE_KEY+codekey,verifyCode,120);
		try {
			// 生成图片
			int w = 111, h = 36;
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			VerifyCodeUtils.outputImage(w, h, stream, verifyCode);
			map.put("codekey", codekey);
			map.put("img", Base64.encode(stream.toByteArray()));
			map.put("code", verifyCode);
			return ResponseData.success(map);
		} catch (IOException e) {
			e.printStackTrace();
			return ResponseData.error("获取验证码失败");
		}
	}

	@RequestMapping(value = "/getInfo", method = RequestMethod.GET)
	public ResponseData getInfo(HttpServletRequest request) {
		Map<String, Set<String>> map = new HashMap<>();
		String token = request.getHeader(DefContants.X_ACCESS_TOKEN);
		if(oConvertUtils.isEmpty(token)) {
			return ResponseData.error("验证码过期");
		}
		String username = JwtUtil.getUsername(token);
		SysUser sysUser = sysUserService.selectUserByUserName(username);
		// 角色集合
		Set<String> roles = permissionService.getRolePermission(sysUser);
		// 权限集合
		Set<String> permissions = permissionService.getMenuPermission(sysUser);
		map.put("roles", roles);
		map.put("permissions", permissions);
		return ResponseData.success(map);
	}


	/**
	 * 获取路由信息
	 * @return 路由信息
	 */
	@RequestMapping(value = "/getRouters", method = RequestMethod.GET)
	public ResponseData getRouters(HttpServletRequest request)
	{
		String token = request.getHeader(DefContants.X_ACCESS_TOKEN);
		if(oConvertUtils.isEmpty(token)) {
			return ResponseData.error("token不能为空！");
		}
		String username = JwtUtil.getUsername(token);
		LoginUser sysUser = sysBaseAPI.getUserByName(username);
		// 用户信息
		List<SysMenu> menus = menuService.selectMenuTreeByUserId(sysUser.getId());
		List<RouterModel> RouterModel = menuService.buildMenus(menus);
		JSONObject obj = new JSONObject();
		obj.put("menu", RouterModel);
		return ResponseData.success(obj);
	}

}