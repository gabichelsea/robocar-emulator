/**
 * @brief Justine Car Window (monitor)
 *
 * @file CarWindow.java
 * @author Norbert Bátfai <nbatfai@gmail.com>
 * @version 0.0.16
 *
 * @section LICENSE
 *
 * Copyright (C) 2014 Norbert Bátfai, batfai.norbert@inf.unideb.hu
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * @section DESCRIPTION
 *
 * Justine - this is a rapid prototype for development of Robocar City Emulator
 * Justine Car Window (a monitor program for Robocar City Emulator)
 *
 */
package justine.robocar;

class Traffic {

    public java.util.Set<org.jxmapviewer.viewer.Waypoint> waypoints;
    public String title;

    public Traffic(java.util.Set<org.jxmapviewer.viewer.Waypoint> waypoints, String title) {

        this.waypoints = waypoints;
        this.title = title;

    }

}

class WaypointPolice implements org.jxmapviewer.viewer.Waypoint {

    org.jxmapviewer.viewer.GeoPosition geoPosition;
    String name;

    public WaypointPolice(double lat, double lon, String name) {
        geoPosition = new org.jxmapviewer.viewer.GeoPosition(lat, lon);
        this.name = name;
    }

    @Override
    public org.jxmapviewer.viewer.GeoPosition getPosition() {
        return geoPosition;
    }

    String getName() {

        return name;
    }
}

class WaypointGangster implements org.jxmapviewer.viewer.Waypoint {

    org.jxmapviewer.viewer.GeoPosition geoPosition;

    public WaypointGangster(double lat, double lon) {
        geoPosition = new org.jxmapviewer.viewer.GeoPosition(lat, lon);
    }

    @Override
    public org.jxmapviewer.viewer.GeoPosition getPosition() {
        return geoPosition;
    }
}

class WaypointCaught implements org.jxmapviewer.viewer.Waypoint {

    org.jxmapviewer.viewer.GeoPosition geoPosition;

    public WaypointCaught(double lat, double lon) {
        geoPosition = new org.jxmapviewer.viewer.GeoPosition(lat, lon);
    }

    @Override
    public org.jxmapviewer.viewer.GeoPosition getPosition() {
        return geoPosition;
    }
}

class Loc {

    double lat;
    double lon;

    public Loc(double lat, double lon) {

        this.lat = lat;
        this.lon = lon;

    }

}

class CopTeamData {
  public int num_cars;
  public int num_caught;
  public java.awt.Color color;

  public CopTeamData(int caught, java.awt.Color cl)  {
      this.num_caught = caught;
      this.color = cl;
  }
}

public class CarWindow extends javax.swing.JFrame {

    org.jxmapviewer.viewer.WaypointPainter<org.jxmapviewer.viewer.Waypoint> waypointPainter
            = new org.jxmapviewer.viewer.WaypointPainter<org.jxmapviewer.viewer.Waypoint>();
    org.jxmapviewer.viewer.GeoPosition[] geopos
            = new org.jxmapviewer.viewer.GeoPosition[4];
    org.jxmapviewer.JXMapViewer jXMapViewer
            = new org.jxmapviewer.JXMapViewer();
    java.util.Map<Long, Loc> lmap = null;
    java.io.File tfile = null;
    java.util.Random rnd = new java.util.Random();
    java.util.Scanner scan = null;

    String hostname = "localhost";
    int port = 10007;
    int num_gangsters = 0;
    String longestTeamName = "";

    StringBuilder scoreboardStringBuilder = new StringBuilder(100);

    java.awt.Color[] available_colors = { java.awt.Color.BLUE, java.awt.Color.RED,
                                          java.awt.Color.GREEN, java.awt.Color.YELLOW,
                                          java.awt.Color.ORANGE, java.awt.Color.CYAN,
                                          java.awt.Color.MAGENTA, java.awt.Color.PINK };

    java.util.Map<String, CopTeamData> cop_teams = new java.util.HashMap<String, CopTeamData>();

