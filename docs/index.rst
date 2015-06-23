********
PIDADMIN
********

============
About
============


A persistent identifier (PID) is a long-lasting reference to a digital object—a single file or set of files. With this approach, references to the data are decoupled from its physical location, thus allowing a high degree of flexibility while ensuring that the information will always be accessible.  Catania Science Gateway is committed to this approach, allowing the usage of PIDs to specify the location of the input files of many of the integrated applications. Of course, the user can also employ it to identify the place where the output of a given execution has been stored.  pidAdmin is a very simple application devoted to manage these PIDs. It allows to create or delete PIDs corresponding to the different existing applications, greatly simplifying their management and making it accesible to any user of Catania Science Gateway.

============
Installation
============
Following instructions are meant for science gateway maintainers while generic users can skip this section.
To install the portlet it is enough to install the war file into the application server and then configure the preference settings into the 
portlet preferences pane.

Preferences are splitted in three separate parts: Generic, Infrastructures and the application execution setting. 
The generic part contains the **Log level** which contains one of following values, sorted by decreasing level: info, debug, warning and err
or. 

The **Application Identifier** refers to theId field value of the GridEngine 'UsersTracking'database table: GridInteractions.
The infrastructure part consists of different settings related to the destination of users job execution. The fields belonging to this categ
ory are:

 **Enable infrastructure**: A true/false flag which enables or disable the current infrastructure;

 **Infrastructure Name**: The infrastructure name for these settings;   

 **Infrastructure Acronym**: A short name representing the infrastructure;

 **BDII host**: The Infrastructure information system endpoint (URL). Infrastructure preferences have been thought initially for the elite G
rid based infrastructures; 

 **WMS host**: It is possible to specify which is the brokering service endpoint (URL);

 **Robot Proxy values**: This is a collection of several values which configures the robot proxy settings (Host, Port, proxyID, VO, Role, pr
oxy renewal);

 **Job requirements**: This field contains the necessary statements to specify a job execution requirement, such as a particular software, a
 particular number of CPUs/RAM, etc.

.. image:: images/settings.jpg

Actually, depending on the infrastructure, some of the fields above have an overloaded meaning. Please contact the support for further infor
mation or watch existing production portlet settings.

============
Usage
============

pidAdmin allows to manage PIDs. Using its very simple interface user can create, list and delete them. As PIDs can be related to different portlets, they can be chosen in the menu.

.. image:: images/input.png
   :align: center


============
References
============

.. _1: http://agenda.ct.infn.it/event/1110/

* CHAIN-REDS Conference: *"Open Science at the Global Scale: Sharing e-Infrastructures, Sharing Knowledge, Sharing Progress"* – March 31, 2015 – Brussels, Belgium [1_];

============
Contributors
============
Please feel free to contact us any time if you have any questions or comments.

.. _Sci-Track: http://rdgroups.ciemat.es/web/sci-track/

:Authors:
 `Manuel RODRIGUEZ-PASCUAL <mailto:manuel.rodriguez@ciemat.es>`_ - CIEMAT Sci-Track



