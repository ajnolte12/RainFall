package com.redbeardstudios.rainfall.android;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.google.android.gms.games.Games;
import com.google.example.games.basegameutils.GameHelper;
import com.redbeardstudios.rainfall.AdsController;
import com.redbeardstudios.rainfall.Drop;
import com.google.android.gms.ads.*;
import com.redbeardstudios.rainfall.IGoogleServices;

public class AndroidLauncher extends AndroidApplication implements AdsController, IGoogleServices{

    private InterstitialAd interstitialAd;
    private GameHelper gameHelper;
    private final static int REQUEST_CODE_UNUSED = 9002;

	@Override
	protected void onCreate (Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        config.useAccelerometer = true;
        config.useCompass = false;

        //Setup Ads
        setupAds();

        //Setup Game Helper
        gameHelper = new GameHelper(this,GameHelper.CLIENT_GAMES);
        gameHelper.enableDebugLog(false);
        gameHelper.setMaxAutoSignInAttempts(0);

        GameHelper.GameHelperListener gameHelperListener = new GameHelper.GameHelperListener() {
            @Override
            public void onSignInFailed() {

            }

            @Override
            public void onSignInSucceeded() {

            }
        };

        gameHelper.setup(gameHelperListener);
		initialize(new Drop(this,this), config);
	}

    @Override
    protected void onStart(){
        super.onStart();
        gameHelper.onStart(this);
    }

    @Override
    protected void onStop(){
        super.onStop();
        gameHelper.onStop();
    }

    @Override
    protected  void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        gameHelper.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return (activeNetwork != null && activeNetwork.isConnectedOrConnecting());
    }

    @Override
    public void showInterstitialAd(final Runnable then) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (then != null) {
                    interstitialAd.setAdListener(new AdListener() {
                        @Override
                        public void onAdClosed() {
                            Gdx.app.postRunnable(then);
                            AdRequest ad = new AdRequest.Builder().addTestDevice("688CBE9DCD785F32868A643506020FF1").build();
                            interstitialAd.loadAd(ad);
                        }
                    });
                }
                interstitialAd.show();
            }
        });
    }

    public void setupAds(){
       interstitialAd = new InterstitialAd(this);
       interstitialAd.setAdUnitId(getString(R.string.restart_ad_id));

       AdRequest ad = new AdRequest.Builder().addTestDevice("688CBE9DCD785F32868A643506020FF1").build();
       interstitialAd.loadAd(ad);
    }

    @Override
    public void signIn() {
        try{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gameHelper.beginUserInitiatedSignIn();
                }
            });
        }
        catch(Exception e){
            Gdx.app.log("MainActivity", "Log in failed: " + e.getStackTrace() + ".");
        }
    }

    @Override
    public void signOut() {
        try{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    gameHelper.signOut();
                }
            });
        }
        catch (Exception e)
        {
            Gdx.app.log("MainActivity", "Log out failed: " + e.getMessage() + ".");
        }
    }

    @Override
    public void rateGame() {
        String str ="https://play.google.com/store/apps/details?id=com.redbeardstudios.rainfall.android";
        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(str)));
    }

    @Override
    public void submitScore(long score) {
        if (isSignedIn() == true){
            Games.Leaderboards.submitScore(gameHelper.getApiClient(), getString(R.string.leaderboard_id), score);
            startActivityForResult(Games.Leaderboards.getLeaderboardIntent(gameHelper.getApiClient(), getString(R.string.leaderboard_id)), REQUEST_CODE_UNUSED);
        }
        else{
            signIn();
            if (isSignedIn() == true){
                Games.Leaderboards.submitScore(gameHelper.getApiClient(), getString(R.string.leaderboard_id), score);
                startActivityForResult(Games.Leaderboards.getLeaderboardIntent(gameHelper.getApiClient(), getString(R.string.leaderboard_id)), REQUEST_CODE_UNUSED);
            }
        }
    }

    @Override
    public void showScores() {
        if (isSignedIn() == true)
            startActivityForResult(Games.Leaderboards.getLeaderboardIntent(gameHelper.getApiClient(), getString(R.string.leaderboard_id)), REQUEST_CODE_UNUSED);
        else
        {
            signIn();
            if (isSignedIn() == true)
                startActivityForResult(Games.Leaderboards.getLeaderboardIntent(gameHelper.getApiClient(), getString(R.string.leaderboard_id)), REQUEST_CODE_UNUSED);
        }

    }

    @Override
    public boolean isSignedIn() {
        return gameHelper.isSignedIn();
    }
}
