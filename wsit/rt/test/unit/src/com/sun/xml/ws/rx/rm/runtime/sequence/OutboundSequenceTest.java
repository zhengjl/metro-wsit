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
package com.sun.xml.ws.rx.rm.runtime.sequence;

import com.sun.xml.ws.rx.rm.runtime.ApplicationMessage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import junit.framework.TestCase;

/**
 *
 * @author Marek Potociar (marek.potociar at sun.com)
 */
public class OutboundSequenceTest extends TestCase {

    private SequenceManager sequenceManager = SequenceManagerFactory.INSTANCE.getClientSequenceManager(null);
    private Sequence sequence;

    public OutboundSequenceTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        sequence = sequenceManager.createOutboundSequence(sequenceManager.generateSequenceUID(), null, -1, null);
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        sequenceManager.terminateSequence(sequence.getId());
        super.tearDown();
    }

    public void testRegisterMessage() throws Exception {
        DummyAppMessage message;

        message = new DummyAppMessage("A");
        sequence.registerMessage(message, true);
        assertEquals(sequence.getId(), message.getSequenceId());
        assertEquals(1, message.getMessageNumber());

        message = new DummyAppMessage("B");
        sequence.registerMessage(message, true);
        assertEquals(sequence.getId(), message.getSequenceId());
        assertEquals(2, message.getMessageNumber());

        message = new DummyAppMessage("C");
        sequence.registerMessage(message, true);
        assertEquals(sequence.getId(), message.getSequenceId());
        assertEquals(3, message.getMessageNumber());

        message = new DummyAppMessage("D");
        sequence.registerMessage(message, true);
        assertEquals(sequence.getId(), message.getSequenceId());
        assertEquals(4, message.getMessageNumber());

        message = new DummyAppMessage("E");
        sequence.registerMessage(message, true);
        assertEquals(sequence.getId(), message.getSequenceId());
        assertEquals(5, message.getMessageNumber());
    }

    public void testGetLastMessageId() throws Exception {
        for (int i = 0; i < 4; i++) {
            sequence.registerMessage(new DummyAppMessage("" + i), true);
        }

        assertEquals(4, sequence.getLastMessageId());
    }

    public void testPendingAcknowedgements() throws Exception {
        for (int i = 0; i < 5; i++) {
            sequence.registerMessage(new DummyAppMessage("" + i), true);
        }

        assertTrue(sequence.hasUnacknowledgedMessages());

        List<Sequence.AckRange> ackedRages;

        sequence.acknowledgeMessageId(1);
        assertTrue(sequence.hasUnacknowledgedMessages());
        ackedRages = sequence.getAcknowledgedMessageIds();
        assertEquals(1, ackedRages.size());
        assertEquals(1, ackedRages.get(0).lower);
        assertEquals(1, ackedRages.get(0).upper);

        sequence.acknowledgeMessageIds(Arrays.asList(new Sequence.AckRange[]{
                    new Sequence.AckRange(1, 2),
                    new Sequence.AckRange(4, 4),
                }));
        assertTrue(sequence.hasUnacknowledgedMessages());
        ackedRages = sequence.getAcknowledgedMessageIds();
        assertEquals(2, ackedRages.size());
        assertEquals(1, ackedRages.get(0).lower);
        assertEquals(2, ackedRages.get(0).upper);
        assertEquals(4, ackedRages.get(1).lower);
        assertEquals(4, ackedRages.get(1).upper);

        sequence.acknowledgeMessageIds(Arrays.asList(new Sequence.AckRange[]{
                    new Sequence.AckRange(1, 5)
                }));
        assertFalse(sequence.hasUnacknowledgedMessages());
        ackedRages = sequence.getAcknowledgedMessageIds();
        assertEquals(1, ackedRages.size());
        assertEquals(1, ackedRages.get(0).lower);
        assertEquals(5, ackedRages.get(0).upper);

        boolean passed = false;
        try {
            sequence.acknowledgeMessageIds(Arrays.asList(new Sequence.AckRange[]{
                        new Sequence.AckRange(1, 6)
                    }));
        } catch (IllegalMessageIdentifierException e) {
            passed = true;
        }
        assertTrue("IllegalMessageIdentifierException expected", passed);

        passed = false;
        try {
            sequence.acknowledgeMessageId(6);
        } catch (IllegalMessageIdentifierException e) {
            passed = true;
        }
        assertTrue("IllegalMessageIdentifierException expected", passed);
    }

    public void testIsAcknowledged() throws Exception {
        for (int i = 0; i < 5; i++) {
            sequence.registerMessage(new DummyAppMessage("" + i), true);
        }
        
        sequence.acknowledgeMessageId(1);
        sequence.acknowledgeMessageId(2);
        sequence.acknowledgeMessageId(4);
        
        assertTrue(sequence.isAcknowledged(1));
        assertTrue(sequence.isAcknowledged(2));
        assertFalse(sequence.isAcknowledged(3));
        assertTrue(sequence.isAcknowledged(4));
        assertFalse(sequence.isAcknowledged(5));
        assertFalse(sequence.isAcknowledged(6));
    }
    
    public void testSequenceStatusAfterCloseOperation() throws Exception {
        sequence.close();
        assertEquals(Sequence.Status.CLOSED, sequence.getStatus());
    }

    public void testBehaviorAfterCloseOperation() throws Exception {
        sequence.registerMessage(new DummyAppMessage("A"), true); // 1
        sequence.close();
        assertEquals(Sequence.Status.CLOSED, sequence.getStatus());

        // sequence acknowledgement behavior
        sequence.acknowledgeMessageId(1); // ok

        // sequence generateNextMessageId behavior
        boolean passed = false;
        try {
            sequence.registerMessage(new DummyAppMessage("B"), true); // error - closed sequence
        } catch (IllegalStateException e) {
            passed = true;
        }
        assertTrue("Expected exception was not thrown", passed);
    }

    public void testStatus() throws Exception {
        Sequence outbound = sequenceManager.createOutboundSequence(sequenceManager.generateSequenceUID(), null, -1, null);
        assertEquals(Sequence.Status.CREATED, outbound.getStatus());

        outbound.close();
        assertEquals(Sequence.Status.CLOSED, outbound.getStatus());

        sequenceManager.terminateSequence(outbound.getId());
        assertEquals(Sequence.Status.TERMINATING, outbound.getStatus());
    }

    public void testStoreAndRetrieveMessage() throws Exception {
        Map<String, ApplicationMessage> correlatedMessageMap = new HashMap<String, ApplicationMessage>();
        for (int i = 0; i < 3; i++) {
            ApplicationMessage message = new DummyAppMessage("" + i);
            sequence.registerMessage(message, true);
            correlatedMessageMap.put(message.getCorrelationId(), message);
        }

        System.gc();

        for (Map.Entry<String, ApplicationMessage> entry : correlatedMessageMap.entrySet()) {
            Object actual = sequence.retrieveMessage(entry.getKey());
            assertEquals("Retrieved message is not the same as stored message", entry.getValue(), actual);
            sequence.acknowledgeMessageId(entry.getValue().getMessageNumber());
        }
        /*
        System.gc();
        Thread.sleep(2000);
        System.gc();

        for (i = 0; i < messages.length; i++) {
        Object actual = outboundSequence.retrieveMessage(i + 1);
        assertEquals("Retrieved message is not the same as stored message", null, actual);
        }      
         */
    }
}