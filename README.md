                              Group-Messenger with TOTAL and FIFO Ordering Guarantees
                                  by implementing modified ISIS Algorithm
                                        
                                        

## Goal:

To implement Group Messaging Android app with TOTAL and FIFO ordering guarantees.This should be implemented by modifying the ISIS algorithm. The appplication should store all the messages along with their sequence numbers(Sequence numbers are used to order all the messages received from different apps/avds) in the content provider.

## TOTAL Ordering
Every process delivers all messages in the same order.
For example,
```
P1: m0, m1, m2
P2: m3, m4, m5
P3: m6, m7, m8
```
Total ordering:
```
P1: m7, m1, m2, m4, m5, m3, m6, m0, m8
P2: m7, m1, m2, m4, m5, m3, m6, m0, m8
P3: m7, m1, m2, m4, m5, m3, m6, m0, m8
```
## FIFO Ordering

The message delivery order at each process should preserve the message sending order from every process. But each process can deliver in a different order.
For example,
```
P1: m0, m1, m2
P2: m3, m4, m5
P3: m6, m7, m8
```
FIFO Ordering:
```
P1: m0, m3, m6, m1, m4, m7, m2, m5, m8
P2: m0, m3, m6, m1, m4, m7, m2, m5, m8
P3: m6, m7, m8, m0, m1, m2, m3, m4, m5
```
## Requirements:
```
1. Your app should multicast every user-entered message to all app instances 
 (including the one that is sending the message). 
2. Your app should use B-multicast. It should not implement R-multicast.
3. You need to come up with an algorithm that provides a total-FIFO ordering under a failure.
4. There will be at most one failure of an app instance in the middle of execution.  
5. Each message should be used to detect a node failure.
6. For this purpose, you can use a timeout for a socket read; you can pick a reasonable timeout value (e.g., 500 ms), and if       a node does not respond within the timeout, you can consider it a failure.
7. This means that you need to handle socket timeout exceptions in addition to socket creation/connection exceptions.
8. Do not just rely on socket creation or connect status to determine if a node has failed. Due to the Android emulator   networking setup, it is not safe to just rely on socket creation or connect status to judge node failures. Please also use socket read timeout exceptions as described above.
9. You cannot assume which app instance will fail. In fact, the grader will run your group messenger multiple times and each time it will kill a different instance. Thus, you should not rely on chance (e.g., randomly picking a central sequencer) to handle failures. This is just hoping to avoid failures. Instead, you should implement a decentralized algorithm (e.g., something based on ISIS).
10. When handling a failure, it is important to make sure that your implementation does not stall. After you detect a failure, you need to clean up any state related to it, and move on.
11. When there is a node failure, the grader will not check how you are ordering the messages sent by the failed node. Please refer to the testing section below for details.
12. We have fixed the ports & sockets.
    * Your app should open one server socket that listens on 10000.
    * You need to use run_avd.py and set_redir.py to set up the testing environment.
    * The grading will use 5 AVDs. The redirection ports are 11108, 11112, 11116, 11120, and 11124.
13. You should just hard-code the above 5 ports and use them to set up connections.
14. Please use the code snippet provided in PA1 on how to determine your local AVD.
    emulator-5554: “5554”
    emulator-5556: “5556”
    emulator-5558: “5558”
    emulator-5560: “5560”
    emulator-5562: “5562”
15. Every message should be stored in your provider individually by all app instances. Each message should be stored as a   <key, value> pair. The key should be the final delivery sequence number for the message (as a string); the value should be the actual message (again, as a string). The delivery sequence number should start from 0 and increase by 1 for each message.
16. For your debugging purposes, you can display all the messages on the screen. However, there is no grading component for this.

```

## Testing

