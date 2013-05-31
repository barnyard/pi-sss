/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.filter;

import java.io.StringWriter;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

/**
 * This is a horrible class to insert a simple "anonymous" namespace declaration into the xml responses. It seems that
 * the format <Tag xmlns="....">
 * 
 * is more client compatible than
 * 
 * <ns1:Tag xmlns:ns1="...."?>
 * 
 * but JAXB seems unable to provide that format and other databinding tools are far less easy to use, especially as the
 * S3 schema seems to be used in a slightly inconsistent way.
 * 
 */
public class NamespaceManglingFilter implements com.sun.jersey.spi.container.ContainerResponseFilter {
    private static final Log LOG = LogFactory.getLog(NamespaceManglingFilter.class);

    public NamespaceManglingFilter() {
        super();
    }

    @Override
    public ContainerResponse filter(ContainerRequest containerRequest, ContainerResponse containerResponse) {
        if (null == containerResponse.getEntity())
            return containerResponse;
        if (!containerResponse.getEntity().getClass().isAnnotationPresent(XmlRootElement.class))
            return containerResponse;
        XmlRootElement annotation = containerResponse.getEntity().getClass().getAnnotation(XmlRootElement.class);
        String name = annotation.name();
        if (null == name || name.equals("##default"))
            return containerResponse;

        StringWriter sw = new StringWriter();
        try {
            JAXBContext c = getJAXBContext(containerResponse.getEntity().getClass());
            Marshaller marshaller = c.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);
            marshaller.marshal(containerResponse.getEntity(), sw);
            String xml = sw.toString();
            xml = xml.replace(String.format("<%s>", name), String.format("<%s xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\">", name));
            // DL: removed this log line, as it was spewing very large messages when user has several buckets
            // LOG.debug(xml);
            containerResponse.setEntity(xml);
            LOG.debug("http headers: " + containerResponse.getHttpHeaders());
            // set the content-length otherwise the server seems to set the Content-Encoding to chunked which some
            // clients don't like
            MultivaluedMap<String, Object> requestHeaders = containerResponse.getHttpHeaders();

            requestHeaders.remove(HttpHeaders.CONTENT_LENGTH);
            requestHeaders.add(HttpHeaders.CONTENT_LENGTH, xml.length());
        } catch (JAXBException e) {
            LOG.error("error formatting xml", e);
            ResponseBuilder responseBuilder = Response.serverError();
            responseBuilder.entity(e.getMessage());
            throw new WebApplicationException(responseBuilder.build());
        }
        return containerResponse;
    }

    @SuppressWarnings("unchecked")
    protected JAXBContext getJAXBContext(Class clazz) throws JAXBException {
        return JAXBContext.newInstance(clazz);
    }
}
