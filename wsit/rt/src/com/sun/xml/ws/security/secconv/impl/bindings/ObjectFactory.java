//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.0-b26-ea3 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2006.02.24 at 05:55:09 PM PST 
//


package com.sun.xml.ws.security.secconv.impl.bindings;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;
import com.sun.xml.ws.security.secconv.impl.bindings.DerivedKeyTokenType;
import com.sun.xml.ws.security.secconv.impl.bindings.ObjectFactory;
import com.sun.xml.ws.security.secconv.impl.bindings.PropertiesType;
import com.sun.xml.ws.security.secconv.impl.bindings.SecurityContextTokenType;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.sun.xml.ws.security.secconv.impl.bindings package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Instance_QNAME = new QName("http://schemas.xmlsoap.org/ws/2005/02/sc", "Instance");
    private final static QName _DerivedKeyToken_QNAME = new QName("http://schemas.xmlsoap.org/ws/2005/02/sc", "DerivedKeyToken");
    private final static QName _Identifier_QNAME = new QName("http://schemas.xmlsoap.org/ws/2005/02/sc", "Identifier");
    private final static QName _Nonce_QNAME = new QName("http://schemas.xmlsoap.org/ws/2005/02/sc", "Nonce");
    private final static QName _Name_QNAME = new QName("http://schemas.xmlsoap.org/ws/2005/02/sc", "Name");
    private final static QName _SecurityContextToken_QNAME = new QName("http://schemas.xmlsoap.org/ws/2005/02/sc", "SecurityContextToken");
    private final static QName _Label_QNAME = new QName("http://schemas.xmlsoap.org/ws/2005/02/sc", "Label");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.sun.xml.ws.security.secconv.impl.bindings
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link SecurityContextTokenType }
     * 
     */
    public SecurityContextTokenType createSecurityContextTokenType() {
        return new SecurityContextTokenType();
    }

    /**
     * Create an instance of {@link DerivedKeyTokenType }
     * 
     */
    public DerivedKeyTokenType createDerivedKeyTokenType() {
        return new DerivedKeyTokenType();
    }

    /**
     * Create an instance of {@link PropertiesType }
     * 
     */
    public PropertiesType createPropertiesType() {
        return new PropertiesType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2005/02/sc", name = "Instance")
    public JAXBElement<String> createInstance(String value) {
        return new JAXBElement<String>(_Instance_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link DerivedKeyTokenType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2005/02/sc", name = "DerivedKeyToken")
    public JAXBElement<DerivedKeyTokenType> createDerivedKeyToken(DerivedKeyTokenType value) {
        return new JAXBElement<DerivedKeyTokenType>(_DerivedKeyToken_QNAME, DerivedKeyTokenType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2005/02/sc", name = "Identifier")
    public JAXBElement<String> createIdentifier(String value) {
        return new JAXBElement<String>(_Identifier_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link byte[]}{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2005/02/sc", name = "Nonce")
    public JAXBElement<byte[]> createNonce(byte[] value) {
        return new JAXBElement<byte[]>(_Nonce_QNAME, byte[].class, null, ((byte[]) value));
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2005/02/sc", name = "Name")
    public JAXBElement<String> createName(String value) {
        return new JAXBElement<String>(_Name_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SecurityContextTokenType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2005/02/sc", name = "SecurityContextToken")
    public JAXBElement<SecurityContextTokenType> createSecurityContextToken(SecurityContextTokenType value) {
        return new JAXBElement<SecurityContextTokenType>(_SecurityContextToken_QNAME, SecurityContextTokenType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2005/02/sc", name = "Label")
    public JAXBElement<String> createLabel(String value) {
        return new JAXBElement<String>(_Label_QNAME, String.class, null, value);
    }

}
