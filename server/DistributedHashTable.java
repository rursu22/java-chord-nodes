import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

public class DistributedHashTable implements IDistributedHashTable{
    /**
     * HashMap of form "<task, progress>", with the progress being a boolean
     */
    HashMap<String, Integer> progressMap = new HashMap<String, Integer>();
    /**
     * HashMap of the form "<task, XML>" that will act as a cache so that we don't send unnecessary requests to the nodes, like when
     * we want to request the XML of a task that's already been requested before
     */
    HashMap<String, String> xmlMap = new HashMap<String, String>();
    static Registry registry = null;
    /**
     * Function that adds a ChordNode to the registry with the name being its key
     */
    public void addNode(IChordNode s) {
        // When a server spins up, give it an ID and bind it to the registry with that name
        try {
            registry.rebind("Node " + s.getKey(), s);
            System.out.println(Arrays.toString(registry.list()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    
    /**
     * Select a Chord node at random and distribute a given task to it. The picked node will have to deal with delegating the task to the correct node
     * @param filename name of the file or task
     * @param fileContent byte content of the task 
     */
    IChordNode getAChordNode() {
        try {
            String[] nodesList = registry.list();
            ArrayList<String> nodesListWithoutDHT = new ArrayList<String>(Arrays.asList(nodesList));
            // We need to remove the "DHT" node from the list so that it doesn't try to connect to itself
            nodesListWithoutDHT.remove("DHT");
            nodesList = new String[nodesListWithoutDHT.size()];
            nodesList = nodesListWithoutDHT.toArray(nodesList);
            Random rand = new Random();
            int randNodeIndex = rand.nextInt(nodesList.length);
            IChordNode pickedNode = (IChordNode) registry.lookup(nodesList[randNodeIndex]);
            return pickedNode;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
    public void distributeToNodes(String filename, byte[] fileContent, String taskType) {
        try {
            // Create an entry in the progress map for this specific task
            progressMap.put(filename, 0);
            
            IChordNode pickedNode = getAChordNode();
            
            pickedNode.put(filename, fileContent, taskType, false);
        } catch (Exception e) {
            e.printStackTrace();
        }  
    }

    /**
     * Receive progress for a given task and updates it
     * @param taskName name of the task
     */
    public void updateTaskProgress(String taskName) {
        try {
            if(progressMap.get(taskName) != 1){
                // System.out.println("Received task progress for: " + taskName);
                progressMap.replace(taskName, 1);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
    /**
     * Sends the task progress map to the client
     * @return HashMap of the form "<task, progress>".
     */
    public HashMap<String, Integer> sendTasksProgress() {
        return progressMap;
    }

    /**
     * Sends the XML for a given task, but only if the task has been completed
     * @return XML of the requested completed task
     */
    public String sendXML(String taskName) {
        try {
            // If the XML has been requested already, it should be in the xmlMap, so get it from there
            if(xmlMap.containsKey(taskName)) {
                return xmlMap.get(taskName);
            }
            // If the XML has not been requested already, pick a node and get it from there
            IChordNode pickedNode = getAChordNode();
            String XML = pickedNode.get(taskName);
            xmlMap.put(taskName, XML);
            return XML;
        } catch (Exception e) {
            e.printStackTrace();
        }
    
        return null;
    }

    /*
     * Function that helps with the robustness of the application
     */
    public void checkForExistingData() {
        // The completed XML files are stored in the /files directory
        File f = new File("./files");
        File[] existingFiles = f.listFiles();

        if(existingFiles.length > 0) {
            for(File currentFile : existingFiles) {
                String name = currentFile.getName();
                // Remove the .xml at the end of the file name 
                String nameWithoutExtension = name.substring(0, name.length() - 4);
                // If the item is already in both the progressMap and xmlMap, skip it
                if(progressMap.containsKey(nameWithoutExtension) && xmlMap.containsKey(nameWithoutExtension)) {
                    continue;
                } else {
                    try {
                        // Get the filename and the content and put it in the hashmap
                        String XML = Files.readString(Paths.get(currentFile.getAbsolutePath()));
                        progressMap.put(nameWithoutExtension, 1);
                        xmlMap.put(nameWithoutExtension, XML);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            DistributedHashTable DHT = new DistributedHashTable();
            // Check for existing data on startup to help with robustness
            DHT.checkForExistingData();
            String name = "DHT";
            IDistributedHashTable stub = (IDistributedHashTable) UnicastRemoteObject.exportObject(DHT, 0);
            registry = LocateRegistry.createRegistry(9002);
            registry.rebind(name, stub);
        } catch (Exception e) {
            e.printStackTrace();
        }
        
    }
}
