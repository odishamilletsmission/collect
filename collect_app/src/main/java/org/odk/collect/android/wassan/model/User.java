package org.odk.collect.android.wassan.model;

import org.odk.collect.android.wassan.app.UserProject;
import org.odk.collect.projects.Project;

import java.util.List;

public class User {
    private Integer userId,district,block,gp,roleId;
    private String username,email,fullname ,phone,position, photo;
    private List<UserProject> userProjects;
    private UserProject defaultProject;
    public User(){

    }
    public User(Integer userId, String username,String email, String fullname, String phone, String position, Integer district, Integer block, Integer gp, Integer roleId, String photo,List<UserProject> userProjects, UserProject defaultProject) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.fullname = fullname;
        this.phone = phone;
        this.position = position;
        this.district = district;
        this.block = block;
        this.gp=gp;
        this.roleId = roleId;
        this.photo = photo;
        this.userProjects = userProjects;
        this.defaultProject = defaultProject;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public UserProject getDefaultProject() {
        return defaultProject;
    }

    public void setDefaultProject(UserProject defaultProject) {
        this.defaultProject = defaultProject;
    }

    public Integer getDistrict() {
        return district;
    }

    public void setDistrict(Integer district) {
        this.district = district;
    }

    public Integer getBlock() {
        return block;
    }

    public void setBlock(Integer block) {
        this.block = block;
    }

    public Integer getGp() {
        return gp;
    }

    public void setGp(Integer gp) {
        this.gp = gp;
    }

    public Integer getRoleId() {
        return roleId;
    }

    public void setRoleId(Integer roleId) {
        this.roleId = roleId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }





    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public List<UserProject> getUserProjects() {
        return userProjects;
    }

    public void setUserProjects(List<UserProject> userProjects) {
        this.userProjects = userProjects;
    }
}
