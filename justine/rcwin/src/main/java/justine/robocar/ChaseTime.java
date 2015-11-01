package justine.robocar;

import java.util.*;

public class ChaseTime {

	private Calendar calendar;
	private Date old;
	private String time;

	ChaseTime() {
		restore();
	}

	public void restore() {
		calendar = Calendar.getInstance();
		old = calendar.getTime();
	}

	public void chaseGangster() {

		calendar = Calendar.getInstance();
		Date now = calendar.getTime();
		long diff = now.getTime() - old.getTime();

		long diffSeconds = diff / 1000 % 60;
		long diffMinutes = diff / (60 * 1000) % 60;
		

		if (diffMinutes < 1) {
			time = "(" + diffSeconds + "s)"; 
		} else {
			time = "(" + diffMinutes + "m:" + diffSeconds + "s)";
		}
	}

	public String getTime() {
		return time;
	}

	@Override
	public String toString() {
		return this.getTime();
	}
}