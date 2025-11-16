package javafxapplication1;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple session manager to handle user sessions
 * 
 * @author marke
 */
public class SessionManager {
    
    private static SessionManager instance;
    private Map<String, Object> sessionData;
    private String currentUser;
    private boolean isLoggedIn;
    
    private SessionManager() {
        sessionData = new HashMap<>();
        isLoggedIn = false;
    }
    
    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Store user session data
     */
    public void setUserSession(String username, String fullName, String email, String role) {
        currentUser = username;
        sessionData.put("username", username);
        sessionData.put("fullName", fullName);
        sessionData.put("email", email);
        sessionData.put("role", role);
        isLoggedIn = true;
        
        System.out.println("User session created for: " + username);
    }
    
    /**
     * Get current user
     */
    public String getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Get session data
     */
    public Object getSessionData(String key) {
        return sessionData.get(key);
    }
    
    /**
     * Check if user is logged in
     */
    public boolean isLoggedIn() {
        return isLoggedIn;
    }
    
    /**
     * Clear all session data
     */
    public void clearSession() {
        sessionData.clear();
        currentUser = null;
        isLoggedIn = false;
        
        System.out.println("User session cleared");
    }
    
    /**
     * Get user role
     */
    public String getUserRole() {
        return (String) sessionData.get("role");
    }
    
    /**
     * Get user full name
     */
    public String getUserFullName() {
        return (String) sessionData.get("fullName");
    }
    
    /**
     * Get user email
     */
    public String getUserEmail() {
        return (String) sessionData.get("email");
    }
}
