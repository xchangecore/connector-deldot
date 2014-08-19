connector-deldot
================

This connector allows user to poll the Delaware Department of Transportation's Real-Time Travel Advisories feed and creates incidents on the XchangeCore.

Dependencies:
connector-base-util
connector-base-async

To Build:
1. Use maven and run "mvn clean install" to build all the dependencies
2. Run "mvn clean install" to build deldot connector

To Run:
1. Copy the deldot/src/main/resources/contexts/deldotAdapter-context to the same directory of the deldotAdapter.jar file.
2. Use an editor to open the deldotAdapter-context file.
3. Look for the webServiceTemplate bean, replace the "defaultUri" to the XchangeCore you are using to run this adapter to create the incidents.
   If not localhost, change http to https, example "https://test4.xchangecore.leidos.com/uicds/core/ws/services"
4. Change the "credentials" to a valid username and password that can access your XchangeCore.
5. Open a cygwin or windows, change directory to where the deldotAdapter.jar file is located, run "java -jar deldotAdapter.jar"


