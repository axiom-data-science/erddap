/*
 *  SSHTools - Java SSH2 API
 *
 *  Copyright (C) 2002-2003 Lee David Painter and Contributors.
 *
 *  Contributions made by:
 *
 *  Brett Smith
 *  Richard Pernavas
 *  Erwin Bolwidt
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Library General Public License
 *  as published by the Free Software Foundation; either version 2 of
 *  the License, or (at your option) any later version.
 *
 *  You may also distribute it and/or modify it under the terms of the
 *  Apache style J2SSH Software License. A copy of which should have
 *  been provided with the distribution.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  License document supplied with your distribution for more details.
 *
 */
package com.sshtools.j2ssh.configuration;

import java.util.*;


/**
 *
 *
 * @author $author$
 * @version $Revision: 1.27 $
 */
public interface SshAPIConfiguration {
    /**
     *
     *
     * @return
     */
    public List getCompressionExtensions();

    /**
     *
     *
     * @return
     */
    public List getCipherExtensions();

    /**
     *
     *
     * @return
     */
    public List getMacExtensions();

    /**
     *
     *
     * @return
     */
    public List getAuthenticationExtensions();

    /**
     *
     *
     * @return
     */
    public List getPublicKeyExtensions();

    /**
     *
     *
     * @return
     */
    public List getKeyExchangeExtensions();

    /**
     *
     *
     * @return
     */
    public String getDefaultCipher();

    /**
     *
     *
     * @return
     */
    public String getDefaultMac();

    /**
     *
     *
     * @return
     */
    public String getDefaultCompression();

    /**
     *
     *
     * @return
     */
    public String getDefaultPublicKey();

    /**
     *
     *
     * @return
     */
    public String getDefaultKeyExchange();

    /**
     *
     *
     * @return
     */
    public String getDefaultPublicKeyFormat();

    /**
     *
     *
     * @return
     */
    public String getDefaultPrivateKeyFormat();

    /**
     *
     *
     * @return
     */
    public List getPublicKeyFormats();

    /**
     *
     *
     * @return
     */
    public List getPrivateKeyFormats();
}
