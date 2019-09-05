package com.meng.mybaits.generator.plugin.plugs;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

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
 * 存在即更新插件 + 批量
 * 
 * <property name="mengDatabaseType" value="oracle" /> //配置文件加入属性mengDatabaseType,值选  oracle  or  mysql
 * 
 * @author ChenMeng
 * @time 2019年8月29日
 */
public class UpsertAndBatchPlugin extends PluginAdapter{
	/**
	 * 更新插入
	 */
	public static final String METHOD_UPSERT = "upsert";
	/**
	 * 选择性更新插入
	 */
    public static final String METHOD_UPSERT_SELECTIVE = "upsertSelective";
    /**
     * 批量更新插入
     */
    public static final String METHOD_BATCH_UPSERT = "upsertAll";
    
    private Integer type;//1 oracle  2 mysql
    
	@Override
	public boolean validate(List<String> warnings) {
		String driverClass = getContext().getJdbcConnectionConfiguration().getDriverClass();
		String targetRuntime = getContext().getTargetRuntime();
		if(!"MyBatis3".equals(targetRuntime)) {
			System.out.println("UpsertAndBatchPlugin要求运行targetRuntime必须为MyBatis3!");
			return false;
		}
		if("oracle.jdbc.driver.OracleDriver".equals(driverClass)) {
			type = 1;
		}else if("com.mysql.jdbc.Driver".equals(driverClass) || "com.mysql.cj.jdbc.Driver".equals(driverClass)) {
			type = 2;
		}else {
			System.out.println("UpsertAndBatchPlugin只支持oracle和mysql!");
			return false;
		}
		return true;
	}
	
	/**
	 * JavaBean生成
	 */
	@Override
    public boolean clientGenerated(Interface interfaze, TopLevelClass topLevelClass, IntrospectedTable introspectedTable) {
		//获取类型
		FullyQualifiedJavaType fullyQualifiedJavaType = FullyQualifiedJavaType.getNewListInstance();
        fullyQualifiedJavaType.addTypeArgument(introspectedTable.getRules().calculateAllFieldsClass());
		
		// 添加 METHOD_BATCH_INSERT
        Method mUpsert = buildMethod(METHOD_UPSERT,introspectedTable);
        interfaze.addMethod(mUpsert);
        
		// 添加 METHOD_BATCH_INSERT
        Method mUpsertSelective = buildMethod(METHOD_UPSERT_SELECTIVE,introspectedTable);
        interfaze.addMethod(mUpsertSelective);
        
        // 添加METHOD_BATCH_UPSERT
        Method mBatchUpsert = buildListMethod(METHOD_BATCH_UPSERT,fullyQualifiedJavaType);
        interfaze.addMethod(mBatchUpsert);
        
		return true;
	}
	
