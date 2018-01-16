package pl.beone.repo.jscript;

import java.util.Map;

import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.tenant.TenantUtil;
import org.alfresco.repo.tenant.TenantUtil.TenantRunAsWork;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.mozilla.javascript.Scriptable;

public class TenantNode extends ScriptNode{

	private static final long serialVersionUID = 2145795242658806547L;

	public TenantNode(NodeRef nodeRef, ServiceRegistry services, Scriptable scope) {
		super(nodeRef, services, scope);
	}

	private String tenant;
    
	public void setTenant(String tenant2) {
		this.tenant=tenant2;
	}

    /**
     * @return Helper to return the 'name' property for the node
     */
    public String getTenant()
    {
        if (this.tenant == null)
        {
        	return "";
        }
        return this.tenant;
    }
    
    public Map<String, Object> getPropertiesTenant()
    {    	
    	Map<String, Object> retProps = TenantUtil.runAsUserTenant(new TenantRunAsWork<Map<String, Object>>()
		{
			@Override
			public Map<String, Object> doWork() throws Exception
			{
				return getProperties();
			}            
		        
		}, services.getAuthenticationService().getCurrentUserName(), tenant);
            
        
        return retProps;
    }
    

	

}
