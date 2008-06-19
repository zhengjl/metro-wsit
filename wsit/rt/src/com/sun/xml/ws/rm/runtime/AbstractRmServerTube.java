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
package com.sun.xml.ws.rm.runtime;

import com.sun.xml.ws.rm.faults.CreateSequenceRefusedFault;
import com.sun.xml.ws.api.addressing.AddressingVersion;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.pipe.Fiber;
import com.sun.xml.ws.api.pipe.NextAction;
import com.sun.xml.ws.api.pipe.TubeCloner;
import com.sun.xml.ws.api.pipe.helper.AbstractFilterTubeImpl;
import com.sun.xml.ws.assembler.WsitServerTubeAssemblyContext;
import com.sun.xml.ws.rm.RmRuntimeException;
import com.sun.xml.ws.rm.faults.AbstractRmSoapFault;
import com.sun.xml.ws.rm.faults.SequenceTerminatedFault;
import com.sun.xml.ws.rm.faults.UnknownSequenceFault;
import com.sun.xml.ws.rm.localization.LocalizationMessages;
import com.sun.xml.ws.rm.localization.RmLogger;
import com.sun.xml.ws.rm.policy.Configuration;
import com.sun.xml.ws.rm.policy.ConfigurationManager;
import com.sun.xml.ws.rm.runtime.sequence.Sequence;
import com.sun.xml.ws.rm.runtime.sequence.SequenceManager;
import com.sun.xml.ws.rm.runtime.sequence.SequenceManagerFactory;
import com.sun.xml.ws.rm.runtime.sequence.UnknownSequenceException;
import java.util.logging.Level;

/**
 *
 * @author Marek Potociar (marek.potociar at sun.com)
 */
public abstract class AbstractRmServerTube extends AbstractFilterTubeImpl {

    private static final RmLogger LOGGER = RmLogger.getLogger(AbstractRmServerTube.class);
    //
    protected final Configuration configuration;
    protected final SequenceManager sequenceManager;
    //
    private PacketAdapter requestAdapter;

    protected AbstractRmServerTube(AbstractRmServerTube original, TubeCloner cloner) {
        super(original, cloner);

        this.configuration = original.configuration;
        this.sequenceManager = original.sequenceManager;

        this.requestAdapter = null;
    }

    public AbstractRmServerTube(WsitServerTubeAssemblyContext context) {
        super(context.getTubelineHead());

        this.configuration = ConfigurationManager.createServiceConfigurationManager(context.getWsdlPort(), context.getEndpoint().getBinding()).getConfigurationAlternatives()[0];

        // TODO don't take the first config alternative automatically...
        if (configuration.getAddressingVersion() != AddressingVersion.W3C) {
            throw new RmRuntimeException(LocalizationMessages.WSRM_1120_UNSUPPORTED_WSA_VERSION(configuration.getAddressingVersion().toString()));
        }

        this.sequenceManager = SequenceManagerFactory.getInstance().getSequenceManager();
        this.requestAdapter = null;
    }

    @Override
    public NextAction processRequest(Packet requestPacket) {
        LOGGER.entering();
        requestAdapter = PacketAdapter.getInstance(configuration, requestPacket);
        try {
            if (requestAdapter.isProtocolMessage()) {
                if (requestAdapter.isProtocolRequest()) {
                    PacketAdapter protocolResponseAdapter = processProtocolRequest(requestAdapter);
                    return doReturnWith(protocolResponseAdapter.getPacket());
                } else {
                    return doThrow(new RmRuntimeException(LocalizationMessages.WSRM_1128_INVALID_WSA_ACTION_IN_PROTOCOL_REQUEST(requestAdapter.getWsaAction())));
                }
            } else {
                processRmHeaders(requestAdapter, true);

                if (configuration.isOrderedDelivery() && !isMessageInOrder(requestAdapter)) {
                    if (FlowControledFibers.INSTANCE.getUsedBufferSize(requestAdapter.getSequenceId()) > configuration.getDestinationBufferQuota()) {
                        Sequence sequence = sequenceManager.getSequence(requestAdapter.getSequenceId());
                        PacketAdapter responseAdapter = requestAdapter.createAckResponse(sequence, configuration.getRmVersion().sequenceAcknowledgementAction);

                        return doReturnWith(responseAdapter.getPacket());
                    }
                    FlowControledFibers.INSTANCE.registerForResume(Fiber.current(), requestAdapter);
                    return doSuspend(super.next);
                }

                return super.processRequest(requestAdapter.getPacket());
            }
        } catch (AbstractRmSoapFault ex) {
            return doReturnWith(ex.getSoapFaultResponse());
        } catch (RmRuntimeException ex) {
            LOGGER.logSevereException(ex);
            return doThrow(ex);
        } finally {
            requestAdapter.getPacket();
            LOGGER.exiting();
        }
    }

