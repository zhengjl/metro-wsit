/*
 * $Id: DynamicPolicyCallback.java,v 1.1 2010-10-05 11:54:04 m_potociar Exp $
 */

/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 1997-2010 Sun Microsystems, Inc. All rights reserved.
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

package com.sun.xml.wss.impl.callback;

import javax.security.auth.callback.Callback;

import com.sun.xml.wss.impl.policy.SecurityPolicy;
import com.sun.xml.wss.impl.policy.StaticPolicyContext;
import com.sun.xml.wss.impl.policy.DynamicPolicyContext;
import com.sun.xml.wss.impl.policy.PolicyGenerationException;

import com.sun.xml.wss.impl.PolicyTypeUtil;

/**
 * Callback implementation for dynamic policy resolution.
 * A DynamicPolicy Callback is made by the XWS-runtime to 
 * allow the application/Handler to decide the incoming/outgoing
 * SecurityPolicy at runtime.
 *<P>
 * When the SecurityPolicy set on the Callback is a DynamicSecurityPolicy then 
 * the CallbackHandler is currently expected to set a com.sun.xml.wss.impl.configuration.MessagePolicy
 * instance as the resolved policy. The MessagePolicy instance can contain policies generated by the
 * PolicyGenerator obtained from the DynamicSecurityPolicy.
 */
public class DynamicPolicyCallback extends XWSSCallback implements Callback {

     boolean isDynamicSecurityPolicy = false;

     SecurityPolicy _policy;
     DynamicPolicyContext _ctx;

    /**
     * Constructor.
     * <P>
     * Associate a DynamicSecurityPolicy or WSSPolicy instance.
     * A DynamicSecurityPolicy can be used to obtain a PolicyGenerator. The DynamicPolicyContext passed 
     * can be used by the handler to dynamically decide the policy based on information in the context.
     *
     * @param _policy DynamicSecurityPolicy or WSSPolicy
     * @param _ctx DynamicPolicyContext the context which provides context information to the Handler.
     *
     * @see com.sun.xml.wss.impl.policy.SecurityPolicyGenerator
     */
    public DynamicPolicyCallback (
    SecurityPolicy _policy, 
    DynamicPolicyContext _ctx) 
    throws PolicyGenerationException {

        checkType (_policy);

        this._policy = _policy;
        this._ctx = _ctx;
    }

    /**
     * The SecurityPolicy set by the invocation of the CallbackHandler.
     * @return SecurityPolicy
     */
    public SecurityPolicy getSecurityPolicy () {
 	return _policy;
    }

    /**
     * @return DynamicPolicyContext passed to the callback
     */
    public DynamicPolicyContext getDynamicContext () {
	return _ctx;
    }

    /**
     * @return the StaticPolicyContext if any associated with the DynamicPolicyContext
     */
    public StaticPolicyContext getStaticContext () {
        return _ctx.getStaticPolicyContext (); 
    }

    /**
     * set the resolved SecurityPolicy in response to this callback
     * @param _policy a MessagePolicy instance containing SecurityPolicy generated by PolicyGenerator or a mutable WSSPolicy
     */
    public void setSecurityPolicy (SecurityPolicy _policy) {
        if (isDynamicSecurityPolicy) { 
            checkType0 (_policy);
 
            this._policy = _policy;
        } else {
            if (!(this._policy.getType().equals(_policy.getType()))) {
                // log
                throw new UnsupportedOperationException (
                          "Can not change object instance for WSSPolicy");
            }
            this._policy = _policy;
        } 
    }

    
    public boolean isDynamicSecurityPolicy () {
        return this.isDynamicSecurityPolicy;
    }

    
    private void checkType (SecurityPolicy policy) 
    throws PolicyGenerationException {
        try {
            if (PolicyTypeUtil. dynamicSecurityPolicy(policy)) {
                isDynamicSecurityPolicy = true;
            } else
            if (!Class.forName("com.sun.xml.wss.impl.policy.mls.WSSPolicy").
                isAssignableFrom(policy.getClass())) {
                // log 
                throw new PolicyGenerationException
                           ("Invalid SecurityPolicy type");
            }
        } catch (ClassNotFoundException cnfe) {}
    }

    
    private void checkType0 (SecurityPolicy policy) {
        if (!PolicyTypeUtil.messagePolicy(policy)) /* ||
             PolicyTypeUtil.signaturePolicy(policy) ||
             PolicyTypeUtil.encryptionPolicy(policy) ||
             PolicyTypeUtil.authenticationTokenPolicy(policy)))*/ {
            // log 
            throw new IllegalArgumentException ("Invalid SecurityPolicy type " + 
                policy + ", Expected MessagePolicy");
        }
    }
    
}
