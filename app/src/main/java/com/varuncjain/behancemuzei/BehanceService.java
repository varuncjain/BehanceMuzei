package com.varuncjain.behancemuzei;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Path;

public interface BehanceService {

    public static String API_URL = "http://www.behance.net";

    @GET("/v2/projects")
    ProjectList getPopularProjects();

    @GET("/v2/projects/{project_id}")
    ProjectDetail getProject(@Path("project_id") int id);

    @GET("/v2/users")
    UserList getPopularUsers();

    @GET("/v2/users/{user}")
    UserDetail getUser(@Path("user") String username);

    @GET("/v2/users/{user}/projects")
    ProjectList getUserProjects(@Path("user") String username);

    static class ProjectList {
        List<Project> projects;
    }

    static class ProjectDetail {
        Project project;
    }

    static class Project {
        int id;
        String name;
        String url;
        List<User> owners;
        List<Module> modules;
    }

    static class UserList {
        List<User> users;
    }

    static class UserDetail {
        User user;
    }

    static class User {
        String username;
        String display_name;
    }

    static class Module {
        String type;
        Size sizes;
    }

    static class Size {
        String original;
        String max_1240;
    }
}