package org.collectionspace.chain.csp.webui.main;
import org.apache.commons.lang.StringUtils;
import org.collectionspace.csp.api.config.ConfigException;
import org.collectionspace.csp.api.config.ConfigRoot;
import org.collectionspace.csp.api.persistence.ExistException;
import org.collectionspace.csp.api.persistence.Storage;
import org.collectionspace.csp.api.persistence.UnderlyingStorageException;
import org.collectionspace.csp.api.persistence.UnimplementedException;
import org.collectionspace.csp.api.ui.UIException;
import org.collectionspace.csp.api.ui.UIRequest;


public class WebDelete implements WebMethod {
	private String base;
	
	public WebDelete(String base) { this.base=base; }

	private void store_delete(Storage storage,UIRequest request,String path) throws UIException {
		try {
			storage.deleteJSON(base+"/"+path);
		} catch (ExistException e) {
			throw new UIException("JSON Not found "+e,e);
		} catch (UnimplementedException e) {
			throw new UIException("Unimplemented",e);
		} catch (UnderlyingStorageException e) {
			throw new UIException("Problem getting",e);
		}
	}
	
	public void configure(ConfigRoot config) throws ConfigException {}

	public void run(Object in, String[] tail) throws UIException {
		Request q=(Request)in;
		store_delete(q.getStorage(),q.getUIRequest(),StringUtils.join(tail,"/"));
	}
}
