/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License).  You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the license at
 * https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * See the License for the specific language governing
 * permissions and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at https://glassfish.dev.java.net/public/CDDLv1.0.html.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * you own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Copyright 2006 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.xml.ws.policy.jaxws;

import com.sun.xml.ws.api.wsdl.writer.WSDLGenExtnContext;
import com.sun.xml.txw2.TypedXmlWriter;
import com.sun.xml.ws.api.WSBinding;
import com.sun.xml.ws.api.model.CheckedException;
import com.sun.xml.ws.api.model.JavaMethod;
import com.sun.xml.ws.api.model.SEIModel;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLBoundPortType;
import com.sun.xml.ws.api.model.wsdl.WSDLFault;
import com.sun.xml.ws.api.model.wsdl.WSDLInput;
import com.sun.xml.ws.api.model.wsdl.WSDLMessage;
import com.sun.xml.ws.api.model.wsdl.WSDLObject;
import com.sun.xml.ws.api.model.wsdl.WSDLOperation;
import com.sun.xml.ws.api.model.wsdl.WSDLOutput;
import com.sun.xml.ws.api.model.wsdl.WSDLPort;
import com.sun.xml.ws.api.model.wsdl.WSDLPortType;
import com.sun.xml.ws.api.model.wsdl.WSDLService;
import com.sun.xml.ws.api.wsdl.writer.WSDLGeneratorExtension;
import com.sun.xml.ws.model.CheckedExceptionImpl;
import com.sun.xml.ws.model.JavaMethodImpl;
import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.PolicyConstants;
import com.sun.xml.ws.policy.PolicyException;
import com.sun.xml.ws.policy.PolicyMap;
import com.sun.xml.ws.policy.PolicyMapExtender;
import com.sun.xml.ws.policy.PolicyMerger;
import com.sun.xml.ws.policy.PolicySubject;
import com.sun.xml.ws.policy.jaxws.spi.PolicyMapUpdateProvider;
import com.sun.xml.ws.policy.privateutil.PolicyLogger;
import com.sun.xml.ws.policy.privateutil.PolicyUtils;
import com.sun.xml.ws.policy.sourcemodel.PolicyModelGenerator;
import com.sun.xml.ws.policy.sourcemodel.PolicyModelMarshaller;
import com.sun.xml.ws.policy.sourcemodel.PolicySourceModel;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.LinkedList;

/**
 * Marshals the contents of a policy map to WSDL.
 */
public class PolicyWSDLGeneratorExtension extends WSDLGeneratorExtension {
    
    private final static PolicyLogger logger = PolicyLogger.getLogger(PolicyWSDLGeneratorExtension.class);
    
    private PolicyMap policyMap;
    private SEIModel seiModel;
    // TODO Determine if service or port were renamed so that we can just map them like the other elements
    private Policy servicePolicy = null;
    private Policy portPolicy = null;
    private Collection<PolicySubject> subjects = new LinkedList<PolicySubject>();
    
    private PolicyModelMarshaller marshaller = PolicyModelMarshaller.getXmlMarshaller(true);
    private PolicyMerger merger = PolicyMerger.getMerger();
    
    public void start(WSDLGenExtnContext context) {
        logger.entering("start");
        try {
            this.seiModel = context.getModel();
            // QName serviceName = model.getServiceQName();
            PolicyMapUpdateProvider[] policyMapUpdateProviders = PolicyUtils.ServiceProvider.load(PolicyMapUpdateProvider.class);
            PolicyMapExtender[] extenders = new PolicyMapExtender[policyMapUpdateProviders.length];
            for (int i=0; i<extenders.length; i++) {
                extenders[i] = PolicyMapExtender.createPolicyMapExtender();
            }
            
            String configId = context.getEndpointClass().getName();
            policyMap = PolicyConfigParser.parse(configId, context.getContainer(), extenders);
            
            if (policyMap!= null) {
                context.getRoot()._namespace(PolicyConstants.POLICY_NAMESPACE_URI, PolicyConstants.POLICY_NAMESPACE_PREFIX);
                WSBinding binding = context.getBinding();                
                // TODO: review after jax-ws interface to it's runtime wsdl generator gets changed
                for (int i=0; i<policyMapUpdateProviders.length; i++) {
                    policyMapUpdateProviders[i].update(extenders[i], policyMap, seiModel, binding);
                    extenders[i].disconnect();
                }
            }
        } catch (PolicyException e) {
            logger.severe("start", "Failed to read wsit.xml", e);
        } finally {
            logger.exiting("start");
        }
    }
    