	/**
	 * xml生成
	 */
	@Override
    public boolean sqlMapDocumentGenerated(Document document, IntrospectedTable introspectedTable) {
		
        // 所有字段
        List<IntrospectedColumn> columns = ListUtilities.removeIdentityAndGeneratedAlwaysColumns(introspectedTable.getAllColumns());
        // 表名
        String tableName = introspectedTable.getFullyQualifiedTableNameAtRuntime();
        // 主键字段
        List<IntrospectedColumn> primaryKeyColumns = introspectedTable.getPrimaryKeyColumns();
        List<String> primaryKeyColumnNames = primaryKeyColumns.stream().map(IntrospectedColumn::getActualColumnName).collect(Collectors.toList());
        
        //Map<字段,属性>  Map<赋值表达式,属性>
        Map<String,String> columnMap = new LinkedHashMap<String,String>();
        Map<String,String> valuesMap = new LinkedHashMap<String,String>();
        Map<String,String> itemValuesMap = new LinkedHashMap<String,String>();
        for (IntrospectedColumn colums : columns) {
        	String javaProperty = colums.getJavaProperty();
        	columnMap.put(MyBatis3FormattingUtilities.getEscapedColumnName(colums), javaProperty);
        	valuesMap.put(MyBatis3FormattingUtilities.getParameterClause(colums), javaProperty);
        	itemValuesMap.put(MyBatis3FormattingUtilities.getParameterClause(colums, "item."), "item." + javaProperty);
		}
		// 1.更新插入节点
        List<Element> upsertElementList = null;
        // 2.选择性更新插入节点
        List<Element> upsertSelectiveElementList = null;
        // 3.批量更新插入节点
        List<Element> batchUpsertElementList = null;
        if(type == 1) {// oracle
        	// 1.更新插入
        	upsertElementList = generateXmlUpsertOracle(columns, tableName, primaryKeyColumnNames, columnMap, valuesMap, false, false);
        	// 2.选择性更新插入
        	upsertSelectiveElementList = generateXmlUpsertOracle(columns, tableName, primaryKeyColumnNames, columnMap, valuesMap, true, false);
        	// 3.批量更新插入
        	batchUpsertElementList = generateXmlUpsertOracle(columns, tableName, primaryKeyColumnNames, columnMap, valuesMap, true, true);
        }
        
        if(type == 2) {// mysql
        	upsertElementList = new ArrayList<Element>();
            upsertSelectiveElementList = new ArrayList<Element>();
            batchUpsertElementList = new ArrayList<Element>();
        	
        	// 1.更新插入
        	upsertElementList.add(new TextElement("replace into " + tableName));
        	upsertElementList.addAll(generateColumnElementList(columnMap,false));
        	upsertElementList.add(new TextElement("values"));
        	upsertElementList.addAll(generateColumnElementList(valuesMap,false));
        	
        	// 2.选择性更新插入
        	upsertSelectiveElementList.add(new TextElement("insert into " + tableName));
        	upsertSelectiveElementList.addAll(generateColumnElementList(columnMap,true));
        	upsertSelectiveElementList.add(new TextElement("values"));
        	upsertSelectiveElementList.addAll(generateColumnElementList(valuesMap,true));
        	upsertSelectiveElementList.add(new TextElement("on duplicate key update"));
        	upsertSelectiveElementList.add(generatorUpdates(columnMap,primaryKeyColumnNames));
        	
        	// 3.批量更新插入
        	batchUpsertElementList.add(new TextElement("replace into " + tableName));
        	batchUpsertElementList.addAll(generateColumnElementList(columnMap,false));
        	batchUpsertElementList.add(new TextElement("values"));
        	List<Element> ceList = generateColumnElementList(itemValuesMap,false);
        	XmlElement buildForeachElement = buildForeachElement();
        	for (Element element : ceList) {
        		buildForeachElement.addElement(element);
			}
        	batchUpsertElementList.add(buildForeachElement);
        }
        
        // 更新插入
        addUpsertElement(document, introspectedTable, METHOD_UPSERT, upsertElementList);
        // 选择性更新插入
        addUpsertElement(document, introspectedTable, METHOD_UPSERT_SELECTIVE, upsertSelectiveElementList);
        // 批量更新插入
        addBatchUpsertElement(document, introspectedTable, METHOD_BATCH_UPSERT, batchUpsertElementList);
		return true;
	}

	private Element generatorUpdates(Map<String, String> columnMap,List<String> primaryKeyColumnNames) {
		//找到非主键Map
		LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap<String, String>();
		for (Entry<String,String> column: columnMap.entrySet()) {
			if(primaryKeyColumnNames.contains(column.getKey())) {
				continue;
			}
			linkedHashMap.put(column.getKey(), column.getValue());
		}
		XmlElement trimXmlElement = new XmlElement("trim");
		trimXmlElement.addAttribute(new Attribute("suffixOverrides", ","));
		for(Entry<String,String> columnEntry : linkedHashMap.entrySet()) {
			String key = columnEntry.getKey();
			XmlElement xmlElement = new XmlElement("if");
			xmlElement.addAttribute(new Attribute("test", columnEntry.getValue() + " != null"));
			xmlElement.addElement(new TextElement(key + "=value(" + key + "),"));
			trimXmlElement.addElement(xmlElement);
		}
		return trimXmlElement;
	}

