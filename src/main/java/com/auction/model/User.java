package com.auction.model;
import com.auction.util.IdGenerator;

public abstract class User extends Entity {
    protected String username;
    protected String password;
    protected String email;
    public User (String username,String password,String email){
        super();
        this.username=username;
        this.password=password;
        this.email=email;
        this.id= IdGenerator.generationUserId();
    }
    public String getUsername(){
        return this.username;
    }
    public String getPassword(){
        return this.password;
    }
    public String getEmail(){
        return this.email;
    }
    public String getId(){
        return this.id;
    }

}
