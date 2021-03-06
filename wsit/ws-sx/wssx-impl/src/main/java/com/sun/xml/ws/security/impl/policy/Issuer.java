/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
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

/*
 * Issuer.java
 *
 * Created on February 22, 2006, 5:01 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.xml.ws.security.impl.policy;

import com.sun.xml.ws.security.addressing.policy.Address;
import com.sun.xml.ws.policy.AssertionSet;
import com.sun.xml.ws.policy.PolicyAssertion;
import com.sun.xml.ws.policy.sourcemodel.AssertionData;
import java.util.Collection;
import java.util.Iterator;
import com.sun.xml.ws.security.policy.SecurityAssertionValidator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
/**
 *
 * @author Abhijit Das
 */
public class Issuer extends PolicyAssertion implements com.sun.xml.ws.security.policy.Issuer, SecurityAssertionValidator {
    private AssertionFitness fitness = AssertionFitness.IS_VALID;
    private Address address;
    private Address metadataAddress;
    private boolean populated = false;
    private PolicyAssertion refProps = null;
    private PolicyAssertion refParams = null;
    private PolicyAssertion serviceName = null;
    private String portType = null;
    private Element identityEle = null;
    
    /**
     * Creates a new instance of Issuer
     */
    public Issuer() {
    }
    
    public Issuer(AssertionData name,Collection<PolicyAssertion> nestedAssertions, AssertionSet nestedAlternative) {
        super(name,nestedAssertions,nestedAlternative);
    }
    
    public AssertionFitness validate(boolean isServer) {
        return populate(isServer);
    }

    private void getAddressFromMetadata(PolicyAssertion addressingMetadata) {
        PolicyAssertion metadata = null;
        PolicyAssertion metadataSection = null;
        PolicyAssertion metadataReference = null;
        if(addressingMetadata != null){
            if ( addressingMetadata.hasParameters() ) {
                final Iterator <PolicyAssertion> iterator = addressingMetadata.getParametersIterator();
                while ( iterator.hasNext() ) {
                    final PolicyAssertion assertion = iterator.next();
                    if ( PolicyUtil.isMetadata(assertion)) {
                        metadata = assertion;
                        break;
                    }
                }
            }
            if(metadata != null){
                if ( metadata.hasParameters() ) {
                    final Iterator <PolicyAssertion> iterator = metadata.getParametersIterator();
                    while ( iterator.hasNext() ) {
                        final PolicyAssertion assertion = iterator.next();
                        if (PolicyUtil.isMetadataSection(assertion)){
                            metadataSection = assertion;
                            break;
                        }
                    }
                }
                if(metadataSection != null){
                    if ( metadataSection.hasParameters() ) {
                        final Iterator <PolicyAssertion> iterator = metadataSection.getParametersIterator();
                        while ( iterator.hasNext() ) {
                            final PolicyAssertion assertion = iterator.next();
                            if ( PolicyUtil.isMetadataReference(assertion)) {
                                metadataReference = assertion;
                                break;
                            }
                        }
                    }
                    if(metadataReference != null){
                        if ( metadataReference.hasParameters() ) {
                            final Iterator <PolicyAssertion> iterator = metadataReference.getParametersIterator();
                            while ( iterator.hasNext() ) {
                                final PolicyAssertion assertion = iterator.next();
                                if ( PolicyUtil.isAddress(assertion)) {
                                    metadataAddress = (Address)assertion;
                                    break;
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    private void populate(){
        populate(false);
    }
    
    private synchronized AssertionFitness populate(boolean isServer) {
        if(!populated){
            if ( this.hasNestedAssertions() ) {
                Iterator <PolicyAssertion> it = this.getNestedAssertionsIterator();
                while ( it.hasNext() ) {
                    PolicyAssertion assertion = it.next();
                    if ( PolicyUtil.isAddress(assertion)) {
                        this.address = (Address) assertion;
                    } else if(PolicyUtil.isPortType(assertion)){
                        this.portType = assertion.getValue();
                    } else if(PolicyUtil.isReferenceParameters(assertion)){
                        this.refParams = assertion;
                    } else if(PolicyUtil.isReferenceProperties(assertion)){
                        this.refProps = assertion;
                    } else if(PolicyUtil.isServiceName(assertion)){
                        this.serviceName = assertion;
                    } else if(PolicyUtil.isAddressingMetadata(assertion)){
                        getAddressFromMetadata(assertion);
                    } else if(Constants.IDENTITY.equals(assertion.getName().getLocalPart())){
                        Document doc = PolicyUtil.policyAssertionToDoc(assertion);
                        identityEle = (Element)doc.getElementsByTagNameNS("*", Constants.IDENTITY).item(0);
                    }
                }
            }
            populated = true;
        }
        return fitness;
    }
    
    public Address getAddress() {
        populate();
        return (Address) address;
    }
    
    public String getPortType(){
        populate();
        return portType;
    }
    
    public PolicyAssertion getReferenceParameters(){
        populate();
        return refParams;
    }
    
    public PolicyAssertion getReferenceProperties(){
        populate();
        return refProps;
    }
    
    public PolicyAssertion getServiceName(){
        populate();
        return serviceName;
    }
    
    public Element getIdentity(){
        populate();
        return identityEle;
    }

    public Address getMetadataAddress() {
        populate();
        return metadataAddress;
    }
}
