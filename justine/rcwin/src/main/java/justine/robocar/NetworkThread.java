package justine.robocar;

import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;



import justine.robocar.TrafficStateProtos.CarData;
import justine.robocar.TrafficStateProtos.TrafficStateHeader;

import org.jxmapviewer.viewer.GeoPosition;

public class NetworkThread extends Thread implements PlayBack {

	private String hostname;
	private int port;
	private Map<String, CopTeamData> cop_teams = new HashMap<String, CopTeamData>();
	private Color[] available_colors = { Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.ORANGE, Color.CYAN, Color.MAGENTA, Color.PINK };
	private String longestTeamName = "";
	final static Map<Long, Loc> lmap = new HashMap<Long, Loc>();
	public static GeoPosition position;
	private OnNewTrafficListener onNewTrafficListener = null;
	private volatile int speed = 200;
	private volatile long neededPosition = -1;
	private volatile boolean playing = true;
	private ChaseTime ctime;

	public static List caughtListForPolice = new ArrayList<Integer>();
	public static List timeListForPolice = new ArrayList<ChaseTime>();
	public static List rankingForPolice = new ArrayList<Long>();

	public interface OnNewTrafficListener {
		public void onNewTraffic(Traffic traffic);
	}

	public NetworkThread(String hostname, int port) {
		super();
		this.hostname = hostname;
		this.port = port;
	}

