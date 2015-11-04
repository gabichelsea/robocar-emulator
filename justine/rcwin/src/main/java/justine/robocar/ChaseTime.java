package justine.robocar;

import java.util.Calendar;
import java.util.Date;

public class ChaseTime {

	private String currentTime;
	private long oldSeconds;
	private long allSeconds;

	ChaseTime() {
		restore();
	}

	public void restore() {
		oldSeconds = System.currentTimeMillis();
	}

	public void chaseGangster() {

		long diff = System.currentTimeMillis() - oldSeconds;

		allSeconds = diff / 1000;
		formatTime();
	}

	public long getAllSeconds() {
		return allSeconds;
	}

	public void formatTime() {
		int minutes = (int) (allSeconds / 60);
		if (minutes < 1) {
			currentTime = "(" + allSeconds + "s)";
		} else {
			int normalSeconds = (int) (allSeconds % 60);
			currentTime = "(" + minutes + "m:" + normalSeconds + "s)";
		}
	}

	public String getCurrentTime() {
		return currentTime;
	}

	@Override
	public String toString() {
		return getCurrentTime();
	}
}