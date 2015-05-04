/**
 * @brief Justine - this is a rapid prototype for development of Robocar City Emulator
 *
 * @file myshmclient.cpp
 * @author  Norbert Bátfai <nbatfai@gmail.com>
 * @version 0.0.10
 *
 * @section LICENSE
 *
 * Copyright(C) 2014 Norbert Bátfai, batfai.norbert@inf.unideb.hu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @section DESCRIPTION
 * Robocar City Emulator and Robocar World Championship
 *
 * desc
 *
 */

#include <myshmclient.hpp>

int
justine::sampleclient::MyShmClient::get_num_vertices(int &sum_edges)
{
  std::set<osmium::unsigned_object_id_type> sum_vertices;

  for(justine::robocar::shm_map_Type::iterator iter=shm_map->begin();
        iter!=shm_map->end(); ++iter)
  {
    sum_vertices.insert(iter->first);
    sum_edges+=iter->second.m_alist.size();

    for(auto noderef : iter->second.m_alist)
    {
      sum_vertices.insert(noderef);
    }

  }

  return sum_vertices.size();
}

void
justine::sampleclient::MyShmClient::PrintEdges(unsigned more)
{
  VertexNameMap vertexNameMap =
    boost::get(boost::vertex_name, *nr_graph_);

  std::pair<NRGVertexIter, NRGVertexIter> vi;

  unsigned count {0};

  for(vi = boost::vertices(*nr_graph_); vi.first != vi.second; ++vi.first)
  {
    if(more)
    {
      if(++count > more)
        break;
    }

    std::cout << vertexNameMap[*vi.first] <<  " ";
  }

  std::cout << std::endl;
}

void
justine::sampleclient::MyShmClient::PrintVertices(unsigned more)
{
  VertexNameMap vertexNameMap =
    boost::get(boost::vertex_name, *nr_graph_);

  unsigned count {0};

  osmium::unsigned_object_id_type prev = 0;
  NRGEdgeIter ei, ei_end;

  for(boost::tie(ei, ei_end) = boost::edges(*nr_graph_);
       ei != ei_end; ++ei)
  {
    auto ii = vertexNameMap[boost::source(*ei, *nr_graph_)];

    if(ii != prev)
        std::cout << std::endl;

    std::cout << "(" << ii
              << " -> " << vertexNameMap[boost::target(*ei, *nr_graph_)]
              << ") ";

    prev = ii;

    if(more)
    {
        if(++count > more)
          break;
    }
  }

  std::cout << std::endl;
}

void
justine::sampleclient::MyShmClient::LogMessage(
  const char *command,
  char *response_buffer)
{
  bool warn_user =
    (strstr(response_buffer, "WARN") || strstr(response_buffer, "ERR"));

  if ((verbose_mode_) || (warn_user))
  {
    std::cout << command << " sent:" << std::endl;

    // so much C-style...
    char *start_position = response_buffer, *end_position;

    while ( (end_position = strchr(start_position, '>')) )
    {
      std::cout << "\t";
      std::cout.write(start_position, end_position - start_position + 1);
      std::cout << "\n";

      start_position = end_position + 1;
    }
  }
}

void
justine::sampleclient::MyShmClient::LogMessage(std::string &&msg)
{
  bool warn_user = (msg.find("WARN") != std::string::npos) ||
                   (msg.find("ERR")  != std::string::npos);

  if ((verbose_mode_) || (warn_user))
  {
    std::cout << msg;
  }
}

// csak, hogy szem előtt legyenek
#ifdef NEVER_DEFINED
class SharedData
{
public:
  uint_vector m_alist; //std::vector of unsigned ints
  uint_vector m_salist;
  uint_vector m_palist;

  int lon;
  int lat;

  SharedData ( const void_allocator &void_alloc )
  :  m_alist ( void_alloc ), m_salist ( void_alloc ), m_palist ( void_alloc )
  {}
};

using map_pair_Type =
  std::pair<const unsigned int, SharedData>;

using map_pair_Type_allocator =
  boost::interprocess::allocator<map_pair_Type, segment_manager_Type>;

using shm_map_Type =
  boost::interprocess::map< unsigned int, SharedData, std::less<unsigned int>,
    map_pair_Type_allocator>;
#endif