    @Override
    public NextAction processResponse(Packet responsePacket) {
        LOGGER.entering();
        try {
            /*
             * TODO response processing:
             * - store unacked response message
             * - append RM headers
             */

            if (configuration.isOrderedDelivery()) {
                /*
                 * TODO: Resume fiber with the next request if possible
                 */
            }

            return super.processResponse(responsePacket);
        } finally {
            LOGGER.exiting();
        }
    }

    @Override
    public void preDestroy() {
        LOGGER.entering();
        try {
            super.preDestroy();
        } finally {
            LOGGER.exiting();
        }
    }

    private boolean isMessageInOrder(PacketAdapter requestAdapter) {
        Sequence inboundSequence = sequenceManager.getSequence(requestAdapter.getSequenceId());
        return inboundSequence.getLastMessageId() == requestAdapter.getMessageNumber();
    }

    /**
     * Handles all protocol message request processing
     * 
     * @param  requestAdapter request packet adapter
     * 
     * @return protocol message response packet adapter
     * 
     * @exception AbstractRmSoapFault exception representing a protocol request 
     *            message processing SOAP fault
     */
    private PacketAdapter processProtocolRequest(PacketAdapter requestAdapter) throws AbstractRmSoapFault {
        if (configuration.getRmVersion().createSequenceAction.equals(requestAdapter.getWsaAction())) {
            return handleCreateSequenceAction(requestAdapter);
        } else if (configuration.getRmVersion().terminateSequenceAction.equals(requestAdapter.getWsaAction())) {
            return handleTerminateSequenceAction(requestAdapter);
        } else if (configuration.getRmVersion().ackRequestedAction.equals(requestAdapter.getWsaAction())) {
            return handleAckRequestedAction(requestAdapter);
        } else if (configuration.getRmVersion().sequenceAcknowledgementAction.equals(requestAdapter.getWsaAction())) {
            return handleSequenceAcknowledgementAction(requestAdapter);
        } else {
            return processVersionSpecificProtocolRequest(requestAdapter);
        }
    }

    /**
     * Handles all RM version-specific protocol message request processing
     * 
     * @param  requestAdapter request packet adapter
     * 
     * @return protocol message response packet adapter
     * 
     * @exception AbstractRmSoapFault exception representing a protocol request 
     *            message processing SOAP fault
     */
    protected PacketAdapter processVersionSpecificProtocolRequest(PacketAdapter requestAdapter) throws AbstractRmSoapFault {
        throw new UnsupportedOperationException(LocalizationMessages.WSRM_1134_UNSUPPORTED_PROTOCOL_MESSAGE(requestAdapter.getWsaAction()));
    }

    /**
     * Handles create sequence request processing
     * 
     * @param  requestAdapter create sequence request packet adapter
     * 
     * @return create sequence response message wrapped in a response packet adapter
     * 
     * @exception CreateSequenceRefusedFault in case of any problems while creating the sequence
     */
    protected abstract PacketAdapter handleCreateSequenceAction(PacketAdapter requestAdapter) throws CreateSequenceRefusedFault;

    /**
     * Handles terminate sequence request processing
     * 
     * @param  requestAdapter terminate sequence request packet adapter
     * 
     * @return terminate sequence response message wrapped in a response packet adapter
     * 
     * @exception UnknownSequenceFault if there is no such sequence registered with current 
     *            sequence manager.
     */
    protected abstract PacketAdapter handleTerminateSequenceAction(PacketAdapter requestAdapter) throws UnknownSequenceFault;

