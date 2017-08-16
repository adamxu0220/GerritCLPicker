package GerritCLPicker.GerritCLPicker;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Formatter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.servlet.http.HttpSession;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;
import org.kohsuke.stapler.bind.JavaScriptMethod;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;

import com.sonyericsson.hudson.plugins.gerrit.trigger.GerritServer;
import com.sonyericsson.hudson.plugins.gerrit.trigger.PluginImpl;
import com.sonyericsson.hudson.plugins.gerrit.trigger.config.IGerritHudsonTriggerConfig;
import com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.actions.manual.ManualTriggerAction;
import com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.actions.manual.ManualTriggerAction.Approval;
import com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.actions.manual.ManualTriggerAction.HighLow;
import com.sonymobile.tools.gerrit.gerritevents.GerritDefaultValues;
import com.sonymobile.tools.gerrit.gerritevents.GerritQueryException;
import com.sonymobile.tools.gerrit.gerritevents.GerritQueryHandler;
import com.sonymobile.tools.gerrit.gerritevents.dto.attr.Change;
import com.sonymobile.tools.gerrit.gerritevents.dto.attr.PatchSet;
import com.sonymobile.tools.gerrit.gerritevents.dto.attr.Provider;
import com.sonymobile.tools.gerrit.gerritevents.dto.events.PatchsetCreated;
import com.sonyericsson.hudson.plugins.gerrit.trigger.utils.StringUtil;
import com.sonyericsson.hudson.plugins.gerrit.trigger.hudsontrigger.GerritTriggerParameters;

import hudson.Extension;
import hudson.Util;
import hudson.model.Descriptor;
import hudson.model.ParameterDefinition;
import hudson.model.ParameterValue;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;



/**
 * @author hzxu
 *
 */
public class GerritCLPicker extends ParameterDefinition {

	private static final long serialVersionUID = 9032072543915872650L;

	private static final Logger logger = Logger.getLogger(GerritCLPicker.class.getName());
		
	
	static String sDisplayName = "CLs picker";
	private String queryString, selectedServer, owner, branch, project, changeIDs;
	private boolean allPatchSets;
	
	@Extension
	public static class DescriptorImpl extends ParameterDescriptor {
		@Override
		public String getDisplayName() {
			return sDisplayName;
		}	
		
		public DescriptorImpl() {
			logger.warning("in DescriptorImpl");
            load();
        }
		
		public boolean configure(org.kohsuke.stapler.StaplerRequest req,
                net.sf.json.JSONObject json)
         throws Descriptor.FormException{
			logger.warning("in configure");
			return false;
		}
                public FormValidation doCheckAllPatchSets(@QueryParameter final boolean allPatchSets) throws IOException {
                        logger.warning("in doCheckAllPatchSets");
                        return FormValidation.ok();
                }
                
		
		@Override
        public GerritCLPicker newInstance(StaplerRequest req,
                JSONObject formData)
                throws hudson.model.Descriptor.FormException
        {
			logger.warning("in newInstance");
			logger.warning(formData.toString());
            return new GerritCLPicker(
                    "CLSets",
                    formData.getString("queryString"),
                    null,//formData.getString("description")
                    formData.getString("selectedServer"),
                    formData.getString("project"),
                    formData.getString("branch"),
                    formData.getString("owner"), 
		    formData.getString("changeIDs"),
                    formData.getBoolean("allPatchSets")
            );
        }
		
	    /**
	     * Returns the list of servers allowed to be queried and manually triggered.
	     *
	     * @return the enabled server names
	     *
	     */
	    public ArrayList<String> getEnabledServers() {
	        ArrayList<String> enabledServers = new ArrayList<String>();
	        for (GerritServer s : PluginImpl.getServers_()) {
	            if (s.getConfig().isEnableManualTrigger()) {
	                enabledServers.add(s.getName());
                        logger.warning("add "+s.getName());
	            }
	        }
	        if (enabledServers.isEmpty()) {
	            logger.warning("No enabled server for manual triggering found.");
	        }
	        return enabledServers;
	    }

		
	}
	
	     /**
             * Returns the list of servers allowed to be queried and manually triggered.
             *
             * @return the enabled server names
             *
             */
            public ArrayList<String> getEnabledServers() {
                ArrayList<String> enabledServers = new ArrayList<String>();
                for (GerritServer s : PluginImpl.getServers_()) {
                    if (s.getConfig().isEnableManualTrigger()) {
                        enabledServers.add(s.getName());
                        logger.warning("add "+s.getName());
                    }
                }
                if (enabledServers.isEmpty()) {
                    logger.warning("No enabled server for manual triggering found.");
                }
                return enabledServers;
            }
	
