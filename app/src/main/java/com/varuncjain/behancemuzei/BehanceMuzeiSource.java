package com.varuncjain.behancemuzei;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.util.Iterator;
import java.util.Random;
import java.util.List;
import java.util.ArrayList;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

import static com.varuncjain.behancemuzei.BehanceService.ProjectList;
import static com.varuncjain.behancemuzei.BehanceService.Project;
import static com.varuncjain.behancemuzei.BehanceService.ProjectDetail;
import static com.varuncjain.behancemuzei.BehanceService.UserList;
import static com.varuncjain.behancemuzei.BehanceService.User;
import static com.varuncjain.behancemuzei.BehanceService.Module;

public class BehanceMuzeiSource extends RemoteMuzeiArtSource {

    private static final String TAG = "BehanceMuzei";
    private static final String SOURCE_NAME = "BehanceMuzeiSource";

    private BehanceService mBehanceService;
    private List<Project> mProjects;

    public BehanceMuzeiSource() {
        super(SOURCE_NAME);
    }

    private int getRotateTimeMillis() {
        return PreferenceHelper.getConfigFreq(this);
    }

    private boolean isConnectedAsPreferred() {
        if (PreferenceHelper.getConfigConnection(this) == PreferenceHelper.CONNECTION_WIFI) {
            return Utils.isWifiConnected(this);
        }
        return true;
    }

    private boolean isPopularEnabled() {
        if (PreferenceHelper.getConfigPopular(this) == PreferenceHelper.USERS_POPULAR_ON) {
            return true;
        }
        return false;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(BehanceService.API_URL)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addQueryParam("client_id", Config.CLIENT_ID);
                    }
                })
                .setErrorHandler(new ErrorHandler() {
                    @Override
                    public Throwable handleError(RetrofitError retrofitError) {
                        int statusCode = retrofitError.getResponse().getStatus();
                        if (retrofitError.isNetworkError()
                                || (500 <= statusCode && statusCode < 600)) {
                            return new RetryException();
                        }
                        scheduleUpdate(System.currentTimeMillis() + getRotateTimeMillis());
                        return retrofitError;
                    }
                })
                .build();

        mBehanceService = restAdapter.create(BehanceService.class);
        mProjects = new ArrayList<Project>();
        PreferenceHelper.limitConfigFreq(this);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        if (!isConnectedAsPreferred()) {
            scheduleUpdate(System.currentTimeMillis() + getRotateTimeMillis());
            return;
        }

        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

        List<String> userNames = PreferenceHelper.userNamesFromPref(getApplicationContext());

        if (isPopularEnabled()) {
            UserList userList = mBehanceService.getPopularUsers();
            if (userList == null || userList.users == null) {
                throw new RetryException();
            }

            List<String> popularUsers = new ArrayList<String>();
            for (User user : userList.users) {
                popularUsers.add(user.username);
            }
            userNames.addAll(popularUsers);
        }

        if(userNames.isEmpty()) {
            Log.w(TAG, "no usernames.");
            scheduleUpdate(System.currentTimeMillis() + getRotateTimeMillis());
            return;
        }

        if(reason != UPDATE_REASON_USER_NEXT || mProjects.isEmpty()) {
            mProjects.clear();
            Iterator<String> iterator = userNames.iterator();
            while (iterator.hasNext()) {
                try {
                    String userName = iterator.next();
                    ProjectList projectList = mBehanceService.getUserProjects(userName);
                    if(projectList.projects.size() == 0) {
                        Log.w(TAG, String.format("user %s has no projects, skip", userName));
                        iterator.remove();
                    }
                    else {
                        mProjects.addAll(projectList.projects);
                    }
                } catch (RetrofitError e) {
                    Log.e(TAG, "error while fetching from behance", e);
                }
            }
        }

        Random random = new Random();
        Project project;
        ProjectDetail projectD;
        User user;
        Module module;
        String token;

        while (true) {
            project = mProjects.get(random.nextInt(mProjects.size()));
            projectD = mBehanceService.getProject(project.id);
            project = projectD.project;
            user = project.owners.get(0); // first owner of project

            filterModules(project);
            if (project.modules.size() == 0) {
                continue;
            }

            module = project.modules.get(random.nextInt(project.modules.size()));
            token = Integer.toString(project.id);
            if (mProjects.size() <= 1 || !TextUtils.equals(token, currentToken)) {
                break;
            }
        }

        publishArtwork(new Artwork.Builder()
                .title(project.name)
                .byline(user.username)
                .imageUri(Uri.parse(module.sizes.original))
                .token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(project.url)))
                .build());

        scheduleUpdate(System.currentTimeMillis() + getRotateTimeMillis());
    }

    private void filterModules(Project project) {
        Iterator<Module> iterator = project.modules.iterator();
        while (iterator.hasNext()) {
            Module module = iterator.next();
            if (!TextUtils.equals(module.type, "image")
                    || (!(module.sizes.original.endsWith(".jpg")
                        || module.sizes.original.endsWith(".jpeg")
                        || module.sizes.original.endsWith(".png")))) {
                iterator.remove(); // remove modules without JPG, JPEG or PNG images
            }
        }
    }
}