package tp1.clients.user;

import tp1.api.User;
import tp1.api.service.util.Result;

import java.util.*;

public class UsersCachedClient implements UsersClient{

    private final UsersClient client;

    private final Map<String,User> usersCache;

    public final static long UPDATE_PERIOD = 500;

    public UsersCachedClient(UsersClient client) {
        this.client = client;
        this.usersCache = new HashMap<>();

        //startCollecting();
    }

    public UsersCachedClient(String serverUrl) throws Exception{
        UsersClient c;
        if (serverUrl.contains("/rest"))
            c = new UsersRestClient(serverUrl);
        else
            c = new UsersSoapClient(serverUrl);

        this.client = new UsersRetryClient(c);
        this.usersCache = new HashMap<>();

        //startCollecting();
    }

    private void startCollecting() {
        new Thread(() -> {
            for (;;) {
                try {
                    List<User> result = client.searchUsers(null).value();
                    for (User u : result) {
                        usersCache.put(u.getUserId(),u);
                    }
                } catch (Exception ignored) {
                }

                try { Thread.sleep(UPDATE_PERIOD); } catch (InterruptedException ignored) {}
            }
        }).start();
    }

    @Override
    public Result<String> createUser(User user) {
        return client.createUser(user);
    }

    @Override
    public Result<User> getUser(String userId, String password) {
        Result<User> result = client.getUser(userId,password);

        if(result.isOK() || result.error() != Result.ErrorCode.NOT_AVAILABLE)
            return result;
        else {
            User u = usersCache.get(userId);

            if(u != null)
                return Result.ok(u);
            else
                return result;
        }
    }

    @Override
    public Result<User> updateUser(String userId, String password, User user) {
        return client.updateUser(userId,password,user);
    }

    @Override
    public Result<User> deleteUser(String userId, String password) {
        return client.deleteUser(userId,password);
    }

    @Override
    public Result<List<User>> searchUsers(String pattern) {
        Result<List<User>> result = client.searchUsers(pattern);

        if(result.isOK() || result.error() != Result.ErrorCode.NOT_AVAILABLE)
            return result;
        else {
            synchronized (this) {
                List<User> users = new LinkedList<>();
                for (User u : usersCache.values()) {
                    if (u.getFullName().toLowerCase().contains(pattern.toLowerCase()))
                        users.add(u);
                }
                return Result.ok(users);
            }
        }
    }
}
