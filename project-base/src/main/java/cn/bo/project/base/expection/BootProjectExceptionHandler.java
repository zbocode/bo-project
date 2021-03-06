package cn.bo.project.base.expection;

import cn.bo.project.base.response.ResponseData;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.authz.UnauthorizedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.connection.PoolException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;

/**
 * @Author zhangbo
 * @Date 2020/1/3 9:04
 * @Description 异常信息处理类
 * @PackageName cn.bo.project.base.expection
 **/
@RestControllerAdvice
@Slf4j
public class BootProjectExceptionHandler {

	/**
	 * 处理自定义异常
	 */
	@ExceptionHandler(BootProjectException.class)
	public ResponseData handleRRException(BootProjectException e){
		log.error(e.getMessage(), e);
		return ResponseData.error(e.getMessage());
	}

	@ExceptionHandler(NoHandlerFoundException.class)
	public ResponseData handlerNoFoundException(Exception e) {
		log.error(e.getMessage(), e);
		return ResponseData.error("路径不存在，请检查路径是否正确");
	}

	@ExceptionHandler(DuplicateKeyException.class)
	public ResponseData handleDuplicateKeyException(DuplicateKeyException e){
		log.error(e.getMessage(), e);
		return ResponseData.error("数据库中已存在该记录");
	}

	@ExceptionHandler({UnauthorizedException.class, AuthorizationException.class})
	public ResponseData handleAuthorizationException(AuthorizationException e){
		log.error(e.getMessage(), e);
		return ResponseData.error("没有权限，请联系管理员授权");
	}

	@ExceptionHandler(Exception.class)
	public ResponseData handleException(Exception e){
		log.error(e.getMessage(), e);
		return ResponseData.error("操作失败，"+e.getMessage());
	}
	
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseData HttpRequestMethodNotSupportedException(Exception e){
		log.error(e.getMessage(), e);
		return ResponseData.error("没有权限，请联系管理员授权");
	}
	
	 /** 
	  * spring默认上传大小100MB 超出大小捕获异常MaxUploadSizeExceededException 
	  */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseData handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
    	log.error(e.getMessage(), e);
        return ResponseData.error("文件大小超出10MB限制, 请压缩或降低文件质量! ");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseData handleDataIntegrityViolationException(DataIntegrityViolationException e) {
    	log.error(e.getMessage(), e);
        return ResponseData.error("字段太长,超出数据库字段的长度");
    }

    @ExceptionHandler(PoolException.class)
    public ResponseData handlePoolException(PoolException e) {
    	log.error(e.getMessage(), e);
        return ResponseData.error("Redis 连接异常!");
    }

}