    /**
     * Handles acknowledgement request message processing
     * 
     * @param  requestAdapter  acknowledgement request message packet adapter
     * 
     * @return response for the acknowledgement request message wrapped in a response packet adapter
     * 
     * @exception UnknownSequenceFault if there is no such sequence registered with current 
     *            sequence manager.
     * 
     * @exception SequenceTerminatedFault if the sequence is currently in TERMINATING state
     */
    protected PacketAdapter handleAckRequestedAction(PacketAdapter requestAdapter) throws UnknownSequenceFault, SequenceTerminatedFault {

        Sequence inboundSequence;
        try {
            inboundSequence = sequenceManager.getSequence(requestAdapter.getAckRequestedHeaderSequenceId());
        } catch (UnknownSequenceException e) {
            LOGGER.logException(e, getProtocolFaultLoggingLevel());
            throw LOGGER.logException(new UnknownSequenceFault(configuration, requestAdapter.getPacket(), e.getMessage()), getProtocolFaultLoggingLevel());
        }

        if (inboundSequence.getStatus() == Sequence.Status.TERMINATING) {
            throw LOGGER.logException(new SequenceTerminatedFault(configuration, requestAdapter.getPacket(), ""), getProtocolFaultLoggingLevel());
        }

        inboundSequence.updateLastActivityTime();

        return requestAdapter.createAckResponse(inboundSequence, configuration.getRmVersion().sequenceAcknowledgementAction);
    }

    /**
     * Handles sequence acknowledgement message processing
     * 
     * @param  requestAdapter sequence acknowledgement message packet adapter
     * 
     * @return closes the transport and returns {@code null}
     * 
     * @exception UnknownSequenceFault if there is no such sequence registered with current 
     *            sequence manager.
     */
    protected PacketAdapter handleSequenceAcknowledgementAction(PacketAdapter requestAdapter) throws UnknownSequenceFault {
        processRmHeaders(requestAdapter, false);

        // FIXME maybe we should send acknowledgements back if any?
        return requestAdapter.closeTransportAndReturnNull();
    }

    /**
     * Returns a preconfigured logging level that should be used to log exceptions 
     * related to protocol message processing.
     * 
     * @return common logging level for protocol message processing errors
     */
    protected final Level getProtocolFaultLoggingLevel() {
        return Level.WARNING;
    }

    /**
     * Processes the WS-RM headers on the request message
     * 
     * @param requestAdapter packet adapter containing the request message to be processed
     * 
     * @param expectSequenceHeader determines whether the message should contain 
     *        a Sequence header or not
     * 
     * @exception UnknownSequenceFault if there is no such sequence registered with current 
     *            sequence manager.
     */
    private void processRmHeaders(PacketAdapter requestAdapter, boolean expectSequenceHeader) throws UnknownSequenceFault {
        if (expectSequenceHeader) {
            if (requestAdapter.getSequenceId() == null) {
                // TODO proper soap fault
                throw new RmRuntimeException(LocalizationMessages.WSRM_1118_MANDATORY_HEADER_NOT_PRESENT("wsrm:Sequence"));
            }

            Sequence inboundSequence = getSequenceOrSoapFault(requestAdapter.getPacket(), requestAdapter.getSequenceId());
            inboundSequence.acknowledgeMessageId(requestAdapter.getMessageNumber());
        }

        String ackRequestedSequenceId = requestAdapter.getAckRequestedHeaderSequenceId();
        if (ackRequestedSequenceId != null) {
            getSequenceOrSoapFault(requestAdapter.getPacket(), ackRequestedSequenceId).setAckRequestedFlag();
        }

        requestAdapter.processAcknowledgements(sequenceManager, getOutboundSequenceId4Request(requestAdapter));
    }

    protected final String getOutboundSequenceId4Request(PacketAdapter requestAdapter) throws UnknownSequenceFault {
        String sequenceId = requestAdapter.getSequenceId();
        if (sequenceId == null) {
            return null;
        }

        Sequence boundSequence;
        try {
            boundSequence = sequenceManager.getBoundSequence(requestAdapter.getSequenceId());
        } catch (UnknownSequenceException e) {
            LOGGER.logException(e, getProtocolFaultLoggingLevel());
            throw new UnknownSequenceFault(configuration, requestAdapter.getPacket(), requestAdapter.getSequenceId());
        }

        return (boundSequence != null) ? boundSequence.getId() : null;
    }
    
    protected final Sequence getSequenceOrSoapFault(Packet packet, String sequenceId) throws UnknownSequenceFault {
        try {
             return sequenceManager.getSequence(sequenceId);
        } catch (UnknownSequenceException e) {
            LOGGER.logException(e, getProtocolFaultLoggingLevel());
            throw LOGGER.logException(new UnknownSequenceFault(configuration, packet, e.getMessage()), getProtocolFaultLoggingLevel());
        }        
    }
}