void
justine::sampleclient::MyShmClient::BuildGraph(void)
{
  this->nr_graph_ = new NodeRefGraph();

  int count {0};

  // végigmegyünk a map osszes párján
  // az egyes párok first-je (key) egy unsigned int, ami
  // az azonosítója az adott csomópontnak
  for(justine::robocar::shm_map_Type::iterator iter = shm_map->begin();
       iter!=shm_map->end();
       ++iter)
  {
    // az adott csomópont azonosítója
    osmium::unsigned_object_id_type u = iter->first;

    // minden csomópont second-ja (value)
    // egy SharedData objektum, ami a csomópont adjacency listjét
    // is tárolja (azoknak a csomópontoknak az azonosítói, amelyekkel kapcsolatban van
    //  az adott csomópont)
    // Tehát ez a belső ciklus megy végig az osszes olyan csomoponton,
    // ami a kulso ciklus csomopontjaval kapcsolatban van
    for(justine::robocar::uint_vector::iterator noderefi = iter->second.m_alist.begin();
        noderefi != iter->second.m_alist.end();
        ++noderefi)
    {
      NodeRefGraph::vertex_descriptor f; // az adott csomópont adatait írja le
                                         // a csomópont neve, ID-ja
                                         // ugyanaz, mint az NRGVertex típus

      // megnézzük, hogy a kis helper struktúránkba benne van-e már
      // a külső csomópont
      std::map<osmium::unsigned_object_id_type, NRGVertex>::iterator it =
        nr2v.find(u); // FIXME: külső ciklusban nem lehetne?

      // ha nincs benne, akkor belerakjuk
      // létrehozunk egy új csomópontot

      // az nr2v minden azonosítóhoz hozzárendeli a csomópont adatait
      // amit a gráf tartalmaz
      if(it == nr2v.end())
      {
        f = boost::add_vertex(u, *nr_graph_);
        nr2v[u] = f;

        ++count;
      }
      else // ha már benne van, akkor egyszerűen csak fogjuk az adatokat
      {
        f = it->second;
      }

      NodeRefGraph::vertex_descriptor t;

      // most a belső ciklus nodejára csináljuk meg ugyanezt
      it = nr2v.find(*noderefi);

      if(it == nr2v.end())
      {
        t = boost::add_vertex(*noderefi, *nr_graph_);
        nr2v[*noderefi] = t;

        ++count;
      }
      else
      {
        t = it->second;
      }

      // megnézzük, hogy az adott elem hanyadik is az adjacency listben
      // ez amolyan index lesz
      int to = std::distance(iter->second.m_alist.begin(), noderefi);

      // a palist tárolja a súlyozást
      // tehát ezzel a függvénnnyel hozzáadunk egy új élt a csomópontok kozott
      // a grafban, melynek súlyát az eredeti SharedData objektum palist-je tárolta
      // ezt a shmclient.palist() fv adja vissza
      boost::add_edge(f, t, palist(iter->first, to), *nr_graph_);
    }
  }

  #ifdef DEBUG
  std::cout << "# vertices count: " << count << std::endl
            << "# BGF edges: " << boost::num_edges(*nr_graph_) << std::endl
            << "# BGF vertices: " << boost::num_vertices(*nr_graph_) << std::endl;;
  #endif
}

std::vector<osmium::unsigned_object_id_type>
justine::sampleclient::MyShmClient::DetermineDijkstraPath(
  osmium::unsigned_object_id_type from,
  osmium::unsigned_object_id_type to)
{
  #ifdef DEBUG
  auto start = std::chrono::high_resolution_clock::now();
  #endif

  std::vector<NRGVertex> parents(boost::num_vertices(*nr_graph_));
  std::vector<int> distances(boost::num_vertices(*nr_graph_));

  VertexIndexMap vertexIndexMap = boost::get(boost::vertex_index, *nr_graph_);

  PredecessorMap predecessorMap(&parents[0], vertexIndexMap);
  DistanceMap distanceMap(&distances[0], vertexIndexMap);

  boost::dijkstra_shortest_paths(
    *nr_graph_, nr2v[from],
    boost::distance_map(distanceMap).predecessor_map(predecessorMap));

  VertexNameMap vertexNameMap = boost::get(boost::vertex_name, *nr_graph_);

  std::vector<osmium::unsigned_object_id_type> path;

  NRGVertex tov = nr2v[to];
  NRGVertex fromv = predecessorMap[tov];

  #ifdef DEBUG
  int dist {0};
  #endif

  while(fromv != tov)
  {
    NRGEdge edge = boost::edge(fromv, tov, *nr_graph_).first;

    #ifdef DEBUG
    std::f << vertexNameMap[boost::source(edge, *nr_graph_)]
              << " lol-> "
              << vertexNameMap[boost::target(edge, *nr_graph_)] << std::endl;
    dist += distanceMap[fromv];
    #endif

    path.push_back(vertexNameMap[boost::target(edge, *nr_graph_)]);

    tov = fromv;
    fromv = predecessorMap[tov];
  }

  path.push_back(from);

  std::reverse(path.begin(), path.end());

  #ifdef DEBUG
  std::cout << std::chrono::duration_cast<std::chrono::milliseconds>(
                std::chrono::high_resolution_clock::now() - start).count()
            << " ms " << dist << " meters" << std::endl;

  std::copy(path.begin(), path.end(),
            std::ostream_iterator<osmium::unsigned_object_id_type>(std::cout, " "));
  #endif

  return path;
}

