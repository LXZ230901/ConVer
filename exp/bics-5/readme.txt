30 节点

interface Fa6/0
 ip address 1.0.0.1 255.255.255.0
 description "To PeerBarcelona_1"
 speed auto
 duplex auto

 neighbor 1.0.0.2 remote-as 50010
 neighbor 1.0.0.2 send-community


100 节点：
interface Fa6/0
 ip address 2.0.0.1 255.255.255.0
 description "To Vienna"
 speed auto
 duplex auto

 neighbor 2.0.0.2 remote-as 50020
 neighbor 2.0.0.2 send-community
!


160节点：

interface Fa6/0
 ip address 3.0.0.1 255.255.255.0
 description "To Dublin"
 speed auto
 duplex auto
!

 neighbor 3.0.0.2 remote-as 50030
 neighbor 3.0.0.2 send-community

10010 节点

interface Fa6/0
 ip address 4.0.0.1 255.255.255.0
 description "To Amsterdam"
 speed auto
 duplex auto
!

 neighbor 4.0.0.2 remote-as 50040
 neighbor 4.0.0.2 send-community
!

10060节点

interface Fa6/0
 ip address 5.0.0.1 255.255.255.0
 description "To Strasbourg"
 speed auto
 duplex auto
!
 neighbor 5.0.0.2 remote-as 50050
 neighbor 5.0.0.2 send-community
!