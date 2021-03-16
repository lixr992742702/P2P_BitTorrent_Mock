# P2P_BitTorrent_Mock
This is the final proj of Computer Network 20Fall in UF. This proj descibes how P2P works. 

# Developer
-Xiangrui Li  xiangrui.li@ufl.edu
-Xuyang Lin xuyang.lin@ufl.edu
-Xiang Zhou xiang.zhou@ufl.edu

# How to compile and run 
  - ./start1001.sh //only run peer1001
  - ./startAll.sh // Run all peers. Peer 1001 and 1006 has file already. They slice the file and send segment to others.

# Desciption
  - At the begin, we input peer id and whether they have file
  - then, we slice the file, and give each segmentation a id
  - peers will send connection request to other added peers. The peers will build connect with it and send back.
  - peer will send seg_id it need to a peer. if this peer has, it will return this seg. 
  - Repeat the last step util all peers own all the segment. Merge seg and delete seg.


