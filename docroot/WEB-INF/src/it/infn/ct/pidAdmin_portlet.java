/**
 * ************************************************************************
 * Copyright (c) 2011: Istituto Nazionale di Fisica Nucleare (INFN), Italy
 * Consorzio COMETA (COMETA), Italy
 *
 * See http://www.infn.it and and http://www.consorzio-cometa.it for details on
 * the copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * @author <a href="mailto:riccardo.bruno@ct.infn.it">Riccardo Bruno</a>(COMETA)
 * **************************************************************************
 */
package it.infn.ct;

// Import generic java libraries
import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Calendar;
import java.util.Map;
import java.text.DateFormat;
import java.text.SimpleDateFormat;





// Importing portlet libraries
import javax.portlet.*;

// Importing liferay libraries
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.model.User;



// Importing Apache libraries
import org.apache.commons.fileupload.*;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.portlet.PortletFileUpload;





// Importing GridEngine Job libraries 
import it.infn.ct.GridEngine.Job.*;
import it.infn.ct.GridEngine.JobCollection.JobCollection;
import it.infn.ct.GridEngine.JobCollection.JobCollectionSubmission;
import it.infn.ct.GridEngine.JobCollection.JobParametric;
import it.infn.ct.GridEngine.JobCollection.WorkflowN1;
import it.infn.ct.GridEngine.JobResubmission.GEJobDescription;
import it.infn.ct.GridEngine.UsersTracking.ActiveInteractions;
import it.infn.ct.GridEngine.UsersTracking.UsersTrackingDBInterface;

import java.util.ArrayList;

import javax.xml.bind.JAXBContext;

import org.apache.commons.io.IOUtils;

import com.google.gson.*;

/**
 * This is the class that overrides the GenericPortlet class methods You can
 * create your own application just customizing this code skeleton This code
 * provides mainly a full working example on: 1) How to manage user interaction
 * managing the Actions/View combination 2) How to manage portlet preferences
 * and help pane 3) How to print application information using the Log object 4)
 * How to execute a collection of job on a distributed infrastructure with
 * GridEngine
 *
 * @author <a href="mailto:riccardo.bruno@ct.infn.it">Riccardo Bruno</a>(COMETA)
 */
public class pidAdmin_portlet extends GenericPortlet {

    // Instantiate the logger object
    AppLogger _log = new AppLogger(pidAdmin_portlet.class);

    String uri = "https://epic.grnet.gr/api/v2/handles/11239/";
	String user = "chain-reds-demo";
	String password = "ozJ/jFhX02CpmtWEozsD893g1TY=";
	
	
    // This portlet uses Aciont/Views enumerations in order to 
    // manage the different portlet modes and the corresponding 
    // view to display
    // You may override the current values with your own business
    // logic best identifiers and manage them through: jsp pages and
    // this java code
    // The jsp parameter PortletStatus will be the responsible of
    // portlet mode switching. This parameter will be read by
    // the processAction method (actionRequest) who will select
    // then the proper view mode. The doView method will read this
    // value (renderResponSe) assigning the correct view mode.
    // 
    // At first boot the application will be in ACTIVATE status
    // that means the application still requires to be registered
    // into the GridEngine' UsersTrackingDB' GridOperations table
    // Once registered the defaul view mode will be the VIEW_INPUT
    /**
     * Actions enumeration contains the possible action status mode managed by
     * the application. Action modes are stored into the 'PortletStatus'
     * parameter inside the actionRequest object
     */
    private enum Actions {

        ACTION_ACTIVATE // User (Admin) activated the portlet
        , ACTION_INPUT // User asked to submit a job
        , ACTION_SUBMIT // User asked to rerutn to the input form
        , ACTION_PILOT    // The user did something in the edit pilot screen pane
        , ACTION_POSTPROCESS
    }

    /**
     * Views enumeration contains the possible view mondes managed b the
     * application. View modes are stored into the parameter 'PortletStatus'
     * inside the renderResponse object
     */
    private enum Views {

        VIEW_ACTIVATE // Show acrivation pane (called 1st time only)
        , VIEW_INPUT // View containing application input fields
        , VIEW_SUBMIT // View reporting the job submission
        , VIEW_PILOT      // Shows the pilot script and makes it editable
    }
    /**
     * Instanciate the AppPreferences object that stores the Application
     * preferences
     *
     * @see AppPreferences
     */
    AppPreferences appPreferences = new AppPreferences(_log);
    AppPreferences appInitPreferences = new AppPreferences(_log);

    /**
     * This class contains all the necessary data to submit a job inside a
     * distributed infrastructure. Each submission will instanciate this object
     */
    
    // Liferay portal data
    // Classes below are used by this portlet code to get information
    // about the current user    
    public String portalName = "localhost";  // Name of the hosting portal   
    public String appServerPath;           // This variable stores the absolute path of the Web applications
    // Other misc valuse
    // (!) Pay attention that altough the use of the LS variable
    //     the replaceAll("\n","") has to be used 
    public static final String LS = System.getProperty("line.separator");
    // Users must have separated inputSandbox files
    // these file will be generated into /tmp directory
    // and prefixed with the format <timestamp>_<user>_*
    // The timestamp format is:
    public static final String tsFormat = "yyyyMMddHHmmss";
    // This variable holds the GridEngine' GridOperation identifier
    // associated to this application
    int gridOperationId = -1;

    //----------------------------
    // Portlet Overriding Methods
    //----------------------------
    /**
     * The init method will be called when installing the portlet for the first
     * time or when restarting the portal server. This is the right time to get
     * default values from WEBINF/portlet.xml file Those values will be assigned
     * into the application preferences as default values If preference values
     * already exists for this application the default settings will be
     * overwritten
     *
     * @see AppInfrastructureInfo
     * @see AppPreferences
     *
     * @throws PortletException
     */
    @Override
    public void init()
            throws PortletException {
        // Load default values from WEBINF/portlet.xml     
        appInitPreferences.setGridOperationDesc("" + getInitParameter("gridOperationDesc"));
        appInitPreferences.setPortletVersion("" + getInitParameter("portletVersion"));
        appInitPreferences.setLogLevel("" + getInitParameter("logLevel"));
        appInitPreferences.setNumInfrastructures("" + getInitParameter("numInfrastructures"));
        appInitPreferences.setGridOperationId("" + getInitParameter("gridOperationId"));
        // Get the number of infrastructures to load
        int numInfra = appInitPreferences.getNumInfrastructures();
        _log.info("Number of infrastructures: '" + numInfra + "'");
        // Load infrastructure settings
        for (int i = 0; i < numInfra; i++) {
            int j = i + 1;
            appInitPreferences.setInfrastructure(
                    i, "" + getInitParameter(j + "_enableInfrastructure"), "" + getInitParameter(j + "_nameInfrastructure"), "" + getInitParameter(j + "_acronymInfrastructure"), "" + getInitParameter(j + "_bdiiHost"), "" + getInitParameter(j + "_wmsHosts"), "" + getInitParameter(j + "_pxServerHost"), "" + getInitParameter(j + "_pxServerPort"), "" + getInitParameter(j + "_pxServerSecure"), "" + getInitParameter(j + "_pxRobotId"), "" + getInitParameter(j + "_pxRobotVO"), "" + getInitParameter(j + "_pxRobotRole"), "" + getInitParameter(j + "_pxRobotRenewalFlag"), "" + getInitParameter(j + "_pxUserProxy"), "" + getInitParameter(j + "_softwareTags"));
        } // Load infrastructure settings
        appInitPreferences.setSciGwyUserTrackingDB_Hostname("" + getInitParameter("sciGwyUserTrackingDB_Hostname"));
        appInitPreferences.setSciGwyUserTrackingDB_Username("" + getInitParameter("sciGwyUserTrackingDB_Username"));
        appInitPreferences.setSciGwyUserTrackingDB_Password("" + getInitParameter("sciGwyUserTrackingDB_Password"));
        appInitPreferences.setSciGwyUserTrackingDB_Database("" + getInitParameter("sciGwyUserTrackingDB_Database"));
        appInitPreferences.setJobRequirements("" + getInitParameter("jobRequirements"));
        appInitPreferences.setPilotScript("" + getInitParameter("pilotScript"));

        // Assigns the log level      
        _log.setLogLevel(appInitPreferences.getLogLevel());

        // Show loaded values into log
        _log.info(appInitPreferences.dump());
    } // init

