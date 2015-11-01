package justine.robocar;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.Map;

public class ScoreBoard {

    Color background = new Color(0, 0, 0, 150);
    int width, height;
    StringBuilder sb = new StringBuilder(100);
    Traffic traffic = null;
    Font font = new Font("Arial", Font.PLAIN, 14);

    FontMetrics font_metrics;
    private int max_name_width;
    private int font_height;
    private boolean recalc = true;
    private boolean firstChange = true;
    private int gangsterLimit = 0;

    public void update(Traffic t) {

    	if (firstChange && traffic != null && traffic.gangsterList.size() != 0) {
    		firstChange = false;
    		gangsterLimit = traffic.gangsterList.size();
    	}


	if (traffic == null) {
	    traffic = t;
	    return;
	}
	if (traffic.cop_teams.size() != t.cop_teams.size())
	    recalc = true;
	traffic = t;

    }

    public void draw(Graphics2D g) {
	g.setPaint(background);
	g.setFont(font);
	if (recalc) {
	    font_metrics = g.getFontMetrics();
	    max_name_width = font_metrics.stringWidth(traffic.longestTeamName);
	    font_height = font_metrics.getHeight();
	    height = (font_height + 10) * (traffic.cop_teams.size() + 1) + 5;
	    width = max_name_width + font_height + font_metrics.charWidth('-') * 12 + 25;
	    recalc = false;
	}
	g.fillRoundRect(10, 10, width, height, 10, 10);
	int draw_y = 15;

	for (Map.Entry<String, CopTeamData> entry : traffic.cop_teams.entrySet()) {
	    String team_name = entry.getKey();
	    CopTeamData team_data = entry.getValue();
	    g.setPaint(team_data.color);
	    g.fillOval(15, draw_y, font_height, font_height);
	    g.setPaint(Color.WHITE);
	    draw_y += font_height;
	    sb.setLength(0);
	    sb.append(team_name);
	    sb.append(" - ");
	    sb.append(team_data.num_caught);
	    g.drawString(sb.toString(), font_height + 5 + 15, draw_y);
	    draw_y += 10;
	}

	sb.setLength(0);
	sb.append("Gangsters: ");
	sb.append(traffic.caughtList.size());
	sb.append("/");
	sb.append(gangsterLimit);

	draw_y += font_height;
	g.drawString(sb.toString(), 15, draw_y);
    }

}
