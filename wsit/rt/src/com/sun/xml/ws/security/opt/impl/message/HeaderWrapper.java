
/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License. You can obtain
 * a copy of the License at https://glassfish.dev.java.net/public/CDDL+GPL.html
 * or glassfish/bootstrap/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 * 
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/bootstrap/legal/LICENSE.txt.
 * Sun designates this particular file as subject to the "Classpath" exception
 * as provided by Sun in the GPL Version 2 section of the License file that
 * accompanied this code.  If applicable, add the following below the License
 * Header, with the fields enclosed by brackets [] replaced by your own
 * identifying information: "Portions Copyrighted [year]
 * [name of copyright owner]"
 * 
 * Contributor(s):
 * 
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package com.sun.xml.ws.security.opt.impl.message;

import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.addressing.WSEndpointReference;
import com.sun.xml.ws.security.opt.api.SecurityElement;
import com.sun.xml.ws.security.opt.api.SecurityElementWriter;
import com.sun.xml.ws.security.opt.impl.outgoing.SecurityHeader;
import com.sun.istack.NotNull;
import com.sun.xml.bind.api.Bridge;
import com.sun.xml.bind.api.BridgeContext;
import com.sun.xml.ws.api.SOAPVersion;
import com.sun.xml.ws.security.opt.impl.dsig.SignedMessageHeader;
import org.xml.sax.ContentHandler;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.Set;

/**
 *
 * @author K.Venugopal@sun.com
 */
public class HeaderWrapper implements com.sun.xml.ws.api.message.Header  {
    
    private SecurityHeader sh;
    private SecurityElement se;
    private com.sun.xml.ws.api.message.Header header = null;
    /** Creates a new instance of HeaderWrapper */
    public HeaderWrapper(SecurityHeader sh) {
        this.sh = sh;
    }
    
    public HeaderWrapper(SecurityElement se) {
        this.se = se;
        if(this.se instanceof SignedMessageHeader){
            header = ((SignedMessageHeader)se).getSignedHeader();
        }
    }
    /**
     * Checks if this header is ignorable for us (IOW, make sure
     * that this header has a problematic "mustUnderstand" header value
     * that we have to reject.)
     *
     * <p>
     * This method is used as a part of the
     * <a href="HeaderList.html#MU">mustUnderstanx processing</a>.
     * At the end of the processing, the JAX-WS identifies a list of {@link Header}s
     * that were not understood. This method is invoked on those {@link Header}s,
     * to verify that we don't need to report an error for it.
     *
     * <p>
     * specifically, this method has to perform the following tasks:
     *
     * <ul>
     *  <li>If this header does not have <tt>mustUnderstand</tt> as "1" nor "true",
     *      then this method must return true.
     *  <li>Otherwise, check the role attribute (for SOAP 1.2) or the actor attribute (for SOAP 1.1).
     *      When those attributes are absent, the default values have to be assumed.
     *      See {@link #getRole(SOAPVersion)} for how the values are defaulted.
     *      Now, see if the {@code roles} set contains the value.
     *      If so, this method must return false (indicating that an error is in order.)
     *  <li>Otherwise return true (since we don't play the role this header is intended for.)
     * </ul>
     *
     * @param soapVersion
     *      The caller specifies the SOAP version that the pipeline is working against.
     *      Often each {@link Header} implementation already knows the SOAP version
     *      anyway, but this allows some {@link Header}s to avoid keeping it.
     *      That's why this redundant parameter is passed in.
     * @param roles
     *      The set of role values that the current JAX-WS pipeline is assuming.
     *      Note that SOAP 1.1 and SOAP 1.2 use different strings for the same role,
     *      and the caller is responsible for supplying a proper value depending on the
     *      active SOAP version in use.
     *
     * @return
     *      true if no error needs to be reported. False if an error needs to be raised.
     *      See the method javadoc for more discussion.
     */
    public boolean isIgnorable( SOAPVersion soapVersion,  Set<String> roles){
        if(header != null){
            return header.isIgnorable(soapVersion, roles);
        }
        throw new UnsupportedOperationException();
    }
    
    /**
     * Gets the value of the soap:role attribute (or soap:actor for SOAP 1.1).
     *
     * <p>
     * If the attribute is omitted, the value defaults to {@link SOAPVersion#implicitRole}.
     *
     * @param soapVersion
     *      The caller specifies the SOAP version that the pipeline is working against.
     *      Often each {@link Header} implementation already knows the SOAP version
     *      anyway, but this allows some {@link Header}s to avoid keeping it.
     *      That's why this redundant parameter is passed in.
     * @return
     *      never null. This string need not be interned.
     */
    public String getRole(SOAPVersion soapVersion){
        if(header != null){
            return header.getRole(soapVersion);
        }
        throw new UnsupportedOperationException();
    }
    
    /**
     * True if this header is to be relayed if not processed.
     * For SOAP 1.1 messages, this method always return false.
     *
     * <p>
     * IOW, this method returns true if there's @soap:relay='true'
     * is present.
     *
     * <h3>Implementation Note</h3>
     * <p>
     * The implementation needs to check for both "true" and "1",
     * but because attribute values are normalized, it doesn't have
     * to consider " true", " 1 ", and so on.
     *
     * @return
     *      false.
     */
    public boolean isRelay(){
        if(header != null){
            return header.isRelay();
        }
        throw new UnsupportedOperationException();
    }
    
