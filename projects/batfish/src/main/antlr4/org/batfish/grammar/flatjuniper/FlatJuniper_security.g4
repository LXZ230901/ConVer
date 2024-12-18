parser grammar FlatJuniper_security;

import FlatJuniper_common;

options {
   tokenVocab = FlatJuniperLexer;
}

address_specifier
:
   ANY
   | ANY_IPV4
   | ANY_IPV6
   | name = variable
;

dh_group
:
   GROUP1
   | GROUP14
   | GROUP2
   | GROUP5
;

encryption_algorithm
:
   THREEDES_CBC
   | AES_128_CBC
   | AES_192_CBC
   | AES_256_CBC
   | DES_CBC
;

hib_protocol
:
   ALL
   | BFD
   | BGP
   | DVMRP
   | IGMP
   | LDP
   | MSDP
   | NHRP
   | OSPF
   | OSPF3
   | PGM
   | PIM
   | RIP
   | RIPNG
   | ROUTER_DISCOVERY
   | RSVP
   | SAP
   | VRRP
;

hib_system_service
:
   ALL
   | ANY_SERVICE
   | DHCP
   | DNS
   | FINGER
   | FTP
   | HTTP
   | HTTPS
   | IDENT_RESET
   | IKE
   | LSPING
   | NETCONF
   | NTP
   | PING
   | R2CP
   | REVERSE_SSH
   | REVERSE_TELNET
   | RLOGIN
   | RPM
   | RSH
   | SIP
   | SNMP
   | SNMP_TRAP
   | SSH
   | TELNET
   | TFTP
   | TRACEROUTE
   | XNM_CLEAR_TEXT
   | XNM_SSL
;

ike_authentication_algorithm
:
   MD5
   | SHA_256
   | SHA_384
   | SHA1
;

ike_authentication_method
:
   DSA_SIGNATURES
   | PRE_SHARED_KEYS
   | RSA_SIGNATURES
;

ipsec_authentication_algorithm
:
   HMAC_MD5_96
   | HMAC_SHA1_96
;

ipsec_protocol
:
   AH
   | ESP
;