```
There are two phases of testing
Phase 1---Testing without any failure: In this phase, all the messages should be delivered in a total-FIFO order. For each message, all the delivery sequence numbers should be the same across processes.
Phase 2---Testing with a failure: In this phase, all the messages sent by live nodes should be delivered in a total-FIFO order. Due to a failure, the delivery sequence numbers might go out of sync if some nodes deliver messages from the failed node, while others do not. This is OK; the grader will only examine the total-FIFO ordering guarantees for the messages sent by live nodes. (Note: in phase 2, the message sequence numbers can go out of sync due to a failure. Thus, when the grader output says that a key is missing, the key means the message sequence number that the grader is verifying. It may not be the exact key.)
Once again, you should implement a decentralized algorithm to handle failures correctly. This means that you should not implement a centralized algorithm. This also means that you should not implement any variation of a centralized algorithm that randomly picks a central node. In our grading, we will run phase 2 as many as possible.
If your implementation uses randomness in failure handling or is centralized, the score you get through the grader is not guaranteed.
On your terminal, it will give you your partial and final score, and in some cases, problems that the testing program finds.
Unlike previous graders, the grader for this assignment requires you to directly give the path of your apk to it. The grader will take care of installing/uninstalling the apk as necessary.
The grader uses multiple threads to test your code and each thread will independently print out its own log messages. This means that an error message might appear in the middle of the combined log messages from all threads, rather than at the end.
The grader has many options you can use for your testing. It allows you to choose which phase to test and for phase 2, how many times to run. It also has an option to print out verbose output, which can be helpful for debugging. You can enter the following command to see the options:

$ <grader executable> -h


You might run into a debugging problem if you're reinstalling your app from Android Studio. This is because your content provider will still retain previous values even after reinstalling. This won't be a problem if you uninstall explicitly before reinstalling; uninstalling will delete your content provider storage as well. In order to do this, you can uninstall with this command:

$ adb uninstall edu.buffalo.cse.cse486586.groupmessenger2
```
## General Notes


* Please do not use a separate timer to handle failures. This will make debugging very difficult. Use socket timeouts and handle all possible exceptions that get thrown when there is a failure. They are:
* SocketTimeoutException, StreamCorruptedException, IOException, FileException, and EOFException.
* Please use full duplex TCP for both sending and receiving. This means that there is no need to create a new connection every time you send a message. If you’re sending and receiving multiple messages from a remote AVD, then you can keep using the same socket. This makes it easier.
* Please do not use Java object serialization (i.e., implementing Serializable). It will create large objects 
that need to be sent and received. The message size overhead is unnecessarily large if you implement Serializable.
* Please do not assume that there is a fixed number of messages (e.g., 25 messages) sent in your system.
* Your implementation should not hardcode the number of messages in any way.
* There is a cap on the number of AsyncTasks that can run at the same time, even when you use THREAD_POOL_EXECUTOR.
* The limit is "roughly" 5. Thus, if you need to create more than 5 AsyncTasks (roughly, once again), 
then you will have to use something else like Thread. However, I really do not think that it is necessary to create that many AsyncTasks for the PAs in this course. Thus, if your code doesn't work because you hit the AsyncTask limit, then please think hard why you're creating that many threads in the first place.

*(http://developer.android.com/reference/java/util/concurrent/ThreadPoolExecutor.html)
(Read "Core and maximum pool sizes.")
* For Windows users: In the past, it was discovered that sometimes you cannot run a grader and Android Studio at the same time. As far as I know, this happens rarely, but there is no guarantee that you will not encounter this issue. Thus, if you think that a grader is not running properly and you don't know why,
first try closing Android Studio and run the grader.

## Running the code

1. Import the project in Android Studio and build the apk file.
2. Download the testing scripts from [here](https://github.com/Aman020/Group-Messenger/tree/master/Testing%20Scripts)

3. Before you run the program, please make sure that you are running five AVDs. The below command will do it: - - python run_avd.py 5
4. Also make sure that the Emulator Networking setup is done. The below command will do it: - - python set_redir.py 10000
5. Run the grader: - - $ chmod +x < grader executable> - $ ./< grader executable> apk file path
6. The grader has many options you can use for your testing. It allows you to choose which phase to test and for phase 2, how many times to run. It also has an option to print out verbose output, which can be helpful for debugging. You can enter the following command to see the options:
$ <grader executable> -h

### Credits

This project contains scripts and other related material that is developed at University of Buffalo, The State University of New York.

### References

[Design Documnet](https://docs.google.com/document/d/1xgXwZ6GYA152WT3K0B1MPP7F0mf0sPCPzfqr528pO5Y/edit#) <br/>
[Lecture Slides](https://cse.buffalo.edu/~stevko/courses/cse486/spring19/lectures/11-multicast1.pdf)

### Acknowledgement

I would like to thank my Professor, [Steve Ko](https://nsr.cse.buffalo.edu/?page_id=272), for the  guidance, encouragement and advice he has provided throughout. I am also thankful to all the TA's who have clarified all my queries.
