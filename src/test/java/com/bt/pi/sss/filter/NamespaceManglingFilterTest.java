package com.bt.pi.sss.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.bt.pi.sss.filter.NamespaceManglingFilter;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;

public class NamespaceManglingFilterTest {
    private static final String NAME = "Root";
    private NamespaceManglingFilter namespaceManglingFilter;
    private ContainerRequest containerRequest;
    private ContainerResponse containerResponse;

    @Before
    public void setUp() throws Exception {
        this.namespaceManglingFilter = new NamespaceManglingFilter();
        containerRequest = Mockito.mock(ContainerRequest.class);
        containerResponse = Mockito.mock(ContainerResponse.class);
    }

    // happy path
    @SuppressWarnings("unchecked")
    @Test
    public void testFilter() {
        // setup
        Mockito.when(containerResponse.getEntity()).thenReturn(new TestXml1());
        MultivaluedMap<String, Object> headers = Mockito.mock(MultivaluedMap.class);
        Mockito.when(containerResponse.getHttpHeaders()).thenReturn(headers);
        final StringBuffer buff = new StringBuffer();
        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String xml = (String) invocation.getArguments()[0];
                buff.append(xml);
                return null;
            }
        }).when(containerResponse).setEntity(Matchers.isA(String.class));

        // act
        this.namespaceManglingFilter.filter(containerRequest, containerResponse);

        // assert
        String expectedResult = "<Root xmlns=\"http://doc.s3.amazonaws.com/2006-03-01\"><data>fred</data></Root>";
        assertTrue(buff.toString().contains(expectedResult));
    }

    @XmlRootElement(name = NAME)
    @XmlAccessorType(XmlAccessType.FIELD)
    static class TestXml1 {
        @SuppressWarnings("unused")
        private String data = "fred";
    }

    @Test
    public void testFilterEntityNull() {
        // setup
        Mockito.when(containerResponse.getEntity()).thenReturn(null);

        // act
        ContainerResponse result = this.namespaceManglingFilter.filter(containerRequest, containerResponse);

        // assert
        assertEquals(containerResponse, result);
        Mockito.verify(containerResponse, Mockito.never()).setEntity(Matchers.anyObject());
    }

    @Test
    public void testFilterEntityNotJaxb() {
        // setup
        Mockito.when(containerResponse.getEntity()).thenReturn("a string");

        // act
        ContainerResponse result = this.namespaceManglingFilter.filter(containerRequest, containerResponse);

        // assert
        assertEquals(containerResponse, result);
        Mockito.verify(containerResponse, Mockito.never()).setEntity(Matchers.anyObject());
    }

    @Test
    public void testFilterEntityJaxbWithNoRootElementName() {
        // setup
        Mockito.when(containerResponse.getEntity()).thenReturn(new TestXml2());

        // act
        ContainerResponse result = this.namespaceManglingFilter.filter(containerRequest, containerResponse);

        // assert
        assertEquals(containerResponse, result);
        Mockito.verify(containerResponse, Mockito.never()).setEntity(Matchers.anyObject());
    }

    @XmlRootElement
    @XmlAccessorType(XmlAccessType.FIELD)
    static class TestXml2 {
        @SuppressWarnings("unused")
        private String data = "fred";
    }

    @Test(expected = WebApplicationException.class)
    public void testFilterJAXBException() {
        // setup
        final String message = "shit happens";
        this.namespaceManglingFilter = new NamespaceManglingFilter() {
            @SuppressWarnings("unchecked")
            @Override
            protected JAXBContext getJAXBContext(Class clazz) throws JAXBException {
                throw new JAXBException(message);
            }
        };
        Mockito.when(containerResponse.getEntity()).thenReturn(new TestXml1());

        // act
        try {
            this.namespaceManglingFilter.filter(containerRequest, containerResponse);
        } catch (WebApplicationException e) {
            // assert
            assertEquals(Status.INTERNAL_SERVER_ERROR.getStatusCode(), e.getResponse().getStatus());
            throw e;
        }
    }
}