    javax.swing.SwingWorker<Void, Traffic> worker = new javax.swing.SwingWorker<Void, Traffic>() {

        @Override
        protected Void doInBackground() throws Exception {

            try {
                java.net.Socket trafficServer = new java.net.Socket(hostname, port);
                java.io.OutputStream os = trafficServer.getOutputStream();
                java.io.DataOutputStream dos
                        = new java.io.DataOutputStream(os);

                dos.writeUTF("<disp>");
                java.io.InputStream is = trafficServer.getInputStream();

                scan = new java.util.Scanner(is);

                int team_counter = 0;

                for (;;) {
                    java.util.Set<org.jxmapviewer.viewer.Waypoint> waypoints
                            = new java.util.HashSet<org.jxmapviewer.viewer.Waypoint>();

                    int time = 0, size = 0, minutes = 0;

                    time = scan.nextInt();
                    minutes = scan.nextInt();
                    size = scan.nextInt();

                    long ref_from = 0, ref_to = 0;
                    int step = 0, maxstep = 1, type = 0;
                    double lat, lon;
                    double lat2, lon2;
                    int num_captured_gangsters;
                    String name = "Cop";

                    num_gangsters = 0;

                    //java.util.Map<String, Integer> cops = new java.util.HashMap<String, Integer>();

                    for (CopTeamData value : cop_teams.values()) {
                      value.num_caught = 0;
                    }

                    for (int i = 0; i < size; ++i) {

                        ref_from = scan.nextLong();
                        ref_to = scan.nextLong();
                        maxstep = scan.nextInt();
                        step = scan.nextInt();
                        type = scan.nextInt();

                        if (type == 1) {
                            num_captured_gangsters = scan.nextInt();
                            name = scan.next();

                            if (cop_teams.containsKey(name)) {
                                //cop_teams.put(name, cops.get(name) + num_captured_gangsters);
                                if (num_captured_gangsters > 0)
                                {
                                  CopTeamData data = cop_teams.get(name);

                                  data.num_caught += num_captured_gangsters;
                                }
                            } else {
                                cop_teams.put(name, new CopTeamData(num_captured_gangsters,
                                                                    available_colors[team_counter % available_colors.length]));

                                if (name.length() > longestTeamName.length())
                                  longestTeamName = name;

                                team_counter++;
                            }
                        }

                        lat = lmap.get(ref_from).lat;
                        lon = lmap.get(ref_from).lon;

                        lat2 = lmap.get(ref_to).lat;
                        lon2 = lmap.get(ref_to).lon;

                        if (maxstep == 0) {
                            maxstep = 1;
                        }

                        lat += step * ((lat2 - lat) / maxstep);
                        lon += step * ((lon2 - lon) / maxstep);

                        if (type == 1) {
                            waypoints.add(new WaypointPolice(lat, lon, name));
                        } else if (type == 2) {
                            waypoints.add(new WaypointGangster(lat, lon));
                            num_gangsters++;
                        } else if (type == 3) {
                            waypoints.add(new WaypointCaught(lat, lon));
                        } else {
                            waypoints.add(new org.jxmapviewer.viewer.DefaultWaypoint(lat, lon));
                        }

                    }

                    if (time >= minutes * 60 * 1000 / 200) {
                        scan = null;
                    }

                    StringBuilder sb = new StringBuilder();

                    int sec = time / 5;
                    int min = sec / 60;
                    sec = sec - min * 60;
                    time = time - min * 60 * 5 - sec * 5;

                    sb.append("|");
                    sb.append(min);
                    sb.append(":");
                    sb.append(sec);
                    sb.append(":");
                    sb.append(2 * time);
                    sb.append("|");
                    //sb.append(" Justine - Car Window (log player for Robocar City Emulator, Robocar World Championshin in Debrecen)");

                    publish(new Traffic(waypoints, sb.toString()));

                }

            } catch (java.io.IOException e) {

                System.out.println(e.toString());

                CarWindow.this.dispatchEvent(
                        new java.awt.event.WindowEvent(CarWindow.this,
                                java.awt.event.WindowEvent.WINDOW_CLOSING));
            }

            return null;
        }

        @Override
        protected void process(java.util.List<Traffic> traffics) {

            Traffic traffic = traffics.get(traffics.size() - 1);
            setTitle(traffic.title);
            waypointPainter.setWaypoints(traffic.waypoints);

            jXMapViewer.repaint();
            repaint();
        }

        @Override
        protected void done() {
        }
    };

