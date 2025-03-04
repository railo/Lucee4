/**
 *
 * Copyright (c) 2014, the Railo Company Ltd. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either 
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public 
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 * 
 **/
package lucee.runtime.tag;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import lucee.commons.db.DBUtil;
import lucee.commons.io.log.Log;
import lucee.commons.io.log.LogUtil;
import lucee.commons.lang.StringUtil;
import lucee.runtime.PageContext;
import lucee.runtime.config.ConfigImpl;
import lucee.runtime.config.ConfigWebImpl;
import lucee.runtime.config.Constants;
import lucee.runtime.db.DataSource;
import lucee.runtime.db.DataSourceManager;
import lucee.runtime.db.DatasourceConnection;
import lucee.runtime.db.SQL;
import lucee.runtime.db.SQLImpl;
import lucee.runtime.db.SQLItem;
import lucee.runtime.db.SQLItemImpl;
import lucee.runtime.debug.DebuggerPro;
import lucee.runtime.debug.DebuggerUtil;
import lucee.runtime.exp.ApplicationException;
import lucee.runtime.exp.PageException;
import lucee.runtime.ext.tag.TagImpl;
import lucee.runtime.functions.displayFormatting.DecimalFormat;
import lucee.runtime.listener.ApplicationContextPro;
import lucee.runtime.op.Caster;
import lucee.runtime.type.QueryImpl;
import lucee.runtime.type.Struct;
import lucee.runtime.type.StructImpl;
import lucee.runtime.type.scope.Form;
import lucee.runtime.type.util.CollectionUtil;
import lucee.runtime.type.util.ListUtil;

/**
* Inserts records in data sources.
*
*
*
**/
public final class Insert extends TagImpl {

	/** If specified, password overrides the password value specified in the ODBC setup. */
	private String password;

	/** Name of the data source that contains your table. */
	private String datasource;

	/** If specified, username overrides the username value specified in the ODBC setup. */
	private String username;

	/** A comma-separated list of form fields to insert. If this attribute is not specified, all 
	** 	fields in the form are included in the operation. */
	private String formfields;

	/** For data sources that support table ownership such as SQL Server, Oracle, and Sybase SQL 
	** 	Anywhere, use this field to specify the owner of the table. */
	private String tableowner="";

	/** Name of the table you want the form fields inserted in. */
	private String tablename;

	/** For data sources that support table qualifiers, use this field to specify the qualifier for the 
	** 	table. The purpose of table qualifiers varies across drivers. For SQL Server and Oracle, the qualifier 
	** 	refers to the name of the database that contains the table. For the Intersolv dBase driver, the 
	** 	qualifier refers to the directory where the DBF files are located. */
	private String tablequalifier="";

	@Override
	public void release()	{
		super.release();
		password=null;
		username=null;
		formfields=null;
		tableowner="";
		tablequalifier="";
		datasource=null;
	}

	/** set the value password
	*  If specified, password overrides the password value specified in the ODBC setup.
	* @param password value to set
	**/
	public void setPassword(String password)	{
		this.password=password;
	}

	/** set the value datasource
	*  Name of the data source that contains your table.
	* @param datasource value to set
	**/
	public void setDatasource(String datasource)	{
		this.datasource=datasource;
	}

	/** set the value username
	*  If specified, username overrides the username value specified in the ODBC setup.
	* @param username value to set
	**/
	public void setUsername(String username)	{
		this.username=username;
	}

	/** set the value formfields
	*  A comma-separated list of form fields to insert. If this attribute is not specified, all 
	* 	fields in the form are included in the operation.
	* @param formfields value to set
	**/
	public void setFormfields(String formfields)	{
		this.formfields=formfields.toLowerCase().trim();
	}

	/** set the value tableowner
	*  For data sources that support table ownership such as SQL Server, Oracle, and Sybase SQL 
	* 	Anywhere, use this field to specify the owner of the table.
	* @param tableowner value to set
	**/
	public void setTableowner(String tableowner)	{
		this.tableowner=tableowner;
	}

	/** set the value tablename
	*  Name of the table you want the form fields inserted in.
	* @param tablename value to set
	**/
	public void setTablename(String tablename)	{
		this.tablename=tablename;
	}

	/** set the value tablequalifier
	*  For data sources that support table qualifiers, use this field to specify the qualifier for the 
	* 	table. The purpose of table qualifiers varies across drivers. For SQL Server and Oracle, the qualifier 
	* 	refers to the name of the database that contains the table. For the Intersolv dBase driver, the 
	* 	qualifier refers to the directory where the DBF files are located.
	* @param tablequalifier value to set
	**/
	public void setTablequalifier(String tablequalifier)	{
		this.tablequalifier=tablequalifier;
	}


	@Override
	public int doStartTag()	{
		return SKIP_BODY;
	}