junos_application
:
   JUNOS_AOL
   | JUNOS_BGP
   | JUNOS_BIFF
   | JUNOS_BOOTPC
   | JUNOS_BOOTPS
   | JUNOS_CHARGEN
   | JUNOS_CIFS
   | JUNOS_CVSPSERVER
   | JUNOS_DHCP_CLIENT
   | JUNOS_DHCP_RELAY
   | JUNOS_DHCP_SERVER
   | JUNOS_DISCARD
   | JUNOS_DNS_TCP
   | JUNOS_DNS_UDP
   | JUNOS_ECHO
   | JUNOS_FINGER
   | JUNOS_FTP
   | JUNOS_FTP_DATA
   | JUNOS_GNUTELLA
   | JUNOS_GOPHER
   | JUNOS_GPRS_GTP_C
   | JUNOS_GPRS_GTP_U
   | JUNOS_GPRS_GTP_V0
   | JUNOS_GPRS_SCTP
   | JUNOS_GRE
   | JUNOS_GTP
   | JUNOS_H323
   | JUNOS_HTTP
   | JUNOS_HTTP_EXT
   | JUNOS_HTTPS
   | JUNOS_ICMP_ALL
   | JUNOS_ICMP_PING
   | JUNOS_ICMP6_ALL
   | JUNOS_ICMP6_DST_UNREACH_ADDR
   | JUNOS_ICMP6_DST_UNREACH_ADMIN
   | JUNOS_ICMP6_DST_UNREACH_BEYOND
   | JUNOS_ICMP6_DST_UNREACH_PORT
   | JUNOS_ICMP6_DST_UNREACH_ROUTE
   | JUNOS_ICMP6_ECHO_REPLY
   | JUNOS_ICMP6_ECHO_REQUEST
   | JUNOS_ICMP6_PACKET_TOO_BIG
   | JUNOS_ICMP6_PARAM_PROB_HEADER
   | JUNOS_ICMP6_PARAM_PROB_NEXTHDR
   | JUNOS_ICMP6_PARAM_PROB_OPTION
   | JUNOS_ICMP6_TIME_EXCEED_REASSEMBLY
   | JUNOS_ICMP6_TIME_EXCEED_TRANSIT
   | JUNOS_IDENT
   | JUNOS_IKE
   | JUNOS_IKE_NAT
   | JUNOS_IMAP
   | JUNOS_IMAPS
   | JUNOS_INTERNET_LOCATOR_SERVICE
   | JUNOS_IRC
   | JUNOS_L2TP
   | JUNOS_LDAP
   | JUNOS_LDP_TCP
   | JUNOS_LDP_UDP
   | JUNOS_LPR
   | JUNOS_MAIL
   | JUNOS_MGCP
   | JUNOS_MGCP_CA
   | JUNOS_MGCP_UA
   | JUNOS_MS_RPC
   | JUNOS_MS_RPC_ANY
   | JUNOS_MS_RPC_EPM
   | JUNOS_MS_RPC_IIS_COM
   | JUNOS_MS_RPC_IIS_COM_1
   | JUNOS_MS_RPC_IIS_COM_ADMINBASE
   | JUNOS_MS_RPC_MSEXCHANGE
   | JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_NSP
   | JUNOS_MS_RPC_MSEXCHANGE_DIRECTORY_RFR
   | JUNOS_MS_RPC_MSEXCHANGE_INFO_STORE
   | JUNOS_MS_RPC_TCP
   | JUNOS_MS_RPC_UDP
   | JUNOS_MS_RPC_UUID_ANY_TCP
   | JUNOS_MS_RPC_UUID_ANY_UDP
   | JUNOS_MS_RPC_WMIC
   | JUNOS_MS_RPC_WMIC_ADMIN
   | JUNOS_MS_RPC_WMIC_ADMIN2
   | JUNOS_MS_RPC_WMIC_MGMT
   | JUNOS_MS_RPC_WMIC_WEBM_CALLRESULT
   | JUNOS_MS_RPC_WMIC_WEBM_CLASSOBJECT
   | JUNOS_MS_RPC_WMIC_WEBM_LEVEL1LOGIN
   | JUNOS_MS_RPC_WMIC_WEBM_LOGIN_CLIENTID
   | JUNOS_MS_RPC_WMIC_WEBM_LOGIN_HELPER
   | JUNOS_MS_RPC_WMIC_WEBM_OBJECTSINK
   | JUNOS_MS_RPC_WMIC_WEBM_REFRESHING_SERVICES
   | JUNOS_MS_RPC_WMIC_WEBM_REMOTE_REFRESHER
   | JUNOS_MS_RPC_WMIC_WEBM_SERVICES
   | JUNOS_MS_RPC_WMIC_WEBM_SHUTDOWN
   | JUNOS_MS_SQL
   | JUNOS_MSN
   | JUNOS_NBDS
   | JUNOS_NBNAME
   | JUNOS_NETBIOS_SESSION
   | JUNOS_NFS
   | JUNOS_NFSD_TCP
   | JUNOS_NFSD_UDP
   | JUNOS_NNTP
   | JUNOS_NS_GLOBAL
   | JUNOS_NS_GLOBAL_PRO
   | JUNOS_NSM
   | JUNOS_NTALK
   | JUNOS_NTP
   | JUNOS_OSPF
   | JUNOS_PC_ANYWHERE
   | JUNOS_PERSISTENT_NAT
   | JUNOS_PING
   | JUNOS_PINGV6
   | JUNOS_POP3
   | JUNOS_PPTP
   | JUNOS_PRINTER
   | JUNOS_R2CP
   | JUNOS_RADACCT
   | JUNOS_RADIUS
   | JUNOS_REALAUDIO
   | JUNOS_RIP
   | JUNOS_ROUTING_INBOUND
   | JUNOS_RSH
   | JUNOS_RTSP
   | JUNOS_SCCP
   | JUNOS_SCTP_ANY
   | JUNOS_SIP
   | JUNOS_SMB
   | JUNOS_SMB_SESSION
   | JUNOS_SMTP
   | JUNOS_SMTPS
   | JUNOS_SNMP_AGENTX
   | JUNOS_SNPP
   | JUNOS_SQL_MONITOR
   | JUNOS_SQLNET_V1
   | JUNOS_SQLNET_V2
   | JUNOS_SSH
   | JUNOS_STUN
   | JUNOS_SUN_RPC
   | JUNOS_SUN_RPC_ANY
   | JUNOS_SUN_RPC_ANY_TCP
   | JUNOS_SUN_RPC_ANY_UDP
   | JUNOS_SUN_RPC_MOUNTD
   | JUNOS_SUN_RPC_MOUNTD_TCP
   | JUNOS_SUN_RPC_MOUNTD_UDP
   | JUNOS_SUN_RPC_NFS
   | JUNOS_SUN_RPC_NFS_ACCESS
   | JUNOS_SUN_RPC_NFS_TCP
   | JUNOS_SUN_RPC_NFS_UDP
   | JUNOS_SUN_RPC_NLOCKMGR
   | JUNOS_SUN_RPC_NLOCKMGR_TCP
   | JUNOS_SUN_RPC_NLOCKMGR_UDP
   | JUNOS_SUN_RPC_PORTMAP
   | JUNOS_SUN_RPC_PORTMAP_TCP
   | JUNOS_SUN_RPC_PORTMAP_UDP
   | JUNOS_SUN_RPC_RQUOTAD
   | JUNOS_SUN_RPC_RQUOTAD_TCP
   | JUNOS_SUN_RPC_RQUOTAD_UDP
   | JUNOS_SUN_RPC_RUSERD
   | JUNOS_SUN_RPC_RUSERD_TCP
   | JUNOS_SUN_RPC_RUSERD_UDP
   | JUNOS_SUN_RPC_SADMIND
   | JUNOS_SUN_RPC_SADMIND_TCP
   | JUNOS_SUN_RPC_SADMIND_UDP
   | JUNOS_SUN_RPC_SPRAYD
   | JUNOS_SUN_RPC_SPRAYD_TCP
   | JUNOS_SUN_RPC_SPRAYD_UDP
   | JUNOS_SUN_RPC_STATUS
   | JUNOS_SUN_RPC_STATUS_TCP
   | JUNOS_SUN_RPC_STATUS_UDP
   | JUNOS_SUN_RPC_TCP
   | JUNOS_SUN_RPC_UDP
   | JUNOS_SUN_RPC_WALLD
   | JUNOS_SUN_RPC_WALLD_TCP
   | JUNOS_SUN_RPC_WALLD_UDP
   | JUNOS_SUN_RPC_YPBIND
   | JUNOS_SUN_RPC_YPBIND_TCP
   | JUNOS_SUN_RPC_YPBIND_UDP
   | JUNOS_SUN_RPC_YPSERV
   | JUNOS_SUN_RPC_YPSERV_TCP
   | JUNOS_SUN_RPC_YPSERV_UDP
   | JUNOS_SYSLOG
   | JUNOS_TACACS
   | JUNOS_TACACS_DS
   | JUNOS_TALK
   | JUNOS_TCP_ANY
   | JUNOS_TELNET
   | JUNOS_TFTP
   | JUNOS_UDP_ANY
   | JUNOS_UUCP
   | JUNOS_VDO_LIVE
   | JUNOS_VNC
   | JUNOS_WAIS
   | JUNOS_WHO
   | JUNOS_WHOIS
   | JUNOS_WINFRAME
   | JUNOS_WXCONTROL
   | JUNOS_X_WINDOWS
   | JUNOS_XNM_CLEAR_TEXT
   | JUNOS_XNM_SSL
   | JUNOS_YMSG
