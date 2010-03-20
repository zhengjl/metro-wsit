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

/*
 * DataWrapper.java
 *
 * Created on May 2, 2005, 9:43 AM
 */

package com.sun.xml.wss.impl.dsig;

import com.sun.xml.wss.impl.MessageConstants;
import com.sun.xml.wss.impl.policy.mls.SignatureTarget;
import javax.xml.crypto.Data;
import javax.xml.crypto.NodeSetData;
import javax.xml.crypto.OctetStreamData;

/**
 * Wrapper class for JSR 105 Data objects.Caches SignatureTarget
 * object and data resolved using this signature target.Reduces
 * the burden of instanceof checks.
 * @author K.Venugopal@sun.com
 */
public class DataWrapper{
    
    private Data data = null;
    private int type = -1;
    private SignatureTarget signatureTarget = null;
    
    /**
     *
     * @param data
     */    
    DataWrapper(Data data){
        this.data = data;
        if(data instanceof AttachmentData){
            type = MessageConstants.ATTACHMENT_DATA;
        }else if (data instanceof NodeSetData){
            type = MessageConstants.NODE_SET_DATA;
        }else if(data instanceof OctetStreamData){
            type = MessageConstants.OCTECT_STREAM_DATA;
        }
        
    }
    
    /**
     *
     * @return Data object.
     */    
    public Data getData(){
        return this.data;
    }
    
    /**
     *
     * @return type of data object wrapped.
     */    
    public int getType(){
        return type;
    }
    
    /**
     *
     * @return if Data is AttachmentData
     */    
    public boolean isAttachmentData(){
        if(type ==MessageConstants.ATTACHMENT_DATA ){
            return true;
        }else{
            return false;
        }
    }
    
    /**
     *
     * @return true if Data is NodeSetData.
     */    
    public boolean isNodesetData(){
        if(type == MessageConstants.NODE_SET_DATA ){
            return true;
        }else{
            return false;
        }
    }
    
    /**
     *
     * @return true if Data is OctetStreamData.
     */    
    public boolean isOctectData(){
        if(type == MessageConstants.OCTECT_STREAM_DATA ){
            return true;
        }else{
            return false;
        }
    }
    
    /**
     * null if no target has been set.
     * @return
     */    
    public SignatureTarget getTarget(){
        return signatureTarget;
    }
    
    /**
     *
     * @param target
     */    
    public void setTarget(SignatureTarget target){
        this.signatureTarget = target;
    }
}