	@Override
	public void run() {
		readMap(lmap, CarWindow.file);
		Socket trafficServer = null;

		try {
			BufferedInputStream bis;
			InputStream is;
			File f = null;
			FileInputStream in = null;
			if (CarWindow.log) {
				f = new File(CarWindow.logfile);
				in = new FileInputStream(f);
				bis = new BufferedInputStream(in);
			} else {
				trafficServer = new Socket(hostname, port);
				OutputStream os = trafficServer.getOutputStream();
				DataOutputStream dos = new DataOutputStream(os);
				dos.writeUTF("<disp>");

				is = trafficServer.getInputStream();
				bis = new BufferedInputStream(is);
			}

			// 512 KB
			byte[] buff = new byte[524288];

			LinkedList<WaypointPolice> cop_list = new LinkedList<WaypointPolice>();
			LinkedList<WaypointGangster> gangster_list = new LinkedList<WaypointGangster>();
			LinkedList<WaypointCaught> caught_list = new LinkedList<WaypointCaught>();
			LinkedList<WaypointNormal> default_list = new LinkedList<WaypointNormal>();
			LinkedList<WaypointPedestrian> pedestrian_list = new LinkedList<WaypointPedestrian>();

			LinkedList<Loc> path = new LinkedList<Loc>();

			Traffic traffic = new Traffic();
			int team_counter = 0;
			while (!Thread.currentThread().isInterrupted()) {
				if (CarWindow.log) {
					Thread.sleep(speed);
					while (!playing) {
						Thread.sleep(200);
					}
					if (neededPosition != -1) {
						bis.close();
						in.close();
						in = new FileInputStream(f);
						bis = new BufferedInputStream(in);
						bis.skip(neededPosition);
						neededPosition = -1;
					}
				}
				try {
					cop_list.clear();
					gangster_list.clear();
					caught_list.clear();
					default_list.clear();
					pedestrian_list.clear();
					path.clear();

					int time = 0, size = 0, minutes = 0;
					long ref_from = 0, ref_to = 0;
					long step = 0, maxstep = 100;
					int id = 0, type = 0;
					double lat, lon;
					double lat2, lon2;
					int num_captured_gangsters = 0;
					String name = "Cop";

					// 4 bájtot olvasunk a bufferünkbe (0. indextől 3-ig)
					// ez a 4 bájt az üzenet hossza, amit fogadunk
					bis.read(buff, 0, 4);
					// a ByteBuffer static wrap() metódusával egy ByteBufferbe
					// rakjuk
					// a beolvasott adatot. Utána gondoskodunk, hogy biztosan
					// megfelelő
					// bájtsorrendet alkalmazzunk, majd int-ként elkérjük
					int toRead;

					toRead = java.nio.ByteBuffer.wrap(buff).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();

					// annyit olvasunk a bufferbe, amennyit előzőleg kaptunk
					// 0
					bis.read(buff, 0, toRead);

					// a protobufnak pontosan akkora méretű bufferra van
					// szüksége,
					// mint
					// az üzenet
					byte[] readFrom = new byte[toRead];

					// a nagy bufferünkből kivesszük a beolvasott az üzenetet és
					// belerakjuk a pontos
					// méretű bufferbe
					java.nio.ByteBuffer.wrap(buff).order(java.nio.ByteOrder.LITTLE_ENDIAN).get(readFrom, 0, toRead);

					// a már ismert TrafficStateHeader objektum
					// a parseFrom() metódussal egy byte[] tombből képes
					// visszaállítani
					// az objektumot
					TrafficStateHeader trafficStateHeader = TrafficStateHeader.parseFrom(readFrom);

					// kiolvassuk a protobuf által biztosított getter
					// függvényekkel az adatokat
					minutes = trafficStateHeader.getTimeMinutes();
					time = trafficStateHeader.getTimeElapsed();
					size = trafficStateHeader.getNumCars();

					for (CopTeamData value : cop_teams.values()) {
						value.num_caught = 0;
					}

					for (int i = 0; i < size; ++i) {
						// az üzenet mérete
						bis.read(buff, 0, 4);
						toRead = java.nio.ByteBuffer.wrap(buff).order(java.nio.ByteOrder.LITTLE_ENDIAN).getInt();

						// maga az üzenet
						bis.read(buff, 0, toRead);

						readFrom = new byte[toRead];

						java.nio.ByteBuffer.wrap(buff).order(java.nio.ByteOrder.LITTLE_ENDIAN).get(readFrom, 0, toRead);

						CarData carData = CarData.parseFrom(readFrom);

						ref_from = carData.getNodeFrom();
						ref_to = carData.getNodeTo();
						maxstep = carData.getMaxStep();
						step = carData.getStep();
						type = carData.getType().getNumber();

						lat = lmap.get(ref_from).lat;
						lon = lmap.get(ref_from).lon;

						lat2 = lmap.get(ref_to).lat;
						lon2 = lmap.get(ref_to).lon;

						if (maxstep == 0) {
							maxstep = 1;
						}

						double lat_ = lat + (step + 1) * ((lat2 - lat) / maxstep);
						double lon_ = lon + (step + 1) * ((lon2 - lon) / maxstep);

						if (maxstep == 1) {
							lat_ = lat + (step) * ((lat2 - lat) / maxstep);
							lon_ = lon + (step) * ((lon2 - lon) / maxstep);
						}

						lat += step * ((lat2 - lat) / maxstep);
						lon += step * ((lon2 - lon) / maxstep);

						switch (type) {
						case 1:
							num_captured_gangsters = carData.getCaught();
							name = carData.getTeam();
							id = carData.getId();
							path.clear();
							int pathLength = carData.getSize();
							for (int j = 0; j < pathLength; j++) {
								path.add(lmap.get(carData.getPath(j)));
							}
							path.addFirst(lmap.get(ref_from));
							if (cop_teams.containsKey(name)) {
								if (num_captured_gangsters > 0) {
									CopTeamData data = cop_teams.get(name);
									data.num_caught += num_captured_gangsters;
								}
							} else {
								cop_teams.put(name, new CopTeamData(num_captured_gangsters, available_colors[team_counter % available_colors.length]));

								if (name.length() > longestTeamName.length())
									longestTeamName = name;

								team_counter++;
							}
							cop_list.add(new WaypointPolice(new GeoPosition(lat, lon), new GeoPosition(lat_, lon_), name, num_captured_gangsters, ref_from, ref_to, id, cop_teams.get(name).color, path));
							break;
						case 2:
							gangster_list.add(new WaypointGangster(new GeoPosition(lat, lon), new GeoPosition(lat_, lon_)));
							break;
						case 3:
							caught_list.add(new WaypointCaught(new GeoPosition(lat, lon), new GeoPosition(lat_, lon_)));
							break;
						case 4:
							pedestrian_list.add(new WaypointPedestrian(new GeoPosition(lat, lon), new GeoPosition(lat_, lon_)));
							break;
						default:
							default_list.add(new WaypointNormal(new GeoPosition(lat, lon), new GeoPosition(lat_, lon_)));
							break;
						}

					}

					if (time >= minutes * 60 * 1000 / 200) {
						throw new Exception();
					}

					StringBuilder sb = new StringBuilder();

					int sec = time / 5;
					int min = sec / 60;
					sec = sec - min * 60;
					time = time - min * 60 * 5 - sec * 5;

					sb.append("|");
					if (min < 10)
						sb.append("0");
					sb.append(min);
					sb.append(":");
					if (sec < 10)
						sb.append("0");
					sb.append(sec);
					sb.append("|");


					int policeID = 0;
					for (WaypointPolice way: cop_list) {
						way.setPoliceID(policeID);

						if (policeID == timeListForPolice.size()) {
							timeListForPolice.add(new ChaseTime());
							caughtListForPolice.add(new Integer(0));
							rankingForPolice.add(new Long(0));	
						} else {
							ctime = (ChaseTime)timeListForPolice.get(policeID);
							rankingForPolice.set(policeID, ctime.getAllSeconds());
						}

						++policeID;
					}



					int index;
					Collections.sort(rankingForPolice);
					for (WaypointPolice way: cop_list) {
						ctime = (ChaseTime)timeListForPolice.get(way.getPoliceID());
						index = rankingForPolice.indexOf(ctime.getAllSeconds());
						way.setRank(index);

						rankingForPolice.set(index, -1);
					}



					traffic = new Traffic();
					traffic.copList = cop_list;
					traffic.gangsterList = gangster_list;
					traffic.caughtList = caught_list;
					traffic.defaultList = default_list;
					traffic.pedestrianList = pedestrian_list;
					traffic.title = sb.toString();
					traffic.cop_teams = cop_teams;
					traffic.longestTeamName = longestTeamName;
					traffic.timestamp = System.currentTimeMillis();
					traffic.seconds = sec;


					if (onNewTrafficListener != null)
						onNewTrafficListener.onNewTraffic(Traffic.create(traffic));

				} catch (Exception e) {
					if (!CarWindow.log)
						throw e;
				}
			}
		} catch (Exception e) {

		} finally {
			try {
				if (!CarWindow.log)
					trafficServer.close();
			} catch (IOException e) {
			}
		}

	}

	public void readMap(Map<Long, Loc> lmap, String name) {

		Scanner scan;
		java.io.File file = new java.io.File(name);

		long ref = 0;
		double lat = 0.0, lon = 0.0;
		try {

			scan = new Scanner(file);
			boolean first = true;
			while (scan.hasNext()) {

				ref = scan.nextLong();
				lat = scan.nextDouble();
				lon = scan.nextDouble();
				if (first) {
					position = new GeoPosition(lat, lon);
					first = false;
				}
				lmap.put(ref, new Loc(lat, lon));
			}

		} catch (Exception e) {

			Logger.getLogger(CarWindow.class.getName()).log(Level.SEVERE, "hibás noderef2GPS leképezés", e);

		}

	}

	public void setOnNewTrafficListener(OnNewTrafficListener onNewTrafficListener) {
		this.onNewTrafficListener = onNewTrafficListener;
	}

	public void start_play() {
		playing = true;
	}

	public void stop_play() {
		playing = false;

	}

	public void seekTo(long position) {
		neededPosition = position;
	}

	public void setSpeed(int speed) {
		if (speed > 0)
			this.speed = speed;
	}

	public int getSpeed() {
		return speed;
	}

}
