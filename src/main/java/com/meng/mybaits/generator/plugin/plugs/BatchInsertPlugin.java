package com.meng.mybaits.generator.plugin.plugs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.mybatis.generator.api.IntrospectedColumn;
import org.mybatis.generator.api.IntrospectedTable;
import org.mybatis.generator.api.PluginAdapter;
import org.mybatis.generator.api.dom.java.FullyQualifiedJavaType;
import org.mybatis.generator.api.dom.java.Interface;
import org.mybatis.generator.api.dom.java.Method;
import org.mybatis.generator.api.dom.java.Parameter;
import org.mybatis.generator.api.dom.java.TopLevelClass;
import org.mybatis.generator.api.dom.xml.Attribute;
import org.mybatis.generator.api.dom.xml.Document;
import org.mybatis.generator.api.dom.xml.Element;
import org.mybatis.generator.api.dom.xml.TextElement;
import org.mybatis.generator.api.dom.xml.XmlElement;
import org.mybatis.generator.codegen.mybatis3.ListUtilities;
import org.mybatis.generator.codegen.mybatis3.MyBatis3FormattingUtilities;

/**
 * 批量插入插件
 * 
 * <property name="mengDatabaseType" value="oracle" /> //配置文件加入属性mengDatabaseType,值选  oracle  or  mysql
 * 
 * @author ChenMeng
 * @time 2019年8月28日
 */
public class BatchInsertPlugin extends PluginAdapter {
	/**
	 * 批量插入
	 */
	public static final String METHOD_BATCH_INSERT = "insertAll";
    
    private Integer type;//1 oracle  2 mysql

	@Override
	public boolean validate(List<String> warnings) {
		String driverClass = getContext().getJdbcConnectionConfiguration().getDriverClass();
		String targetRuntime = getContext().getTargetRuntime();
		if(!"MyBatis3".equals(targetRuntime)) {
			System.out.println("BatchInsertPlugin要求运行targetRuntime必须为MyBatis3!");
			return false;
		}
		if("oracle.jdbc.driver.OracleDriver".equals(driverClass)) {
			type = 1;
		}else if("com.mysql.jdbc.Driver".equals(driverClass) || "com.mysql.cj.jdbc.Driver".equals(driverClass)) {
			type = 2;
		}else {
			System.out.println("BatchInsertPlugin只支持oracle和mysql!");
			return false;
		}
		return true;
	}
	
	/**
	 * 生成函数
	 */
	@Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		//获取类型
		FullyQualifiedJavaType fullyQualifiedJavaType = FullyQualifiedJavaType.getNewListInstance();
        fullyQualifiedJavaType.addTypeArgument(introspectedTable.getRules().calculateAllFieldsClass());
		
		// 添加 METHOD_BATCH_INSERT
        Method mBatchInsert = buildMethod(METHOD_BATCH_INSERT,fullyQualifiedJavaType);
        interfaze.addMethod(mBatchInsert);
		