    /**
     * This method allows the portlet to process an action request; this method
     * is normally called upon each user interaction (i.e. A submit button
     * inside a jsp' <form statement) This method determines the current
     * application mode through the actionRequest value: 'PortletStatus' and
     * then determines the correct view mode to assign through the
     * ActionResponse 'PortletStatus' variable that will be read by the doView
     * This method will also takes care about the std JSR168/286: EDIT and HELP
     * portlet modes.
     * @param request ActionRequest object instance
     * @param response ActionResponse object instance
     *
     * @throws PortletException
     * @throws IOException
     */
    @Override
    public void processAction(ActionRequest request, ActionResponse response)
            throws PortletException, IOException {
        _log.info("calling processAction ...");

        // Determine the username
        ThemeDisplay themeDisplay = (ThemeDisplay) request.getAttribute(WebKeys.THEME_DISPLAY);
        User user = themeDisplay.getUser();
        String username = user.getScreenName();
        String mail = user.getEmailAddress();
        // Determine the application pathname                   
        PortletSession portletSession = request.getPortletSession();
        PortletContext portletContext = portletSession.getPortletContext();
        appServerPath = portletContext.getRealPath("/");
        // Show info
        _log.info("appUserName   : '" + username + "'"
                + LS + "appServerPath : '" + appServerPath + "'");
        // Determine the current portlet mode and forward this state to the response
        // Accordingly to JSRs168/286 the standard portlet modes are:
        // VIEW, EDIT, HELP
        PortletMode mode = request.getPortletMode();
        response.setPortletMode(mode);

        // Switch among different portlet modes: VIEW, EDIT, HELP
        // any custom modes are not covered by this template

        //----------
        // VIEW Mode
        //
        // The actionStatus value will be taken from the calling jsp file
        // through the 'PortletStatus' parameter; the corresponding
        // VIEW mode will be stored registering the portlet status
        // as render parameter. See the call to setRenderParameter
        // If the actionStatus parameter is null or empty the default
        // action will be the ACTION_INPUT (input form)
        // This happens the first time the portlet is shown
        // The PortletStatus variable is managed by jsp and this java code
        //----------
        
        //A VER SI ESTO SE INCIALIZA AQUI

        
        if (mode.equals(PortletMode.VIEW)) {
            // The VIEW mode is the normal portlet mode where normal portlet
            // content will be shown to the user
            _log.info("Portlet mode: VIEW");

            String actionStatus = request.getParameter("PortletStatus");
            // Assigns the default ACTION mode
            
            /*if (null == actionStatus
                    || actionStatus.equals("")) {
                actionStatus = "" + Actions.ACTION_INPUT;
            }
            */

            if (null == actionStatus) 
                actionStatus = request.getParameter("PostProcessPortletStatus");
            
            if (null == actionStatus
                    || actionStatus.equals("")) {
                actionStatus = "" + Actions.ACTION_INPUT;
            }
            
             
            // Different actions will be performed accordingly to the
            // different possible statuses
            switch (Actions.valueOf(actionStatus)) {
                case ACTION_ACTIVATE:
                    _log.info("Got action: 'ACTION_ACTIVATE'");
                    // Called when activating the portlet for the first time
                    // it will be used to save the gridOperationId value
                    // into the application preferences 
                    gridOperationId = Integer.parseInt(request.getParameter("gridOperationId"));
                    _log.info("Received gridOperationId: '" + gridOperationId + "'");
                    // If the application is registered go to the VIEW_INPUT
                    // and the application will no longer go to the ACTIVATE pane                     
                    if (gridOperationId > 0) {
                        storePreferences(request);
                        response.setRenderParameter("PortletStatus", "" + Views.VIEW_INPUT);
                    }
                    break;
                case ACTION_INPUT:
                    _log.info("Got action: 'ACTION_INPUT'");
                              
                    // Assign the correct view
                    response.setRenderParameter("PortletStatus", "" + Views.VIEW_INPUT);
                    break;
                case ACTION_PILOT:
                    _log.info("Got action: 'ACTION_PILOT'");
                    // Stores the new pilot script
                    String pilotScript = request.getParameter("pilotScript");
                    pilotScript.replaceAll("\r", "");
//                    storeString(appServerPath + "WEB-INF/job/" + appPreferences.getPilotScript(), pilotScript);
                    // Assign the correct view
                    response.setPortletMode(PortletMode.EDIT);
                    break;
                case ACTION_SUBMIT:
                    _log.info("Got action: 'ACTION_SUBMIT'");
                    // Get current preference values
                    getPreferences(request, null);


                    //AQUI SE LE LLAMA CUANDO LE DAS AL BOTON. ES DONDE HAY QUE HACER LAS COSAS
                    processRequest(request);                                                                              
                    
                    
                    // Send the jobIdentifier and assign the correct view                    
                    response.setRenderParameter("PortletStatus", "" + Views.VIEW_SUBMIT);
                    break;

                    
                default:
                        _log.info("Unhandled action: '" + actionStatus + "'");
                        response.setRenderParameter("PortletStatus", "" + Views.VIEW_INPUT);

                    

                    
                    
                    
                    
            } // switch actionStatus
        } // VIEW
        //----------
        // HELP Mode
        //
        // The HELP mode used to give portlet usage HELP to the user
        // This code will be called after the call to doHelp method                         
        //----------
        else if (mode.equals(PortletMode.HELP)) {
            _log.info("Portlet mode: HELP");
        } //----------
        // EDIT Mode
        //
        // The EDIT mode is used to view/setup portlet preferences
        // This code will be called after the user sends the actionURL 
        // generated by the doEdit method 
        // The code below just stores new preference values or
        // reacts to the preference settings changes
        //----------
        else if (mode.equals(PortletMode.EDIT)) {
            _log.info("Portlet mode: EDIT");

            // Retrieve the current ifnrstructure in preference
            int numInfrastructures = appPreferences.getNumInfrastructures();
            int currInfra = appPreferences.getCurrPaneInfrastructure();

            _log.info(
                    LS + "Number of infrastructures: '" + numInfrastructures + "'"
                    + LS + "currentInfrastructure:     '" + currInfra + "'"
                    + LS);

            // Take care of the preference action (Infrastructure preferences)
            // <,>,+,- buttons
            String pref_action = "" + request.getParameter("pref_action");
            _log.info("pref_action: '" + pref_action + "'");

            // Reacts to the current infrastructure change and
            // determine the next view mode (return to the input pane)                        
            if (pref_action.equalsIgnoreCase("next")) {
                appPreferences.switchNextInfrastructure();
                _log.info("Got next infrastructure action; switching to: '" + appPreferences.getCurrPaneInfrastructure() + "'");
            } else if (pref_action.equalsIgnoreCase("previous")) {
                appPreferences.switchPreviousInfrastructure();
                _log.info("Got prev infrastructure action; switching to: '" + appPreferences.getCurrPaneInfrastructure() + "'");
            } else if (pref_action.equalsIgnoreCase("add")) {
                appPreferences.addNewInfrastructure();
                _log.info("Got add infrastructure action; current infrastrucure is now: '" + appPreferences.getCurrPaneInfrastructure() + "'");
            } else if (pref_action.equalsIgnoreCase("remove")) {
                appPreferences.delCurrInfrastructure();
                _log.info("Got remove infrastructure action; current infrastrucure is now: '" + appPreferences.getCurrPaneInfrastructure() + "' and infrastructures are now: '" + appPreferences.getNumInfrastructures() + "'");
            } else if (pref_action.equalsIgnoreCase("done")) {
                // None of the above actions selected; return to the VIEW mode
                response.setPortletMode(PortletMode.VIEW);
                response.setRenderParameter("PortletStatus", "" + Views.VIEW_INPUT);
            } else if (pref_action.equalsIgnoreCase("viewPilot")) {
                // None of the above actions selected; return to the VIEW mode
                response.setPortletMode(PortletMode.VIEW);
                response.setRenderParameter("PortletStatus", "" + Views.VIEW_PILOT);
//                response.setRenderParameter("pilotScript", updateString(appServerPath + "WEB-INF/job/" + appPreferences.getPilotScript()));
            } else {
                // No other special actions to do ...
            }

            // Number of infrastructures and Currentinfrastructure values
            // may be changed by add/delete,<,> actions
            int newCurrInfra = appPreferences.getCurrPaneInfrastructure();
            int newNumInfrastructures = appPreferences.getNumInfrastructures();

            // Store infrastructure changes
            String infrastructuresInformations = "";

            // Preference settings (logLevel has been taken above)                                    
            String newpref_logLevel = "" + request.getParameter("pref_logLevel");
            String newpref_gridOperationId = "" + request.getParameter("pref_gridOperationId");
            String newpref_jobRequirements = "" + request.getParameter("pref_jobRequirements");
            String newpref_pilotScript = "" + request.getParameter("pref_pilotScript");
//LIC
            String newpref_sciGwyUserTrackingDB_Hostname = "" + request.getParameter("pref_sciGwyUserTrackingDB_Hostname");
            String newpref_sciGwyUserTrackingDB_Username = "" + request.getParameter("pref_sciGwyUserTrackingDB_Username");
            String newpref_sciGwyUserTrackingDB_Password = "" + request.getParameter("pref_sciGwyUserTrackingDB_Password");
            String newpref_sciGwyUserTrackingDB_Database = "" + request.getParameter("pref_sciGwyUserTrackingDB_Database");

            // Store infrastructure changes only if the user did not select the delete button
            if (newNumInfrastructures >= numInfrastructures) {
                // Current infrastructure preference settings
                AppInfrastructureInfo newpref_appInfrastructureInfo = new AppInfrastructureInfo(
                        "" + request.getParameter("pref_enableInfrastructure"), "" + request.getParameter("pref_nameInfrastructure"), "" + request.getParameter("pref_acronymInfrastructure"), "" + request.getParameter("pref_bdiiHost"), "" + request.getParameter("pref_wmsHosts"), "" + request.getParameter("pref_pxServerHost"), "" + request.getParameter("pref_pxServerPort"), "" + request.getParameter("pref_pxServerSecure"), "" + request.getParameter("pref_pxRobotId"), "" + request.getParameter("pref_pxRobotVO"), "" + request.getParameter("pref_pxRobotRole"), "" + request.getParameter("pref_pxRobotRenewalFlag"), "" + request.getParameter("pref_pxUserProxy"), "" + request.getParameter("pref_softwareTags"));
                // newNumInfrastructures == numInfrastructures
                // the user selected < or > buttons; changes goes to the old (currInfra) value
                // otherwise + has been selected; changes goes again on the old (currInfra) value
                // the - case has been filtered out by newNumInfrastructures >= numInfrastructures
                String pref_enableInfrastructure = appPreferences.getEnableInfrastructure(currInfra - 1);
                String pref_nameInfrastructure = appPreferences.getNameInfrastructure(currInfra - 1);
                String pref_acronymInfrastructure = appPreferences.getAcronymInfrastructure(currInfra - 1);
                String pref_bdiiHost = appPreferences.getBdiiHost(currInfra - 1);
                String pref_wmsHosts = appPreferences.getWmsHosts(currInfra - 1);
                String pref_pxServerHost = appPreferences.getPxServerHost(currInfra - 1);
                String pref_pxServerPort = appPreferences.getPxServerPort(currInfra - 1);
                String pref_pxServerSecure = appPreferences.getPxServerSecure(currInfra - 1);
                String pref_pxRobotId = appPreferences.getPxRobotId(currInfra - 1);
                String pref_pxRobotVO = appPreferences.getPxRobotVO(currInfra - 1);
                String pref_pxRobotRole = appPreferences.getPxRobotRole(currInfra - 1);
                String pref_pxRobotRenewalFlag = appPreferences.getPxRobotRenewalFlag(currInfra - 1);
                String pref_pxUserProxy = appPreferences.getPxUserProxy(currInfra - 1);
                String pref_softwareTags = appPreferences.getSoftwareTags(currInfra - 1);
                // New preference values
                String newpref_enableInfrastructure = newpref_appInfrastructureInfo.getEnableInfrastructure();
                String newpref_nameInfrastructure = newpref_appInfrastructureInfo.getNameInfrastructure();
                String newpref_acronymInfrastructure = newpref_appInfrastructureInfo.getAcronymInfrastructure();
                String newpref_bdiiHost = newpref_appInfrastructureInfo.getBdiiHost();
                String newpref_wmsHosts = newpref_appInfrastructureInfo.getWmsHosts();
                String newpref_pxServerHost = newpref_appInfrastructureInfo.getPxServerHost();
                String newpref_pxServerPort = newpref_appInfrastructureInfo.getPxServerPort();
                String newpref_pxServerSecure = newpref_appInfrastructureInfo.getPxServerSecure();
                String newpref_pxRobotId = newpref_appInfrastructureInfo.getPxRobotId();
                String newpref_pxRobotVO = newpref_appInfrastructureInfo.getPxRobotVO();
                String newpref_pxRobotRole = newpref_appInfrastructureInfo.getPxRobotRole();
                String newpref_pxRobotRenewalFlag = newpref_appInfrastructureInfo.getPxRobotRenewalFlag();
                String newpref_pxUserProxy = newpref_appInfrastructureInfo.getPxUserProxy();
                String newpref_softwareTags = newpref_appInfrastructureInfo.getSoftwareTags();
                // Prepare the Log string with differences
                infrastructuresInformations +=
                        LS + "Infrastructure #" + currInfra
                        + LS + "  enableInfrastructure  : '" + pref_enableInfrastructure + "' -> '" + newpref_enableInfrastructure + "'"
                        + LS + "  nameInfrastructures   : '" + pref_nameInfrastructure + "' -> '" + newpref_nameInfrastructure + "'"
                        + LS + "  acronymInfrastructures: '" + pref_acronymInfrastructure + "' -> '" + newpref_acronymInfrastructure + "'"
                        + LS + "  bdiiHost              : '" + pref_bdiiHost + "' -> '" + newpref_bdiiHost + "'"
                        + LS + "  wmsHosts              : '" + pref_wmsHosts + "' -> '" + newpref_wmsHosts + "'"
                        + LS + "  pxServerHost          : '" + pref_pxServerHost + "' -> '" + newpref_pxServerHost + "'"
                        + LS + "  pxServerPort          : '" + pref_pxServerPort + "' -> '" + newpref_pxServerPort + "'"
                        + LS + "  pxServerSecure        : '" + pref_pxServerSecure + "' -> '" + newpref_pxServerSecure + "'"
                        + LS + "  pxRobotId             : '" + pref_pxRobotId + "' -> '" + newpref_pxRobotId + "'"
                        + LS + "  pxRobotVO             : '" + pref_pxRobotVO + "' -> '" + newpref_pxRobotVO + "'"
                        + LS + "  pxRobotRole           : '" + pref_pxRobotRole + "' -> '" + newpref_pxRobotRole + "'"
                        + LS + "  pxRobotRenewalFlag    : '" + pref_pxRobotRenewalFlag + "' -> '" + newpref_pxRobotRenewalFlag + "'"
                        + LS + "  pxUserProxy           : '" + pref_pxUserProxy + "' -> '" + newpref_pxUserProxy + "'"
                        + LS + "  softwareTags          : '" + pref_softwareTags + "' -> '" + newpref_softwareTags + "'"
                        + LS;
                // Assigns the new values
                appPreferences.setInfrastructure(
                        currInfra - 1, newpref_enableInfrastructure, newpref_nameInfrastructure, newpref_acronymInfrastructure, newpref_bdiiHost, newpref_wmsHosts, newpref_pxServerHost, newpref_pxServerPort, newpref_pxServerSecure, newpref_pxRobotId, newpref_pxRobotVO, newpref_pxRobotRole, newpref_pxRobotRenewalFlag, newpref_pxUserProxy, newpref_softwareTags);
            } // newCurrInfra >= currInfra
            // Show preference value changes
            _log.info(
                    LS + "variable name          : 'Old Value' -> 'New value'"
                    + LS + "---------------------------------------------------"
                    + LS + "pref_logLevel                      : '" + appPreferences.getLogLevel() + "' -> '" + newpref_logLevel + "'"
                    + LS + "pref_gridOperationId               : '" + appPreferences.getGridOperationId() + "' -> '" + newpref_gridOperationId + "'"
                    + LS + "pref_numInfrastructures            : '" + appPreferences.getNumInfrastructures() + "' -> '" + numInfrastructures + "'"
                    + LS + infrastructuresInformations
                    + LS + "pref_jobRequirements               : '" + appPreferences.getJobRequirements() + "' -> '" + newpref_jobRequirements + "'"
                    + LS + "pref_pilotScript                   : '" + appPreferences.getPilotScript() + "' -> '" + newpref_pilotScript + "'"
                    + LS + "pref_sciGwyUserTrackingDB_Hostname : '" + appPreferences.getSciGwyUserTrackingDB_Hostname() + "' -> '" + newpref_sciGwyUserTrackingDB_Hostname + "'"
                    + LS + "pref_sciGwyUserTrackingDB_Username : '" + appPreferences.getSciGwyUserTrackingDB_Username() + "' -> '" + newpref_sciGwyUserTrackingDB_Username + "'"
                    + LS + "pref_sciGwyUserTrackingDB_Password : '" + appPreferences.getSciGwyUserTrackingDB_Password() + "' -> '" + newpref_sciGwyUserTrackingDB_Password + "'"
                    + LS + "pref_sciGwyUserTrackingDB_Database : '" + appPreferences.getSciGwyUserTrackingDB_Database() + "' -> '" + newpref_sciGwyUserTrackingDB_Database + "'"
                    + LS);

            // Assign the new variable to the preference object
            appPreferences.setLogLevel(newpref_logLevel);
            appPreferences.setGridOperationId(newpref_gridOperationId);
            appPreferences.setJobRequirements(newpref_jobRequirements);
            appPreferences.setPilotScript(newpref_pilotScript);
//LIC
            appPreferences.setSciGwyUserTrackingDB_Hostname(newpref_sciGwyUserTrackingDB_Hostname);
            appPreferences.setSciGwyUserTrackingDB_Username(newpref_sciGwyUserTrackingDB_Username);
            appPreferences.setSciGwyUserTrackingDB_Password(newpref_sciGwyUserTrackingDB_Password);
            appPreferences.setSciGwyUserTrackingDB_Database(newpref_sciGwyUserTrackingDB_Database);

            // Store new preferences
            storePreferences(request);
        } // EDIT Mode
        //----------
        // EDIT Mode
        //
        // Any custom portlet mode should be placed here below
        //----------
        else {
            // Unsupported portlet modes come here
            _log.warn("Custom portlet mode: '" + mode.toString() + "'");
        } // CUSTOM Mode                               
    } // processAction