;

nat_rule_set
:
   RULE_SET name = variable
   (
      rs_from
      | rs_rule
      | rs_to
   )
;

proposal_set_type
:
   BASIC
   | COMPATIBLE
   | STANDARD
;

rs_from
:
   FROM rsf_common
;

rs_rule
:
   RULE name = variable
   (
      rsr_description
      | rsr_match
      | rsr_then
   )
;

rs_to
:
   TO rsf_common
;

rsf_common
:
   rsf_zone
;

rsf_zone
:
   ZONE name = variable
;

rsr_description
:
   DESCRIPTION null_filler
;

rsr_match
:
   MATCH
   (
      rsrm_destination_address
      | rsrm_destination_port
      | rsrm_source_address
   )
;

rsr_then
:
   THEN
   (
      rsrt_source_nat
      | rsrt_static_nat
   )
;

rsrm_destination_address
:
   DESTINATION_ADDRESS IP_PREFIX
;

rsrm_destination_port
:
   DESTINATION_PORT from = DEC
   (
      TO to = DEC
   )?
;

rsrm_source_address
:
   SOURCE_ADDRESS IP_PREFIX
;

rsrt_source_nat
:
   SOURCE_NAT
   (
      rsrts_interface
      | rsrts_off
      | rsrts_pool
   )
