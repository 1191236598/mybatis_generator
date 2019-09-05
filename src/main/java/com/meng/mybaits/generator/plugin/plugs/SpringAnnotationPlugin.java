package com.meng.mybaits.generator.plugin.plugs;

import java.util.List;

import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.TopLevelClass;

/**
 * 添加Spring @Repository 插件
 * @author ChenMeng
 *
 * @time 2019年8月29日
 */
public class SpringAnnotationPlugin extends PluginAdapter{
	
	@Override
	public boolean validate(List<String> warnings) {
		return true;
	}

	/**
	 * 生成函数
	 */
	@Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		interfaze.addAnnotation("@Repository");
		interfaze.addImportedType(new FullyQualifiedJavaType("org.springframework.stereotype.Repository"));
		return true;
	}
}