    /**
     * This method is responsible to assign the correct Application view the
     * view mode is taken from the renderRequest instance by the PortletStatus
     * patameter or automatically assigned accordingly to the Application
     * status/default view mode
     *
     * @param request RenderRequest instance normally sent by the processAction
     * @param response RenderResponse used to send values to the jsp page
     *
     * @throws PortletException
     * @throws IOException
     */
    @Override
    protected void doView(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        _log.info("calling doView ...");
        response.setContentType("text/html");

        // Get current preference values
        getPreferences(null, request);
        gridOperationId = Integer.parseInt(appPreferences.getGridOperationId());
        _log.info("GridOperationId: '" + gridOperationId + "'");
        // currentView comes from the processAction; unless such method
        // is not called before (example: page shown with no user action)
        // In case the application is not yet register (gridOperationId<0)
        // the VIEW_INITIALIZE pane will be enforced otherwise the
        // VIEW_INPUT will be selected as default view
        String currentView = request.getParameter("PortletStatus");
        if (currentView == null) {
            currentView = "VIEW_INPUT";
        }
        if (gridOperationId < 0) {
            currentView = "VIEW_ACTIVATE";
        }

        
        
        String  pids = obtainPIDList();
        request.setAttribute("pidList", pids);

        
        String[] portlets = getPortlets();
        String serialPortlets = "";
        for (String portlet:portlets)
        	serialPortlets+=portlet+",";
        request.setAttribute("portlets",serialPortlets);
        
        // Different actions will be performed accordingly to the
        // different possible view modes
        switch (Views.valueOf(currentView)) {
            // The following code is responsible to call the proper jsp file
            // that will provide the correct portlet interface
            case VIEW_ACTIVATE: {
                _log.info("VIEW_ACTIVATE Selected ...");
              
            }
            break;
            case VIEW_INPUT: {
                _log.info("VIEW_INPUT Selected ...");
               //obtaining runnning collections of jobs
               
               //jobs will be store here
               java.util.Vector<ActiveInteractions> myJobs = new java.util.Vector<ActiveInteractions>();
               
               //db conection 
               UsersTrackingDBInterface dbInterface = new UsersTrackingDBInterface();

               // this is where the info will be stored
               Map<String, String> jobs = new HashMap<String, String>();

               //extract relevant job information
                myJobs = dbInterface.getActiveInteractionsByName("test");
               for (ActiveInteractions job: myJobs){
            	   String[] interactionInfos = job.getInteractionInfos();
            	   jobs.put(interactionInfos[0], interactionInfos[3] + "-" + interactionInfos[5]);
           	   
               }
               
               
                PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/input.jsp");
                dispatcher.include(request, response);
            }
            break;
            case VIEW_PILOT: {
                _log.info("VIEW_PILOT Selected ...");
                String pilotScript = request.getParameter("pilotScript");
                request.setAttribute("pilotScript", pilotScript);
                PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/viewPilot.jsp");
                dispatcher.include(request, response);
            }
            break;
            case VIEW_SUBMIT: {
                _log.info("VIEW_SUBMIT Selected ...");
                String jobIdentifier = request.getParameter("jobIdentifier");
                request.setAttribute("jobIdentifier", jobIdentifier);
                PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/submit.jsp");
                dispatcher.include(request, response);
            }
            break;
            default:
                _log.info("Unknown view mode: " + currentView.toString());
        } // switch            
    } // doView