	/**
         * @param name
	 * @param queryString
	 * @param description
         * @param selectedServer
         * @param project
         * @param branch
         * @param owner
	 * @param changeIDs
         * @param allPatchSets
	 */
	@DataBoundConstructor
	public GerritCLPicker(String name, 
				String queryString, 
				String description, 
				String selectedServer, 
				String project, 
				String branch, 
				String owner,
				String changeIDs, 
				boolean allPatchSets) {

		super(name, description);
                this.queryString = queryString;
		this.selectedServer = selectedServer;
                this.project = project;
                this.branch = branch;
                this.owner = owner;
		this.changeIDs = changeIDs;
		this.allPatchSets = allPatchSets;
		logger.warning("@@@@@@@@@in GerritCLPicker");
                logger.warning("getAllPatchSets:" + allPatchSets);
	}

	
	
	@Override
	public ParameterValue createValue(StaplerRequest request) {
		logger.warning("in createValue(StaplerRequest request)");
		String value[] = request.getParameterValues(getName());
		if(value == null) {
			return getDefaultParameterValue();
		}
		return null;
	}

	@Override
	public ParameterValue createValue(StaplerRequest request, JSONObject jO) {
		Object value = jO.get("value");
		String strValue = "";
		logger.warning("in createValue(StaplerRequest request, JSONObject jO)");
		logger.warning("jO:"+jO.toString()); 
                logger.warning("allPatchSets:"+allPatchSets);
                logger.warning("getQueryString:"+queryString);

                ArrayList selected_list = new ArrayList(); 
                //JSONObject JO_result = jO.getJSONObject("searchResultTable");
                Object selectedRow = jO.getJSONObject("searchResultTable").get("selectedRow");
                int i=0;
                if (selectedRow instanceof Boolean){
                    logger.warning("bool type");
		}
		else  if (selectedRow instanceof List){
                    logger.warning("List type");
                    JSONArray selectRows = jO.getJSONObject("searchResultTable").getJSONArray("selectedRow");
                    for (Object o: selectRows){
			//JSONObject tmp_jO = (JSONObject)o;
                        if (((Boolean)o).booleanValue()){ logger.warning("value: true");selected_list.add(i);}
                        else {logger.warning("value: false");}
                        i++;
                    }
                }
 


                //JSONObject newJO = new JSONObject();

                //jsonObject.put("name","");
                logger.warning("enabled index:"+selected_list.toString());
                JSONObject JO_result = jO.getJSONObject("searchResultTable");
                JSONObject JO_selected_result = new JSONObject();
                Iterator keys = JO_result.keys();
                while(keys.hasNext()) {
                    Object key_o = keys.next();
                    String key = key_o.toString();
                    logger.warning("find key:"+key);
                    if (selectedRow instanceof List) {
                        JSONArray JA_tmp = new JSONArray();
                        JSONArray JA_old = JO_result.getJSONArray(key);
                        for (i=0; i<selected_list.size(); i++){
                            int index = Integer.parseInt(selected_list.get(i).toString());
                            logger.warning(String.format("index:%d", index));
                            JA_tmp.add(JA_old.get(index));
                        }
                        JO_selected_result.element(key, JA_tmp);
                    }
                    else if (selectedRow.toString() == "true") {
                        JSONArray JA_tmp = new JSONArray();
                        JA_tmp.add(JO_result.get(key));
                        JO_selected_result.element(key, JA_tmp);
                    }
                }
                if (selectedRow instanceof List) {
                JO_selected_result.element("total", selected_list.size());
                }
                else  if (selectedRow.toString() == "true") {
                    JO_selected_result.element("total", 1);
                }
                logger.warning("jO:"+JO_selected_result.toString());
		return new FileSystemListParameterValue(getName(), JO_selected_result.toString());
	}

	@Override
	public ParameterValue getDefaultParameterValue() {
		//String defaultValue = "";
		logger.warning("in getDefaultParameterValue");	
		return super.getDefaultParameterValue();
	}

	
	
	

	public String getQueryString() {
                logger.warning("getQueryString:" + queryString);
		return queryString;
	}

	public boolean getAllPatchSets() {
                logger.warning("getAllPatchSets:" + allPatchSets);
		return allPatchSets;
	}
	
