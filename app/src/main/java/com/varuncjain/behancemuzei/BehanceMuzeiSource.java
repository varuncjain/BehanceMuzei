package com.varuncjain.behancemuzei;

import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.google.android.apps.muzei.api.UserCommand;

import java.net.SocketTimeoutException;
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
    private static final int COMMAND_ID_SHARE = MAX_CUSTOM_COMMAND_ID - 1;
    private static final int COMMAND_ID_DOWNLOAD = MAX_CUSTOM_COMMAND_ID - 2;

    private List<UserCommand> mCommands;
    private BehanceService mBehanceService;
    private List<User> mPopularUsers;
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
        mCommands = new ArrayList<UserCommand>();
        mCommands.add(new UserCommand(BUILTIN_COMMAND_ID_NEXT_ARTWORK));
        mCommands.add(new UserCommand(COMMAND_ID_SHARE, getString(R.string.action_share_artwork)));
        mCommands.add(new UserCommand(COMMAND_ID_DOWNLOAD, getString(R.string.action_download_artwork)));
        setUserCommands(mCommands);
        PreferenceHelper.limitConfigFreq(this);
    }

    @Override
    protected void onCustomCommand(int id) {
        super.onCustomCommand(id);

        Artwork currentArtwork = getCurrentArtwork();

        Log.d(TAG, String.format("custom action called: %s", id));

        switch (id) {
            case COMMAND_ID_SHARE:
                if (currentArtwork == null) {
                    Log.w(TAG, "no current image, can't share!");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(BehanceMuzeiSource.this,
                                    R.string.action_no_artwork_to_share,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.action_share_default)
                        + "\nfrom "
                        + currentArtwork.getTitle()
                        + "\n"
                        + currentArtwork.getViewIntent().getDataString()
                        + "\n\n"
                        + currentArtwork.getImageUri().toString());
                shareIntent = Intent.createChooser(shareIntent, getString(R.string.action_share_artwork));
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(shareIntent);
                break;

            case COMMAND_ID_DOWNLOAD:
                if (currentArtwork == null) {
                    Log.w(TAG, "no current image, can't download!");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(BehanceMuzeiSource.this,
                                    R.string.action_no_artwork_to_download,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                    return;
                }
                Intent downloadIntent = new Intent(DownloadReceiver.ACTION_DOWNLOAD);
                downloadIntent.putExtra(DownloadReceiver.TITLE, currentArtwork.getTitle());
                downloadIntent.putExtra(DownloadReceiver.URL, currentArtwork.getImageUri().toString());
                sendBroadcast(downloadIntent);
                break;
        }
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        if (!isConnectedAsPreferred()) {
            scheduleUpdate(System.currentTimeMillis() + getRotateTimeMillis());
            return;
        }

        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

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
                        if (retrofitError.isNetworkError() || (500 <= statusCode && statusCode < 600)) {
                            return new RetryException();
                        }
                        scheduleUpdate(System.currentTimeMillis() + getRotateTimeMillis());
                        return retrofitError;
                    }
                })
                .build();

        mBehanceService = restAdapter.create(BehanceService.class);
        mPopularUsers = new ArrayList<User>();
        mProjects = new ArrayList<Project>();

        List<String> userNames = PreferenceHelper.userNamesFromPref(getApplicationContext());

        if (reason != UPDATE_REASON_USER_NEXT && isPopularEnabled()) {
            mPopularUsers.clear();
            try {
                UserList userList = mBehanceService.getPopularUsers();
                if (userList == null || userList.users == null) {
                    throw new RetryException();
                }
                    for (User user : userList.users) {
                    mPopularUsers.add(user);
                }
                for (User user : mPopularUsers) {
                    userNames.add(user.username);
                }
            } catch (RetrofitError e) {
                Log.e(TAG, "error while fetching popular users from behance", e);
                errorToast(e);
                return;
            }
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
                    if (projectList.projects.size() == 0) {
                        Log.w(TAG, String.format("user %s has no projects, skip", userName));
                        iterator.remove();
                    }
                    else {
                        mProjects.addAll(projectList.projects);
                    }
                } catch (RetrofitError e) {
                    Log.e(TAG, "error while fetching user projects from behance", e);
                    errorToast(e);
                    return;
                }
            }
        }

        Log.i(TAG, String.format("there are %s available projects", mProjects.size()));
        if (mProjects.isEmpty()) {
            Log.w(TAG, "no projects to choose images from");
            throw new RetryException();
        }

        Random random = new Random();
        Project project;
        ProjectDetail projectD;
        User user;
        Module module;
        String token;

        while (true) {
            try {
                project = mProjects.get(random.nextInt(mProjects.size()));
                projectD = mBehanceService.getProject(project.id);
                project = projectD.project;
                user = project.owners.get(0); // first owner of project

                filterModules(project);
                if (project.modules.size() == 0) {
                    Log.w(TAG, String.format("project %s: %s has no qualifying images", project.id, project.name));
                    continue;
                }

                module = project.modules.get(random.nextInt(project.modules.size()));
                token = Integer.toString(project.id);
                if (mProjects.size() <= 1 || !TextUtils.equals(token, currentToken)) {
                    Log.i(TAG, String.format("selected project: %s", project.name));
                    Log.i(TAG, String.format("selected user: %s", user.username));
                    Log.i(TAG, String.format("selected artwork: %s", module.sizes.max_1240));
                    break;
                }
            } catch (RetrofitError e) {
                Log.e(TAG, "error while fetching user projects from behance", e);
                errorToast(e);
                return;
            }
        }

        publishArtwork(new Artwork.Builder()
                .title(project.name)
                .byline(user.username)
                .imageUri(Uri.parse(module.sizes.max_1240))
                .token(token)
                .viewIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(project.url)))
                .build());

        scheduleUpdate(System.currentTimeMillis() + getRotateTimeMillis());
    }

    private void filterModules(Project project) {
        Iterator<Module> iterator = project.modules.iterator();
        while (iterator.hasNext()) {
            Module module = iterator.next();
            if (!(TextUtils.equals(module.type, "image")
                    && (module.sizes.max_1240 != null)
                    && ((module.sizes.max_1240.endsWith(".jpg")
                        || module.sizes.max_1240.endsWith(".jpeg")
                        || module.sizes.max_1240.endsWith(".png"))))) {
                iterator.remove(); // remove modules without JPG, JPEG or PNG images
            }
        }
    }

    private void errorToast(RetrofitError retrofitError) {
        int statusCode = retrofitError.getResponse().getStatus();
        if (statusCode == 429) {
            Toast.makeText(BehanceMuzeiSource.this,
                    R.string.error_too_many_request,
                    Toast.LENGTH_SHORT).show();
        }
        else if (statusCode == 403) {
            Toast.makeText(BehanceMuzeiSource.this,
                    R.string.error_forbidden,
                    Toast.LENGTH_SHORT).show();
        }
        else if (statusCode == 404) {
            Toast.makeText(BehanceMuzeiSource.this,
                    R.string.error_not_found,
                    Toast.LENGTH_SHORT).show();
        }
    }
}