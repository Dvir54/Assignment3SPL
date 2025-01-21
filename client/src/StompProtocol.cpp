#include "../include/StompProtocol.h"
#include "../include/event.h"
#include <ctime>
#include <string>
#include <iomanip>
#include <fstream>
#include "StompProtocol.h"

StompProtocol::StompProtocol()
    : receiptId(0), subscriptionId(0), logoutReceiptId(0) , userLoggedIn(false), shouldLogOut(false), gotError(false), userNameToUse(""), eventsMap(), userChannelReports(), joinReciepts(), exitReciepts()
{
}

vector<string> StompProtocol::parseArgs(const std::string &input)
{
    std::vector<std::string> args;
    std::stringstream ss(input);
    std::string word;

    while (ss >> word)
    {
        args.push_back(word);
    }

    return args;
}

string StompProtocol::epoch_to_date(time_t epoch)
{
    std::stringstream ss;
    ss << std::put_time(std::localtime(&epoch), "%d/%m/%y %H:%M");
    return ss.str();
}

void StompProtocol::process(std::string inputMsg)
{
    vector<string> linesMsg;
    string lineToAdd = "";
    size_t i = 0;

    while (i < inputMsg.length())
    {
        if (inputMsg[i] != '\n')
        {
            lineToAdd.push_back(inputMsg[i]);
        }

        if (inputMsg[i] == '\n')
        {
            linesMsg.push_back(lineToAdd);
            lineToAdd = "";
        }
        i = i + 1;
    }

    if (!linesMsg.empty())
    {

        if (linesMsg[0] == "MESSAGE")
        {   
            string newChannelName = linesMsg[1].substr(13);
            string newUserName = linesMsg[4].substr(6);
            string newcity = linesMsg[5].substr(6);
            string newEventName = linesMsg[6].substr(12);
            string newDateTime = linesMsg[7].substr(11);

            map<string, string> newGeneralInformation;
            newGeneralInformation["active"] = linesMsg[9].substr(8);
            newGeneralInformation["forces_arrival_at_scene"] = linesMsg[10].substr(25);

            string newDescription = linesMsg[12];

            Event newEvent(newChannelName, newcity, newEventName, stoi(newDateTime), newDescription, newGeneralInformation);

            if (userChannelReports.find(newUserName) == userChannelReports.end())
            {
                userChannelReports[newUserName] = map<string, vector<Event>>();
            }

            if (userChannelReports[newUserName].find(newChannelName) == userChannelReports[newUserName].end())
            {
                userChannelReports[newUserName][newChannelName] = vector<Event>();
            }

            userChannelReports[newUserName][newChannelName].push_back(newEvent);
        }

        if (linesMsg[0] == "ERROR")
        {
            for (string line : linesMsg)
            {   
                cout << line << endl;
            }

            setgotError(true);
        }

        if (linesMsg[0] == "RECEIPT")
        {   
            cout << linesMsg[1] << endl;
            int recieptNum = stoi(linesMsg[1].substr(11));

            if(recieptNum == logoutReceiptId)
            {   
                shouldLogOut = true;

            }else if (joinReciepts.find(recieptNum) != joinReciepts.end())
            {   
                cout << "Joined channel " << joinReciepts[recieptNum] << endl;
            }
            else if (exitReciepts.find(recieptNum) != exitReciepts.end())
            {   
                cout << "Exited channel " << exitReciepts[recieptNum] << endl;
            }
        }

        if (linesMsg[0] == "CONNECTED")
        {   
            userLoggedIn = true;
            cout << "Login successful" << endl;
        }
    }
}

