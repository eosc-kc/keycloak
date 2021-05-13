package org.keycloak.saml.processing.core.parsers.saml.metadata;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;
import org.keycloak.saml.processing.core.parsers.saml.assertion.SAMLAttributeValueParser;

public abstract class SAMLAttributeTypeParser  extends AbstractStaxSamlMetadataParser<AttributeType> {
	
	 public SAMLAttributeTypeParser(SAMLMetadataQNames element) {
	        super(element);
	    }
	
	 @Override
	    protected AttributeType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
	        String name = StaxParserUtil.getRequiredAttributeValue(element, SAMLMetadataQNames.ATTR_NAME);
	        final AttributeType attribute = new AttributeType(name);

	        attribute.setFriendlyName(StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_FRIENDLY_NAME));
	        attribute.setNameFormat(StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_NAME_FORMAT));

	        final String x500Encoding = StaxParserUtil.getAttributeValue(element, SAMLMetadataQNames.ATTR_X500_ENCODING);
	        if (x500Encoding != null) {
	            attribute.getOtherAttributes().put(SAMLMetadataQNames.ATTR_X500_ENCODING.getQName(), x500Encoding);
	        }

	        return attribute;
	    }

	    @Override
	    protected void processSubElement(XMLEventReader xmlEventReader, AttributeType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
	        switch (element) {
	            case ATTRIBUTE_VALUE:
	                target.addAttributeValue(SAMLAttributeValueParser.getInstance().parse(xmlEventReader));
	                break;

	            default:
	                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
	        }
	    }

}
