import java.util.Vector;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.Math;
import java.nio.charset.StandardCharsets;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.UUID;

class Finger {
	public int key;
	public IChordNode node;
}

class Store {
	String key;
	byte[] value;
	String type;
	boolean replicated = false;
	boolean analysed = false;
	String XML = "";
}

public class ChordNode implements Runnable, IChordNode {

	static final int KEY_BITS = 8;

	static Registry registry;
	static IDistributedHashTable DHT;
	static TextAnalyser textAnalyser = new TextAnalyser();
	static CSVAnalyser csvAnalyser = new CSVAnalyser();
	static ImageAnalyser imageAnalyser = new ImageAnalyser();

	// for each peer link that we have, we store a reference to the peer node plus a
	// "cached" copy of that node's key; this means that whenever we change e.g. our
	// successor reference we also set successorKey by doing successorKey =
	// successor.getKey()
	IChordNode successor;
	int successorKey;

	IChordNode predecessor;
	int predecessorKey;

	int fingerTableLength;
	Finger finger[];
	int nextFingerFix;

	Vector<Store> dataStore = new Vector<Store>();

	private int myKey;

	ChordNode(String myKeyString) {
		myKey = hash(myKeyString);

		successor = this;
		successorKey = myKey;

		// initialise finger table (note all "node" links will be null!)
		finger = new Finger[KEY_BITS];
		for (int i = 0; i < KEY_BITS; i++)
			finger[i] = new Finger();
		fingerTableLength = KEY_BITS;

		// start up the periodic maintenance thread
		new Thread(this).start();
	}

	// -- API functions --

	// VERY IMPORTANT!!!!! NODES ARE RESPONSIBLE FOR THE RANGE
	// (PREDECESSORKEY,MYKEY]
	// WHEN WE WANT TO SEE IF A KEY IS WITHIN THAT RANGE, WE GET ITS SUCCESSOR

	/**
	 * Put some data on the node responsible for it
	 * Nodes are responsible for (PredecessorKey,MyKey]
	 * @param key key linked to the data
	 * @param value value linked to the data
	 */