    public void addDefinitionsExtension(TypedXmlWriter definitions) {
        try {
            logger.entering("addDefinitionsExtension");
            if (policyMap != null) {
                subjects = policyMap.getPolicySubjects();
                boolean usingPolicy = false;
                PolicyModelGenerator generator = PolicyModelGenerator.getGenerator();
                for (PolicySubject subject : subjects) {
                    Object wsdlSubject = subject.getSubject();
                    if (wsdlSubject != null) {
                        if (!usingPolicy) {
                            definitions._element(PolicyConstants.USING_POLICY, TypedXmlWriter.class);
                            usingPolicy = true;
                        }
                        Policy policy = subject.getEffectivePolicy(merger);
                        if (null != policy.getIdOrName()) {
                            PolicySourceModel policyInfoset = generator.translate(policy);
                            marshaller.marshal(policyInfoset, definitions);
                        }
                    } else {
                        logger.fine("addDefinitionsExtension", "Subject was null, not marshalling attached policy: " + subject);
                    }
                }
            } else {
                logger.fine("addDefinitionsExtension", "Policy map was null, not marshalling any policies");
            }
        } catch (PolicyException e) {
            // TODO Throw WebServiceException
            logger.severe("addDefinitionsExtension", "Failed to marshal policies", e);
        } finally {
            logger.exiting("addDefinitionsExtension");
        }
    }
    
