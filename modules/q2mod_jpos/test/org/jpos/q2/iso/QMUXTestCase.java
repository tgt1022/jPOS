/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2008 Alejandro P. Revilla
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jpos.q2.iso;

import junit.framework.*;
import org.jpos.q2.Q2;
import org.jpos.iso.ISOUtil;
import org.jpos.space.Space;
import org.jpos.space.TSpace;
import org.jpos.space.SpaceFactory;
import org.jpos.util.NameRegistrar;
import org.jpos.iso.MUX;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOResponseListener;

public class QMUXTestCase extends TestCase implements ISOResponseListener {
    Q2 q2;
    Space sp;
    MUX mux;
    boolean expiredCalled;
    ISOMsg responseMsg;
    Object receivedHandback;

    public void setUp () throws Exception {
        sp = SpaceFactory.getSpace();
        q2 = new Q2(new String[] { "-d", "../test/org/jpos/q2/iso" });
        expiredCalled=false;
        new Thread(q2).start();
        Thread.sleep (2000L);
        try {
            mux = (MUX) NameRegistrar.get ("mux.mux");
        } catch (NameRegistrar.NotFoundException e) { 
            fail ("MUX not found");
        }
        receivedHandback = null;
    }
    public void testExpiredMessage() throws Exception {
        mux.request (createMsg("000001"), 500L, this, "Handback One");
        assertFalse ("expired called too fast", expiredCalled);
        assertNotNull ("Space doesn't contain message key", 
            sp.rdp ("send.0800000000029110001000001.req")
        );
        Thread.sleep (1000L);
        assertTrue ("expired has not been called after 1 second", expiredCalled);
        assertNull ("Cleanup failed, Space still contains message key", 
            sp.rdp ("send.0800000000029110001000001.req")
        );
        assertEquals ("Handback One not received", "Handback One", receivedHandback);
    }
    public void testAnsweredMessage() throws Exception {
        mux.request (createMsg("000002"), 500L, this, "Handback Two");
        assertFalse ("expired called too fast", expiredCalled);
        ISOMsg m = (ISOMsg) sp.in ("send", 500L);
        assertNotNull ("Message not received by pseudo-channel", m);
        assertNotNull ("Space doesn't contain message key", 
            sp.rdp ("send.0800000000029110001000002.req")
        );
        m.setResponseMTI();
        sp.out ("receive", m);
        Thread.sleep (100L);
        assertNotNull ("Response not received", responseMsg);
        Thread.sleep (1000L);
        assertFalse ("Response received but expired was called", expiredCalled);
        assertNull ("Cleanup failed, Space still contains message key", 
            sp.rdp ("send.0800000000029110001000002.req")
        );
        assertEquals ("Handback Two not received", "Handback Two", receivedHandback);
    }
    public void tearDown() throws Exception {
        Thread.sleep (1500); // let the thing run
        q2.shutdown(true);
    }
    private ISOMsg createMsg(String stan) throws ISOException {
        ISOMsg m = new ISOMsg("0800");
        m.set (11, stan);
        m.set (41, "29110001"); // our favourite test terminal
        return m;
    }
    public void responseReceived (ISOMsg m, Object handBack) {
        responseMsg = m;
        receivedHandback = handBack;
    }
    public void expired (Object handBack) {
        expiredCalled=true;
        receivedHandback = handBack;
    }
}
