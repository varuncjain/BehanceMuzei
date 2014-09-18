package com.varuncjain.behancemuzei;

import java.util.List;

import retrofit.http.GET;
import retrofit.http.Path;

public interface BehanceService {
    @GET("/v2/projects?sort=featured_date")
    ProjectList getProjects();

    @GET("/v2/projects/{project_id}")
    ProjectDetail getProject(@Path("project_id") int id);

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
        List<Owner> owners;
        List<Module> modules;
    }

    static class Owner {
        String username;
    }

    static class Module {
        String type;
        String src;
        Size sizes;
    }

    static class Size {
        String original;
    }
}