    /**
     * This method is responsible to retrieve the current Application preference
     * settings and then show the edit.jsp page where the user can edit the
     * Application preferences This methods prepares an actionURL that will be
     * used by edit.jsp file into a <input ...> form As soon the user press the
     * action button the processAction will be called going in EDIT mode This
     * method is equivalent to the doView method
     *
     * @param request Render request object instance
     * @param response Render response object isntance
     *
     * @throws PortletException
     * @throws IOException
     *
     */
    @Override
    public void doEdit(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        response.setContentType("text/html");
        _log.info("Calling doEdit ...");

        // Get current preference values
        getPreferences(null, request);

        // Get the current infrastructure and the number of infrastructure
        int currInfra = appPreferences.getCurrPaneInfrastructure();
        int numInfrastructures = appPreferences.getNumInfrastructures();

        // ActionURL and the current preference value will be passed to the edit.jsp
        PortletURL pref_actionURL = response.createActionURL();
        request.setAttribute("pref_actionURL", pref_actionURL.toString());

        // Send preference values
        request.setAttribute("pref_logLevel", "" + appPreferences.getLogLevel());
        request.setAttribute("pref_numInfrastructures", "" + appPreferences.getNumInfrastructures());
        request.setAttribute("pref_currInfrastructure", "" + appPreferences.getCurrPaneInfrastructure());
        request.setAttribute("pref_gridOperationId", "" + appPreferences.getGridOperationId());
        request.setAttribute("pref_gridOperationDesc", "" + appPreferences.getGridOperationDesc());
        // Send Infrastructure specific data        
        if (0 <= currInfra
                && currInfra <= numInfrastructures) {
            request.setAttribute("pref_enableInfrastructure", appPreferences.getEnableInfrastructure(currInfra - 1));
            request.setAttribute("pref_nameInfrastructure", appPreferences.getNameInfrastructure(currInfra - 1));
            request.setAttribute("pref_acronymInfrastructure", appPreferences.getAcronymInfrastructure(currInfra - 1));
            request.setAttribute("pref_bdiiHost", appPreferences.getBdiiHost(currInfra - 1));
            request.setAttribute("pref_wmsHosts", appPreferences.getWmsHosts(currInfra - 1));
            request.setAttribute("pref_pxServerHost", appPreferences.getPxServerHost(currInfra - 1));
            request.setAttribute("pref_pxServerPort", appPreferences.getPxServerPort(currInfra - 1));
            request.setAttribute("pref_pxServerSecure", appPreferences.getPxServerSecure(currInfra - 1));
            request.setAttribute("pref_pxRobotId", appPreferences.getPxRobotId(currInfra - 1));
            request.setAttribute("pref_pxRobotVO", appPreferences.getPxRobotVO(currInfra - 1));
            request.setAttribute("pref_pxRobotRole", appPreferences.getPxRobotRole(currInfra - 1));
            request.setAttribute("pref_pxRobotRenewalFlag", appPreferences.getPxRobotRenewalFlag(currInfra - 1));
            request.setAttribute("pref_pxUserProxy", appPreferences.getPxUserProxy(currInfra - 1));
            request.setAttribute("pref_softwareTags", appPreferences.getSoftwareTags(currInfra - 1));
        } // if paneInfrastructure > 0
        request.setAttribute("pref_jobRequirements", appPreferences.getJobRequirements());
        request.setAttribute("pref_pilotScript", appPreferences.getPilotScript());
//LIC
        request.setAttribute("pref_sciGwyUserTrackingDB_Hostname", appPreferences.getSciGwyUserTrackingDB_Hostname());
        request.setAttribute("pref_sciGwyUserTrackingDB_Username", appPreferences.getSciGwyUserTrackingDB_Username());
        request.setAttribute("pref_sciGwyUserTrackingDB_Password", appPreferences.getSciGwyUserTrackingDB_Password());
        request.setAttribute("pref_sciGwyUserTrackingDB_Database", appPreferences.getSciGwyUserTrackingDB_Database());

        // The edit.jsp will be the responsible to show/edit the current preference values
        PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/edit.jsp");
        dispatcher.include(request, response);
    } // doEdit