    public void addServiceExtension(TypedXmlWriter service) {
        logger.entering("addServiceExtension");
        if (this.seiModel != null) {
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLService) {
                    processPolicy(subject, service);
                }
            }
        }
        logger.exiting("addServiceExtension");
    }
    
    public void addPortExtension(TypedXmlWriter port) {
        logger.entering("addPortExtension");
        if (this.seiModel != null) {
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLPort) {
                    processPolicy(subject, port);
                }
            }
        }
        logger.exiting("addPortExtension");
    }
    
    public void addPortTypeExtension(TypedXmlWriter portType) {
        logger.entering("addPortTypeExtension");
        if (this.seiModel != null) {
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLPortType) {
                    processPolicy(subject, portType);
                }
            }
        }
        logger.exiting("addPortTypeExtension");
    }
    
    public void addBindingExtension(TypedXmlWriter binding) {
        logger.entering("addBindingExtension");
        if (this.seiModel != null) {
            // TODO Do not rely on a naming algorithm that is private to WSDLGenerator
            //String bindingName = this.seiModel.getPortName().getLocalPart() + "Binding";
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLBoundPortType) {
                    processPolicy(subject, binding);
                }
            }
        }
        logger.exiting("addBindingExtension");
    }
    
    public void addOperationExtension(TypedXmlWriter operation, JavaMethod method) {
        logger.entering("addOperationExtension");
        if (this.seiModel != null) {
            String operationName = method.getOperationName();
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLOperation && operationName.equals(((WSDLOperation)wsdlSubject).getName().getLocalPart())) {
                    processPolicy(subject, operation);
                }
            }
        }
        logger.exiting("addOperationExtension");
    }
    
    public void addBindingOperationExtension(TypedXmlWriter operation, JavaMethod method) {
        logger.entering("addBindingOperationExtension");
        if (this.seiModel != null) {
            String operationName = method.getOperationName();
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLBoundOperation && operationName.equals(((WSDLBoundOperation)wsdlSubject).getName().getLocalPart())) {
                    processPolicy(subject, operation);
                }
            }
        }
        logger.exiting("addBindingOperationExtension");
    }
    
    public void addInputMessageExtension(TypedXmlWriter message, JavaMethod method) {
        logger.entering("addInputMessageExtension");
        if (this.seiModel != null) {
            String messageName = method.getRequestMessageName();
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLMessage && messageName.equals(((WSDLMessage)wsdlSubject).getName().getLocalPart())) {
                    processPolicy(subject, message);
                }
            }
        }
        logger.exiting("addInputMessageExtension");
    }
    
    public void addOutputMessageExtension(TypedXmlWriter message, JavaMethod method) {
        logger.entering("addOutputMessageExtension");
        if (this.seiModel != null) {
            String messageName = method.getResponseMessageName();
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLMessage && messageName.equals(((WSDLMessage)wsdlSubject).getName().getLocalPart())) {
                    processPolicy(subject, message);
                }
            }
        }
        logger.exiting("addOutputMessageExtension");
    }
    
    public void addFaultMessageExtension(TypedXmlWriter message, JavaMethod method, CheckedException ce) {
        logger.entering("addFaultMessageExtension");
        if (this.seiModel != null) {
            String messageName = ce.getMessageName();
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLMessage && messageName.equals(((WSDLMessage)wsdlSubject).getName().getLocalPart())) {
                    processPolicy(subject, message);
                }
            }
        }
        logger.exiting("addFaultMessageExtension");
    }
    
    public void addOperationInputExtension(TypedXmlWriter input, JavaMethod method) {
        logger.entering("addOperationInputExtension");
        if (this.seiModel != null) {
            String messageName = method.getRequestMessageName();
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLInput && messageName.equals(((WSDLInput)wsdlSubject).getName())) {
                    processPolicy(subject, input);
                }
            }
        }
        logger.exiting("addOperationInputExtension");
    }
    
    public void addOperationOutputExtension(TypedXmlWriter output, JavaMethod method) {
        logger.entering("addOperationOutputExtension");
        if (this.seiModel != null) {
            String messageName = method.getResponseMessageName();
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLOutput && messageName.equals(((WSDLOutput)wsdlSubject).getName())) {
                    processPolicy(subject, output);
                }
            }
        }
        logger.exiting("addOperationOutputExtension");
    }
    
    public void addOperationFaultExtension(TypedXmlWriter fault, JavaMethod method, CheckedException ce) {
        logger.entering("addOperationFaultExtension");
        if (this.seiModel != null) {
            String messageName = ce.getMessageName();
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLFault && messageName.equals(((WSDLFault)wsdlSubject).getName())) {
                    processPolicy(subject, fault);
                }
            }
        }
        logger.exiting("addOperationFaultExtension");
    }
    
    public void addBindingOperationInputExtension(TypedXmlWriter input, JavaMethod method) {
        logger.entering("addBindingOperationInputExtension");
        if (this.seiModel != null) {
            String messageName = method.getOperationName();
            for (PolicySubject subject : subjects) {
                if (policyMap.isInputMessageSubject(subject)) {
                    Object wsdlSubject = subject.getSubject();
                    if (wsdlSubject != null && wsdlSubject instanceof WSDLBoundOperation && messageName.equals(((WSDLBoundOperation)wsdlSubject).getName().getLocalPart())) {
                        processPolicy(subject, input);
                    }
                }
            }
        }
        logger.exiting("addBindingOperationInputExtension");
    }
    
    public void addBindingOperationOutputExtension(TypedXmlWriter output, JavaMethod method) {
        logger.entering("addBindingOperationOutputExtension");
        if (this.seiModel != null) {
            String messageName = method.getOperationName();
            for (PolicySubject subject : subjects) {
                if (policyMap.isOutputMessageSubject(subject)) {
                    Object wsdlSubject = subject.getSubject();
                    if (wsdlSubject != null && wsdlSubject instanceof WSDLBoundOperation && messageName.equals(((WSDLBoundOperation)wsdlSubject).getName().getLocalPart())) {
                        processPolicy(subject, output);
                    }
                }
            }
        }
        logger.exiting("addBindingOperationOutputExtension");
    }
    
    public void addBindingOperationFaultExtension(TypedXmlWriter fault, JavaMethod method, CheckedException ce) {
        logger.entering("addBindingOperationFaultExtension");
        if (this.seiModel != null) {
            String messageName = ce.getMessageName();
            for (PolicySubject subject : subjects) {
                Object wsdlSubject = subject.getSubject();
                if (wsdlSubject != null && wsdlSubject instanceof WSDLFault && messageName.equals(((WSDLFault)wsdlSubject).getName())) {
                    processPolicy(subject, fault);
                }
            }
        }
        logger.exiting("addBindingOperationFaultExtension");
    }
        
    /**
     * Adds a PolicyReference element that points to the policy of the element,
     * if the policy does not have any id or name. Writes policy inside the element otherwise.
     *
     * @param policy to be referenced or marshalled
     * @param element A TXW element to which we shall add the PolicyReference
     */
    private void processPolicy(PolicySubject policySubject, TypedXmlWriter xmlWriter) {
        try {
            Policy policy = policySubject.getEffectivePolicy(merger);
            if (policy != null) {
                if (null != policy.getIdOrName()) {
                    TypedXmlWriter policyReference = xmlWriter._element(PolicyConstants.POLICY_REFERENCE, TypedXmlWriter.class);
                    policyReference._attribute(PolicyConstants.POLICY_URI.getLocalPart(), '#' + policy.getIdOrName());
                } else {
                    PolicyModelGenerator generator = PolicyModelGenerator.getGenerator();
                    PolicySourceModel policyInfoset = generator.translate(policy);
                    marshaller.marshal(policyInfoset, xmlWriter);
                }
            }
        } catch (PolicyException pe) {
            //TODO: handle pe
            logger.severe("processPolicy", "Unable to marshall policy or it's reference.");
        }
    }
    
}