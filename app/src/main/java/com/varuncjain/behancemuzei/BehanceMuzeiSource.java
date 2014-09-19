package com.varuncjain.behancemuzei;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.util.Random;

import retrofit.ErrorHandler;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

import static com.varuncjain.behancemuzei.BehanceService.Module;
import static com.varuncjain.behancemuzei.BehanceService.Owner;
import static com.varuncjain.behancemuzei.BehanceService.Project;
import static com.varuncjain.behancemuzei.BehanceService.ProjectDetail;
import static com.varuncjain.behancemuzei.BehanceService.ProjectList;

public class BehanceMuzeiSource extends RemoteMuzeiArtSource {

    private static final String TAG = "BehanceMuzei";
    private static final String SOURCE_NAME = "BehanceMuzeiSource";

    private static final int ROTATE_TIME_MILLIS = 3 * 60 * 60 * 1000; // rotate every 3 hours

    public BehanceMuzeiSource() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        String currentToken = (getCurrentArtwork() != null) ? getCurrentArtwork().getToken() : null;

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://www.behance.net")
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
                        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
                        return retrofitError;
                    }
                })
                .build();

        BehanceService service = restAdapter.create(BehanceService.class);
        ProjectList projectList = service.getProjects();

        if (projectList == null || projectList.projects == null) {
            throw new RetryException();
        }

        Random random = new Random();
        Project project = null;
        Owner owner = null;
        Module module = null;

        while (true) {
            project = projectList.projects.get(random.nextInt(projectList.projects.size()));
            ProjectDetail projectD = service.getProject(project.id);
            owner = projectD.project.owners.get(0); // first owner of project
            while (true) {
                module = projectD.project.modules.get
                        (random.nextInt(projectD.project.modules.size()));
                if (TextUtils.equals(module.type, "image")
                        && ((module.sizes.original.endsWith(".jpg"))
                        || (module.sizes.original.endsWith(".jpeg"))
                        || (module.sizes.original.endsWith(".png")))) {
                    break; // select module with JPG, JPEG or PNG image
                }
            }
            if (projectList.projects.size() <= 1
                    || !TextUtils.equals(Integer.toString(project.id), currentToken)) {
                break;
            }
        }

        publishArtwork(new Artwork.Builder()
                .title(project.name)
                .byline(owner.username)
                .imageUri(Uri.parse(module.sizes.original))
                .token(Integer.toString(project.id))
                .viewIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(project.url)))
                .build());

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}