	/**
	 * 
	 * @param columns					全字段集合
	 * @param tableName					表名
	 * @param primaryKeyColumnNames		主键集合
	 * @param columnMap					字段Map<字段Str,属性Str>
	 * @param valuesMap					赋值Map<赋值Str,属性Str>
	 * @param selective					是否选择性
	 * @param batch						是否批量
	 * @return	生成的Xml节点集合
	 */
	private List<Element> generateXmlUpsertOracle(List<IntrospectedColumn> columns, String tableName,
			List<String> primaryKeyColumnNames, Map<String, String> columnMap, Map<String, String> valuesMap, boolean selective, boolean batch) {
		
		List<Element> upsertElementList = new ArrayList<Element>();
		
		upsertElementList.add(new TextElement("merge into " + tableName + " t1"));
		upsertElementList.add(new TextElement("using"));
		upsertElementList.addAll(generateUsingElement(columns, selective, batch));
		upsertElementList.add(generatePrimaryKeyOnElement(primaryKeyColumnNames));
		upsertElementList.add(new TextElement("when matched then"));
		upsertElementList.add(new TextElement("update set"));
		//非主键更新赋值
		upsertElementList.add(generateColumnUpdateElement(columnMap,primaryKeyColumnNames,selective));
		upsertElementList.add(new TextElement("when not matched then"));
		upsertElementList.add(new TextElement("insert"));
		// 选中字段
		upsertElementList.addAll(generateColumnElementList(columnMap, selective));
		upsertElementList.add(new TextElement("values"));
		// 赋值选中字段
		upsertElementList.addAll(generateColumnElementList(valuesMap, selective));
		
		return upsertElementList;
	}
	private XmlElement buildForeachElement() {
		XmlElement foreachElement = new XmlElement("foreach");
        foreachElement.addAttribute(new Attribute("collection", "list"));
        foreachElement.addAttribute(new Attribute("item", "item"));
        foreachElement.addAttribute(new Attribute("separator", ","));
        return foreachElement;
	}
	
	private void addBatchUpsertElement(Document document, IntrospectedTable introspectedTable, String method, List<Element> upsertElementList) {
		XmlElement batchUpsertElt = new XmlElement("insert");
		batchUpsertElt.addAttribute(new Attribute("id", method));// 方法名
		batchUpsertElt.addAttribute(new Attribute("parameterType", "list"));// 参数类型
 		
        for (Element element : upsertElementList) {
        	batchUpsertElt.addElement(element);
		}
        
        if (context.getPlugins().sqlMapInsertElementGenerated(batchUpsertElt, introspectedTable)) {
            document.getRootElement().addElement(batchUpsertElt);
        }
	}
	
	private Element generateColumnUpdateElement(Map<String, String> columnMap,
			List<String> primaryKeyColumnNames, boolean selective) {
		
		// 不修改原集合, 创建非主键字段Map
		Map<String, String> linkedHashMap = new LinkedHashMap<String, String>();
		for (Entry<String, String> column : columnMap.entrySet()) {
			if(!primaryKeyColumnNames.contains(column.getKey()))
				linkedHashMap.put(column.getKey(), column.getValue());
		}
		
		XmlElement trimXmlElement = new XmlElement("trim");
		trimXmlElement.addAttribute(new Attribute("suffixOverrides", ","));
		for (Entry<String,String> columnEntry : linkedHashMap.entrySet()) {
			StringBuilder sb = new StringBuilder();
			sb.append("t1.");
			sb.append(columnEntry.getKey());
			sb.append("=t2.");
			sb.append(columnEntry.getKey());
			sb.append(", ");
			Element element = new TextElement(sb.toString());
			if(selective) {// 添加if标签
				XmlElement xmlElement = new XmlElement("if");
				xmlElement.addAttribute(new Attribute("test", columnEntry.getValue() + " != null"));
				xmlElement.addElement(element);
				element = xmlElement;
			}
			trimXmlElement.addElement(element);
		}
		return trimXmlElement;
	}

