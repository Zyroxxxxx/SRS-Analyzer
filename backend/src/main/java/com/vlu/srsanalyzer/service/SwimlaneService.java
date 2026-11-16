package com.vlu.srsanalyzer.service;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Ve so do SwimLane (PNG) bang Java2D: moi lane la 1 actor (lay tu "As a ..."), lane cuoi la System
@Service
public class SwimlaneService {

    static { System.setProperty("java.awt.headless", "true"); }

    private static final Pattern ACTOR = Pattern.compile("(?is)^\\s*as\\s+(?:an?|the)\\s+([^,]+?)\\s*,");
    private static final Pattern ACTION = Pattern.compile("(?is)i\\s+want\\s+(?:to\\s+)?(.+?)(?:\\s*,?\\s+so\\s+that\\b.*)?$");
    private static final Pattern THEN = Pattern.compile("(?is)\\bthen\\b[\\s,]*(.+)$");
    private static final int MAX_STORIES = 12, SCALE = 2;
    private static final int HEAD_W = 150, COL_W = 230, BOX_W = 196, BOX_H = 96, LANE_H = 150, TOP = 56;

    public byte[] render(String title, List<WorkspaceService.StoryDto> all) throws IOException {
        if (all.isEmpty()) throw new RuntimeException("Chua co User Story. Hay phan tich yeu cau truoc.");
        List<WorkspaceService.StoryDto> list = all.size() > MAX_STORIES ? all.subList(0, MAX_STORIES) : all;

        LinkedHashSet<String> lanes = new LinkedHashSet<>();
        for (var s : list) lanes.add(actor(s.content()));
        lanes.remove("System");
        lanes.add("System");
        List<String> laneList = new ArrayList<>(lanes);
        int sysLane = laneList.size() - 1;

        int w = HEAD_W + list.size() * COL_W + 30, h = TOP + laneList.size() * LANE_H + 16;
        BufferedImage img = new BufferedImage(w * SCALE, h * SCALE, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.scale(SCALE, SCALE);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, w, h);

        Font base = new Font("SansSerif", Font.PLAIN, 12), bold = base.deriveFont(Font.BOLD, 12f);
        g.setFont(bold.deriveFont(16f));
        g.setColor(new Color(0x111827));
        String t = title == null ? "" : title;
        g.drawString("SwimLane - " + (t.length() > 90 ? t.substring(0, 90) + "..." : t), 14, 30);

        // lanes
        for (int i = 0; i < laneList.size(); i++) {
            int y = TOP + i * LANE_H;
            g.setColor(i % 2 == 0 ? new Color(0xF3F4F6) : new Color(0xFFFFFF));
            g.fillRect(0, y, w, LANE_H);
            g.setColor(new Color(0xD1D5DB));
            g.drawRect(0, y, w - 1, LANE_H);
            g.setColor(i == sysLane ? new Color(0x065F46) : new Color(0x1E3A8A));
            g.fillRect(0, y, HEAD_W - 20, LANE_H);
            g.setColor(Color.WHITE);
            g.setFont(bold);
            drawWrapped(g, laneList.get(i), 10, y + LANE_H / 2 - 8, HEAD_W - 40, 3, 16);
        }

        // cac buoc
        int[][] actorBox = new int[list.size()][], sysBox = new int[list.size()][];
        for (int i = 0; i < list.size(); i++) {
            var s = list.get(i);
            int x = HEAD_W + 8 + i * COL_W;
            int aLane = laneList.indexOf(actor(s.content()));
            int ya = TOP + aLane * LANE_H + (LANE_H - BOX_H) / 2, ys = TOP + sysLane * LANE_H + (LANE_H - BOX_H) / 2;
            actorBox[i] = new int[]{x, ya};
            sysBox[i] = new int[]{x, ys};
            box(g, x, ya, "US" + (i + 1) + (s.confirmed() ? "  \u2713" : ""), action(s.content()), new Color(0xDBEAFE), new Color(0x1D4ED8), base, bold);
            String then = s.criteria().isEmpty() ? "Xu ly va phan hoi" : then(s.criteria().get(0).content());
            if (aLane != sysLane) box(g, x, ys, "System", then, new Color(0xD1FAE5), new Color(0x047857), base, bold);
            else sysBox[i] = null;
        }

        // mui ten
        g.setStroke(new BasicStroke(1.6f));
        g.setColor(new Color(0x374151));
        for (int i = 0; i < list.size(); i++) {
            if (sysBox[i] != null) {
                int cx = actorBox[i][0] + BOX_W / 2;
                boolean down = actorBox[i][1] < sysBox[i][1];
                arrow(g, cx, down ? actorBox[i][1] + BOX_H : actorBox[i][1], cx, down ? sysBox[i][1] : sysBox[i][1] + BOX_H);
            }
            if (i + 1 < list.size()) {
                int[] from = sysBox[i] != null ? sysBox[i] : actorBox[i];
                int x1 = from[0] + BOX_W, y1 = from[1] + BOX_H / 2, x2 = actorBox[i + 1][0], y2 = actorBox[i + 1][1] + BOX_H / 2, mid = (x1 + x2) / 2;
                g.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{5f, 4f}, 0f));
                Path2D p = new Path2D.Float();
                p.moveTo(x1, y1); p.lineTo(mid, y1); p.lineTo(mid, y2);
                g.draw(p);
                g.setStroke(new BasicStroke(1.6f));
                arrow(g, mid, y2, x2, y2);
            }
        }
        if (all.size() > MAX_STORIES) {
            g.setColor(new Color(0x6B7280));
            g.setFont(base);
            g.drawString("(Hien thi " + MAX_STORIES + "/" + all.size() + " User Story dau tien)", HEAD_W + 8, h - 4);
        }
        g.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private static String actor(String story) {
        Matcher m = ACTOR.matcher(story);
        if (!m.find()) return "User";
        String a = m.group(1).strip();
        return a.isEmpty() ? "User" : Character.toUpperCase(a.charAt(0)) + (a.length() > 1 ? a.substring(1) : "");
    }

    private static String action(String story) {
        Matcher m = ACTION.matcher(story.strip());
        return m.find() ? m.group(1).strip() : story.strip();
    }

    private static String then(String ac) {
        Matcher m = THEN.matcher(ac);
        return m.find() ? m.group(1).strip() : ac.strip();
    }

    private static void box(Graphics2D g, int x, int y, String label, String text, Color fill, Color stroke, Font base, Font bold) {
        g.setColor(fill);
        g.fillRoundRect(x, y, BOX_W, BOX_H, 12, 12);
        g.setColor(stroke);
        g.setStroke(new BasicStroke(1.4f));
        g.drawRoundRect(x, y, BOX_W, BOX_H, 12, 12);
        g.setFont(bold);
        g.drawString(label, x + 10, y + 17);
        g.setColor(new Color(0x111827));
        g.setFont(base);
        drawWrapped(g, text, x + 10, y + 34, BOX_W - 20, 4, 15);
    }

    private static void drawWrapped(Graphics2D g, String text, int x, int y, int maxW, int maxLines, int lineH) {
        FontMetrics fm = g.getFontMetrics();
        List<String> lines = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split("\\s+")) {
            String tryLine = cur.isEmpty() ? word : cur + " " + word;
            if (fm.stringWidth(tryLine) <= maxW) { cur = new StringBuilder(tryLine); continue; }
            if (!cur.isEmpty()) lines.add(cur.toString());
            cur = new StringBuilder(word);
        }
        if (!cur.isEmpty()) lines.add(cur.toString());
        boolean cut = lines.size() > maxLines;
        for (int i = 0; i < Math.min(lines.size(), maxLines); i++) {
            String ln = lines.get(i);
            if (cut && i == maxLines - 1) {
                while (!ln.isEmpty() && fm.stringWidth(ln + "...") > maxW) ln = ln.substring(0, ln.length() - 1);
                ln += "...";
            }
            while (fm.stringWidth(ln) > maxW && ln.length() > 1) ln = ln.substring(0, ln.length() - 1);
            g.drawString(ln, x, y + i * lineH);
        }
    }

    private static void arrow(Graphics2D g, int x1, int y1, int x2, int y2) {
        g.drawLine(x1, y1, x2, y2);
        double a = Math.atan2(y2 - y1, x2 - x1);
        Path2D head = new Path2D.Double();
        head.moveTo(x2, y2);
        head.lineTo(x2 - 9 * Math.cos(a - 0.4), y2 - 9 * Math.sin(a - 0.4));
        head.lineTo(x2 - 9 * Math.cos(a + 0.4), y2 - 9 * Math.sin(a + 0.4));
        head.closePath();
        g.fill(head);
    }
}