    /**
     * This enumerated type contains all JSP input items to be managed by the
     * getInputForm method
     *
     * @see getInputForm
     */
    private enum inputControlsIds {

        portlet // User defined Job identifier
        , pidName
        , inputPIDURL
    	/*
        JobIdentifier, // User defined Job identifier
        collection_type, 
        task_number, 
        parametric_executable, 
        executables, 
        argument, 
        final_executable, 
        final_argument
        */
    };

    /**
     * This method manages the user input fields managing two cases
     * distinguished by the type of the input <form ... statement The use of
     * upload file controls needs the use of "multipart/form-data" while the
     * else condition of the isMultipartContent check manages the standard input
     * case. The multipart content needs a manual processing of all <form items
     * All form' input items are identified by the 'name' input property inside
     * the jsp file
     * @param request ActionRequest instance (processAction)
     * @param appInput AppInput instance storing the jobSubmission data
     */
    
    

    void processRequest(ActionRequest request) {
   		
    	
        
        
        Enumeration<String> e  = request.getParameterNames();
        
        while(e.hasMoreElements()){
        	String param = (String) e.nextElement();
        	System.out.println("PARAM " + param + ": " + (String) request.getParameter(param));
        }
        
        
        
        String pidName = (String) request.getParameter("pidName");
        
        if (pidName !=null)
        	createPID(request);
        
        String action = (String) request.getParameter("delete");
        if (action != null)
        	deletePID(request);
        

    }	
        	
       
    void deletePID(ActionRequest request){
    	
        _log.warn("STARTING DELETE REQUEST");

        String pidName = (String) request.getParameter("delete");
        
    	try {
	    	String userpass = user + ":" + password;
	    	String encoded = new sun.misc.BASE64Encoder().encode(userpass.getBytes());
	  	
	    	URL url = new URL(uri + pidName);
	    	   	
	    	
	    	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	    	connection.setRequestProperty ("Authorization", "Basic " + encoded);
	    	connection.addRequestProperty("Accept", "application/json");
	    	connection.addRequestProperty("Content-Type", "application/json");
	    	connection.setDoOutput(true);
	    	connection.setRequestMethod("DELETE");

	    	OutputStream out = connection.getOutputStream();
	    	System.out.println("ERROR: " + connection.getResponseCode());
	    	System.out.println("URL: " + url.toString());
	    	
	    	
	    	out.close();
    		System.out.println("DELETION REQUEST FINISHED");

	    	// Check here that you succeeded!
    	}
    	catch(Exception e){
    		System.out.println("HA cascado borrando un PID");
    		e.printStackTrace();
    	}
    	
    }
    
        
        