    /**
     * Gets the namespace URI of this header element.
     *
     * @return
     *      this string must be interned.
     */
    public  String getNamespaceURI(){
        return se.getNamespaceURI();
    }
    
    /**
     * Gets the local name of this header element.
     *
     * @return
     *      this string must be interned.
     */
    public  String getLocalPart(){
        return se.getLocalPart();
    }
    
    /**
     * Gets the attribute value on the header element.
     *
     * @param nsUri
     *      The namespace URI of the attribute. Can be empty.
     * @param localName
     *      The local name of the attribute.
     *
     * @return
     *      if the attribute is found, return the whitespace normalized value.
     *      (meaning no leading/trailing space, no consequtive whitespaces in-between.)
     *      Otherwise null. Note that the XML parsers are responsible for
     *      whitespace-normalizing attributes, so {@link Header} implementation
     *      doesn't have to do anything.
     */
    public   String getAttribute( String nsUri,  String localName){
        if(header != null){
            return header.getAttribute(nsUri, localName);
        }
        throw new UnsupportedOperationException();
    }
    
    /**
     * Gets the attribute value on the header element.
     *
     * <p>
     * This is a convenience method that calls into {@link #getAttribute(String, String)}
     *
     * @param name
     *      Never null.
     *
     * @see #getAttribute(String, String)
     */
    public String getAttribute( QName name){
        if(header != null){
            return header.getAttribute(name);
        }
        throw new UnsupportedOperationException();
    }
    
    /**
     * Reads the header as a {@link XMLStreamReader}.
     *
     * <p>
     * The returned parser points at the start element of this header.
     * (IOW, {@link XMLStreamReader#getEventType()} would return
     * {@link XMLStreamReader#START_ELEMENT}.
     *
     * <h3>Performance Expectation</h3>
     * <p>
     * For some {@link Header} implementations, this operation
     * is a non-trivial operation. Therefore, use of this method
     * is discouraged unless the caller is interested in reading
     * the whole header.
     *
     * <p>
     * Similarly, if the caller wants to use this method only to do
     * the API conversion (such as simply firing SAX events from
     * {@link XMLStreamReader}), then the JAX-WS team requests
     * that you talk to us.
     *
     * <p>
     * {@link Message}s that come from tranport usually provides
     * a reasonably efficient implementation of this method.
     *
     * @return
     *      must not null.
     */
    public XMLStreamReader readHeader() throws XMLStreamException{
        if(header != null){
            return header.readHeader();
        }
        throw new UnsupportedOperationException();
        //We should avoid such operations for Security operated headers.
        
    }
    
    /**
     * Reads the header as a JAXB object by using the given unmarshaller.
     */
    @SuppressWarnings("unchecked")
    public <T> T readAsJAXB(Unmarshaller unmarshaller) throws JAXBException{
        if(header != null){
            return (T)header.readAsJAXB(unmarshaller);
        }
        throw new UnsupportedOperationException();
    }
    
   
    
    /**
     * Reads the header as a JAXB object by using the given unmarshaller.
     */
    public <T> T readAsJAXB(Bridge<T> bridge) throws JAXBException{
        if(header != null){
            return header.readAsJAXB(bridge);
        }
        throw new UnsupportedOperationException();
    }
    
    /**
     * Writes out the header.
     *
     * @throws XMLStreamException
     *      if the operation fails for some reason. This leaves the
     *      writer to an undefined state.
     */
    public void writeTo(XMLStreamWriter w) throws XMLStreamException{
        ((SecurityElementWriter)se).writeTo(w);
    }
    
    /**
     * Writes out the header to the given SOAPMessage.
     *
     * <p>
     * Sometimes a {@link Message} needs to produce itself
     * as {@link SOAPMessage}, in which case each header needs
     * to turn itself into a header.
     *
     * @throws SOAPException
     *      if the operation fails for some reason. This leaves the
     *      writer to an undefined state.
     */
    public void writeTo(SOAPMessage saaj) throws SOAPException{
        throw new UnsupportedOperationException("use writeTo(XMLStreamWriter w) ");
    }
    
    /**
     * Writes out the header as SAX events.
     *
     * <p>
     * Sometimes a {@link Message} needs to produce SAX events,
     * and this method is necessary for headers to participate to it.
     *
     * <p>
     * A header is responsible for producing the SAX events for its part,
     * including <tt>startPrefixMapping</tt> and <tt>endPrefixMapping</tt>,
     * but not startDocument/endDocument.
     *
     * <p>
     * Note that SAX contract requires that any error that does NOT originate
     * from {@link ContentHandler} (meaning any parsing error and etc) must
     * be first reported to {@link ErrorHandler}. If the SAX event production
     * cannot be continued and the processing needs to abort, the code may
     * then throw the same {@link SAXParseException} reported to {@link ErrorHandler}.
     *
     * @param contentHandler
     *      The {@link ContentHandler} that receives SAX events.
     *
     * @param errorHandler
     *      The {@link ErrorHandler} that receives parsing errors.
     */
    public void writeTo(ContentHandler contentHandler, ErrorHandler errorHandler) throws SAXException{
        throw new UnsupportedOperationException("use writeTo(XMLStreamWriter w) ");
    }
    
    public String getStringContent(){
        if(header != null){
            return header.getStringContent();
        }
        throw new UnsupportedOperationException();
    }
    public @NotNull WSEndpointReference readAsEPR(AddressingVersion expected) throws XMLStreamException{
        if(header != null){
            return header.readAsEPR(expected);
        }
        throw new UnsupportedOperationException();
    }
    
}
