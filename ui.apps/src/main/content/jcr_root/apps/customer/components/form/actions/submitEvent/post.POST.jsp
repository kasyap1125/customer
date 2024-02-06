<%@include file="/libs/foundation/global.jsp"%>
<%@page session="false" %>
<%@page import="au.com.customer.core.services.events.EventsRegisterService"%>
<%
    final EventsRegisterService service = sling.getService(EventsRegisterService.class);
    service.handleFormRequest(slingRequest, slingResponse);
%>