      void createPID(ActionRequest request){
          _log.warn("STARTING CREATION REQUEST");

        String portlet = (String) request.getParameter("portlet");
        String inputPIDURL = (String) request.getParameter("inputPIDURL");
        String pidName = (String) request.getParameter("pidName");
        String infoToJSon = "[{\"type\":\"URL\",\"parsed_data\":\"" + inputPIDURL + "\"}]";
        
    	try {
	    	String userpass = user + ":" + password;
	    	String encoded = new sun.misc.BASE64Encoder().encode(userpass.getBytes());
	  	
	    	URL url = new URL(uri  + portlet + "." + pidName);
	    	
	    	HttpURLConnection connection = (HttpURLConnection) url.openConnection();
	    	connection.setRequestProperty ("Authorization", "Basic " + encoded);
	    	connection.addRequestProperty("Accept", "application/json");
	    	connection.addRequestProperty("Content-Type", "application/json");
	    	connection.setDoOutput(true);
	    	connection.setRequestMethod("PUT");

	    	OutputStream out = connection.getOutputStream();
	    	out.write(infoToJSon.getBytes("UTF-8"));
	    	System.out.println("ERROR: " + connection.getResponseCode());
	    	
	    	
	    	out.close();
    		System.out.println("CREATION REQUEST FINISHED");

	    	// Check here that you succeeded!
    	}
    	catch(Exception e){
    		System.out.println("HA cascado escribiendo un PID");
    		e.printStackTrace();
    	}
        

    } // getInputForm 
   
    

    
    //this should access the database to get the whole portlet list
    String[] getPortlets() {
    	String[] portlets = {"molon", "jmodeltest"};
    	return portlets;    	
    	
    }
    

    //obtain PID list from GRNET server
    String obtainPIDList(){
    	String userpass = user + ":" + password;
    	String encoded = new sun.misc.BASE64Encoder().encode (userpass.getBytes());
  	
    	String results = "";

    	try {
	    	URL url = new URL(uri);
	    	HttpURLConnection connection = 
	    	    (HttpURLConnection) url.openConnection();
	    	connection.setRequestMethod("GET");
	    	connection.setRequestProperty("Accept", "application/json");
	    	connection.setRequestProperty ("Authorization", "Basic " + encoded);

	    	InputStream input = connection.getInputStream();
	    	String theString = IOUtils.toString(input, "UTF-8");

	    	connection.disconnect();

	    	
	    	
	    	//parse results
	    	String[] portlets = getPortlets();
	    	String[] strings = theString.split("\n");
	    	
	    	for (int i = 0; i < strings.length; i++)
	    		for (int j = 0; j < portlets.length; j++)
	    			if (strings[i].toLowerCase().contains(portlets[j].toLowerCase()))
	    				results+= portlets[j] + "," + strings[i];
	    			
	    	
    	}
    	catch(Exception e){
    		System.out.println("HA cascado obteniendo PID list");
    		e.printStackTrace();
    	}

    	return results;
    		
    }
    

    
    
    
   
    

    
    
    
    
    
    
    
    
    //----------------------------
    // Portlet Standard Methods
    //----------------------------
    /**
     * This method is used to retrieve from the Application preferences the
     * GridEngine' GridOperations identifier associated to this application Such
     * index is automatically created when registering the application with the
     * couple (portalName,applicationDesc) The portal name is automatically
     * extracted from the Application The portal description is defined in the
     * default parameters (portlet.xml) This method can be called by
     * processAction or doViewe evakuating one of the corresponding
     * actionRequest or renderRequest object instances
     *
     * @param actionRequest an ActionRequest instance or,
     * @param renderRequest a RenderRequest instance
     * @return The GridOperationId associated to this application or -1 if the
     * application is not yet registered
     *
     * @see AppPreferences
     *
     * private String getPrefGridOperationId(ActionRequest actionRequest ,
     * RenderRequest renderRequest) { PortletPreferences portletPreferences;
     * String prefOperationId=""; if(null != actionRequest) {
     * portletPreferences= actionRequest.getPreferences(); prefOperationId =
     * portletPreferences.getValue("pref_gridOperationId","-1"); } if(null !=
     * renderRequest) { portletPreferences= renderRequest.getPreferences();
     * prefOperationId =
     * portletPreferences.getValue("pref_gridOperationId","-1"); } return
     * prefOperationId; }
     */
    /**
     * This method Uses the AppPreference object settings to store Application
     * preferences
     *
     * @param request ActinRequest instance (called by the processAction)
     *
     * @throws PortletException
     * @throws IOException
     */
    
