
<%
/**
 * Copyright (c) 2000-2011 Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
 

%>



<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet"%>

<portlet:defineObjects />
<%//
  // Multi infrastructure portlet 
  //
%>

<%
// Below the descriptive area of the GATE web form 
%>
<table>
	<tr>
		<td valign="top"></td>
		<td> PID administration</td>
		</td>
	<tr>
	<tr>
		</td> You can add a new PID, list the existing ones and edit/delete them </td>
	</tr>
</table align="center">
<%
// Below the application submission web form 
//
// The <form> tag contains a portlet parameter value called 'PortletStatus' the value of this item
// will be read by the processAction portlet method which then assigns a proper view mode before
// the call to the doView method.
// PortletStatus values can range accordingly to values defined into Enum type: Actions
// The processAction method will assign a view mode accordingly to the values defined into
// the Enum type: Views. This value will be assigned calling the function: setRenderParameter
//
%>
<center>
	<form 
		action="<portlet:actionURL portletMode="view"><portlet:param name="PortletStatus" value="ACTION_SUBMIT"/></portlet:actionURL>"
		method="post">
		<dl>
			<!-- This block contains: label, file input and textarea for GATE Macro file -->
			<dd>
				<p>
				<td>
					<b>New PID</b> 
						Portlet: 
						 <select name="portlet">   
						 <jsp:useBean id="portlets" class="java.lang.String"          scope="request"/>
						  
						 <%
						 
						 String[] portletsSplitted = portlets.split(",");
						 for (int i = 0; i < portletsSplitted.length; i++)
						 	out.println("<option value=\"" + portletsSplitted[i] + "\">" + portletsSplitted[i] + "</option>");
						%>
						  </select>
						  
						  name: 
						  <input type="text" name="pidName" id="pidName" />
						  
						  URL:
						   <input type="text" name="inputPIDURL" id="inputPIDURL" />
 				</td>		
 				<td>
 					<input type="submit" value="Create" onClick="preSubmitPID()">
 				</td>
 
				</p>
	</form>
				
				<jsp:useBean id="pidList" class="java.lang.String"          scope="request"/>
	<form 
		action="<portlet:actionURL portletMode="view"><portlet:param name="PortletStatus" value="ACTION_SUBMIT"/></portlet:actionURL>"
		method="post">
			</dd>
			<!-- This block contains the experiment name -->
			<dd>
				<p>
					PID LIST</b>
				</p>
				
				<p>
				

       <table id="view" style="width:100%">
            <head>
                <tr>
                    <th>
                        Portlet
                    </th>
                    <th>
                        PID name
                    </th>
                    <th>
                       Delete
                    </th>

                </tr>
            </head>
            <body>

					<%
					String[] fragments = pidList.split(",");
					if (fragments.length > 1)
						for (int i = 0; i < fragments.length; i+=2)
		            		out.println("<tr><td>" + fragments[i] + "</td><td>" + fragments[i+1] + "</td><td><button type=\"submit\" name=\"delete\" value=" + fragments[i+1] + " >Delete</button></td></tr>");
		        	 %>
		     
            </body>
        </table>


			</dd>
		</dl>
	</form>
	
	
	
	</table>
</center>

<%
// Below the javascript functions used by the GATE web form 
%>
<script type="text/javascript">
//
// preSubmit
//
function preSubmitPID() {  
    var pidName=document.getElementById('pidName');
    var inputPIDURL=document.getElementById('inputPIDURL');
   
    
    var state_pidName=false;
    var state_url=false;
    
    if(pidName.value=="") state_pidName=true;
    if(inputPIDURL.value=="") state_url=true;
    
    var missingFields="";

   	if(state_pidName) missingFields+="  PID name\n";
   	if(state_url) missingFields+=" URL\n";

    if(missingFields == "") {
      document.forms[0].submit();
      
    }
    else {
      alert("Incomplete information:\n"+missingFields);
        
    }
}

</script>