string StompProtocol::handleInput(std::string input)
{
    vector<string> vecIn = this->parseArgs(input);

    if (vecIn[0] == "login")
    {   
        userNameToUse = vecIn[2];
        string username = vecIn[2];
        string password = vecIn[3];
        receiptId = receiptId + 1;

        return "CONNECT\n"
               "accept-version:1.2\n"
               "host:stomp.cs.bgu.ac.il\n"
               "login:" +
               username + "\n"
                          "passcode:" +
               password + "\n"
                          "receipt:" +
               to_string(receiptId) + "\n\n\0";
    }
    else if (vecIn[0] == "join")
    {
        string channelName = vecIn[1];
        receiptId = receiptId + 1;
        subscriptionId = subscriptionId + 1;

        eventsMap[channelName] = subscriptionId;
        joinReciepts[receiptId] = channelName;

        return "SUBSCRIBE\n"
               "destination:" +
               channelName + "\n"
                             "id:" +
               to_string(subscriptionId) + "\n"
                                           "receipt:" +
               to_string(receiptId) + "\n\n\0";
    }

    else if (vecIn[0] == "exit")
    {
        string channelName = vecIn[1];
        int subId = eventsMap[channelName];

        receiptId = receiptId + 1;
        exitReciepts[receiptId] = channelName;

        return "UNSUBSCRIBE\n"
               "id:" +
               to_string(subId) + "\n"
                                  "receipt:" +
               to_string(receiptId) + "\n\n\0";
    }

    else if (vecIn[0] == "summary")
    {   
        string SummaryMsg = "Channel " + vecIn[1] + "\n" + "Stats:\n";

        vector<Event> userChannelEventVec = userChannelReports[vecIn[2]][vecIn[1]];
        int TotalEvents = userChannelEventVec.size();
        int TotalActiveEvents = 0;
        int numOfArrivedForces = 0;
        int reportNum = 0;

        SummaryMsg = SummaryMsg + "Total: " + to_string(TotalEvents) + "\n";

        for (Event event : userChannelEventVec)
        {
            map<string, string> general_information = event.get_general_information();
            if (general_information["active"] == "true")
            {
                TotalActiveEvents = TotalActiveEvents + 1;
            }
            if (general_information["forces_arrival_at_scene"] == "true")
            {
                numOfArrivedForces = numOfArrivedForces + 1;
            }
        }

        SummaryMsg = SummaryMsg + "active: " + to_string(TotalActiveEvents) + "\n" + "forces: " + to_string(numOfArrivedForces) + "\n\n" + "Event Report:\n\n";

        std::sort(userChannelEventVec.begin(), userChannelEventVec.end(), [](const Event &a, const Event &b)
             { return (a.get_date_time() < b.get_date_time()) || (a.get_date_time() == b.get_date_time() && a.get_name() < b.get_name()); });

        for (Event event : userChannelEventVec)
        {
            reportNum = reportNum + 1;
            string readableDate = epoch_to_date(event.get_date_time());

            SummaryMsg = SummaryMsg + "Report_" + to_string(reportNum) + "\n" + "city: " + event.get_city() + "\n" + "date time: " + readableDate + "\n" + "event name: " + event.get_name() + "\n" + "description: " + event.get_description().substr(0, 27);

            if (event.get_description().length() > 27)
            {
                SummaryMsg = SummaryMsg + "...\n\n";
            }
            else
            {
                SummaryMsg = SummaryMsg + "\n\n";
            }
        }

        string fileName = vecIn[3];
        ofstream outFile(fileName);

        if (outFile.is_open())
        {
            outFile << SummaryMsg;
            outFile.close();
            cout << "Summary appended to file: " << fileName << endl;
        }
        else
        {
            cerr << "Error: Unable to open or create file " << fileName << endl;
        }
    }

    else if (vecIn[0] == "logout")
    {   
        receiptId = receiptId + 1;
        logoutReceiptId = receiptId;

        return "DISCONNECT\n"
               "receipt:" +
               to_string(receiptId) + "\n\n\0";
    }

    return "";
}

vector<string> StompProtocol::handleReport(string path)
{   
    vector<string> vecIn = this->parseArgs(path);
    string newPath = vecIn[1];

    names_and_events namesAndEvents = parseEventsFile(newPath);
    vector<string> emergMsgs;

    for (Event event : namesAndEvents.events)
    {
        event.setEventOwnerUser(userNameToUse);
        string eventOwnerUser = event.getEventOwnerUser();
        string channelName = event.get_channel_name();
        string city = event.get_city();
        string name = event.get_name();
        string dateTime = to_string(event.get_date_time());
        string description = event.get_description();
        map<string, string> general_information = event.get_general_information();

        string eventMsg = "SEND\n"
                          "destination:/" +
                          channelName + "\n\n"
                                        "user: " +
                          eventOwnerUser + "\n"
                                           "city: " +
                          city + "\n"
                                 "event name: " +
                          name + "\n"
                                 "date time: " +
                          dateTime + "\n" + "general information:\n";

        for (auto const &x : general_information)
        {
            eventMsg = eventMsg + x.first + ": " + x.second + "\n";
        }

        eventMsg = eventMsg + "description: \n" + description + "\n";

        receiptId = receiptId + 1;
        eventMsg = eventMsg + "receipt: " + to_string(receiptId) + "\n\0";

        emergMsgs.push_back(eventMsg);
    }

    return emergMsgs;
}

void StompProtocol::resetProtocol()
{
    receiptId = 0;
    subscriptionId = 0;
    logoutReceiptId = 0;
    userLoggedIn = false;
    shouldLogOut = false;
    gotError = false;
    userNameToUse = "";
    eventsMap.clear();
    userChannelReports.clear();
    joinReciepts.clear();
    exitReciepts.clear();
}

int StompProtocol::getReceiptId()
{
    return this->receiptId;
}

int StompProtocol::getSubscriptionId()
{
    return this->subscriptionId;
}
bool StompProtocol::getUserLoggedIn()
{
    return this->userLoggedIn;
}

void StompProtocol::setgotError(bool update)
{
    gotError = update;
}

bool StompProtocol::getgotError()
{
    return gotError;
}

int StompProtocol::getLogoutReceiptId()
{
    return this->logoutReceiptId;
}

void StompProtocol::setLogoutReceiptId(int update)
{
    logoutReceiptId = update;
}

bool StompProtocol::getShouldLogOut()
{
    return shouldLogOut;
}
