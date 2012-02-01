/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.jasig.portal.rendering;

import java.util.Map;

import javax.portlet.WindowState;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.jasig.portal.layout.IUserLayoutManager;
import org.jasig.portal.layout.om.IStylesheetDescriptor;
import org.jasig.portal.layout.om.IStylesheetParameterDescriptor;
import org.jasig.portal.portlet.PortletUtils;
import org.jasig.portal.portlet.om.IPortletWindow;
import org.jasig.portal.portlet.registry.IPortletWindowRegistry;
import org.jasig.portal.url.IPortalRequestInfo;
import org.jasig.portal.url.IUrlSyntaxProvider;
import org.jasig.portal.utils.Tuple;
import org.jasig.portal.utils.cache.CacheKey;
import org.jasig.portal.xml.stream.FilteringXMLEventReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Enforces minimized/detached window states for portlets when rendering a mobile view
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public class MobileWindowStateSettingsStAXComponent extends StAXPipelineComponentWrapper {
    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private IUrlSyntaxProvider urlSyntaxProvider;
    private IPortletWindowRegistry portletWindowRegistry;
    private StylesheetAttributeSource stylesheetAttributeSource;

    @Autowired
    public void setPortletWindowRegistry(IPortletWindowRegistry portletWindowRegistry) {
        this.portletWindowRegistry = portletWindowRegistry;
    }

    @Autowired
    public void setUrlSyntaxProvider(IUrlSyntaxProvider urlSyntaxProvider) {
        this.urlSyntaxProvider = urlSyntaxProvider;
    }

    public void setStylesheetAttributeSource(StylesheetAttributeSource stylesheetAttributeSource) {
        this.stylesheetAttributeSource = stylesheetAttributeSource;
    }

    /* (non-Javadoc)
     * @see org.jasig.portal.rendering.PipelineComponent#getCacheKey(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public CacheKey getCacheKey(HttpServletRequest request, HttpServletResponse response) {
        //Initiating rendering of portlets will change the stream at all
        return this.wrappedComponent.getCacheKey(request, response);
    }
    
    /* (non-Javadoc)
     * @see org.jasig.portal.rendering.PipelineComponent#getEventReader(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    public PipelineEventReader<XMLEventReader, XMLEvent> getEventReader(HttpServletRequest request, HttpServletResponse response) {
        final PipelineEventReader<XMLEventReader, XMLEvent> pipelineEventReader = this.wrappedComponent.getEventReader(request, response);

        final XMLEventReader eventReader = pipelineEventReader.getEventReader();
        
        final IStylesheetDescriptor stylesheetDescriptor = stylesheetAttributeSource.getStylesheetDescriptor(request);
        
        final IStylesheetParameterDescriptor defaultWindowStateParam = stylesheetDescriptor.getStylesheetParameterDescriptor("defaultWindowState");
        
        final XMLEventReader filteredEventReader;
        if (defaultWindowStateParam != null) {
            final IPortalRequestInfo portalRequestInfo = this.urlSyntaxProvider.getPortalRequestInfo(request);
            
            if (portalRequestInfo.getTargetedPortletWindowId() == null) {
                final WindowState windowState = PortletUtils.getWindowState(defaultWindowStateParam.getDefaultValue());
                filteredEventReader = new PortletMinimizingXMLEventReader(request, eventReader, windowState);
            }
            else {
                //single portlet targeted, no minimization needed
                filteredEventReader = eventReader;
            }
        }
        else {
            //Not mobile, don't bother filtering
            filteredEventReader = eventReader;
        }
        
        final Map<String, String> outputProperties = pipelineEventReader.getOutputProperties();
        return new PipelineEventReaderImpl<XMLEventReader, XMLEvent>(filteredEventReader, outputProperties);
    }

    private class PortletMinimizingXMLEventReader extends FilteringXMLEventReader {
        private final HttpServletRequest request;
        private final WindowState state;
        
        public PortletMinimizingXMLEventReader(HttpServletRequest request, XMLEventReader reader, WindowState state) {
            super(reader);
            this.request = request;
            this.state = state;
        }

        @Override
        protected XMLEvent filterEvent(XMLEvent event, boolean peek) {
        	if (event.isStartElement()) {
                final StartElement startElement = event.asStartElement();
                
                final QName name = startElement.getName();
                final String localName = name.getLocalPart();
                if (IUserLayoutManager.CHANNEL.equals(localName) || IUserLayoutManager.CHANNEL_HEADER.equals(localName)) {
                    final Tuple<IPortletWindow, StartElement> portletWindowAndElement = portletWindowRegistry.getPortletWindow(request, startElement);
					if (portletWindowAndElement == null) {
						logger.warn("No portlet window found for: "  + localName);
						return null;
					}
					
					final IPortletWindow portletWindow = portletWindowAndElement.first;
					
					//Set the portlet's state, skip if the state is already correct
					if (!state.equals(portletWindow.getWindowState())) {
    					portletWindow.setWindowState(state);
    					
    					portletWindowRegistry.storePortletWindow(request, portletWindow);
					}
					
					return portletWindowAndElement.second;
                }
            } 
            
            return event;
        }
    }
}