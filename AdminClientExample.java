import java.util.Properties;
import java.util.Date;
import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;


import javax.management.ObjectName;

import com.ibm.websphere.management.AdminClient;
import com.ibm.websphere.management.AdminClientFactory;
import com.ibm.websphere.management.exception.ConnectorException;

import com.ibm.websphere.pmi.*;
import com.ibm.websphere.pmi.client.*;
import com.ibm.websphere.pmi.stat.*;

public class AdminClientExample {
        private AdminClient adminClient;
        private ObjectName queryName;
        private Set<ObjectName> perfOns = new HashSet<ObjectName>();
        private static String username;
        private static String passwd;
        private static String host;
        private static String port;
        private static String serverName;
        
 public static void main(String[] args){
                //username = args[0];
                //passwd = args[1];
                //host = args[2];
                //port = args[3];
   if(args.length > 0){
    serverName = args[0];   
   }          
   AdminClientExample ace = new AdminClientExample();   
   ace.createAdminClient();
   if(serverName != null){
   	ace.getMBean(serverName);
   }
   else{ 
    ace.getMBean();
   }
   ace.getStats();
 }
 private void createAdminClient(){
   String wasPath = "/usr/websphere/AppServer/profiles/FS_DM_01";
   String trustStore = "DummyClientTrustFile.jks";
   String keyStore = "DummyClientKeyFile.jks";
   String trustStorePw = "WebAS";
   String keyStorePw = "WebAS";
   Properties connectProps = new Properties();
   connectProps.setProperty(AdminClient.CONNECTOR_TYPE,AdminClient.CONNECTOR_TYPE_SOAP);
   connectProps.setProperty(AdminClient.CONNECTOR_SECURITY_ENABLED,"true");
   connectProps.setProperty(AdminClient.CONNECTOR_AUTO_ACCEPT_SIGNER,"true");
   connectProps.setProperty(AdminClient.CACHE_DISABLED,"false");
   connectProps.setProperty("javax.net.ssl.trustStore", wasPath + "/etc/DummyClientTrustFile.jks");
   connectProps.setProperty("javax.net.ssl.keyStore", wasPath+"/etc/DummyClientKeyFile.jks");
   connectProps.setProperty("javax.net.ssl.trustStorePassword",trustStorePw);
   connectProps.setProperty("javax.net.ssl.keyStorePassword",keyStorePw);
   connectProps.setProperty(AdminClient.CONNECTOR_HOST,"localhost");
   connectProps.setProperty(AdminClient.CONNECTOR_PORT,"8879");
   connectProps.setProperty(AdminClient.USERNAME,"wasadmin");
   connectProps.setProperty(AdminClient.PASSWORD,"Ngboss#0902");
   System.setProperty("was.install.root","/usr/websphere/AppServer");
   try{
    adminClient = AdminClientFactory.createAdminClient(connectProps);
   }
   catch(Exception e){
    e.printStackTrace();
   }
}
 private void getMBean(){
  String query = "WebSphere:type=Perf,*";
  Set mBeans = null;
  try{
    mBeans = adminClient.queryNames(new ObjectName(query),null);
  }catch(Exception e){
    e.printStackTrace();
  }
  Iterator mi = mBeans.iterator();
  while(mi.hasNext()){  
   try{
    ObjectName on = (ObjectName)mi.next();
    String proc = on.getKeyProperty("process");     
    if(proc !=null && proc.equals("nodeagent")){
      continue;
    }
    else{
      perfOns.add(on);
    }
   }
   catch(Exception e){
    e.printStackTrace();
   }
  }
 }
 private void getMBean(String objectName){
  String query = "WebSphere:process=" + objectName +",type=Perf,*";
  Set mBeans = null;
  try{
    mBeans = adminClient.queryNames(new ObjectName(query),null);
  }catch(Exception e){
    e.printStackTrace();
  }
  Iterator mi = mBeans.iterator();
  while(mi.hasNext()){  
   try{
    ObjectName on = (ObjectName)mi.next();
    String proc = on.getKeyProperty("process");     
    if(proc !=null && proc.equals("nodeagent")){
      continue;
    }
    else{
      perfOns.add(on);
    }
   }
   catch(Exception e){
    e.printStackTrace();
   }
  }
 }
 private void getStats(){
 	Iterator pi = perfOns.iterator();
 	while(pi.hasNext()){
  try{
   ObjectName perfOn = (ObjectName)pi.next();
   String serverName = perfOn.getKeyProperty("process");
   String wsStatsName;	
   Object[] params;
   String[] signature;
   com.ibm.websphere.pmi.stat.WSStats[] wsStats;
   signature = new String[]{"[Lcom.ibm.websphere.pmi.stat.StatDescriptor;","java.lang.Boolean"};
   StatDescriptor webContainerPoolSD = new StatDescriptor(new String[] {WSThreadPoolStats.NAME, "WebContainer"});
   StatDescriptor jvmSD = new StatDescriptor(new String[] {WSJVMStats.NAME});
   StatDescriptor jdbcSD = new StatDescriptor(new String[] {WSJDBCConnectionPoolStats.NAME,"Oracle JDBC Driver (XA)"});
   StatDescriptor servletSD = new StatDescriptor(new String[] {WSWebAppStats.NAME,"CRM_CRM_FS.war#CRM_CRM.war"});
   StatDescriptor jtaSD = new StatDescriptor(new String[] {WSJTAStats.NAME});
   params = new Object[] {new StatDescriptor[]{webContainerPoolSD,jvmSD,jdbcSD,servletSD,jtaSD}, new Boolean(true)};
   wsStats = (com.ibm.websphere.pmi.stat.WSStats[])adminClient.invoke(perfOn, "getStatsArray", params, signature); 
   
   System.out.println(serverName);
   for(int i=0;i<wsStats.length;i++){
   	    if(wsStats[i] == null){
   	     continue;
   	    }
   	    wsStatsName = wsStats[i].getName();
        if(wsStatsName.equals("WebContainer")){
         WSRangeStatistic poolSize = (WSRangeStatistic)wsStats[i].getStatistic(WSThreadPoolStats.PoolSize);
         WSRangeStatistic activeCount = (WSRangeStatistic)wsStats[i].getStatistic(WSThreadPoolStats.ActiveCount);
         System.out.println(wsStatsName);
         if(poolSize != null) System.out.println("PoolSize : " + poolSize.getCurrent());
         if(activeCount !=null) System.out.println("ActiveCount : " + activeCount.getCurrent());
        }
        if(wsStatsName.equals("jvmRuntimeModule")){
         WSRangeStatistic heapSize = (WSRangeStatistic)wsStats[i].getStatistic(WSJVMStats.HeapSize);
         WSCountStatistic usedMemory = (WSCountStatistic)wsStats[i].getStatistic(WSJVMStats.UsedMemory);
         System.out.println(wsStatsName);
         System.out.println("HeapSize : " + heapSize.getCurrent());
         System.out.println("UsedMemory : " + usedMemory.getCount());
        }
        if(wsStatsName.equals("Oracle JDBC Driver (XA)")){
         WSStats[] jdbcSubStats = wsStats[i].getSubStats();
         for(int j=0;j<jdbcSubStats.length;j++){
          String dsName = jdbcSubStats[j].getName();
          WSRangeStatistic poolSize = (WSRangeStatistic)jdbcSubStats[j].getStatistic(WSJDBCConnectionPoolStats.PoolSize);
          WSRangeStatistic freePoolSize = (WSRangeStatistic)jdbcSubStats[j].getStatistic(WSJDBCConnectionPoolStats.FreePoolSize);
          WSRangeStatistic percentUsed = (WSRangeStatistic)jdbcSubStats[j].getStatistic(WSJDBCConnectionPoolStats.PercentUsed);
          System.out.println("DataSource : " + dsName); 
          System.out.println("PoolSize : " + poolSize.getCurrent());
          System.out.println("FreePoolSize : " + freePoolSize.getCurrent());
          System.out.println("PercentUsed : " + percentUsed.getCurrent());
         }
        }
        if(wsStatsName.equals("CRM_CRM_FS.war#CRM_CRM.war")){
         WSStats servletSubStats = wsStats[i].getStats("webAppModule.servlets");
         WSCountStatistic totalRequests = (WSCountStatistic)servletSubStats.getStatistic(WSWebAppStats.ServletStats.RequestCount);
         WSTimeStatistic responseTime = (WSTimeStatistic)servletSubStats.getStatistic(WSWebAppStats.ServletStats.ServiceTime);
         System.out.println("WebAppName : " + wsStatsName);
         System.out.println("TotalRequest : " + totalRequests.getCount());
         System.out.println("ResponseTime : " + responseTime.getMean());
        }
        if(wsStatsName.equals("transactionModule")){
         System.out.println(wsStatsName);
         WSCountStatistic activeCount = (WSCountStatistic)wsStats[i].getStatistic(WSJTAStats.ActiveCount);
         WSCountStatistic committedCount = (WSCountStatistic)wsStats[i].getStatistic(WSJTAStats.CommittedCount);
         WSCountStatistic rolledbackCount = (WSCountStatistic)wsStats[i].getStatistic(WSJTAStats.RolledbackCount);
         System.out.println(activeCount.getName() + " : " + activeCount.getCount());
         System.out.println(committedCount.getName() + " : " + committedCount.getCount());
         System.out.println(rolledbackCount.getName() + " : " + rolledbackCount.getCount());
        }        
        //System.out.println(wsStats[i].toString());
   }
  }catch(Exception e){
   e.printStackTrace(); 
  }
 }
}
}