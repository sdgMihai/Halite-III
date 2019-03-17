cmake_minimum_required(VERSION 2.8.2)

project(deps-download NONE)

set(EXTERNAL_INSTALL_LOCATION /home/student/facultate/an2/sem2/pa/Proiect/INTegralistii/environment/external)

include(ExternalProject)
ExternalProject_Add(catch-download
  URL               https://github.com/catchorg/Catch2/archive/v2.2.3.zip
  URL_HASH          SHA256=659679a47edb72f0bdc535770368d5fac4abd115f02376e3c5990fdd6fac4629
  PREFIX            /home/student/facultate/an2/sem2/pa/Proiect/INTegralistii/environment/external
  SOURCE_DIR        /home/student/facultate/an2/sem2/pa/Proiect/INTegralistii/environment/external/catch
  CONFIGURE_COMMAND ""
  BUILD_COMMAND     ""
  INSTALL_COMMAND   ""
  TEST_COMMAND      ""
)

ExternalProject_Add(nlohmann-download
    URL https://github.com/nlohmann/json/releases/download/v3.1.2/include.zip
    URL_HASH SHA256=495362ee1b9d03d9526ba9ccf1b4a9c37691abe3a642ddbced13e5778c16660c
    PREFIX /home/student/facultate/an2/sem2/pa/Proiect/INTegralistii/environment/external
    SOURCE_DIR /home/student/facultate/an2/sem2/pa/Proiect/INTegralistii/environment/external/nlohmann
    CMAKE_COMMAND ""
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    UPDATE_COMMAND ""
    INSTALL_COMMAND ""
)

ExternalProject_Add(tclap-download
    URL https://netix.dl.sourceforge.net/project/tclap/tclap-1.2.2.tar.gz
    URL_HASH SHA256=f5013be7fcaafc69ba0ce2d1710f693f61e9c336b6292ae4f57554f59fde5837
    PREFIX /home/student/facultate/an2/sem2/pa/Proiect/INTegralistii/environment/external
    SOURCE_DIR /home/student/facultate/an2/sem2/pa/Proiect/INTegralistii/environment/external/tclap
    CMAKE_COMMAND ""
    CONFIGURE_COMMAND ""
    BUILD_COMMAND ""
    UPDATE_COMMAND ""
    INSTALL_COMMAND ""
)

ExternalProject_Add(zstd-download
    URL https://github.com/facebook/zstd/archive/v1.3.4.zip
    URL_HASH SHA256=76f3e6cadd9e1714cad7b550e53b82d4a5a5f9e08fbc0b3eb54abd0d0ebeb42d
    PREFIX /home/student/facultate/an2/sem2/pa/Proiect/INTegralistii/environment/external
    SOURCE_DIR        /home/student/facultate/an2/sem2/pa/Proiect/INTegralistii/environment/external/zstd
    CONFIGURE_COMMAND "/usr/bin/cmake" /home/student/facultate/an2/sem2/pa/Proiect/INTegralistii/environment/external/zstd/build/cmake/
    BUILD_COMMAND ""
    INSTALL_COMMAND ""
    UPDATE_COMMAND ""
)