	@Override
	public int doEndTag() throws PageException	{
		Object ds=getDatasource(pageContext,datasource);
		
		
		
		DataSourceManager manager = pageContext.getDataSourceManager();
	    DatasourceConnection dc=ds instanceof DataSource?
	    		manager.getConnection(pageContext,(DataSource)ds,username,password):
	    		manager.getConnection(pageContext,Caster.toString(ds),username,password);
	    try {
	    	
	    	Struct meta =null;
	    	try {
	    		meta=getMeta(dc,tablequalifier,tableowner,tablename);
	    	}
	    	catch(SQLException se){
	    		meta=new StructImpl();
	    	}
		    
	    	
	    	SQL sql=createSQL(meta);
			if(sql!=null) {
				lucee.runtime.type.Query query = new QueryImpl(pageContext,dc,sql,-1,-1,-1,"query");
				
				if(pageContext.getConfig().debug()) {
					String dsn=ds instanceof DataSource?((DataSource)ds).getName():Caster.toString(ds);
					boolean logdb=((ConfigImpl)pageContext.getConfig()).hasDebugOptions(ConfigImpl.DEBUG_DATABASE);
					if(logdb) {
						boolean debugUsage=DebuggerUtil.debugQueryUsage(pageContext,query);
						((DebuggerPro)pageContext.getDebugger()).addQuery(debugUsage?query:null,dsn,"",sql,query.getRecordcount(),pageContext.getCurrentPageSource(),query.getExecutionTime());
					}
				}
				// log
				Log log = ((ConfigWebImpl)pageContext.getConfig()).getLog("datasource", true);
				if(log.getLogLevel()>=Log.LEVEL_INFO) {
					log.info("insert tag", "executed ["+sql.toString().trim()+"] in "+DecimalFormat.call(pageContext, query.getExecutionTime()/1000000D)+" ms");
				}
			    
			}
			return EVAL_PAGE;
	    }
		catch (PageException pe) {
			// log
			LogUtil.log(((ConfigWebImpl)pageContext.getConfig()).getLog("datasource", true)
					, Log.LEVEL_ERROR, "insert tag", pe);		
			throw pe;
		}
	    finally {
	    	manager.releaseConnection(pageContext,dc);
	    }
	}

	
	

	public static Object getDatasource(PageContext pageContext, String datasource) throws ApplicationException {
		if(StringUtil.isEmpty(datasource)){
			Object ds = ((ApplicationContextPro)pageContext.getApplicationContext()).getDefDataSource();

			if(StringUtil.isEmpty(ds))
				throw new ApplicationException(
						"attribute [datasource] is required, when no default datasource is defined",
						"you can define a default datasource as attribute [defaultdatasource] of the tag "+Constants.CFAPP_NAME+" or as data member of the "+Constants.APP_CFC+" (this.defaultdatasource=\"mydatasource\";)");
			return ds;
		}
		return datasource;
	}

	public static Struct getMeta(DatasourceConnection dc,String tableQualifier, String tableOwner, String tableName) throws SQLException {
    	DatabaseMetaData md = dc.getConnection().getMetaData();
    	Struct  sct=new StructImpl();
		ResultSet columns = md.getColumns(tableQualifier, tableOwner, tableName, null);
		
		try{
			String name;
			while(columns.next()) {
				name=columns.getString("COLUMN_NAME");
				sct.setEL(name, new ColumnInfo(name,columns.getInt("DATA_TYPE"),columns.getBoolean("IS_NULLABLE")));
				
			}
		}
		finally {
			DBUtil.closeEL(columns);
		}
		return sct;
	}

	/**
     * @param meta 
	 * @return return SQL String for insert
     * @throws PageException
     */
    private SQL createSQL(Struct meta) throws PageException {
        String[] fields=null; 
        Form form = pageContext.formScope();
        if(formfields!=null) fields=ListUtil.toStringArray(ListUtil.listToArrayRemoveEmpty(formfields,','));
        else fields=CollectionUtil.keysAsString(pageContext.formScope());
        
        StringBuffer names=new StringBuffer();
        StringBuffer values=new StringBuffer();
        ArrayList items=new ArrayList();
        String field;
        for(int i=0;i<fields.length;i++) {
            field = StringUtil.trim(fields[i],null);
            if(StringUtil.startsWithIgnoreCase(field, "form."))
            	field=field.substring(5);
            
            if(!field.equalsIgnoreCase("fieldnames")) {
                if(names.length()>0) {
                    names.append(',');
                    values.append(',');
                }
                names.append(field);
                values.append('?');
                ColumnInfo ci=(ColumnInfo) meta.get(field,null);
                if(ci!=null)items.add(new SQLItemImpl(form.get(field,null),ci.getType())); 
                else items.add(new SQLItemImpl(form.get(field,null))); 
            }
        }
        if(items.size()==0) return null;
        
        StringBuffer sql=new StringBuffer();
        sql.append("insert into ");
        if(tablequalifier.length()>0) {
            sql.append(tablequalifier);
            sql.append('.');
        }
        if(tableowner.length()>0) {
            sql.append(tableowner);
            sql.append('.');
        }
        sql.append(tablename);
        sql.append('(');
        sql.append(names);
        sql.append(")values(");
        sql.append(values);
        sql.append(")");
        
        return new SQLImpl(sql.toString(),(SQLItem[])items.toArray(new SQLItem[items.size()]));
    }
    
    
    
    
    
    
    
    
    
}

class    ColumnInfo {

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the type
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return the nullable
	 */
	public boolean isNullable() {
		return nullable;
	}

	private String name;
	private int type;
	private boolean nullable;

	public ColumnInfo(String name, int type, boolean nullable) {
		this.name=name;
		this.type=type;
		this.nullable=nullable;
	}
	
	@Override
	public String toString(){
		return name+"-"+type+"-"+nullable;
	}
	
}