	public String getSelectedServer() {
		return selectedServer;
	}

	public String getProject() {
                return project;
        }
	public String getBranch() {
		return branch;
	}
	public String getOwner() {
		return owner;
	}
	public String getChangeIDs() {
		return changeIDs;
	}
	

    private static final String SESSION_RESULT = "result";
    private static final String SESSION_SEARCH_ERROR = "error_search";
    private static final String SESSION_BUILD_ERROR = "error_build";
    private static final String SESSION_TRIGGER_MONITOR = "trigger_monitor";

    public static final String ID_SEPARATOR = ":";
    private static final int MAX_SUBJECT_STR_LENGTH = 65;

    
    public List<JSONObject> GerritSearch() throws IOException {

        ////HttpSession session = request.getSession();
        // Create session if nothing.
        ////if (session == null) {
        ////    session = request.getSession(true);
        ////}
        ////session.setAttribute("allPatchSets", allPatchSets);
        ////session.setAttribute("selectedServer", selectedServer);
        ////if (!isServerEnabled(selectedServer)) {
        ////    response.sendRedirect2(".");
        ////    return;
        ////}
        //Jenkins jenkins = Jenkins.getInstance();
        //assert jenkins != null;
        //jenkins.checkPermission(PluginImpl.MANUAL_TRIGGER);
    	logger.warning(String.format("in GerritSearch, selectedServer:%s; queryString:%s, allPatchSets:%b", selectedServer, queryString, allPatchSets));
        IGerritHudsonTriggerConfig config = getServerConfig(selectedServer);

        if (config != null) {
            GerritQueryHandler handler = new GerritQueryHandler(config);
            ////clearSessionData(session);
            ////session.setAttribute("queryString", queryString);

            try {
                List<JSONObject> json = handler.queryJava(queryString, allPatchSets, true, false);
                if (!allPatchSets) {
                    int count = 0;
                    for (JSONObject j : json) {
                        if (j.containsKey("id")) {
                            JSONArray jsonArray = new JSONArray();
                            jsonArray.add(j.getJSONObject("currentPatchSet"));
                            j.put("patchSets", jsonArray);
                            count += 1;
                            if (count > 50) {break;}
                            //logger.warning(String.format("get id:%s; data:%s", j.getString("id"), jsonArray.toString()));
                        }
                        else {
                            //logger.warning(String.format("get id:null!; data:%s", j.toString()));
                        }
                    }
                }
                return json;
                ////session.setAttribute(SESSION_RESULT, json);
                //TODO Implement some smart default selection.
                //That can notice that a specific revision is searched or that there is only one result etc.
            } catch (GerritQueryException gqe) {
                logger.warning("Bad query " + gqe);
                ////session.setAttribute(SESSION_SEARCH_ERROR, gqe);
            } catch (Exception ex) {
                logger.warning(String.format("Could not query Gerrit for [%s] , exception:%s", queryString, ex.toString()));
                ////session.setAttribute(SESSION_SEARCH_ERROR, ex);
            }
            ////response.sendRedirect2(".");
        } else {
            logger.warning(String.format("Could not find config for the server %s", selectedServer));
        }
        return null;
    }
    


    /**
     * Check whether a server is allowed to be queried and manually triggered.
     *
     * @param serverName the name of the server selected in the dropdown.
     * @return true if server exists and manual trigger is enabled.
     * @see com.sonyericsson.hudson.plugins.gerrit.trigger.config.IGerritHudsonTriggerConfig#isEnableManualTrigger()
     */
    private boolean isServerEnabled(String serverName) {
        if (getServerConfig(serverName) != null) {
            return getServerConfig(serverName).isEnableManualTrigger();
        } else {
            return false;
        }
    }

    /**
     * Get the server config.
     *
     * @param serverName the name of the server.
     * @return the config of the server or null if config not found.
     */
    private IGerritHudsonTriggerConfig getServerConfig(String serverName) {
        GerritServer server = PluginImpl.getServer_(serverName);
        if (server != null) {
            IGerritHudsonTriggerConfig config = server.getConfig();
            if (config != null) {
                return config;
            } else {
                logger.warning(String.format("Could not find the config of server: %s", serverName));
            }
        } else {
            logger.warning(String.format("Could not find server %s", serverName));
        }
        return null;
    }

