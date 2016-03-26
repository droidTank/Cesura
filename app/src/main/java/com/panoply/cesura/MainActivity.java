package com.panoply.cesura;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, MediaController.MediaPlayerControl {

    private static final String TAG = "MainActivity";

    private ListView songListView;
    private ArrayList<Song> songArrayList;

    private MusicService musicService;
    private Intent playIntent;
    private boolean musicBound = false;

    private MusicController controller;

    private ServiceConnection musicConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setList(songArrayList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        songListView = (ListView)findViewById(R.id.songList);
        songArrayList = new ArrayList<Song>();
        populateSongList();
        Collections.sort(songArrayList, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });
        songListView.setAdapter(new SongAdapter(this, songArrayList));

        RelativeLayout root = (RelativeLayout) findViewById(R.id.relLayout);
        ViewTreeObserver vto = root.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setController();
            }
        });
    }

    private void setController(){
        Log.d(TAG, "Setting up the MusicController");
        controller = new MusicController(this);
        controller.setPrevNextListeners(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playNext();
                    }
                },
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        playPrev();
                    }
                }
        );
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.songList));
        controller.setEnabled(true);
        controller.show();
    }

    private void playNext(){
        musicService.playNext();
        controller.show(0);
    }

    private void playPrev(){
        musicService.playPrev();
        controller.show(0);
    }

    public void populateSongList(){
        Log.d(TAG, "Populating the list of songs");
        ContentResolver musicResolver = getContentResolver();
        Uri externalMusicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        /*
        Uri internalMusicUri = MediaStore.Audio.Media.INTERNAL_CONTENT_URI;
        Support for internal audio required?
         */
        Cursor musicCursor = musicResolver.query(externalMusicUri, null, null, null, null);

        if(musicCursor!=null && musicCursor.moveToFirst()){
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);

            Song song;
            do{
                song = new Song(musicCursor.getString(artistColumn), musicCursor.getLong(idColumn), musicCursor.getString(titleColumn));
                songArrayList.add(song);
            } while(musicCursor.moveToNext());
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();
        if(playIntent == null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent,musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        switch(id){
            case R.id.nav_songs:
                break;
            case R.id.nav_artists:
                break;
            case R.id.nav_playlists:
                break;
            case R.id.nav_recommendation:
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void songPicked(View view){
        musicService.setSongPosition(Integer.parseInt(view.getTag().toString()));
        musicService.playSong();
    }

    @Override
    public void start() {
        //if(musicService!=null && musicBound)
        Log.d(TAG, "Starting MediaPlayer");
        musicService.startPlayer();
    }

    @Override
    public void pause() {
        Log.d(TAG,"Pause");
        musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if(musicService!=null && musicBound && musicService.isPlaying())
            return musicService.getDuration();
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if(musicService!=null && musicBound && musicService.isPlaying())
            return musicService.getCurrentPosition();
        return 0;
    }

    @Override
    public void seekTo(int pos) {
        if(musicService!=null && musicBound)
            musicService.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if(musicService!=null && musicBound)
            return musicService.isPlaying();
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}












class SongAdapter extends BaseAdapter{

    private ArrayList<Song> songList;
    private LayoutInflater layoutInflater;

    public SongAdapter(Context context, ArrayList<Song> songList){
        this.songList = songList;
        layoutInflater = LayoutInflater.from(context);
    }

    @Override
    public int getCount() {
        return songList.size();
    }

    @Override
    public Object getItem(int position) {
        return songList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return songList.get(position).getId();
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LinearLayout linearLayout = (LinearLayout) layoutInflater.inflate(R.layout.song, parent, false);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        TextView titleTV = (TextView) linearLayout.findViewById(R.id.songName);
        TextView artistTV = (TextView) linearLayout.findViewById(R.id.songArtist);
        Song song = songList.get(position);
        titleTV.setText(song.getTitle());
        artistTV.setText(song.getArtist());
        linearLayout.setTag(position);
        return linearLayout;
    }
}