void justine::sampleclient::MyShmClient::AcquireCarDataFromServer(
  boost::asio::ip::tcp::socket & socket,
  int id, unsigned *f, unsigned *t, unsigned* s)
{
  boost::system::error_code error_code;

  char buffer[kMaxBufferLen];

  size_t msg_length = std::sprintf(buffer, "<car ");
  msg_length += std::sprintf(buffer + msg_length, "%d %d>", auth_code_, id);

  socket.send(boost::asio::buffer(buffer, msg_length));

  msg_length = socket.read_some(boost::asio::buffer(buffer), error_code);

  if(error_code == boost::asio::error::eof)
  {
    // TODO
  }
  else if(error_code)
  {
    throw boost::system::system_error(error_code);
  }

  std::sscanf(buffer, "<OK %*d %u %u %u", f, t, s);

  LogMessage("CAR", buffer);
}

std::vector<justine::sampleclient::MyShmClient::Gangster>
justine::sampleclient::MyShmClient::AcquireGangstersFromServer(
    boost::asio::ip::tcp::socket &socket,
    int id,
    osmium::unsigned_object_id_type cop)
{
  boost::system::error_code error_code;

  char buffer[kMaxBufferLen];

  size_t msg_length = std::sprintf(buffer, "<gangsters ");
  msg_length += std::sprintf(buffer + msg_length, "%d>", auth_code_);

  socket.send(boost::asio::buffer(buffer,msg_length));

  msg_length = socket.read_some(boost::asio::buffer(buffer), error_code);

  if(error_code == boost::asio::error::eof)
  {
    // TODO
  }
  else if(error_code)
  {
    throw boost::system::system_error(error_code);
  }

  /* reading all gangsters into a vector */
  int gangster_car_id {0};

  int bytes_read      {0};
  int seek_pointer    {0};

  unsigned from_node, to_node, step;

  std::vector<Gangster> gangsters;

  while(std::sscanf(buffer+seek_pointer, "<OK %d %u %u %u>%n",
                    &gangster_car_id, &from_node, &to_node, &step, &bytes_read) == 4)
  {
    seek_pointer += bytes_read;
    gangsters.emplace_back(Gangster {gangster_car_id, from_node, to_node, step});
  }

  std::sort(gangsters.begin(), gangsters.end(), [this, cop](Gangster x, Gangster y)
  {
    return dst(cop, x.to) < dst(cop, y.to);
  });

  LogMessage("GANGSTER", buffer);

  return gangsters;
}

//std::vector<justine::sampleclient::MyShmClient::Cop>
int
justine::sampleclient::MyShmClient::InitializeCops(
    boost::asio::ip::tcp::socket & socket)
{
  boost::system::error_code error_code;

  char buffer[kMaxBufferLen];

  size_t msg_length = std::sprintf(buffer, "<init guided %d %d c>",
                                   auth_code_, num_cops_);

  socket.send(boost::asio::buffer(buffer, msg_length));

  msg_length = socket.read_some(boost::asio::buffer(buffer), error_code);

  if(error_code == boost::asio::error::eof)
  {
      // TODO
  }
  else if(error_code)
  {
      throw boost::system::system_error(error_code);
  }

  int cop_car_id   {0};
  int bytes_read   {0};
  int seek_pointer {0};

  while(std::sscanf(buffer+seek_pointer, "<OK %d %*d/%*d %*c>%n", &cop_car_id, &bytes_read) == 1)
  {
    seek_pointer += bytes_read;
    cops_.push_back(cop_car_id);
  }

  LogMessage("INIT", buffer);

  return cops_.size();
}