    /**
     * Clears the HTTP session from search and manual-trigger related data.
     *
     * @param session the HTTP session.
     */
    private void clearSessionData(HttpSession session) {
        session.removeAttribute(SESSION_SEARCH_ERROR);
        session.removeAttribute(SESSION_BUILD_ERROR);
        session.removeAttribute(SESSION_RESULT);
        session.removeAttribute(SESSION_TRIGGER_MONITOR);
    }

    //called from jelly
    public String getJsUrl(String jsName) {
        return StringUtil.getPluginJsUrl(jsName);
    }

    //called from jelly.
    public ParameterValue getChangeUrlParamForPatchSet(
            JSONObject jsonChange,
            JSONObject jsonPatchSet,
            String serverName) {
        List<ParameterValue> parameters = getParametersForPatchSet(jsonChange, jsonPatchSet, serverName);
        for (ParameterValue parameterValue : parameters) {
            if (hasUrl(parameterValue)) {
                return parameterValue;
            }
        }
        return null;
    }
    
  //called from jelly.
    public boolean hasUrl(ParameterValue parameterValue) {
        return GerritTriggerParameters.GERRIT_CHANGE_URL.name().equals(parameterValue.getName());
    }



    //called from jelly.
    public List<ParameterValue> getParametersForPatchSet(
            JSONObject jsonChange,
            JSONObject jsonPatchSet,
            String serverName) {
        List<ParameterValue> parameters = new LinkedList<ParameterValue>();
        Change change = new Change(jsonChange);
        PatchSet patchSet = new PatchSet(jsonPatchSet);
        PatchsetCreated event = new PatchsetCreated();
        Provider provider = createProviderFromGerritServer(serverName);
        event.setChange(change);
        event.setPatchset(patchSet);
        event.setProvider(provider);
        GerritTriggerParameters.setOrCreateParameters(event, parameters);
        return parameters;
    }
    

    public static Provider createProviderFromGerritServer(String serverName) {
        GerritServer server = PluginImpl.getServer_(serverName);
        if (server == null) {
            logger.warning("Could not find GerritServer: {}" + serverName);
        }
        return createProvider(server);
    }


    public static Provider createProvider(GerritServer server) {
        if (server != null) {
            return new Provider(
                    server.getName(),
                    server.getConfig().getGerritHostName(),
                    String.valueOf(server.getConfig().getGerritSshPort()),
                    GerritDefaultValues.DEFAULT_GERRIT_SCHEME,
                    server.getConfig().getGerritFrontEndUrl(),
                    server.getGerritVersion()
            );
        } else {
            return new Provider();
        }
    }
    
    /**
     * Generates a "unique" id for the change and/or patch.
     * So it can be identified as a single row in the search result.
     *
     * @param change the change.
     * @param patch  the patch-set in the change.
     * @return the generated id.
     */
    public String generateTheId(JSONObject change, JSONObject patch) {
        StringBuilder theId = new StringBuilder(change.getString("id"));
        if (patch != null) {
            theId.append(ID_SEPARATOR);
            theId.append(patch.getString("revision"));
        }
        theId.append(ID_SEPARATOR);
        theId.append(change.getString("number"));
        if (patch != null) {
            theId.append(ID_SEPARATOR);
            theId.append(patch.getString("number"));
        }
        return theId.toString();
    }


    //Called from jelly
    public String toReadableHtml(String subject) {
        if (subject != null && subject.length() > MAX_SUBJECT_STR_LENGTH) {
            subject = subject.substring(0, MAX_SUBJECT_STR_LENGTH);
        }
        if (subject != null) {
            return subject;
        } else {
            return "";
        }
    }

    /**
     * Finds the lowest and highest verified vote for the provided patch set.
     *
     * @param res the patch-set.
     * @return the highest and lowest verified vote.
     */
    @Deprecated
    public HighLow getVerified(JSONObject res) {
        return getVerified(res, 0);
    }

    /**
     * Finds the lowest and highest verified vote for the provided patch set.
     *
     * @param res the patch-set.
     * @param patchSetNumber the patch set number.
     * @return the highest and lowest verified vote.
     */
    public HighLow getVerified(JSONObject res, int patchSetNumber) {
        return Approval.VERIFIED.getApprovals(res, patchSetNumber);
    }

    /**
     * Finds the highest and lowest code review vote for the provided patch set.
     *
     * @param res the patch set.
     * @return the highest and lowest code review vote for the patch set.
     */
    @Deprecated
    public HighLow getCodeReview(JSONObject res) {
        return getCodeReview(res, 0);
    }

