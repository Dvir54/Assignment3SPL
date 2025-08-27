# Emergency Service Platform (Course Project)

This project was developed as part of the Advanced Programming course assignment.  
It implements an **Emergency Service messaging platform** based on the **STOMP protocol**, consisting of:

- **Java server** supporting both **Thread-Per-Client (TPC)** and **Reactor** concurrency models.  
- **C++ client** with multithreading (keyboard & socket threads using `std::thread` and `mutex`).  
- Full support for **STOMP frames** (CONNECT, SUBSCRIBE, SEND, UNSUBSCRIBE, DISCONNECT, etc.).  
- Users can **subscribe to emergency channels** (fire, police, medical, disasters), **report events from JSON files**, receive updates in real time, and **summarize reports**.  
- Implemented **Connections, Protocol, Encoder/Decoder** layers to support communication.  
- Tools & Practices: **Git, Docker, Maven, Makefile**, with clear separation of server–client logic.  

The system produces structured outputs of emergency reports, enabling real-time collaboration across multiple channels.