    void storePreferences(ActionRequest request)
            throws PortletException, IOException {
        _log.info("Calling storePreferences ...");
        // Stored preference content
        String storedPrefs = "Stored preferences:"
                + LS + "-------------------"
                + LS;
        // The code below stores all the portlet preference values
        PortletPreferences prefs = request.getPreferences();
        if (prefs != null) {
            String logLevel = appPreferences.getLogLevel();
            String gridOperationId = appPreferences.getGridOperationId();
            int numInfrastructures = appPreferences.getNumInfrastructures();
            int currPaneInfrastructure = appPreferences.getCurrPaneInfrastructure();
            String gridOperationDesc = appPreferences.getGridOperationDesc();
            prefs.setValue("pref_logLevel", "" + logLevel);
            prefs.setValue("pref_gridOperationId", "" + gridOperationId);
            prefs.setValue("pref_gridOperationDesc", "" + gridOperationDesc);
            prefs.setValue("pref_numInfrastructures", "" + numInfrastructures);
            prefs.setValue("pref_currInfrastructure", "" + currPaneInfrastructure);
            storedPrefs += "pref_logLevel           : '" + logLevel + "'"
                    + LS + "pref_gridOperationId    : '" + gridOperationId + "'"
                    + LS + "pref_gridOperationDesc  : '" + gridOperationDesc + "'"
                    + LS + "pref_numInfrastructures : '" + numInfrastructures + "'"
                    + LS + "pref_currInfrastructure : '" + currPaneInfrastructure + "'"
                    + LS;
            // For each preference infrastructure
            for (int i = 0; i < numInfrastructures; i++) {
                int j = i + 1;
                storedPrefs = LS + "Infrastructure #" + j
                        + LS + "--------------------"
                        + LS;
                String enableInfrastructure = appPreferences.getEnableInfrastructure(i);
                String nameInfrastructure = appPreferences.getNameInfrastructure(i);
                String acronymInfrastructure = appPreferences.getAcronymInfrastructure(i);
                String bdiiHost = appPreferences.getBdiiHost(i);
                String wmsHost = appPreferences.getWmsHosts(i);
                String pxServerHost = appPreferences.getPxServerHost(i);
                String pxServerPort = appPreferences.getPxServerPort(i);
                String pxServerSecure = appPreferences.getPxServerSecure(i);
                String pxRobotId = appPreferences.getPxRobotId(i);
                String pxRobotVO = appPreferences.getPxRobotVO(i);
                String pxRobotRole = appPreferences.getPxRobotRole(i);
                String pxRobotRenewalFlag = appPreferences.getPxRobotRenewalFlag(i);
                String pxUserProxy = appPreferences.getPxUserProxy(i);
                String softwareTags = appPreferences.getSoftwareTags(i);
                // Set preference values
                prefs.setValue("pref_" + j + "_enableInfrastructure", enableInfrastructure);
                prefs.setValue("pref_" + j + "_nameInfrastructure", nameInfrastructure);
                prefs.setValue("pref_" + j + "_acronymInfrastructure", acronymInfrastructure);
                prefs.setValue("pref_" + j + "_bdiiHost", bdiiHost);
                prefs.setValue("pref_" + j + "_wmsHosts", wmsHost);
                prefs.setValue("pref_" + j + "_pxServerHost", pxServerHost);
                prefs.setValue("pref_" + j + "_pxServerPort", pxServerPort);
                prefs.setValue("pref_" + j + "_pxServerSecure", pxServerSecure);
                prefs.setValue("pref_" + j + "_pxRobotId", pxRobotId);
                prefs.setValue("pref_" + j + "_pxRobotVO", pxRobotVO);
                prefs.setValue("pref_" + j + "_pxRobotRole", pxRobotRole);
                prefs.setValue("pref_" + j + "_pxRobotRenewalFlag", pxRobotRenewalFlag);
                prefs.setValue("pref_" + j + "_pxUserProxy", pxUserProxy);
                prefs.setValue("pref_" + j + "_softwareTags", softwareTags);
                // Dumps the infrastructure preferences
                storedPrefs += "  pref_" + j + "_enableInfrastructure : '" + enableInfrastructure + "'"
                        + LS + "  pref_" + j + "_nameInfrastructure   : '" + nameInfrastructure + "'"
                        + LS + "  pref_" + j + "_acronymInfrastructure: '" + acronymInfrastructure + "'"
                        + LS + "  pref_" + j + "_bdiiHost             : '" + bdiiHost + "'"
                        + LS + "  pref_" + j + "_wmsHosts             : '" + wmsHost + "'"
                        + LS + "  pref_" + j + "_pxServerHost         : '" + pxServerHost + "'"
                        + LS + "  pref_" + j + "_pxServerPort         : '" + pxServerPort + "'"
                        + LS + "  pref_" + j + "_pxServerSecure       : '" + pxServerSecure + "'"
                        + LS + "  pref_" + j + "_pxRobotId            : '" + pxRobotId + "'"
                        + LS + "  pref_" + j + "_pxRobotVO            : '" + pxRobotVO + "'"
                        + LS + "  pref_" + j + "_pxRobotRole          : '" + pxRobotRole + "'"
                        + LS + "  pref_" + j + "_pxRobotRenewalFlag   : '" + pxRobotRenewalFlag + "'"
                        + LS + "  pref_" + j + "_pxUserProxy          : '" + pxUserProxy + "'"
                        + LS + "  pref_" + j + "_softwareTags         : '" + softwareTags + "'"
                        + LS;
            } // for each preference infrastructure                   
            String jobRequirements = appInitPreferences.getJobRequirements();
            String pilotScript = appInitPreferences.getPilotScript();
            prefs.setValue("pref_jobRequirements", jobRequirements);
            prefs.setValue("pref_pilotScript", pilotScript);
//LIC
            String sciGwyUserTrackingDB_Hostname = appPreferences.getSciGwyUserTrackingDB_Hostname();
            String sciGwyUserTrackingDB_Username = appPreferences.getSciGwyUserTrackingDB_Username();
            String sciGwyUserTrackingDB_Password = appPreferences.getSciGwyUserTrackingDB_Password();
            String sciGwyUserTrackingDB_Database = appPreferences.getSciGwyUserTrackingDB_Database();
            prefs.setValue("pref_sciGwyUserTrackingDB_Hostname", sciGwyUserTrackingDB_Hostname);
            prefs.setValue("pref_sciGwyUserTrackingDB_Username", sciGwyUserTrackingDB_Username);
            prefs.setValue("pref_sciGwyUserTrackingDB_Password", sciGwyUserTrackingDB_Password);
            prefs.setValue("pref_sciGwyUserTrackingDB_Database", sciGwyUserTrackingDB_Database);

            storedPrefs += "pref_jobRequirements              : '" + jobRequirements + "'"
                    + LS + "pref_pilotScript                  : '" + pilotScript + "'"
                    + LS + "pref_sciGwyUserTrackingDB_Hostname: '" + sciGwyUserTrackingDB_Hostname + "'"
                    + LS + "pref_sciGwyUserTrackingDB_Username: '" + sciGwyUserTrackingDB_Username + "'"
                    + LS + "pref_sciGwyUserTrackingDB_Password: '" + sciGwyUserTrackingDB_Password + "'"
                    + LS + "pref_sciGwyUserTrackingDB_Database: '" + sciGwyUserTrackingDB_Database + "'"
                    + LS;
            // Store preferences
            prefs.store();
        } // pref !=null

        // Show saved preferences
        _log.info("Stored preferences"
                + LS + "------------------"
                + storedPrefs
                + LS);

    } // storePreferences     