    /**
     * Finds the highest and lowest code review vote for the provided patch set.
     *
     * @param res the patch set.
     * @param patchSetNumber the patch set number.
     * @return the highest and lowest code review vote for the patch set.
     */
    public HighLow getCodeReview(JSONObject res, int patchSetNumber) {
        return Approval.CODE_REVIEW.getApprovals(res, patchSetNumber);
    }

    /**
     * A tuple of a high and a low number.
     */
    public static class HighLow {

        private final int high;
        private final int low;

        /**
         * Standard constructor.
         *
         * @param high the highest number.
         * @param low  the lowest number.
         */
        public HighLow(int high, int low) {
            this.high = high;
            this.low = low;
        }

        /**
         * Get the High number.
         *
         * @return the high number.
         */
        public int getHigh() {
            return high;
        }

        /**
         * Get the Low number.
         *
         * @return the low number.
         */
        public int getLow() {
            return low;
        }

        @Override
        public String toString() {
            return "HighLow(" + high + "," + low + ")";
        }
    }

    /**
     * Represents a "vote"-type or Approval of a change in the JSON structure.
     */
    public static enum Approval {
        /**
         * A Code Review Approval type <i>Code-Review</i>.
         */
        CODE_REVIEW("Code-Review"),
        /**
         * A Verified Approval type <i>Verified</i>.
         */
        VERIFIED("Verified");
        private String type;

        /**
         * Standard constructor.
         *
         * @param type the approval type.
         */
        Approval(String type) {
            this.type = type;
        }

        /**
         * Finds the highest and lowest approval value of the approval's type for the specified change.
         *
         * @param res the change.
         * @return the highest and lowest value. Or 0,0 if there are no values.
         */
        @Deprecated
        public HighLow getApprovals(JSONObject res) {
            return getApprovals(res, 0);
        }

        /**
         * Finds the highest and lowest approval value of the approval's type for the specified change.
         *
         * @param res the change.
         * @param patchSetNumber the patch set number.
         * @return the highest and lowest value. Or 0,0 if there are no values.
         */
        public HighLow getApprovals(JSONObject res, int patchSetNumber) {
            //logger.warning(String.format("Get Approval: {%s} {%s}", type, res.toString()));
            int highValue = Integer.MIN_VALUE;
            int lowValue = Integer.MAX_VALUE;
            if (res.has("currentPatchSet")) {
                //logger.warning("Has currentPatchSet");
                JSONObject patchSet = res.getJSONObject("currentPatchSet");
                if (patchSet.has("number") && patchSet.has("approvals")) {
                    if (patchSet.getInt("number") == patchSetNumber) {
                        JSONArray approvals = patchSet.getJSONArray("approvals");
                        //logger.warning("Approvals: {}"+approvals.toString());
                        for (Object o : approvals) {
                            JSONObject ap = (JSONObject)o;
                            if (type.equalsIgnoreCase(ap.optString("type"))) {
                                //logger.warning("A {}"+type);
                                try {
                                    int approval = Integer.parseInt(ap.getString("value"));
                                    highValue = Math.max(highValue, approval);
                                    lowValue = Math.min(lowValue, approval);
                                } catch (NumberFormatException nfe) {
                                    logger.warning("Gerrit is bad at giving me Approval-numbers! {}" + nfe.toString());
                                }
                            }
                        }
                    }
                }
            }
            if (highValue == Integer.MIN_VALUE && lowValue == Integer.MAX_VALUE) {
                //logger.warning("Returning all 0");
                return new HighLow(0, 0);
            } else {
                HighLow r = new HighLow(highValue, lowValue);
                //logger.warning("Returning something {}"+r.toString());
                return r;
            }
        }
    }



         /**
     * The annotation exposes this method to JavaScript proxy.
     */
    @JavaScriptMethod
    public void update(String gerritServer, String queryString, boolean allPatchSets, String Branch, String Project, String Owner, String ChangeIDs) {
        logger.warning(String.format("gerritServer: from %s to %s, queryString from %s to %s; allPatchSets from %b to %b", this.selectedServer, gerritServer, this.queryString, queryString, this.allPatchSets, allPatchSets));
        this.selectedServer = gerritServer;
        this.queryString = queryString;
        this.allPatchSets = allPatchSets;
        this.project = Project;
        this.branch = Branch;
        this.owner = Owner;
	this.changeIDs = ChangeIDs;
    }




}