		return true;
	}

	/**
	 * xml生成
	 */
	@Override
	public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		// 表名
		String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
		// 全字段集合
		List<IntrospectedColumn> columns = ListUtilities.removeIdentityAndGeneratedAlwaysColumns(introspectedTable.getAllColumns());
		
		// 表列名集合<列名,类属性名>
        Map<String,String> columnMap = new LinkedHashMap<String,String>();
        // 赋值名集合<赋值名,类属性名>
        Map<String,String> valuesMap = new LinkedHashMap<String,String>();
        String itemStr = "item.";
        for (IntrospectedColumn colums : columns) {
        	String javaProperty = colums.getJavaProperty();
        	columnMap.put(MyBatis3FormattingUtilities.getEscapedColumnName(colums), itemStr + javaProperty);
        	valuesMap.put(MyBatis3FormattingUtilities.getParameterClause(colums, itemStr), itemStr + javaProperty);
		}
        // 批量插入节点
        List<Element> insertElementList = new ArrayList<Element>();
        
        if(this.type == 1) {// oracle
        	// 批量插入节点
        	insertElementList.add(new TextElement("insert all"));
        	insertElementList.add(buildForeachClauseOracle(tableName, columnMap, valuesMap, false));
        }
        if(this.type == 2) {// mysql
        	// 批量插入节点
        	insertElementList.add(new TextElement("insert into " + tableName));
        	insertElementList.addAll(generateColumnElementList(columnMap, false));
        	insertElementList.add(new TextElement("values"));
        	insertElementList.add(buildForeachClauseClauseMysql(valuesMap, false));
        }
        
        // 添加批量插入节点
        addBatchIndertElement(document, introspectedTable, METHOD_BATCH_INSERT, insertElementList);
		return true;
	}
	
	private Method buildMethod(String methodName, FullyQualifiedJavaType fullyQualifiedJavaType) {
		Method method = new Method(methodName);
		method.setReturnType(FullyQualifiedJavaType.getIntInstance());
		method.addParameter(new Parameter(fullyQualifiedJavaType, "list", "@Param(\"list\")"));
		return method;
	}

	private void addBatchIndertElement(Document document, IntrospectedTable introspectedTable, String method, List<Element> insertElementList) {
		XmlElement batchInsertElt = new XmlElement("insert");
 		batchInsertElt.addAttribute(new Attribute("id", method));// 方法名
 		batchInsertElt.addAttribute(new Attribute("parameterType", "list"));// 参数类型
 		
        for (Element element : insertElementList) {
        	batchInsertElt.addElement(element);
		}
        
        if (context.getPlugins().sqlMapInsertElementGenerated(batchInsertElt, introspectedTable)) {
            document.getRootElement().addElement(batchInsertElt);
        }
	}

	private XmlElement buildForeachElement() {
		XmlElement foreachElement = new XmlElement("foreach");
        foreachElement.addAttribute(new Attribute("collection", "list"));
        foreachElement.addAttribute(new Attribute("item", "item"));
        foreachElement.addAttribute(new Attribute("separator", ","));
        return foreachElement;
	}

	private Element buildForeachClauseOracle(String tableName, Map<String,String> columnMap, Map<String,String> valuesMap, boolean selective) {

		List<Element> columnElementList = generateColumnElementList(columnMap,selective);
		List<Element> valuesElementList = generateColumnElementList(valuesMap,selective);
		
		XmlElement foreachElement = buildForeachElement();
		foreachElement.addElement(new TextElement("into " + tableName));
		for (Element cElt : columnElementList) {
			foreachElement.addElement(cElt);
		}
		foreachElement.addElement(new TextElement("values"));
		for (Element vElt : valuesElementList) {
			foreachElement.addElement(vElt);
		}
		return foreachElement;
	}

	private List<Element> generateColumnElementList(Map<String, String> columnMap, boolean selective) {
		List<Element> elementList = new ArrayList<Element>();
		elementList.add(new TextElement("("));
		if(selective) {
			XmlElement trimXmlElement = new XmlElement("trim");
			trimXmlElement.addAttribute(new Attribute("suffixOverrides", ","));
			
			Set<Entry<String, String>> entrySet = columnMap.entrySet();
			for (Map.Entry<String, String> column : entrySet) {
				XmlElement xmlElement = new XmlElement("if");
				xmlElement.addAttribute(new Attribute("test", column.getValue() + " != null"));
				xmlElement.addElement(new TextElement(column.getKey() + ","));
				trimXmlElement.addElement(xmlElement);
			}
			elementList.add(trimXmlElement);
		}else {
			elementList.add(new TextElement(String.join(", ", columnMap.keySet())));
		}
		elementList.add(new TextElement(")"));
		return elementList;
	}

	private Element buildForeachClauseClauseMysql(Map<String,String> valueMap, boolean selective) {
		XmlElement foreachElement = buildForeachElement();
		List<Element> elementList = generateColumnElementList(valueMap,selective);
		for (Element element : elementList) {
			foreachElement.addElement(element);
		}
        return foreachElement;
	}

}
