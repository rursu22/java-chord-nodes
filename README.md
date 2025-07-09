# java-chord-nodes
Java implementation of the Chord protocol and algorithm, with added distributed systems features such as caching, data consistency, and reliability

Allows for uploading of different file types to the Chord back-end, which will do different analysis depending on the file type that it has received, and send the result back to the front-end.


### Features

![image](https://github.com/user-attachments/assets/4d975018-2930-466c-a4a5-c52e0fd3e7c5)

Front-end interface that allows for file upload to the Chord back-end

![image](https://github.com/user-attachments/assets/957ba01f-93ef-4d08-b8e1-fd177f95952d)

3 different types of tasks can be sent to the Chord back-end: analysing text files, image files, or CSV files.

![image](https://github.com/user-attachments/assets/7c1ea88e-3938-47b5-b266-193fff840025)

Clean cards that show the result of the file analysis, with respective status to show that tasks are being worked on.

The results can be downloaded and saved locally as XML.

![image](https://github.com/user-attachments/assets/f77dfc4a-1112-41a5-9ccc-f2fec2baac9c)

Each node shows its own key, successor's key and predecessor's key.

![image](https://github.com/user-attachments/assets/2fd4d837-e639-478f-8497-18288b26fb24)

When a new node is added to the peer to peer Chord network, it will find its own successor and predecessor. All of the nodes in the network are made aware of this new node, and they will also change their state accordingly.


