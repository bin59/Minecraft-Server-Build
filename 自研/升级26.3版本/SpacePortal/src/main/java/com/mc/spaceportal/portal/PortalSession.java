package com.mc.spaceportal.portal;

/**
 * 玩家充能会话：玩家站在阵法内蓄力期间的状态。
 */
public class PortalSession {

    public final String portalName;
    public final int total;
    public int tick;

    public PortalSession(String portalName, int total) {
        this.portalName = portalName;
        this.total = total;
        this.tick = 0;
    }

    public double progress() {
        return Math.min(1.0, (double) tick / total);
    }
}
