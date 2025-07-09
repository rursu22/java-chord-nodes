import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

public interface IDistributedHashTable extends Remote{
    void addNode(IChordNode s) throws RemoteException;
    void distributeToNodes(String filename, byte[] fileContent, String taskType) throws RemoteException;
    void updateTaskProgress(String taskName) throws RemoteException;  
    HashMap<String, Integer> sendTasksProgress() throws RemoteException;
    String sendXML(String taskName) throws RemoteException;
}