    /**
     * This method fills the appPreferences values retrieving them frorm the
     * portlet preference object. This method can be called by both
     * processAction or doView methods in case no preference values are yet
     * defined the default settings loaded by the init method will be used
     *
     * @param actionRequest an ActionRequest instance or,
     * @param renderRequest a RenderRequest instance
     *
     */
    private void getPreferences(ActionRequest actionRequest, RenderRequest renderRequest) {
        _log.info("Calling: getPreferences ...");
        PortletPreferences prefs = null;

        if (null != actionRequest) {
            prefs = actionRequest.getPreferences();
        } else if (null != renderRequest) {
            prefs = renderRequest.getPreferences();
        } else {
            _log.warn("Both render request and action request are null");
        }

        if (null != prefs) {
            appPreferences.updateValue("logLevel", "" + prefs.getValue("pref_logLevel", appInitPreferences.getLogLevel()));
            appPreferences.updateValue("gridOperationId", "" + prefs.getValue("pref_gridOperationId", appInitPreferences.getGridOperationId()));
            appPreferences.updateValue("gridOperationDesc", "" + prefs.getValue("pref_gridOperationDesc", appInitPreferences.getGridOperationDesc()));
            appPreferences.updateValue("numInfrastructures", "" + prefs.getValue("pref_numInfrastructures", "" + appInitPreferences.getNumInfrastructures()));

            // Now retrieves the infrastructures information
            int numInfras = appPreferences.getNumInfrastructures();
            _log.info("getpref: num infra=" + numInfras);

            // For each infrastructure ...
            // The preference name is indexed with the infrastructure number: 1,2,...
            String infrastructuresInfrormations = "";
            for (int i = 0; i < numInfras; i++) {
                int j = i + 1;
                int k = appInitPreferences.getNumInfrastructures();
                appPreferences.updateInfrastructureValue(i, "enableInfrastructure", "" + prefs.getValue("pref_" + j + "_enableInfrastructure", (i < k) ? appInitPreferences.getEnableInfrastructure(i) : ""));
                appPreferences.updateInfrastructureValue(i, "nameInfrastructure", "" + prefs.getValue("pref_" + j + "_nameInfrastructure", (i < k) ? appInitPreferences.getNameInfrastructure(i) : ""));
                appPreferences.updateInfrastructureValue(i, "acronymInfrastructure", "" + prefs.getValue("pref_" + j + "_acronymInfrastructure", (i < k) ? appInitPreferences.getAcronymInfrastructure(i) : ""));
                appPreferences.updateInfrastructureValue(i, "bdiiHost", "" + prefs.getValue("pref_" + j + "_bdiiHost", (i < k) ? appInitPreferences.getBdiiHost(i) : ""));
                appPreferences.updateInfrastructureValue(i, "wmsHosts", "" + prefs.getValue("pref_" + j + "_wmsHosts", (i < k) ? appInitPreferences.getWmsHosts(i) : ""));
                appPreferences.updateInfrastructureValue(i, "pxServerHost", "" + prefs.getValue("pref_" + j + "_pxServerHost", (i < k) ? appInitPreferences.getPxServerHost(i) : ""));
                appPreferences.updateInfrastructureValue(i, "pxServerPort", "" + prefs.getValue("pref_" + j + "_pxServerPort", (i < k) ? appInitPreferences.getPxServerPort(i) : ""));
                appPreferences.updateInfrastructureValue(i, "pxServerSecure", "" + prefs.getValue("pref_" + j + "_pxServerSecure", (i < k) ? appInitPreferences.getPxServerSecure(i) : ""));
                appPreferences.updateInfrastructureValue(i, "pxRobotId", "" + prefs.getValue("pref_" + j + "_pxRobotId", (i < k) ? appInitPreferences.getPxRobotId(i) : ""));
                appPreferences.updateInfrastructureValue(i, "pxRobotVO", "" + prefs.getValue("pref_" + j + "_pxRobotVO", (i < k) ? appInitPreferences.getPxRobotVO(i) : ""));
                appPreferences.updateInfrastructureValue(i, "pxRobotRole", "" + prefs.getValue("pref_" + j + "_pxRobotRole", (i < k) ? appInitPreferences.getPxRobotRole(i) : ""));
                appPreferences.updateInfrastructureValue(i, "pxRobotRenewalFlag", "" + prefs.getValue("pref_" + j + "_pxRobotRenewalFlag", (i < k) ? appInitPreferences.getPxRobotRenewalFlag(i) : ""));
                appPreferences.updateInfrastructureValue(i, "pxUserProxy", "" + prefs.getValue("pref_" + j + "_pxUserProxy", (i < k) ? appInitPreferences.getPxUserProxy(i) : ""));
                appPreferences.updateInfrastructureValue(i, "softwareTags", "" + prefs.getValue("pref_" + j + "_softwareTags", (i < k) ? appInitPreferences.getSoftwareTags(i) : ""));
                _log.info("dump: "
                        + LS + appPreferences.dumpInfrastructure(i));
            } // for each Infrastructure                           

            appPreferences.updateValue("jobRequirements", "" + prefs.getValue("pref_jobRequirements", appInitPreferences.getJobRequirements()));
            appPreferences.updateValue("pilotScript", "" + prefs.getValue("pref_pilotScript", appInitPreferences.getPilotScript()));
//LIC
            appPreferences.updateValue("sciGwyUserTrackingDB_Hostname",
                    "" + prefs.getValue("pref_sciGwyUserTrackingDB_Hostname",
                    appInitPreferences.getSciGwyUserTrackingDB_Hostname()));
            appPreferences.updateValue("sciGwyUserTrackingDB_Username",
                    "" + prefs.getValue("pref_sciGwyUserTrackingDB_Username",
                    appInitPreferences.getSciGwyUserTrackingDB_Username()));
            appPreferences.updateValue("sciGwyUserTrackingDB_Password",
                    "" + prefs.getValue("pref_sciGwyUserTrackingDB_Password",
                    appInitPreferences.getSciGwyUserTrackingDB_Password()));
            appPreferences.updateValue("sciGwyUserTrackingDB_Database",
                    "" + prefs.getValue("pref_sciGwyUserTrackingDB_Database",
                    appInitPreferences.getSciGwyUserTrackingDB_Database()));
            // Assigns the log level      
            _log.setLogLevel(appPreferences.getLogLevel());

            // Show preference values into log
            _log.info(appPreferences.dump());
        } // if
    } // getPreferences

    /**
     * This method just calls the jsp responsible to show the portlet
     * information This method is equivalent to the doView method
     *
     * @param request Render request object instance
     * @param response Render response object isntance
     *
     * @throws PortletException
     * @throws IOException
     */
    @Override
    public void doHelp(RenderRequest request, RenderResponse response)
            throws PortletException, IOException {
        _log.info("Calling doHelp ...");
        response.setContentType("text/html");
        request.setAttribute("portletVersion", appPreferences.getPortletVersion());
        PortletRequestDispatcher dispatcher = getPortletContext().getRequestDispatcher("/help.jsp");
        dispatcher.include(request, response);
    } // doHelp

} // pidAdmin_portlet
