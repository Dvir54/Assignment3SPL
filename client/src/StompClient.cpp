#include "../include/ConnectionHandler.h"
#include "../include/StompProtocol.h"
#include <string>
#include <iostream>
#include <thread>
#include <mutex>
#include <atomic>
#include <functional>
#include <queue>
#include <vector>
using namespace std;

mutex coutMutex;
mutex queueMutex;

atomic<bool> isRunning(true);

vector<string> parseArgsClient(const string &input)
{
	vector<string> args;
	stringstream ss(input);
	string word;

	while (ss >> word)
	{
		args.push_back(word);
	}

	return args;
}

pair<string, string> splitString(const string &input, char delimiter)
{
	size_t pos = input.find(delimiter);
	if (pos == string::npos)
	{

		return {input, ""};
	}

	string left = input.substr(0, pos);
	string right = input.substr(pos + 1);
	return {left, right};
}

bool validateInput(const vector<string> &args)
{
	string command = args[0];

	if (command == "login")
	{
		if (args.size() != 4)
		{
			cerr << "login command needs 3 args: {host:port} {username} {password}" << endl;
			return false;
		}

		if (splitString(args[1], ':').second.empty())
		{
			cerr << "host:port are illegal" << endl;
			return false;
		}
	}
	else if (command == "report")
	{
		if (args.size() != 2)
		{
			cerr << "join command needs 1 args: {channel_name}" << endl;
			return false;
		}
	}
	else if (command == "logout")
	{
		if (args.size() != 1)
		{
			cerr << "logout command needs 0 args" << endl;
			return false;
		}
	}
	else if (command == "join")
	{
		if (args.size() != 2)
		{
			cerr << "join command needs 1 args: {channel_name}" << endl;
			return false;
		}
	}
	else if (command == "exit")
	{
		if (args.size() != 2)
		{
			cerr << "exit command needs 1 args: {channel_name}" << endl;
			return false;
		}
	}
	else
	{
		cerr << "Illegal command, please try a different one" << endl;
		return false;
	}

	return true;
}

void serverThreadfunc(ConnectionHandler &connectionHandler, StompProtocol &protocol, queue<string> &messageQueue)
{
	while (isRunning && connectionHandler.isSocketOpen())
	{
		string OutputMsg;
		string InputMsg;

		lock_guard<mutex> lock(queueMutex);
		while (!messageQueue.empty())
		{
			string msgToSend = messageQueue.front();
			messageQueue.pop();
			// if(&messageQueue != nullptr)
			// {
			connectionHandler.sendMessage(msgToSend);
			// }
		}

		if (connectionHandler.isSocketOpen() && connectionHandler.available() > 0)
		{
			if (connectionHandler.getMessage(InputMsg))
			{
				protocol.process(InputMsg);
			}
		}
		else
		{
			this_thread::sleep_for(chrono::milliseconds(50));
		}
	}
}

int main(int argc, char *argv[])
{
	ConnectionHandler *connectionHandler = nullptr;
	StompProtocol protoStmp;
	queue<string> messageQueue;
	thread serverThread;
	string input;
	string output;
	string host;
	short port;

	while (isRunning)
	{

		getline(cin, input);
		vector<string> InputVec = parseArgsClient(input);

		if(InputVec[0] != "login" && protoStmp.getUserLoggedIn() == false)
		{
			cerr << "please login first" << endl;
			continue;

		} else if(!validateInput(InputVec))
		{
			continue;
		}

		if (InputVec[0] == "login")
		{
			if (connectionHandler != nullptr && connectionHandler->isSocketOpen())
			{
				cerr << "The client is already logged in, log out before trying again" << endl;
				continue;
			}
			// reset the protocol?!?!?!!?!?!?!?!?!?!?!?!?!?!!?!?!?

			host = splitString(InputVec[1], ':').first;
			port = stoi(splitString(InputVec[1], ':').second);
			connectionHandler = new ConnectionHandler(host, port);

			if (!connectionHandler->connect())
			{
				cerr << "Could not connect to server." << endl;
				delete connectionHandler;
				connectionHandler = nullptr;
				continue;
			}

			output = protoStmp.handleInput(input);

			serverThread = thread(serverThreadfunc, ref(*connectionHandler), ref(protoStmp), ref(messageQueue));
		}
		else if (InputVec[0] == "report")
		{
			vector<string> vecIn = protoStmp.handleReport(input);
			for (string msg : vecIn)
			{
				lock_guard<mutex> lock(queueMutex);
				messageQueue.push(msg);
			}
		}
		else if (InputVec[0] == "logout")
		{
			output = protoStmp.handleInput(input);
			// connectionHandler->close(); whereee?!?!?
		}
		else
		{
			output = protoStmp.handleInput(input);
		}

		if (!output.empty())
		{
			lock_guard<mutex> lock(queueMutex);
			messageQueue.push(output);
		}
	}

	if (connectionHandler != nullptr)
	{
		connectionHandler->close();
		delete connectionHandler;
	}

	if (serverThread.joinable())
	{
		// isRunning = false; // check if needed!!!!!!!!
		serverThread.join();
	}

	cout << "Program terminated." << endl;
}