	private Element generatePrimaryKeyOnElement(List<String> primaryKeyColumnNames) {
		StringBuilder sb = new StringBuilder();
		sb.append("on (");
		for (String primaryKey : primaryKeyColumnNames) {
			sb.append("t1.");
			sb.append(primaryKey);
			sb.append(" = ");
			sb.append("t2.");
			sb.append(primaryKey);
			
			if(primaryKeyColumnNames.indexOf(primaryKey) < primaryKeyColumnNames.size() - 1) {
				sb.append(" and ");
			}
		}
		sb.append(")");
		return new TextElement(sb.toString());
	}

	private List<Element> generateUsingElement(List<IntrospectedColumn> columns, boolean selective ,boolean batch) {
		List<Element> list = new ArrayList<Element>();
		list.add(new TextElement("("));
		List<Element> columnElementList = new ArrayList<Element>();
		
		columnElementList.add(new TextElement("select "));
		XmlElement trimXmlElement = new XmlElement("trim");
		trimXmlElement.addAttribute(new Attribute("suffixOverrides", ","));
		for (IntrospectedColumn column : columns) {
			
			String columnStr = batch?MyBatis3FormattingUtilities.getParameterClause(column,"item."):MyBatis3FormattingUtilities.getParameterClause(column);
			columnStr += " ";
			columnStr += MyBatis3FormattingUtilities.getEscapedColumnName(column);
			columnStr += ",";
			
			if(selective) {
				String property =  batch?"item." + column.getJavaProperty():column.getJavaProperty();
				XmlElement xmlElement = new XmlElement("if");
				xmlElement.addAttribute(new Attribute("test", property + " != null"));
				xmlElement.addElement(new TextElement(columnStr));
				trimXmlElement.addElement(xmlElement);
			}else {
				trimXmlElement.addElement(new TextElement(columnStr));
			}
		}
		columnElementList.add(trimXmlElement);
		columnElementList.add(new TextElement("from dual"));
		
		if(batch) {
			XmlElement foreachElement = new XmlElement("foreach");
	        foreachElement.addAttribute(new Attribute("collection", "list"));
	        foreachElement.addAttribute(new Attribute("item", "item"));
	        foreachElement.addAttribute(new Attribute("separator", "union"));
	        for (Element element : columnElementList) {
	        	foreachElement.addElement(element);
			}
	        list.add(foreachElement);
		}else {
			list.addAll(columnElementList);
		}
		
		list.add(new TextElement(") t2"));
		return list;
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

	private Method buildMethod(String methodName, IntrospectedTable introspectedTable) {
		Method method = new Method(methodName);
		method.setReturnType(FullyQualifiedJavaType.getIntInstance());
		method.addParameter(new Parameter(introspectedTable.getRules().calculateAllFieldsClass(), "record"));
		return method;
	}
	
	private void addUpsertElement(Document document, IntrospectedTable introspectedTable, String method, List<Element> upsertElementList) {
		XmlElement upsertElt = new XmlElement("insert");
		upsertElt.addAttribute(new Attribute("id", method));// 方法名
 		
        for (Element element : upsertElementList) {
        	upsertElt.addElement(element);
		}
        document.getRootElement().addElement(upsertElt);
	}
	
	private Method buildListMethod(String methodName, FullyQualifiedJavaType fullyQualifiedJavaType) {
		Method method = new Method(methodName);
		method.setReturnType(FullyQualifiedJavaType.getIntInstance());
		method.addParameter(new Parameter(fullyQualifiedJavaType, "list", "@Param(\"list\")"));
		return method;
	}

}
