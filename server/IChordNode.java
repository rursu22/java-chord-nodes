import java.rmi.Remote;
import java.rmi.RemoteException;

public interface IChordNode extends Remote {
    int getKey() throws RemoteException;
    void put(String key, byte[] value, String type, boolean replicated) throws RemoteException;
    String get(String key) throws RemoteException;
    void join(IChordNode atNode) throws RemoteException;
    IChordNode findSuccessor(int key) throws RemoteException;
    IChordNode closestPrecedingNode(int key) throws RemoteException;
    IChordNode getPredecessor() throws RemoteException;
    void notify(IChordNode potentialPredecessor) throws RemoteException;
    void replicateData(String key, byte[] value, String type, boolean replicated) throws RemoteException;
}