;

rsrt_static_nat
:
   STATIC_NAT
   (
      rsrtst_prefix
   )
;

rsrts_interface
:
   INTERFACE
;

rsrts_off
:
   OFF
;

rsrts_pool
:
   POOL
   (
      rsrtsp_common
      | rsrtsp_named
   )
;

rsrtsp_common
:
   apply
   | rsrtsp_persistent_nat
;

rsrtsp_named
:
   name = variable rsrtsp_common
;

rsrtsp_persistent_nat
:
   PERSISTENT_NAT
   (
      apply
      | rsrtspp_inactivity_timeout
      | rsrtspp_max_session_number
      | rsrtspp_permit
   )
;

rsrtspp_inactivity_timeout
:
   INACTIVITY_TIMEOUT seconds = DEC
;

rsrtspp_max_session_number
:
   MAX_SESSION_NUMBER max = DEC
;

rsrtspp_permit
:
   PERMIT
   (
      ANY_REMOTE_HOST
      | TARGET_HOST
      | TARGET_HOST_PORT
   )
;

rsrtst_prefix
:
   PREFIX
   (
      rsrtstp_mapped_port
      | rsrtstp_prefix
   )
;

rsrtstp_mapped_port
:
   MAPPED_PORT low = DEC
   (
      TO high = DEC
   )?
;

rsrtstp_prefix
:
   IP_PREFIX
;

s_security
:
   SECURITY
   (
      se_address_book
      | se_authentication_key_chain
      | se_certificates
      | se_ike
      | se_ipsec
      | se_nat
      | se_null
      | se_policies
      | se_zones
   )
;

se_address_book
:
   ADDRESS_BOOK GLOBAL
   (
       apply
       | sead_address
       | sead_address_set
   )
;

se_authentication_key_chain
:
   AUTHENTICATION_KEY_CHAINS KEY_CHAIN name = string
   (
      sea_key
      | sea_description
      | sea_tolerance
   )
;

se_certificates
:
   CERTIFICATES
   (
      sec_local
   )
;

se_ike
:
   IKE
   (
      seik_gateway
      | seik_policy
      | seik_proposal
   )
;

se_ipsec
:
   IPSEC
   (
      seip_policy
      | seip_proposal
      | seip_vpn
   )
;

se_nat
:
   NAT
   (
      sen_proxy_arp
      | sen_source
      | sen_static
   )
;

se_null
:
   (
      ALG
      | APPLICATION_TRACKING
      | FLOW
      | LOG
      | SCREEN
   ) null_filler
;

se_policies
:
   POLICIES
   (
      sep_default_policy
      | sep_from_zone
      | sep_global
   )
;

se_zones
:
   ZONES
   (
      apply
      | sez_security_zone
   )
;

sea_description
:
   description
;

sea_key
:
   KEY name = string
   (
      seak_algorithm
      | seak_options
      | seak_secret
      | seak_start_time
   )
