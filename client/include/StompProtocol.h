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
    int logoutReceiptId;
    bool userLoggedIn;
    bool shouldLogOut;
    bool gotError;
    string userNameToUse;
    map<string, int> eventsMap;
    map<string, map<string, vector<Event>>> userChannelReports;
    map<int, string> joinReciepts;
    map<int, string> exitReciepts;

public:
    StompProtocol();
    vector<string> parseArgs(const string &input);
    string epoch_to_date(time_t epoch);
    void process(string inputMsg);
    string handleInput(string input);
    vector<string> handleReport(string path);
    void resetProtocol();
    int getReceiptId();
    int getSubscriptionId();
    bool getUserLoggedIn();
    void setgotError(bool update);
    bool getgotError();
    int getLogoutReceiptId();
    void setLogoutReceiptId(int update);
    bool getShouldLogOut();
};