/*
void justine::sampleclient::MyShmClient::pos(boost::asio::ip::tcp::socket & socket, int id)
{
  boost::system::error_code err;

  size_t length = std::sprintf(buffer, "<pos ");
  length += std::sprintf(buffer+length, "%d %u %u>", id, 2969934868u, 651365957u);

  socket.send(boost::asio::buffer(buffer, length));

  length = socket.read_some(boost::asio::buffer(buffer), err);

  if(err == boost::asio::error::eof)
  {
    // TODO
  }
  else if(err)
  {
    throw boost::system::system_error(err);
  }

  std::cout.write(buffer, length);
  std::cout << "Command POS sent." << std::endl;
}*/

void justine::sampleclient::MyShmClient::SendRouteToServer(
  boost::asio::ip::tcp::socket & socket,
  int id,
  std::vector<osmium::unsigned_object_id_type> & path
)
{
  boost::system::error_code error_code;

  char buffer[kMaxBufferLen];

  size_t msg_length = std::sprintf(buffer,
                                   "<route %zu %d %d", path.size(), auth_code_, id);

  for(auto ui: path)
  {
    msg_length += std::sprintf(buffer + msg_length, " %lu", ui);
  }

  msg_length += std::sprintf(buffer + msg_length, ">");

  socket.send(boost::asio::buffer(buffer, msg_length));

  msg_length = socket.read_some(boost::asio::buffer(buffer), error_code);

  if(error_code == boost::asio::error::eof)
  {
    // TODO
  }
  else if(error_code)
  {
    throw boost::system::system_error(error_code);
  }

  LogMessage("ROUTE", buffer);
}

void justine::sampleclient::MyShmClient::Authenticate(boost::asio::ip::tcp::socket &socket)
{
  boost::system::error_code error_code;

  char buffer[kMaxBufferLen];

  size_t msg_length = std::sprintf(buffer, "<auth %s c>",
                                   m_team_name_.c_str());

  socket.send(boost::asio::buffer(buffer, msg_length));

  msg_length = socket.read_some(boost::asio::buffer(buffer), error_code);

  if(error_code == boost::asio::error::eof)
  {
      // TODO
  }
  else if(error_code)
  {
      throw boost::system::system_error(error_code);
  }

  std::sscanf(buffer, "<OK %d>", &auth_code_);

  LogMessage("AUTH", buffer);

  if (auth_code_ != 0)
    this->is_authenticated_ = true;
}

void justine::sampleclient::MyShmClient::SimulateCarsLoop(void)
{
  boost::asio::io_service io_service;

  boost::asio::ip::tcp::resolver resolver(io_service);
  boost::asio::ip::tcp::resolver::query query(boost::asio::ip::tcp::v4(), "localhost", port_);
  boost::asio::ip::tcp::resolver::iterator iterator = resolver.resolve(query);

  boost::asio::ip::tcp::socket socket(io_service);
  boost::asio::connect(socket, iterator);

  Authenticate(socket);

  if (!is_authenticated_)
    return;
  else
    LogMessage("NOTE:\n\tAuthenticated successfully");

  int cops_initialized = InitializeCops(socket);

  if (cops_initialized < num_cops_)
  {
    LogMessage("WARN:\n\tFailed to initialize the number of cops requested\n");
  }
  else
  {
    LogMessage("NOTE:\n\tAll cops have been initialized successfully\n");
  }

  unsigned from_node  {0u};
  unsigned to_node    {0u};
  unsigned step       {0u};

  std::vector<Gangster> gangsters;

  for(;;)
  {
    std::this_thread::sleep_for(std::chrono::milliseconds(200));

    for(auto cop:cops_)
    {
      AcquireCarDataFromServer(socket, cop, &from_node, &to_node, &step);

      gangsters = AcquireGangstersFromServer(socket, cop, to_node);

      if(gangsters.size() > 0)
      {
        std::vector<osmium::unsigned_object_id_type> path =
          DetermineDijkstraPath(to_node, gangsters[0].to);

        if(path.size() > 1)
        {
          #ifdef DEBUG
          std::copy(path.begin(), path.end(),
                    std::ostream_iterator<osmium::unsigned_object_id_type>(std::cout, " -> "));
          #endif

          SendRouteToServer(socket, cop, path);
        }
      }

    }
  }
}
