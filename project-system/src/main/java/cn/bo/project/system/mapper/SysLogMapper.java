package cn.bo.project.system.mapper;

import cn.bo.project.system.entity.SysLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;


/**
 * @Author zhangbo
 * @Date 2020/1/4 13:12
 * @Description 系统日志mapper接口
 * @PackageName cn.bo.project.system.mapper
 **/
public interface SysLogMapper extends BaseMapper<SysLog> {

    /**
     * 获取分页数据
     * @param page
     * @param param
     * @return
     */
    Page<SysLog> pageList(@Param("page") Page page, @Param("param") SysLog param);
}
