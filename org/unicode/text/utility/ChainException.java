/**
*******************************************************************************
* Copyright (C) 1996-2001, International Business Machines Corporation and    *
* others. All Rights Reserved.                                                *
*******************************************************************************
*
* $Source: /home/cvsroot/unicodetools/org/unicode/text/utility/ChainException.java,v $
*
*******************************************************************************
*/

package org.unicode.text.utility;


import java.text.MessageFormat;

public class ChainException extends RuntimeException {
    Object[] keyData;
    String messageFormat;
    //Exception chain;

    public ChainException (String messageFormat, Object[] objects) {
        this.messageFormat = messageFormat;
        keyData = objects == null ? null : (Object[]) objects.clone();
    }

    public ChainException (String messageFormat, Object[] objects, Exception chainedException) {
        this.messageFormat = messageFormat;
        keyData = objects == null ? null : (Object[]) objects.clone();
        initCause(chainedException);
    }

    public String getMessage() {
        String chainMsg = "";
//        if (chain != null) {
//            chainMsg = "; " + chain.getClass().getName()
//                + ", " + chain.getMessage();
//            StringWriter w = new StringWriter();
//            PrintWriter p = new PrintWriter(w);
//            chain.printStackTrace(p);
//            chainMsg += ", " + w.getBuffer();
//            p.close();
//        }
        String main = "";
        if (keyData != null) main = MessageFormat.format(messageFormat, keyData);
        return main + chainMsg;
    }
}

