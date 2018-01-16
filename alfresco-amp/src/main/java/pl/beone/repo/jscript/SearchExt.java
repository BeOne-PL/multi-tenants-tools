package pl.beone.repo.jscript;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.domain.tenant.TenantAdminDAO;
import org.alfresco.repo.domain.tenant.TenantEntity;
import org.alfresco.repo.jscript.ScriptFacetResult;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.ScriptSpellCheckResult;
import org.alfresco.repo.jscript.Search;
import org.alfresco.repo.jscript.Search.SortColumn;
import org.alfresco.repo.search.impl.solr.facet.handler.FacetLabel;
import org.alfresco.repo.search.impl.solr.facet.handler.FacetLabelDisplayHandler;
import org.alfresco.repo.search.impl.solr.facet.handler.FacetLabelDisplayHandlerRegistry;
import org.alfresco.repo.tenant.TenantUtil;
import org.alfresco.repo.tenant.TenantUtil.TenantRunAsWork;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.search.SpellCheckResult;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class SearchExt extends Search {

    private static Log logger = LogFactory.getLog(SearchExt.class);

    protected TenantAdminDAO tenantAdminDAO;
    
    /**
     * Execute the query
     * 
     * Removes any duplicates that may be present (ID search can cause duplicates -
     * it is better to remove them here)
     * 
     * @param sp                SearchParameters describing the search to execute.
     * @param exceptionOnError  True to throw a runtime exception on error, false to return empty resultset
     * 
     * @return Pair containing Object[] of Node objects, and the ResultSet metadata hash.
     */
    protected Pair<Object[], Map<String,Object>> queryResultMetaTenant(final SearchParameters sp, final boolean exceptionOnError)
    {
        final Map<String, Object> meta = new HashMap<>(8);
        final Collection<ScriptNode> set = new LinkedHashSet<ScriptNode>();
        
        long time = 0L;
        if (logger.isDebugEnabled())
        {
           logger.debug("query=" + sp.getQuery() + " limit=" + (sp.getLimitBy() != LimitBy.UNLIMITED ? sp.getLimit() : "none"));
           time = System.currentTimeMillis();
        }
        
        // perform the search against the repo
		List<TenantEntity> tenants = tenantAdminDAO.listTenants(true);
		
        final Map<String, List<ScriptFacetResult>> facetMeta = new HashMap<>();
        
		for (final TenantEntity tenant:tenants) {

			Collection<ScriptNode> setpart = TenantUtil.runAsUserTenant(new TenantRunAsWork<Collection<ScriptNode>>()
			{
				@Override
				public Collection<ScriptNode> doWork() throws Exception
				{
			        ResultSet results = null;
			
			        try
			        {
			            results = services.getSearchService().query(sp);
			            
			            // results nodes
			            if (results.length() != 0)
			            {
			                NodeService nodeService = services.getNodeService();
			                for (ResultSetRow row: results)
			                {
			                    NodeRef nodeRef = row.getNodeRef();
			                    if (nodeService.exists(nodeRef))
			                    {
			                    	TenantNode tmpScriptNode = new TenantNode(nodeRef, services, getScope());
			                    	tmpScriptNode.setTenant(tenant.getTenantDomain());
			                       set.add(tmpScriptNode);
			                    }
			                }
			            }
			            // results metadata
			            meta.put("numberFound", meta.get("numberFound")==null ? results.getNumberFound() : ((Long)meta.get("numberFound") +  results.getNumberFound()));
			            meta.put("hasMore", meta.get("hasMore") == null ? results.hasMore() : ((boolean)meta.get("hasMore") || results.hasMore()));
			            // results facets
			            FacetLabelDisplayHandlerRegistry facetLabelDisplayHandlerRegistry = services.getFacetLabelDisplayHandlerRegistry();
			            for (FieldFacet ff: sp.getFieldFacets())
			            {
			                // for each field facet, get the facet results
			                List<Pair<String, Integer>> fs = results.getFieldFacet(ff.getField());
			                List<ScriptFacetResult> facets = new ArrayList<>();
			                for (Pair<String, Integer> f : fs)
			                {
			                    // ignore zero hit fields
			                    if (f.getSecond() > 0)
			                    {
			                        String facetValue = f.getFirst();
			                        FacetLabelDisplayHandler handler = facetLabelDisplayHandlerRegistry.getDisplayHandler(ff.getField());
			                        String label = (handler == null) ? facetValue : handler.getDisplayLabel(facetValue).getLabel();
			                        
			                        facets.add(new ScriptFacetResult(facetValue, label, -1, f.getSecond()));
			                    }
			                }
			                // store facet results per field
			                facetMeta.put(ff.getField(), facets);
			            } 
			            
			            // Start of bucketing
			            // ACE-1615: Populate the facetMeta map with empty lists. If there is a
			            // facet query with >0 hits, the relevant list will be populated
			            // with the results, otherwise the list remains empty.
			            for(String bucketedField : services.getSolrFacetHelper().getBucketedFieldFacets())
			            {
			                facetMeta.put(bucketedField, new ArrayList<ScriptFacetResult>());
			            }
			            Set<Entry<String, Integer>> facetQueries = results.getFacetQueries().entrySet();
			            for(Entry<String, Integer> entry : facetQueries)
			            {
			                // ignore zero hit facet queries
			                if (entry.getValue() > 0)
			                {
			                    String key = entry.getKey();
			                    // for example the key could be: {!afts}@{http://www.alfresco.org/model/content/1.0}created:[NOW/DAY-1DAY TO NOW/DAY+1DAY]
			                    // qName => @{http://www.alfresco.org/model/content/1.0}created
			                    // 7 => {!afts}
			                    key = key.substring(7);
			                    String qName = key.substring(0, key.lastIndexOf(':'));
			
			                    // Retrieve the previous facet queries
			                    List<ScriptFacetResult> fqs = facetMeta.get(qName);
			                    if (fqs == null)
			                    {
			                        fqs = new ArrayList<>();
			                        logger.info("Field facet [" + key + "] has not been registered.");
			                    }
			                    // Get the handler for this qName
			                    FacetLabelDisplayHandler handler = facetLabelDisplayHandlerRegistry.getDisplayHandler(qName);
			                    FacetLabel facetLabel = (handler == null) ? new FacetLabel(key, key, -1) : handler.getDisplayLabel(key);
			
			                    fqs.add(new ScriptFacetResult(facetLabel.getValue(), facetLabel.getLabel(), facetLabel.getLabelIndex(), entry.getValue()));
			                }
			            }// End of bucketing
			            meta.put("facets", facetMeta);
			            SpellCheckResult spellCheckResult = results.getSpellCheckResult();
			            meta.put("spellcheck", new ScriptSpellCheckResult(
			                                    sp.getSearchTerm(),
			                                    spellCheckResult.getResultName(),
			                                    spellCheckResult.isSearchedFor(),
			                                    spellCheckResult.getResults(),
			                                    spellCheckResult.isSpellCheckExist()));
			        }
			        catch (Throwable err)
			        {
			            if (exceptionOnError)
			            {
			                throw new AlfrescoRuntimeException("Failed to execute search: " + sp.getQuery(), err);
			            }
			            else
			            {
			                if (logger.isDebugEnabled())
			                    logger.debug("Failed to execute search: " + sp.getQuery(), err);
			                // put expected values to handle case where exception occurs in search
			                meta.put("numberFound", 0);
			                meta.put("hasMore", false);
			            }
			        }
			        finally
			        {
			            if (results != null)
			            {
			                results.close();
			            }
			        }
					return null;
        
				}
			}, services.getAuthenticationService().getCurrentUserName(), tenant.getTenantDomain());
            if (logger.isDebugEnabled()) {
                logger.debug("query time: " + (System.currentTimeMillis()-time) + "ms");
                
                logger.debug("USER: " + services.getAuthenticationService().getCurrentUserName());
                logger.debug("TENANT: " + tenant.getTenantDomain());
                
            }
		}
    	
        
        Object[] res = set != null ? set.toArray(new Object[(set.size())]) : new Object[0];
        return new Pair<Object[], Map<String,Object>>(res, meta);
    }
	 
    
    public Scriptable tenantSearch(String search)
    {

        if (search != null && search.length() != 0)
        {
            SearchParameters sp = new SearchParameters();
            sp.addStore(this.storeRef);
            sp.setLanguage(SearchService.LANGUAGE_LUCENE);
            sp.setQuery(search);
        	
        	Object[] resToProcess = queryResultMetaTenant(sp, true).getFirst();
            
            return Context.getCurrentContext().newArray(getScope(), resToProcess);
            
        }
        else
        {
            return Context.getCurrentContext().newArray(getScope(), 0);
        }
    	
    }
    
	public TenantAdminDAO getTenantAdminDAO() {
		return tenantAdminDAO;
	}

	public void setTenantAdminDAO(TenantAdminDAO tenantAdminDAO) {
		this.tenantAdminDAO = tenantAdminDAO;
	}
    
}