    /*javax.swing.Action paintTimer = new javax.swing.AbstractAction() {

        public void actionPerformed(java.awt.event.ActionEvent event) {

            java.util.Set<org.jxmapviewer.viewer.Waypoint> waypoints
                    = new java.util.HashSet<org.jxmapviewer.viewer.Waypoint>();

            if (scan != null) {

                try {

                    int time = 0, size = 0, minutes = 0;

                    time = scan.nextInt();
                    minutes = scan.nextInt();
                    size = scan.nextInt();

                    long ref_from = 0, ref_to = 0;
                    int step = 0, maxstep = 1, type = 0;
                    double lat, lon;
                    double lat2, lon2;
                    int num_captured_gangsters;
                    String name = "Cop";

                    java.util.Map<String, Integer> cops = new java.util.HashMap<String, Integer>();

                    for (int i = 0; i < size; ++i) {

                        ref_from = scan.nextLong();
                        ref_to = scan.nextLong();
                        maxstep = scan.nextInt();
                        step = scan.nextInt();
                        type = scan.nextInt();

                        if (type == 1) {
                            num_captured_gangsters = scan.nextInt();
                            name = scan.next();

                            if (cops.containsKey(name)) {
                                cops.put(name, cops.get(name) + num_captured_gangsters);
                            } else {
                                cops.put(name, num_captured_gangsters);
                            }
                        }

                        lat = lmap.get(ref_from).lat;
                        lon = lmap.get(ref_from).lon;

                        lat2 = lmap.get(ref_to).lat;
                        lon2 = lmap.get(ref_to).lon;

                        if (maxstep == 0) {
                            maxstep = 1;
                        }

                        lat += step * ((lat2 - lat) / maxstep);
                        lon += step * ((lon2 - lon) / maxstep);

                        if (type == 1) {
                            waypoints.add(new WaypointPolice(lat, lon, name));
                        } else if (type == 2) {
                            waypoints.add(new WaypointGangster(lat, lon));
                        } else if (type == 3) {
                            waypoints.add(new WaypointCaught(lat, lon));
                        } else {
                            waypoints.add(new org.jxmapviewer.viewer.DefaultWaypoint(lat, lon));
                        }

                    }

                    if (time >= minutes * 60 * 1000 / 200) {
                        scan = null;
                    }

                    StringBuilder sb = new StringBuilder();

                    int sec = time / 5;
                    int min = sec / 60;
                    sec = sec - min * 60;
                    time = time - min * 60 * 5 - sec * 5;

                    sb.append("|");
                    sb.append(min);
                    sb.append(":");
                    sb.append(sec);
                    sb.append(":");
                    sb.append(2 * time);
                    sb.append("|");
                    //sb.append(" Justine - Car Window (log player for Robocar City Emulator, Robocar World Championshin in Debrecen)");
                    sb.append(java.util.Arrays.toString(cops.entrySet().toArray()));

                    setTitle(sb.toString());
                    waypointPainter.setWaypoints(waypoints);

                    jXMapViewer.repaint();
                    repaint();

                } catch (java.util.InputMismatchException imE) {

                    java.util.logging.Logger.getLogger(
                            CarWindow.class.getName()).log(java.util.logging.Level.SEVERE, "Hibás bemenet...", imE);

                } catch (java.util.NoSuchElementException e) {

                    java.util.logging.Logger.getLogger(
                            CarWindow.class.getName()).log(java.util.logging.Level.SEVERE, "Tervezett leállás: input végét kapott el a kivételkezelő.");

                    CarWindow.this.dispatchEvent(
                            new java.awt.event.WindowEvent(CarWindow.this,
                                    java.awt.event.WindowEvent.WINDOW_CLOSING));
                }

            }

        }

    };*/

