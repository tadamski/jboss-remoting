/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.remoting3;

import static javax.xml.stream.XMLStreamConstants.END_ELEMENT;
import static javax.xml.stream.XMLStreamConstants.START_ELEMENT;

import java.io.IOException;
import java.util.Collections;

import org.wildfly.client.config.ClientConfiguration;
import org.wildfly.client.config.ConfigXMLParseException;
import org.wildfly.client.config.ConfigurationXMLStreamReader;
import org.xnio.XnioWorker;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class RemotingXmlParser {
    private static final String NS_REMOTING_5_0 = "urn:jboss-remoting:5.0";

    private RemotingXmlParser() {
    }

    static Endpoint parseEndpoint() throws ConfigXMLParseException, IOException {
        final ClientConfiguration clientConfiguration = ClientConfiguration.getInstance();
        final EndpointBuilder builder = new EndpointBuilder();
        builder.setXnioWorker(XnioWorker.getContextManager().get());
        if (clientConfiguration != null) try (final ConfigurationXMLStreamReader streamReader = clientConfiguration.readConfiguration(Collections.singleton(NS_REMOTING_5_0))) {
            parseDocument(streamReader, builder);
            return builder.build();
        } else {
            return null;
        }
    }

    private static void parseDocument(final ConfigurationXMLStreamReader reader, final EndpointBuilder builder) throws ConfigXMLParseException {
        if (reader.hasNext()) switch (reader.nextTag()) {
            case START_ELEMENT: {
                switch (reader.getNamespaceURI()) {
                    case NS_REMOTING_5_0: break;
                    default: throw reader.unexpectedElement();
                }
                switch (reader.getLocalName()) {
                    case "endpoint": {
                        parseEndpointElement(reader, builder);
                        break;
                    }
                    default: throw reader.unexpectedElement();
                }
                break;
            }
            default: {
                throw reader.unexpectedContent();
            }
        }
    }

    private static void parseEndpointElement(final ConfigurationXMLStreamReader reader, final EndpointBuilder builder) throws ConfigXMLParseException {
        final int attributeCount = reader.getAttributeCount();
        for (int i = 0; i < attributeCount; i++) {
            switch (reader.getAttributeLocalName(i)) {
                case "name": {
                    builder.setEndpointName(reader.getAttributeValueResolved(i));
                    break;
                }
                default: {
                    throw reader.unexpectedAttribute(i);
                }
            }
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case START_ELEMENT: {
                    switch (reader.getNamespaceURI()) {
                        case NS_REMOTING_5_0: break;
                        default: throw reader.unexpectedElement();
                    }
                    switch (reader.getLocalName()) {
                        case "providers": {
                            parseProvidersElement(reader, builder);
                            break;
                        }
                        default: throw reader.unexpectedElement();
                    }
                    break;
                }
                case END_ELEMENT: {
                    return;
                }
            }
        }
    }

    private static void parseProvidersElement(final ConfigurationXMLStreamReader reader, final EndpointBuilder builder) throws ConfigXMLParseException {
        final int attributeCount = reader.getAttributeCount();
        if (attributeCount > 0) {
            throw reader.unexpectedAttribute(0);
        }
        while (reader.hasNext()) {
            switch (reader.nextTag()) {
                case START_ELEMENT: {
                    switch (reader.getNamespaceURI()) {
                        case NS_REMOTING_5_0: break;
                        default: throw reader.unexpectedElement();
                    }
                    switch (reader.getLocalName()) {
                        case "provider": {
                            parseProviderElement(reader, builder);
                            break;
                        }
                        default: throw reader.unexpectedElement();
                    }
                }
                case END_ELEMENT: {
                    return;
                }
            }
        }
    }

    private static void parseProviderElement(final ConfigurationXMLStreamReader reader, final EndpointBuilder builder) throws ConfigXMLParseException {
        final int attributeCount = reader.getAttributeCount();
        String scheme = null;
        String[] aliases = null;
        String module = null;
        String clazz = null;
        for (int i = 0; i < attributeCount; i ++) {
            final String attributeNamespace = reader.getAttributeNamespace(i);
            if (attributeNamespace != null && ! attributeNamespace.isEmpty()) {
                throw reader.unexpectedAttribute(i);
            }
            switch (reader.getAttributeLocalName(i)) {
                case "scheme": {
                    scheme = reader.getAttributeValueResolved(i);
                    break;
                }
                case "aliases": {
                    aliases = reader.getListAttributeValueAsArrayResolved(i);
                    break;
                }
                case "module": {
                    module = reader.getAttributeValueResolved(i);
                    break;
                }
                case "class": {
                    clazz = reader.getAttributeValueResolved(i);
                    break;
                }
                default: {
                    throw reader.unexpectedAttribute(i);
                }
            }
        }
        final ConnectionProviderFactoryBuilder providerBuilder = builder.addProvider(scheme);
        if (aliases != null) for (String alias : aliases) {
            providerBuilder.addAlias(alias);
        }
        if (module == null && clazz == null) {
            throw new ConfigXMLParseException("At least one of the 'module' or 'class' attributes must be given", reader);
        }
        if (module != null) {
            providerBuilder.setModuleName(module);
        }
        if (clazz != null) {
            providerBuilder.setClassName(clazz);
        }
        switch (reader.nextTag()) {
            case END_ELEMENT: {
                return;
            }
            default: {
                throw reader.unexpectedElement();
            }
        }
    }
}