	public void put(String key, byte[] value, String type, boolean replicated) {
		try {
			int hashedKey = hash(key);
			System.out.println(hashedKey);

			// find the node that should hold this key and add the key and value to that
			// node's local store
			IChordNode node = findSuccessor(hashedKey);

			if (node.getKey() == myKey) {
				// Loop through the data store and try to find the first store whose key matches the 
				// key data that we're trying to put on the node
				// If we find one, it means that the data already exists on the node
				// If we don't, then it means that it's new data
				Store existingData = dataStore.stream()
									.filter(data -> data.key.equals(key))
									.findFirst()
									.orElse(null);

				if(existingData == null) {
					Store dataPoint = new Store();
					dataPoint.key = key;
					dataPoint.value = value;
					dataPoint.type = type;
					dataPoint.replicated = replicated;
					dataStore.add(dataPoint);
					System.out.println("Data with key " + hashedKey + " stored on node with key " + myKey);
				}
				// Previous way of checking if data already exists 
				// Store dataPoint = new Store();
				// dataPoint.key = key;
				// dataPoint.value = value;
				// dataPoint.type = type;
				// dataPoint.replicated = replicated;
				// if(!dataStore.contains(dataPoint)) {
				// 	dataStore.add(dataPoint);
				// 	System.out.println("Data with key " + hashedKey + " stored on node with key " + myKey);
				// }

				// Check if the node has a successor that is not itself and add that data to its dataStore
				if(successor != null && successorKey != myKey) {
					System.out.println("Replicated data with key: " + key + " to node with key: " + successorKey);
					successor.replicateData(key, value, type, true);
				}

				if(predecessor != null && predecessorKey != myKey && predecessorKey != successorKey) {
					System.out.println("Replicated data with key: " + key + " to node with key: " + predecessorKey);
					predecessor.replicateData(key, value, type, true);
				}
				
				
			} else {
				node.put(key, value, type, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Function that deals with data replication, will only be used with the nearest successor
	 * @param dataPoint a store holding a key and value pair that gets replicated
	 */
	public void replicateData(String key, byte[] value, String type, boolean replicated) {
		Store dataPoint = new Store();
		dataPoint.key = key;
		dataPoint.value = value;
		dataPoint.type = type;
		dataPoint.replicated = true;
		if(!dataStore.contains(dataPoint)) {
			dataStore.add(dataPoint);
		}
	}

	/**
	 * Get some data stored on the node
	 * @param key key linked to the data
	 * @return data in the form of a byte[]
	 */

	public String get(String key) {
		try {
			// find the node that should hold this key, request the corresponding value from
			// that node's local store, and return it
			int hashedKey = hash(key);
			IChordNode node = findSuccessor(hashedKey);
			if (node.getKey() == myKey) {
				for (Store store : dataStore) {
					// System.out.println(dataStore.get(i).XML);
					System.out.println("DataStore key: " + store.key);
					System.out.println("Item Key: " + key);
					if (store.key.equals(key) ) {
						System.out.println("Found right item");
						System.out.println(store.XML);
						if (!store.XML.isEmpty()) {
							System.out.println("Item came out successfully out of node: " + myKey);
							// System.out.println("Found key " + key + " at node with key: " + getKey());
							return store.XML;
						}
					}
				}
			} else {
				return node.get(key);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("Item came out unsuccessfully out of node: " + myKey);
		return "";
	}

	/**
	 * Returns the key of the node
	 */

	public int getKey() {
		return myKey;
	}

	/**
	 * Returns the predecessor of the node
	 */
	public IChordNode getPredecessor() {
		return predecessor;
	}

	
	/**
	 * Important function that is called whenever a new node wants to join the network
	 * @param atNode node that the current node wants to join, atNode becomes the successor of the current node.
	 */
	public void join(IChordNode atNode) {
		try {
			predecessor = null;
			predecessorKey = 0;

			nextFingerFix = 0;
			if (atNode != null) {
				// Find a successor for the node and set the successor variables accordingly
				successor = atNode.findSuccessor(myKey);
				successorKey = successor.getKey();
			} else {
				successor = this;
				successorKey = myKey;
			}

			// The first entry in the finger table is going to be the node's successor
			finger[0].node = successor;
			finger[0].key = successorKey;
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Finds the successor of a node
	 * @param key the key value for which to find the successor for
	 */
	public IChordNode findSuccessor(int key) {
		try {
			// If the key is in this node's responsible range, then it is the successor
			if (isInHalfOpenRangeR(key, myKey, successorKey)) {
				return successor;
			} else {
				// Otherwise, look for the closest preceding node and find that one's successor
				IChordNode nextNode = closestPrecedingNode(key);
				if (nextNode.getKey() == myKey) {
					return this;
				}
				return nextNode.findSuccessor(key);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	/**
	 * Finds the closest preceding node of a given key
	 * @param key the key value for which to find the closest preceding node for
	 */
	public IChordNode closestPrecedingNode(int key) {
		// Look through the finger table backwards
		for (int i = fingerTableLength - 1; i > 0; i--) {
			// If we find a finger node and its key sits between this node's key and that
			// key, then it is the closest predecessor
			if (finger[i].node != null && isInOpenRange(finger[i].key, myKey, key)) {
				return finger[i].node;
			}
		}
		return this;
	}

	String asyncAnalysisText(byte[] content, String contentAsString) {
		HashMap<String, Integer> mostFrequent = textAnalyser.analyseMostFrequent(contentAsString);
		String mostFrequentWord = (String) mostFrequent.keySet().toArray()[0];
		int highestFrequency = (int) mostFrequent.get(mostFrequentWord);
		int wordCount = textAnalyser.analyseWordCount(contentAsString);
		int averageLength = textAnalyser.analyseAverageWordLength(contentAsString);

		// Generate the XML with the post analysis data that we got
		String generatedXML = textAnalyser.generateXML(content.length, mostFrequentWord, highestFrequency, averageLength, wordCount);

		return generatedXML;
	}

	String asyncAnalysisCSV(byte[] content, String contentAsString) {
		int[] numberOfColsAndRows = csvAnalyser.countColsAndRows(contentAsString);
		int[] textAndNumericalCols = csvAnalyser.numericalAndTextCols(contentAsString);
		int cols = numberOfColsAndRows[0];
		int rows = numberOfColsAndRows[1];
		int numericalCols = textAndNumericalCols[0];
		int textCols = textAndNumericalCols[1];

		String generatedXML = csvAnalyser.generateXML(content.length, rows, cols, numericalCols, textCols);

		return generatedXML;
	}

	String asyncAnalysisImage(byte[] content) {
		Object[] imageData = imageAnalyser.getImageData(content);
		int imageWidth = (int) imageData[0];
		int imageHeight = (int) imageData[1];
		String colorSpace = (String) imageData[2];
		int colorChannels = (int) imageData[3];
		boolean hasAlpha = (boolean) imageData[4];
		int bitDepth = (int) imageData[5];

		String XML = imageAnalyser.generateXML(content.length, colorSpace, imageWidth, imageHeight, colorChannels, hasAlpha, bitDepth);

		return XML;
	}

	/**
	 * Function that is called once in a while asynchronously to generate analysis reports on the tasks
	 */
	void asyncAnalysis() {
		if (dataStore.size() > 0) {
			for (int i = 0; i < dataStore.size(); i++) {
				// Check if the file has already been done before
				Store currentStore = dataStore.get(i);
				if(currentStore.analysed) {
					continue;
				}
				// Prepare the base data for the file
				
				String name = currentStore.key;
				byte[] content = currentStore.value;
				String type = currentStore.type;
				// Convert the content of the file into a string so that we can more easily
				// analyse its text when needed
				String contentAsString = new String(content, StandardCharsets.UTF_8);
				String generatedXML = "";

				// Analyse based on the task's type.
				if(type.equals("text")) {
					generatedXML = asyncAnalysisText(content, contentAsString);
				} else if(type.equals("csv")) {
					generatedXML = asyncAnalysisCSV(content, contentAsString);
				} else if(type.equals("image")) {
					generatedXML = asyncAnalysisImage(content);
				}

				// Store the XML on file
				File f = new File("./files/" + name + ".xml");

				try {
					currentStore.analysed = true;

					currentStore.XML = generatedXML;
					
					// Update the stored data
					dataStore.set(i, currentStore);

					// Notify the DHT that this task has been completed. Do this before writing the file just in case there is an exception there
					DHT.updateTaskProgress(name);

					FileOutputStream fis = new FileOutputStream(f);

					fis.write(generatedXML.getBytes());

					fis.close();

				} catch (Exception e) {
					e.printStackTrace();
				}
					
			}
		}
	}

	/**
	 * [a,b] Range check function
	 * @param key key that we want to check within the range
	 * @param a predecessor key
	 * @param b successor key
	 * @return whether or not the key is within the range [a,b]
	 */
	// x is in [a,b] ?
	boolean isInClosedRange(int key, int a, int b) {
		if (b > a)
			return key >= a && key <= b;
		else
			return key >= a || key <= b;
	}
	/**
	 * (a,b) Range check function
	 * @param key key that we want to check within the range
	 * @param a predecessor key
	 * @param b successor key
	 * @return whether or not the key is within the range (a,b)
	 */
	// x is in (a,b) ?
	boolean isInOpenRange(int key, int a, int b) {
		if (b > a)
			return key > a && key < b;
		else
			return key > a || key < b;
	}

	/**
	 * [a,b) Range check function
	 * @param key key that we want to check within the range
	 * @param a predecessor key
	 * @param b successor key
	 * @return whether or not the key is within the range [a,b)
	 */
	boolean isInHalfOpenRangeL(int key, int a, int b) {
		if (b > a)
			return key >= a && key < b;
		else
			return key >= a || key < b;
	}

	/**
	 * (a,b] Range check function
	 * @param key key that we want to check within the range
	 * @param a predecessor
	 * @param b successor
	 * @return whether or not the key is within the range (a,b]
	 */
	boolean isInHalfOpenRangeR(int key, int a, int b) {
		if (b > a)
			return key > a && key <= b;
		else
			return key > a || key <= b;
	}

	/**
	 * Hash function
	 * @param s - String to be hashed
	 * @return hash of the string
	 */
	int hash(String s) {
		int hash = 0;

		for (int i = 0; i < s.length(); i++)
			hash = hash * 31 + (int) s.charAt(i);

		if (hash < 0)
			hash = hash * -1;

		return hash % ((int) Math.pow(2, KEY_BITS));
	}

	/**
	 * Maintenance function, makes sure each node is the predecessor of the correct successor
	 */
	public void notify(IChordNode potentialPredecessor) {
		try {
			if (predecessor == null || isInOpenRange(potentialPredecessor.getKey(), predecessorKey, myKey)) {
				predecessor = potentialPredecessor;
				predecessorKey = predecessor.getKey();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	/**
	 * Maintenance function, makes sure each node is the successor of the correct predecessor
	 */
	void stabilise() {
		try {
			if(successor != null) {
				IChordNode thisNode = successor.getPredecessor();
				if (successor.getPredecessor() != null) {
					if (isInOpenRange(thisNode.getKey(), myKey, successorKey)) {
						successor = thisNode;
						successorKey = successor.getKey();
						// System.out.println("Node " + myKey + " updated successor to " +
						// successorKey);
					}
			}
			successor.notify(this);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	/**
	 * Maintenance function, tries to fix the finger table of the node
	 */

	void fixFingers() {
		try {
			nextFingerFix += 1;
			if (nextFingerFix > fingerTableLength - 1) {
				nextFingerFix = 0;
			}
			IChordNode nextNode = findSuccessor(myKey + (int) Math.pow(2, nextFingerFix - 1));
			finger[nextFingerFix].node = nextNode;
			finger[nextFingerFix].key = nextNode.getKey();
		} catch (Exception e) {
			// e.printStackTrace();
		}
	}

	/**
	 * Maintenance function, checks if the predecessor has failed
	 */

	void checkPredecessor() {
		try {
			if (predecessor != null) {
				predecessor.getKey();
			}
		} catch (Exception e) {
			try {
				// Remove the faulty node from the registry
				// We need to check if it's still there because it may have been removed by a different node
				if(Arrays.asList(registry.list()).contains("Node " + predecessorKey)) {
					registry.unbind("Node " + predecessorKey);
				}
				// Remove the link to the faulty node
				predecessor = null;
				predecessorKey = 0;
				// Try to find a new predecessor for the node
				fixFingers();
				predecessor = closestPrecedingNode(myKey);
				predecessorKey = predecessor.getKey();
				notify(predecessor);
			} catch (Exception extraException) {
				System.out.println("got extra exception");
				extraException.printStackTrace();
			}
		}
	}

	/**
	 * Maintenance function, checks if the successor has failed
	 */

	void checkSuccessor() {
		try {
			if (successor != null) {
				successor.getKey();
			}
		} catch (RemoteException e) {
			try {
				// Remove the faulty node from the registry
				// We need to check if it's still there because it may have been removed by a different node
				if(Arrays.asList(registry.list()).contains("Node " + successorKey)) {
					registry.unbind("Node " + successorKey);
				}
				// Remove the link to the faulty node
				successor = null;
				successorKey = 0;
				for(Finger currentFinger : finger) {
					// Look through each finger node to see if we can find one that's not faulty and connect to that 
					if (currentFinger.node != null) {
						try {
							currentFinger.node.getKey();
							successor = currentFinger.node;
							successorKey = currentFinger.key;
							break;
						} catch (Exception exception) {
							// If it reaches this code block, it means that this entry in the finger table is also faulty
							// So we continue to look for a new one
						}
					}
					
				}
				// If we couldn't find a new successor with the finger table, we can try with the findSuccessor function
				if (successor == null) {
					successor = findSuccessor(myKey);
					successorKey = successor.getKey();
				}
				
			} catch (Exception extraException) {
				System.out.println("got extra exception");
				e.printStackTrace();
			}
		}
	}

	/**
	 * To minimise the time it takes for the nodes to stabilise, we try to join nodes to their immediate successor, key-wise.
	 *  */ 
	IChordNode lookForBestNode() {
		try {
			// Registry registry = LocateRegistry.getRegistry(9002);
			// We are going to be sorting the array, so we want to get rid of the "DHT" node on the registry because it does not contain
			// a key, so it cannot be sorted numerically
			String[] nodesList = registry.list();
			ArrayList<String> nodesListWithoutDHT = new ArrayList<String>(Arrays.asList(nodesList));
			nodesListWithoutDHT.remove("DHT");
			nodesList = new String[nodesListWithoutDHT.size()];
			nodesList = nodesListWithoutDHT.toArray(nodesList);
			// Only look for nodes to join if it's not the only node in the network
			if(nodesList.length > 0) {
				// We sort the array to make sure that if we find a node with a key larger than this node's, it's the immediate largest, not necessarily the 
				// largest in the network
				// If we connect it to the largest in the network when there's other better nodes fit for that, then we would still need to stabilise everything
				Arrays.sort(nodesList, new Comparator<String>() {
					public int compare(String s1, String s2) {
						// Since all nodes are named "Node <nodeKey>", we can always find their key in the second part of the string, so we 
						// split it, and get the key. 
						// This saves time by not having the node connect to all other nodes and ask for their keys
						return Integer.valueOf(s1.split(" ")[1]).compareTo(Integer.valueOf(s2.split(" ")[1]));
					}
				});
				// DEBUG: print all of the nodes currently present in the DHT
				// for(String node : nodesList) {
				// 	System.out.println(node);
				// }
				
				for(String node : nodesList) {
					if (Integer.valueOf(node.split(" ")[1]) > myKey) {
						IChordNode nodeToJoin = (IChordNode) registry.lookup(node);
						return nodeToJoin;
					} 
				}

				// If it gets to this code block, then it means that this node's key is the largest in the network
				// meaning that the only possible successor it has is the one with the smallest key value
				// and since we sorted the nodes, it's the first in the array

				IChordNode nodeToJoin = (IChordNode) registry.lookup(nodesList[0]);
				return nodeToJoin;
			}
			return null;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	/**
	 * Check if there is some data in this node that belongs to the predecessor
	 */
	void checkDataMoveDown() {
		try {
			if (predecessor != null) {
				for (Store data : dataStore) {
					int hashedKey = hash(data.key);
					if (!isInHalfOpenRangeR(hashedKey, predecessorKey, myKey)) {
						// If the data has been replicated, don't move it down, otherwise it will cause an infinite loop
						// because it will keep trying to send data that already exists down
						if (data.replicated) {
							continue;
						}
						System.out.println("Moved data with key " + data.key);
						predecessor.put(data.key, data.value, data.type, false);
						dataStore.remove(data);
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public void run() {
		while (true) {
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				System.out.println("Interrupted");
			}

			// DEBUG: See what to the node's variables as it runs
			try {
				System.out.println("MyKey: " + myKey);
				System.out.println("Successor Key: " + successorKey);
				System.out.println("Predecessor Key: " + predecessorKey);
				System.out.println("Store length: " + dataStore.size());
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				stabilise();
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				fixFingers();
			} catch (Exception e) {
				e.printStackTrace();
			}

			try {
				checkPredecessor();
			} catch (Exception e) {
				// No need to print the stack trace as that is already done in the checkPredecessor just in case
				// It would throw up an error because this runs on a thread once every half second,
				// but that error gets solved in the function
			}

			try {
				checkSuccessor();
			} catch (Exception e) {
				// No need to print the stack trace as that is already done in the checkSuccessor just in case
				// It would throw up an error because this runs on a thread once every half second,
				// but that error gets solved in the function
			}

			try {
				checkDataMoveDown();
			} catch (Exception e) {
				e.printStackTrace();
			}
			try {
				asyncAnalysis();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String args[]) {
		try {
			ChordNode node = new ChordNode(UUID.randomUUID().toString());
			IChordNode stub = (IChordNode) UnicastRemoteObject.exportObject(node, 0);
			registry = LocateRegistry.getRegistry("localhost", 9002);
			DHT = (IDistributedHashTable) registry.lookup("DHT");
			IChordNode bestNode = node.lookForBestNode();
			node.join(bestNode);
			DHT.addNode(stub);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}