    public CarWindow(double lat, double lon, java.util.Map<Long, Loc> lmap, String hostname, int port) {

        this.lmap = lmap;
        this.hostname = hostname;
        this.port = port;

        final org.jxmapviewer.viewer.TileFactory tileFactoryArray[] = {
            new org.jxmapviewer.viewer.DefaultTileFactory(
            new org.jxmapviewer.OSMTileFactoryInfo()),
            new org.jxmapviewer.viewer.DefaultTileFactory(
            new org.jxmapviewer.VirtualEarthTileFactoryInfo(org.jxmapviewer.VirtualEarthTileFactoryInfo.MAP)),
            new org.jxmapviewer.viewer.DefaultTileFactory(
            new org.jxmapviewer.VirtualEarthTileFactoryInfo(org.jxmapviewer.VirtualEarthTileFactoryInfo.SATELLITE)),
            new org.jxmapviewer.viewer.DefaultTileFactory(
            new org.jxmapviewer.VirtualEarthTileFactoryInfo(org.jxmapviewer.VirtualEarthTileFactoryInfo.HYBRID))

        };

        org.jxmapviewer.viewer.GeoPosition debrecen
                = new org.jxmapviewer.viewer.GeoPosition(lat, lon);

        javax.swing.event.MouseInputListener mouseListener
                = new org.jxmapviewer.input.PanMouseInputListener(jXMapViewer);
        jXMapViewer.addMouseListener(mouseListener);
        jXMapViewer.addMouseMotionListener(mouseListener);
        jXMapViewer.addMouseListener(
                new org.jxmapviewer.input.CenterMapListener(jXMapViewer));
        jXMapViewer.addMouseWheelListener(
                new org.jxmapviewer.input.ZoomMouseWheelListenerCursor(jXMapViewer));
        jXMapViewer.addKeyListener(
                new org.jxmapviewer.input.PanKeyListener(jXMapViewer));

        jXMapViewer.setTileFactory(tileFactoryArray[0]);

        org.jxmapviewer.painter.Painter<org.jxmapviewer.JXMapViewer> scoreboardPainter = new org.jxmapviewer.painter.Painter<org.jxmapviewer.JXMapViewer>() {
          public void paint(java.awt.Graphics2D g2d, org.jxmapviewer.JXMapViewer map, int width, int height) {
              int num_caught = 0;

              g2d.setPaint(new java.awt.Color(0,0,0,150));

              java.awt.FontMetrics font_metrics = g2d.getFontMetrics();

              int max_name_width = font_metrics.stringWidth(longestTeamName);

              int font_height = font_metrics.getHeight();

              int scoreboard_height = (font_height + 10) * (cop_teams.size() + 1) + 5;
              int scoreboard_width =  max_name_width + font_height + font_metrics.charWidth('-') * 12 + 15;
              g2d.fillRoundRect(10, 10, scoreboard_width, scoreboard_height, 10, 10);

              int draw_y = 15;

              for (java.util.Map.Entry<String, CopTeamData> entry : cop_teams.entrySet()) {
                  String team_name = entry.getKey();
                  CopTeamData team_data = entry.getValue();

                  g2d.setPaint(team_data.color);

                  g2d.fillOval(15, draw_y, font_height, font_height);

                  g2d.setPaint(java.awt.Color.WHITE);

                  draw_y += font_height;

                  scoreboardStringBuilder.setLength(0);

                  scoreboardStringBuilder.append(team_name);
                  scoreboardStringBuilder.append(" - ");
                  scoreboardStringBuilder.append(team_data.num_caught);

                  g2d.drawString(scoreboardStringBuilder.toString(),
                                 font_height + 5 + 15, draw_y);

                  draw_y += 10;

                  num_caught += team_data.num_caught;
              }

              scoreboardStringBuilder.setLength(0);

              scoreboardStringBuilder.append("Gangsters: ");
              scoreboardStringBuilder.append(num_caught);
              scoreboardStringBuilder.append("/");
              scoreboardStringBuilder.append(num_gangsters);

              draw_y += font_height;
              g2d.drawString(scoreboardStringBuilder.toString(),
                             15, draw_y);
          }
        };

        ClassLoader classLoader = this.getClass().getClassLoader();

        final java.awt.Image markerImg
                = new javax.swing.ImageIcon(classLoader.getResource("logo1.png")).getImage();
        final java.awt.Image markerImgPolice
                = new javax.swing.ImageIcon(classLoader.getResource("logo2.png")).getImage();
        final java.awt.Image markerImgGangster
                = new javax.swing.ImageIcon(classLoader.getResource("logo3.png")).getImage();
        final java.awt.Image markerImgCaught
                = new javax.swing.ImageIcon(classLoader.getResource("logo4.png")).getImage();

        waypointPainter.setRenderer(
                new org.jxmapviewer.viewer.WaypointRenderer<org.jxmapviewer.viewer.Waypoint>() {
                    @Override
                    public void paintWaypoint(java.awt.Graphics2D g2d, org.jxmapviewer.JXMapViewer jXMapV,
                            org.jxmapviewer.viewer.Waypoint w) {

                        java.awt.geom.Point2D point = jXMapV.getTileFactory().geoToPixel(
                                w.getPosition(), jXMapV.getZoom());
                        java.awt.Rectangle sb = new java.awt.Rectangle(10, 10, 200, 200);

                        g2d.setColor(java.awt.Color.WHITE);
                        g2d.fill(sb);
                        g2d.setColor(java.awt.Color.BLACK);
                        g2d.draw(sb);

                        if (w instanceof WaypointPolice) {
                            g2d.drawImage(markerImgPolice, (int) point.getX() - markerImgPolice.getWidth(jXMapV),
                                    (int) point.getY() - markerImgPolice.getHeight(jXMapV), null);

                            java.awt.Color border_color =
                              cop_teams.get(((WaypointPolice) w).getName()).color;
                              //available_colors[teams.indexOf(((WaypointPolice) w).getName()) % available_colors.length];

                            g2d.setFont(new java.awt.Font("Serif", java.awt.Font.BOLD, 14));
                            java.awt.FontMetrics fm = g2d.getFontMetrics();
                            int nameWidth = fm.stringWidth(((WaypointPolice) w).getName());
                            g2d.setColor(java.awt.Color.GRAY);
                            java.awt.Rectangle rect = new java.awt.Rectangle((int) point.getX(), (int) point.getY(), nameWidth + 4, 20);

                            g2d.fill(rect);
                            g2d.setColor(border_color);
                            g2d.draw(rect);
                            g2d.setColor(java.awt.Color.WHITE);
                            g2d.drawString(((WaypointPolice) w).getName(), (int) point.getX() + 2, (int) point.getY() + 20 - 5);

                        } else if (w instanceof WaypointGangster) {
                            g2d.drawImage(markerImgGangster, (int) point.getX() - markerImgGangster.getWidth(jXMapV),
                                    (int) point.getY() - markerImgGangster.getHeight(jXMapV), null);
                        } else if (w instanceof WaypointCaught) {
                            g2d.drawImage(markerImgCaught, (int) point.getX() - markerImgCaught.getWidth(jXMapV),
                                    (int) point.getY() - markerImgCaught.getHeight(jXMapV), null);
                        } else {
                            g2d.drawImage(markerImg, (int) point.getX() - markerImg.getWidth(jXMapV),
                                    (int) point.getY() - markerImg.getHeight(jXMapV), null);
                        }
                    }
                });


        org.jxmapviewer.painter.CompoundPainter<org.jxmapviewer.JXMapViewer> painters =
          new org.jxmapviewer.painter.CompoundPainter<org.jxmapviewer.JXMapViewer>();

        painters.setPainters(waypointPainter, scoreboardPainter);

        jXMapViewer.setOverlayPainter(painters);
        jXMapViewer.setZoom(9);
        jXMapViewer.setAddressLocation(debrecen);
        jXMapViewer.setCenterPosition(debrecen);

        jXMapViewer.addKeyListener(new java.awt.event.KeyAdapter() {
            int index = 0;

            public void keyPressed(java.awt.event.KeyEvent evt) {

                if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
                    jXMapViewer.setTileFactory(tileFactoryArray[++index % 4]);
                    jXMapViewer.repaint();
                    repaint();
                }
            }
        });