;

sea_tolerance
:
   TOLERANCE DEC
;

sead_address
:
   ADDRESS name = variable
   (
      apply
      | address = IP_ADDRESS
      | prefix = IP_PREFIX
      | WILDCARD_ADDRESS wildcard_address
   )
;

sead_address_set
:
   ADDRESS_SET name = variable
   (
      apply
      | seada_address
      | seada_address_set
   )
;

seada_address
:
   ADDRESS name = variable
;

seada_address_set
:
   ADDRESS_SET name = variable
;

sec_local
:
   LOCAL name = variable cert = DOUBLE_QUOTED_STRING
;

seak_algorithm
:
   ALGORITHM
   (
      HMAC_SHA1
      | MD5
   )
;

seak_options
:
   OPTIONS
   (
      BASIC
      | ISIS_ENHANCED
   )
;

seak_secret
:
   SECRET key = string
;

seak_start_time
:
   START_TIME time = variable_permissive
;

seik_gateway
:
   GATEWAY name = variable
   (
      seikg_address
      | seikg_dead_peer_detection
      | seikg_dynamic
      | seikg_external_interface
      | seikg_ike_policy
      | seikg_local_address
      | seikg_local_identity
      | seikg_no_nat_traversal
      | seikg_version
      | seikg_xauth
   )
;

seik_policy
:
   POLICY name = variable
   (
      seikp_description
      | seikp_mode
      | seikp_pre_shared_key
      | seikp_proposal_set
      | seikp_proposals
   )
;

seik_proposal
:
   PROPOSAL name = variable
   (
      seikpr_authentication_algorithm
      | seikpr_authentication_method
      | seikpr_description
      | seikpr_dh_group
      | seikpr_encryption_algorithm
      | seikpr_lifetime_seconds
   )
;

seikg_address
:
   ADDRESS IP_ADDRESS
;

seikg_dead_peer_detection
:
   DEAD_PEER_DETECTION ALWAYS_SEND?
;

seikg_dynamic
:
   DYNAMIC
   (
      apply
      | seikgd_connections_limit
      | seikgd_hostname
      | seikgd_ike_user_type
   )
;

seikg_external_interface
:
   EXTERNAL_INTERFACE interface_id
;

seikg_ike_policy
:
   IKE_POLICY name = variable
;

seikg_local_address
:
   LOCAL_ADDRESS IP_ADDRESS
;

seikg_local_identity
:
   LOCAL_IDENTITY
   (
      seikgl_inet
   )
;

seikg_no_nat_traversal
:
   NO_NAT_TRAVERSAL
;

seikg_version
:
   VERSION V1_ONLY
;

seikg_xauth
:
   XAUTH ACCESS_PROFILE name = variable
;

seikgd_connections_limit
:
   CONNECTIONS_LIMIT limit = DEC
;

seikgd_hostname
:
   HOSTNAME name = variable
;

seikgd_ike_user_type
:
   IKE_USER_TYPE
   (
      GROUP_IKE_ID
      | SHARED_IKE_ID
   )
;

seikgl_inet
:
   INET name = variable
;

seikp_description
:
   DESCRIPTION null_filler
;

seikp_mode
:
   MODE
   (
      AGGRESSIVE
      | MAIN
   )
;

seikp_pre_shared_key
:
   PRE_SHARED_KEY ASCII_TEXT key = DOUBLE_QUOTED_STRING
;

seikp_proposal_set
:
   PROPOSAL_SET proposal_set_type
;

seikp_proposals
:
   PROPOSALS name = variable
;

seikpr_authentication_algorithm
:
   AUTHENTICATION_ALGORITHM ike_authentication_algorithm
;

seikpr_authentication_method
:
   AUTHENTICATION_METHOD ike_authentication_method
;

seikpr_description
:
   DESCRIPTION null_filler
;

seikpr_dh_group
:
   DH_GROUP dh_group
;

seikpr_encryption_algorithm
:
   ENCRYPTION_ALGORITHM encryption_algorithm
