package com.bt.pi.sss.request;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

public class CreateBucketConfiguration {

    private static final String LOCATION_CONSTRAINT = "LocationConstraint";
    /**
     * Has to be a valid region name.
     */

    private String locationConstraint;

    public CreateBucketConfiguration(String alocationConstraint) {
        this.locationConstraint = alocationConstraint;
    }

    public String getLocationConstraint() {
        return locationConstraint;
    }

    @Override
    public String toString() {
        return this.getLocationConstraint();
    }

    public static CreateBucketConfiguration parseCreateBucketConfiguration(String createBucketConfigString) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(new InputSource(new StringReader(createBucketConfigString)));
        String locationConstraint = doc.getElementsByTagName(LOCATION_CONSTRAINT).item(0).getFirstChild().getNodeValue();

        return new CreateBucketConfiguration(locationConstraint);

    }

}
