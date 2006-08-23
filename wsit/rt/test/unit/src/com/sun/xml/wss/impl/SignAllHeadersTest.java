/*
 * SignAllHeadersTest.java
 *
 * Created on March 31, 2006, 11:36 AM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package com.sun.xml.wss.impl;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.sun.xml.wss.callback.PolicyCallbackHandler1;
import com.sun.xml.ws.security.impl.policy.*;
import com.sun.xml.ws.security.policy.AlgorithmSuiteValue;
import com.sun.xml.wss.impl.policy.mls.*;
import com.sun.xml.wss.impl.*;
import com.sun.xml.wss.*;
import com.sun.xml.wss.impl.misc.DefaultSecurityEnvironmentImpl;
import com.sun.xml.wss.impl.filter.*;
import com.sun.xml.ws.security.policy.WSSAssertion;
import com.sun.xml.wss.impl.util.PolicyResourceLoader;
import com.sun.xml.wss.impl.util.TestUtil;

import com.sun.xml.ws.policy.Policy;
import com.sun.xml.ws.policy.PolicyAssertion;
import com.sun.xml.ws.policy.AssertionSet;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.crypto.dsig.DigestMethod;
import javax.xml.namespace.QName;
import javax.xml.soap.*;

import java.util.*;
import java.io.*;

/**
 *
 * @author ashutosh.shahi@sun.com
 */
public class SignAllHeadersTest extends TestCase{
    
    private static HashMap client = new HashMap();
    private static HashMap server = new HashMap();
    private static  AlgorithmSuite alg = new AlgorithmSuite();
    
    /** Creates a new instance of SignAllHeadersTest */
    public SignAllHeadersTest(String testName) {
        super(testName);
    }
    
    protected void setUp() throws Exception {
    	
    }
                                                                                                                                                             
    protected void tearDown() throws Exception {
    }
    
    public static Test suite() {
        TestSuite suite = new TestSuite(SignAllHeadersTest.class);                                                                                                 return suite;
    }
    
    public static void testSignAllHeadersTest() throws Exception {
        try{ 
            alg.setType(AlgorithmSuiteValue.Basic256);
            SignaturePolicy signaturePolicy = new SignaturePolicy();
            SignatureTarget st = new SignatureTarget();
            st.setType("uri");
            st.setValue("ALL_MESSAGE_HEADERS");
            st.setDigestAlgorithm(DigestMethod.SHA1);
            ((SignaturePolicy.FeatureBinding)signaturePolicy.getFeatureBinding()).
                    addTargetBinding(st);
            ((SignaturePolicy.FeatureBinding)signaturePolicy.getFeatureBinding()).
                    setCanonicalizationAlgorithm(MessageConstants.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
            
            QName name = new QName("X509Certificate");
            Token tok = new Token(name);
            
            SymmetricKeyBinding sigKb = 
                    (SymmetricKeyBinding)signaturePolicy.newSymmetricKeyBinding();
            AuthenticationTokenPolicy.X509CertificateBinding x509bind = 
                    (AuthenticationTokenPolicy.X509CertificateBinding)sigKb.newX509CertificateKeyBinding();
            x509bind.setReferenceType(MessageConstants.DIRECT_REFERENCE_TYPE);
            x509bind.setPolicyToken(tok);
            x509bind.setUUID(tok.getTokenId());
            
            // create SOAPMessage
            SOAPMessage msg = MessageFactory.newInstance().createMessage();
            SOAPHeader header = msg.getSOAPHeader();
            SOAPHeaderElement she1 = header.addHeaderElement(SOAPFactory.newInstance().createName("StockHeader","stkheader","http://stockhome.com/quote"));
            she1.addTextNode("Head Text Node1");
            SOAPHeaderElement she2 = header.addHeaderElement(SOAPFactory.newInstance().createName("Quote","quote","http://stockhome.com/quote"));
            she2.addTextNode("Head Text Node2");
            SOAPBody body = msg.getSOAPBody();
            SOAPBodyElement sbe = body.addBodyElement(
                    SOAPFactory.newInstance().createName(
                    "StockSymbol",
                    "tru",
                    "http://fabrikam123.com/payloads"));
            sbe.addTextNode("QQQ");
            
            //Create processing context and set the soap message to be processed.
            ProcessingContextImpl context = new ProcessingContextImpl(client);
            context.setSOAPMessage(msg);
            
            WSSAssertion wssAssertion = null;
            AssertionSet as = null;
            Policy wssPolicy = new PolicyResourceLoader().loadPolicy("policy-binding2.xml");
            Iterator<AssertionSet> i = wssPolicy.iterator();
            if(i.hasNext())
                as = i.next();
            
            for(PolicyAssertion assertion:as){
                if(assertion instanceof WSSAssertion){
                    wssAssertion = (WSSAssertion)assertion;
                }                      
            }
            
            MessagePolicy pol = new MessagePolicy();
            pol.append(signaturePolicy);
            pol.setWSSAssertion(wssAssertion);
            
            context.setAlgorithmSuite(alg);
            context.setSecurityPolicy(pol);
            CallbackHandler handler = new PolicyCallbackHandler1("client");
            SecurityEnvironment env = new DefaultSecurityEnvironmentImpl(handler);
            context.setSecurityEnvironment(env);
            SecurityAnnotator.secureMessage(context);
            
            SOAPMessage secMsg = context.getSOAPMessage();
            DumpFilter.process(context);
            
            // now persist the message and read-back
            FileOutputStream sentFile = new FileOutputStream("golden.msg");
            secMsg.saveChanges();
            TestUtil.saveMimeHeaders(secMsg, "golden.mh");
            secMsg.writeTo(sentFile);
            sentFile.close();
            
            // now create the message
            SOAPMessage recMsg = TestUtil.constructMessage("golden.mh", "golden.msg");
            // verify
            verify(recMsg, null, null);
            
        } catch(Exception e){
            e.printStackTrace();
            assertTrue(false);
        }
    }

   public static ProcessingContextImpl verify(SOAPMessage msg, byte[] proofKey, Map map) throws Exception {
       //Create processing context and set the soap
       //message to be processed.
       ProcessingContextImpl context = new ProcessingContextImpl(map);
       context.setSOAPMessage(msg);
        
       WSSAssertion wssAssertion = null;
       AssertionSet as = null;
       Policy wssPolicy = new PolicyResourceLoader().loadPolicy("./etc/policy-binding2.xml");
       Iterator<AssertionSet> i = wssPolicy.iterator();
       if(i.hasNext())
           as = i.next();
            
       for(PolicyAssertion assertion:as){
           if(assertion instanceof WSSAssertion){
               wssAssertion = (WSSAssertion)assertion;
           }                      
       }
       //wssAssertion.addRequiredProperty("RequireSignatureConfirmation");
       
        MessagePolicy pol = new MessagePolicy();
        context.setAlgorithmSuite(alg);
        pol.setWSSAssertion(wssAssertion);
                                                                                                           
        context.setSecurityPolicy(pol);
        CallbackHandler handler = new PolicyCallbackHandler1("server");
        SecurityEnvironment env = new DefaultSecurityEnvironmentImpl(handler);
        context.setSecurityEnvironment(env);

        SecurityRecipient.validateMessage(context);

        System.out.println("Verfied Message");
        DumpFilter.process(context);

        return context;
   }
    
    public static void main(String[] args) throws Exception{
        testSignAllHeadersTest();
    }
    
}