;

seikpr_lifetime_seconds
:
   LIFETIME_SECONDS seconds = DEC
;

seip_policy
:
   POLICY name = variable
   (
      seipp_perfect_forward_secrecy
      | seipp_proposal_set
      | seipp_proposals
   )
;

seip_proposal
:
   PROPOSAL name = variable
   (
      apply
      | seippr_authentication_algorithm
      | seippr_description
      | seippr_encryption_algorithm
      | seippr_lifetime_kilobytes
      | seippr_lifetime_seconds
      | seippr_protocol
   )
;

seip_vpn
:
   VPN name = variable
   (
      seipv_bind_interface
      | seipv_df_bit
      | seipv_establish_tunnels
      | seipv_ike
      | seipv_vpn_monitor
   )
;

seipp_perfect_forward_secrecy
:
   PERFECT_FORWARD_SECRECY KEYS dh_group
;

seipp_proposal_set
:
   PROPOSAL_SET proposal_set_type
;

seipp_proposals
:
   PROPOSALS name = variable
;

seippr_authentication_algorithm
:
   AUTHENTICATION_ALGORITHM ipsec_authentication_algorithm
;

seippr_description
:
   DESCRIPTION null_filler
;

seippr_encryption_algorithm
:
   ENCRYPTION_ALGORITHM encryption_algorithm
;

seippr_lifetime_kilobytes
:
   LIFETIME_KILOBYTES kilobytes = DEC
;

seippr_lifetime_seconds
:
   LIFETIME_SECONDS seconds = DEC
;

seippr_protocol
:
   PROTOCOL ipsec_protocol
;

seipv_bind_interface
:
   BIND_INTERFACE interface_id
;

seipv_df_bit
:
   DF_BIT CLEAR
;

seipv_establish_tunnels
:
   ESTABLISH_TUNNELS IMMEDIATELY
;

seipv_ike
:
   IKE
   (
      seipvi_gateway
      | seipvi_ipsec_policy
      | seipvi_null
      | seipvi_proxy_identity
   )
;

seipv_vpn_monitor
:
   VPN_MONITOR
   (
      apply
      | seipvv_destination_ip
      | seipvv_source_interface
   )
;

seipvi_gateway
:
   GATEWAY name = variable
;

seipvi_ipsec_policy
:
   IPSEC_POLICY name = variable
;

seipvi_null
:
   (
      NO_ANTI_REPLAY
   ) null_filler
;

seipvi_proxy_identity
:
   PROXY_IDENTITY
   (
      seipvip_local
      | seipvip_remote
      | seipvip_service
   )
;

seipvip_local
:
   LOCAL IP_PREFIX
;

seipvip_remote
:
   REMOTE IP_PREFIX
;

seipvip_service
:
   SERVICE
   (
      ANY
      | name = variable
   )
;

seipvv_destination_ip
:
   DESTINATION_IP IP_ADDRESS
;

seipvv_source_interface
:
   SOURCE_INTERFACE interface_id
;

sen_proxy_arp
:
   PROXY_ARP
   (
      apply
      | senp_interface
   )
;

sen_source
:
   SOURCE
   (
      nat_rule_set
      | sens_interface
      | sens_pool
      | sens_port_randomization
   )
;

sen_static
:
   STATIC nat_rule_set
;

senp_interface
:
   INTERFACE interface_id
   (
      apply
      | senpi_address
   )
;

senpi_address
:
   ADDRESS
   (
      from = IP_ADDRESS
      | from = IP_PREFIX
   )
   (
      TO
      (
         to = IP_ADDRESS
         | to = IP_PREFIX
      )
   )?
;

sens_interface
:
   INTERFACE
   (
      sensi_port_overloading
      | sensi_port_overloading_factor
   )
;

sens_pool
:
   POOL name = variable
   (
      sensp_address
      | sensp_description
   )
;

sens_port_randomization
:
   PORT_RANDOMIZATION DISABLE
;

