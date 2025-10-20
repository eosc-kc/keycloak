package org.keycloak.saml.processing.core.parsers.saml.metadata;

import static org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLMetadataQNames.ATTR_LANG;

import java.io.Serializable;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.metadata.EntityAttributes;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

public class SAMLEntityAttributesParser extends AbstractStaxSamlMetadataParser<EntityAttributes> implements Serializable {
	 private static final SAMLEntityAttributesParser INSTANCE = new SAMLEntityAttributesParser();

	    private SAMLEntityAttributesParser() {
	        super(SAMLMetadataQNames.ENTITY_ATTRIBUTES);
	    }

	    public static SAMLEntityAttributesParser getInstance() {
	        return INSTANCE;
	    }

	    @Override
	    protected EntityAttributes instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
	    	return new EntityAttributes();
	    }

	    
	    @Override
	    protected void processSubElement(XMLEventReader xmlEventReader, EntityAttributes target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
	        switch (element) {
	            case ATTRIBUTE:
	            	target.addAttribute(SAMLAttributeParser.getInstance().parse(xmlEventReader));
	                break;
	            case ASSERTION:
	            	target.addAttribute(SAMLAssertionParser.getInstance().parse(xmlEventReader));
	                break;
	           
	            default:
	                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
	        }
	    }
	}