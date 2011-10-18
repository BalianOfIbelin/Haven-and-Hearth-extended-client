/*
 *  This file is part of the Haven & Hearth game client.
 *  Copyright (C) 2009 Fredrik Tolf <fredrik@dolda2000.com>, and
 *                     Björn Johannessen <johannessen.bjorn@gmail.com>
 *
 *  Redistribution and/or modification of this file is subject to the
 *  terms of the GNU Lesser General Public License, version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  Other parts of this source tree adhere to other copying
 *  rights. Please see the file `COPYING' in the root directory of the
 *  source tree for details.
 *
 *  A copy the GNU Lesser General Public License is distributed along
 *  with the source tree of which this file is a part in the file
 *  `doc/LPGL-3'. If it is missing for any reason, please see the Free
 *  Software Foundation's website at <http://www.fsf.org/>, or write
 *  to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA
 */

package haven;

import ender.timer.Timer;

import java.util.*;

public class Glob {
    public static final int GMSG_TIME = 0;
    public static final int GMSG_ASTRO = 1;
    public static final int GMSG_LIGHT = 2;

    public long time;
    public Astronomy ast;
    public final OCache oc = new OCache(this);
    public final MCache map;
    public final Session sess;
    public final Party party;
    public final Collection<Resource> paginae = new TreeSet<Resource>();
    public final Map<String, CAttr> cattr = new HashMap<String, CAttr>();
    public final Map<Integer, Buff> buffs = new TreeMap<Integer, Buff>();
    public java.awt.Color amblight = null;

    public Glob(final Session sess) {
        this.sess = sess;
        map = new MCache(sess);
        party = new Party(this);

        CustomConfig.setGlob(this);

        //adding extended commands
        paginae.add(Resource.load("paginae/act/add"));
        paginae.add(Resource.load("paginae/add/rad"));
        paginae.add(Resource.load("paginae/add/shhd"));
        paginae.add(Resource.load("paginae/add/wiki"));
        paginae.add(Resource.load("paginae/add/animal"));
        paginae.add(Resource.load("paginae/add/plants"));
        paginae.add(Resource.load("paginae/add/global"));
        paginae.add(Resource.load("paginae/add/timer"));
        paginae.add(Resource.load("paginae/add/hide"));
        paginae.add(Resource.load("paginae/add/hide/tree"));
        paginae.add(Resource.load("paginae/add/hide/flav"));
        paginae.add(Resource.load("paginae/add/hide/wall"));
        paginae.add(Resource.load("paginae/add/hide/gate"));
        paginae.add(Resource.load("paginae/add/hide/bush"));
        paginae.add(Resource.load("paginae/add/hide/thik"));
        paginae.add(Resource.load("paginae/add/hide/cabi"));
        paginae.add(Resource.load("paginae/add/hide/mans"));
        paginae.add(Resource.load("paginae/add/hide/plan"));
        paginae.add(Resource.load("paginae/add/hide/ston"));
    }

    public static class CAttr extends Observable {
        final String nm;
        int base, comp;

        public CAttr(final String nm, final int base, final int comp) {
            this.nm = nm.intern();
            this.base = base;
            this.comp = comp;
        }

        public void update(final int base, final int comp) {
            if ((base == this.base) && (comp == this.comp))
                return;
            this.base = base;
            this.comp = comp;
            setChanged();
            notifyObservers(null);
        }
    }

    private static double defix(final int i) {
        return (((double) i) / 1e9);
    }

    public void blob(final Message msg) {
        while (!msg.eom()) {
            switch (msg.uint8()) {
                case GMSG_TIME:
                    Timer.server = msg.int32();
                    Timer.local = System.currentTimeMillis() / 1000;
                    break;
                case GMSG_ASTRO:
                    final double dt = defix(msg.int32());
                    final double mp = defix(msg.int32());
                    final double yt = defix(msg.int32());
                    final boolean night = (dt < 0.25) || (dt > 0.75);
                    ast = new Astronomy(dt, mp, yt, night);
                    break;
                case GMSG_LIGHT:
                    amblight = msg.color();
                    break;
            }
        }
    }

    public void paginae(final Message msg) {
        synchronized (paginae) {
            while (!msg.eom()) {
                final int act = msg.uint8();
                if (act == '+') {
                    final String nm = msg.string();
                    final int ver = msg.uint16();
                    paginae.add(Resource.load(nm, ver));
                } else if (act == '-') {
                    final String nm = msg.string();
                    final int ver = msg.uint16();
                    paginae.remove(Resource.load(nm, ver));
                }
            }
        }
    }

    public void cattr(final Message msg) {
        synchronized (cattr) {
            while (!msg.eom()) {
                final String nm = msg.string();
                final int base = msg.int32();
                final int comp = msg.int32();
                CAttr a = cattr.get(nm);
                if (a == null) {
                    a = new CAttr(nm, base, comp);
                    cattr.put(nm, a);
                } else {
                    a.update(base, comp);
                }
            }
        }
    }

    public void buffmsg(final Message msg) {
        final String name = msg.string().intern();
        synchronized (buffs) {
            if (name.equals("clear")) {
                buffs.clear();
            } else if (name.equals("set")) {
                final int id = msg.int32();
                final Indir<Resource> res = sess.getres(msg.uint16());
                final String tt = msg.string();
                final int ameter = msg.int32();
                final int nmeter = msg.int32();
                final int cmeter = msg.int32();
                final int cticks = msg.int32();
                final boolean major = msg.uint8() != 0;
                Buff buff;
                if ((buff = buffs.get(id)) == null) {
                    buff = new Buff(id, res);
                } else {
                    buff.res = res;
                }
                if (tt.length() == 0)
                    buff.tt = null;
                else
                    buff.tt = tt;
                buff.ameter = ameter;
                buff.nmeter = nmeter;
                buff.ntext = null;
                buff.cmeter = cmeter;
                buff.cticks = cticks;
                buff.major = major;
                buff.gettime = System.currentTimeMillis();
                buffs.put(id, buff);
            } else if (name.equals("rm")) {
                final int id = msg.int32();
                buffs.remove(id);
            }
        }
    }
}