        setTitle("Justine - Car Window (log player for Robocar City Emulator, Robocar World Championshin in Debrecen)");
        getContentPane().add(jXMapViewer);

        java.awt.Dimension screenDim = java.awt.Toolkit.getDefaultToolkit().getScreenSize();

        setSize(screenDim.width/2, screenDim.height/2);
        setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

        worker.execute();

    }

    public static void readMap(java.util.Map<Long, Loc> lmap, String name) {

        java.util.Scanner scan;
        java.io.File file = new java.io.File(name);

        long ref = 0;
        double lat = 0.0, lon = 0.0;
        try {

            scan = new java.util.Scanner(file);

            while (scan.hasNext()) {

                ref = scan.nextLong();
                lat = scan.nextDouble();
                lon = scan.nextDouble();

                lmap.put(ref, new Loc(lat, lon));
            }

        } catch (Exception e) {

            java.util.logging.Logger.getLogger(
                    CarWindow.class
                    .getName()).log(java.util.logging.Level.SEVERE, "hibás noderef2GPS leképezés", e);

        }

    }

    public static void main(String[] args) {

        final java.util.Map<Long, Loc> lmap = new java.util.HashMap<Long, Loc>();

        if (args.length == 1) {

            readMap(lmap, args[0]);

            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {

                    java.util.Map.Entry<Long, Loc> e = lmap.entrySet().iterator().next();

                    new CarWindow(e.getValue().lat, e.getValue().lon, lmap, "localhost", 10007).setVisible(true);
                }
            });

        } else if (args.length == 2) {

            readMap(lmap, args[0]);

            final String hostname = args[1];

            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {

                    java.util.Map.Entry<Long, Loc> e = lmap.entrySet().iterator().next();

                    new CarWindow(e.getValue().lat, e.getValue().lon, lmap, hostname, 10007).setVisible(true);
                }
            });

        } else if (args.length == 3) {

            readMap(lmap, args[0]);

            final String hostname = args[1];
            final int port = Integer.parseInt(args[2]);

            javax.swing.SwingUtilities.invokeLater(new Runnable() {
                public void run() {

                    java.util.Map.Entry<Long, Loc> e = lmap.entrySet().iterator().next();

                    new CarWindow(e.getValue().lat, e.getValue().lon, lmap, hostname, port).setVisible(true);
                }
            });

        } else {

            System.out.println("java -jar target/site/justine-rcwin-0.0.16-jar-with-dependencies.jar ../../../lmap.txt localhost");
        }

    }

}
