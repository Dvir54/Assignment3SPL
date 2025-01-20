#pragma once
#include "../include/ConnectionHandler.h"
#include "../include/event.h"
#include <string>
#include <iostream>
#include <vector>
#include <map>
#include <sstream>

using namespace std;

// TODO: implement the STOMP protocol
class StompProtocol
{
private:
    int receiptId;
    int subscriptionId;
    bool userLoggedIn;
    bool shouldLogOut;
    map<string, int> eventsMap;
    map<string, map<string, vector<Event>>> userChannelReports;

public:
    StompProtocol();
    vector<string> parseArgs(const string &input);
    string epoch_to_date(time_t epoch);
    void process(string inputMsg);
    string handleInput(string input);
    vector<string> handleReport(string path);
    int getReceiptId();
    int getSubscriptionId();
    bool getUserLoggedIn();
};