sensi_port_overloading
:
   PORT_OVERLOADING OFF
;

sensi_port_overloading_factor
:
   PORT_OVERLOADING_FACTOR factor = DEC
;

sensp_address
:
   ADDRESS IP_PREFIX
   (
      TO IP_PREFIX
   )?
;

sensp_description
:
   DESCRIPTION null_filler
;

sep_default_policy
:
   DEFAULT_POLICY
   (
      apply
      | DENY_ALL
      | PERMIT_ALL
   )
;

sep_from_zone
:
   FROM_ZONE from = zone TO_ZONE to = zone
   (
      apply
      | sepctx_policy
   )
;

sep_global
:
   GLOBAL
   (
      apply
      | sepctx_policy
   )
;

sepctx_policy
:
   POLICY name = variable
   (
      apply
      | sepctxp_description
      | sepctxp_match
      | sepctxp_then
   )
;

sepctxp_description
:
   DESCRIPTION null_filler
;

sepctxp_match
:
   MATCH
   (
      sepctxpm_application
      | sepctxpm_destination_address
      | sepctxpm_destination_address_excluded
      | sepctxpm_source_address
      | sepctxpm_source_identity
   )
;

sepctxp_then
:
   THEN
   (
      sepctxpt_deny
      | sepctxpt_log
      | sepctxpt_permit
   )
;

sepctxpm_application
:
   APPLICATION
   (
      ANY
      | junos_application
      | name = variable
   )
;

sepctxpm_destination_address
:
   DESTINATION_ADDRESS address_specifier
;

sepctxpm_destination_address_excluded
:
   DESTINATION_ADDRESS_EXCLUDED
;

sepctxpm_source_address
:
   SOURCE_ADDRESS address_specifier
;

sepctxpm_source_identity
:
   SOURCE_IDENTITY
   (
      ANY
      | name = variable
   )
;

sepctxpt_deny
:
   DENY
   | REJECT
;

sepctxpt_log
:
   LOG null_filler
;

sepctxpt_permit
:
   PERMIT
   (
      apply
      | sepctxptp_tunnel
   )
;

sepctxptp_tunnel
:
   TUNNEL
   (
      apply
      | sepctxptpt_ipsec_vpn
   )
;

sepctxptpt_ipsec_vpn
:
   IPSEC_VPN name = variable
;

sez_security_zone
:
   SECURITY_ZONE zone
   (
      apply
      | sezs_address_book
      | sezs_application_tracking
      | sezs_host_inbound_traffic
      | sezs_interfaces
      | sezs_screen
      | sezs_tcp_rst
   )
;

sezs_address_book
:
   ADDRESS_BOOK
   (
      apply
      | sezsa_address
      | sezsa_address_set
   )
;

sezs_application_tracking
:
   APPLICATION_TRACKING
;

sezs_host_inbound_traffic
:
   HOST_INBOUND_TRAFFIC
   (
      apply
      | sezsh_protocols
      | sezsh_system_services
   )
;

sezs_interfaces
:
   INTERFACES interface_id
   (
      apply
      | sezs_host_inbound_traffic
   )
;

sezs_screen
:
   SCREEN
   (
      UNTRUST_SCREEN
      | name = variable
   )
;

sezs_tcp_rst
:
   TCP_RST
;

sezsa_address
:
   ADDRESS name = variable
   (
      apply
      | address = IP_ADDRESS
      | prefix = IP_PREFIX
      | WILDCARD_ADDRESS wildcard_address
   )
;

sezsa_address_set
:
   ADDRESS_SET name = variable
   (
      apply
      | sezsaad_address
      | sezsaad_address_set
   )
;

sezsaad_address
:
   ADDRESS name = variable
;

sezsaad_address_set
:
   ADDRESS_SET name = variable
;

sezsh_protocols
:
   PROTOCOLS hib_protocol
;

sezsh_system_services
:
   SYSTEM_SERVICES hib_system_service
;

zone
:
   JUNOS_HOST
   | TRUST
   | UNTRUST
   | name = variable
;
