package com.delete.audiorecorder;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.SimpleDateFormat;
import java.util.Date;

import mp3.Main;




public class AudioTool extends LinearLayout {
	private static final String TAG = "VoiceTool";
	private RecordAudio recordTask;
	private PlayAudio playTask;

	private ImageButton mRecordingStartButton = null;
	private ImageButton mRecordingStopButton = null;
	
	private ImageButton mPlayStartButton = null;
	private ImageButton mDeleteVoiceButton = null;
	private ImageButton mPlayStopButton = null;

	private ImageButton mSaveButton;
	
	private SeekBar mPlaySeekBar = null;
	private File mRecordingFile;

	private boolean isRecording = false;
	private boolean isPlaying = false;

	private final int FREQUENCY = 11025;
	private final int CUSTOM_FREQ_SOAP = 1;
	private final int OUT_FREQUENCY = FREQUENCY * CUSTOM_FREQ_SOAP;
	private final int CHANNEL_CONTIGURATION = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	private final int AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
	
	
	
	public AudioTool(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context);
	}

	public AudioTool(Context context) {
		super(context);
		init(context);
		
		
	}

	public void init(Context context) {
		LayoutInflater inflater = (LayoutInflater) getContext()
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);      
		View view = inflater.inflate(R.layout.audio_recorder_layout, this,
				false);
		addView(view);

		mRecordingStartButton = (ImageButton) view
				.findViewById(R.id.voicetool_record_start_button);
		mRecordingStopButton = (ImageButton) view
				.findViewById(R.id.voicetool_record_stop_button);
		mPlayStartButton = (ImageButton) view
				.findViewById(R.id.voicetool_play_start_button);
		mPlayStopButton = (ImageButton) view
				.findViewById(R.id.voicetool_play_stop_button);

		mDeleteVoiceButton = (ImageButton) view
				.findViewById(R.id.voicetool_delete_button);
		
		mSaveButton=(ImageButton)findViewById(R.id.button_save);
		mSaveButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(mRecordingFile.length()>0)
                {
                    //save();

                    new Save_task().execute();
                }
                else
                {
                    Toast.makeText(getContext(),"None Recorded",Toast.LENGTH_SHORT).show();
                }
			}
		});
		
		mPlaySeekBar = (SeekBar) view.findViewById(R.id.voicetool_seekbar);
		mPlaySeekBar.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (isRecording)
					return true;
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					isPlaying = false;
				case MotionEvent.ACTION_UP:
					play();
					break;
				default:
					break;
				}
				return false;
			}
		});
		mRecordingStartButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				record();
			}
		});
		mRecordingStopButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				stopRecording();
			}
		});
		mPlayStartButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				play();

			}
		});
		mPlayStopButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				stopPlaying();
			}
		});
		mDeleteVoiceButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				delete();
			}

		});

		mRecordingStopButton.setEnabled(false);
		mPlayStartButton.setEnabled(false);
		mPlayStopButton.setEnabled(false);
		mDeleteVoiceButton.setEnabled(false);
		
		
		
		File path = context.getExternalFilesDir(null);

		//for test
		/*
		String root_dir=Environment.getExternalStorageDirectory().getAbsolutePath()+"/mic_record";
		File temp=new File(root_dir);
		*/
		
		path.mkdirs();
		try {
			mRecordingFile = File.createTempFile("recording", ".pcm", path);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't create file on SD card", e);
		}
	}

	public void play() {
		mPlayStartButton.setEnabled(true);

		playTask = new PlayAudio();
		playTask.execute();
		// TODO : ∏’¿˙ µÈæÓø»
		mPlayStopButton.setEnabled(true);
		mDeleteVoiceButton.setEnabled(false);
		mPlayStopButton.setVisibility(View.VISIBLE);
		mPlayStartButton.setVisibility(View.GONE);
		mRecordingStartButton.setEnabled(false);
	}

	private void delete() {
		if (mRecordingFile != null)
			mRecordingFile.delete();
		mDeleteVoiceButton.setEnabled(false);
		mPlayStartButton.setEnabled(false);
		mRecordingStartButton.setEnabled(true);
		mPlaySeekBar.setProgress(0);
		mPlaySeekBar.setMax(0);
	}

	public void stopPlaying() {
		isPlaying = false;
		mPlayStopButton.setEnabled(false);
		mPlayStopButton.setVisibility(View.GONE); 
		mPlayStartButton.setEnabled(true);
		mPlayStartButton.setVisibility(View.VISIBLE);
		mRecordingStartButton.setEnabled(true);
	}

	public void record() {
		mRecordingStartButton.setEnabled(false);
		mRecordingStartButton.setVisibility(View.GONE);

		mRecordingStopButton.setEnabled(true);
		mRecordingStopButton.setVisibility(View.VISIBLE);

		mPlayStartButton.setEnabled(false);
		mDeleteVoiceButton.setEnabled(false);

		mPlaySeekBar.setMax(1500);
		recordTask = new RecordAudio();
		recordTask.execute();
		
		
	}

	public void stopRecording() {
		isRecording = false;

		mPlayStartButton.setEnabled(true);
		mDeleteVoiceButton.setEnabled(true);
		// mPlaySeekBar.setMax(max)
	}

	public void save()
	{
		//pcm file val:mRecordingFile
		String root_dir=Environment.getExternalStorageDirectory().getAbsolutePath()+"/mic_record";
		File dir=new File(root_dir);
		if(!dir.exists())
		{
			dir.mkdir();
            Log.i("saving","mkdir");
		}
		
		try
		{
			OutputStream os=new FileOutputStream(root_dir+"/test.wav");
			//OutputStream up=new FileOutputStream(root_dir+"/up.wav");
			//OutputStream down=new FileOutputStream(root_dir+"/down.wav");
			//PCMtoFile(os, mRecordingFile, 11025, CHANNEL_CONTIGURATION, AUDIO_ENCODING);
			
			
			//normal
			PCMtoFile(os, mRecordingFile, 11025, 2, 16);

            Log.i("saving","pcm->wav");
			/*
			//pitch up
			PCMtoFile(up, mRecordingFile, 16000, 2, 16);
			
			//pitch down
			PCMtoFile(down, mRecordingFile, 8000, 2, 16);
			*/
			
			WavtoMp3(new File(root_dir,"test.wav"));

            Log.i("saving","wav->mp3");
			//VUlist_save();
			mRecordingFile.delete();
			
			
			
			
			//Toast.makeText(getContext(), "save complete", Toast.LENGTH_SHORT).show();
			//temp_filecopy(mRecordingFile);
		}
		catch(Exception e)
		{
			Toast.makeText(getContext(), "save_error", Toast.LENGTH_SHORT).show();
		}
	}
	
	
	public void PCMtoFile(OutputStream os, File pcmdata, int srate, int channel, int format) throws IOException {
	    byte[] header = new byte[44];
	  
	    byte[] data=org.apache.commons.io.FileUtils.readFileToByteArray(pcmdata);
	    
	    short[] shorts=new short[data.length/2];
	    ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(shorts);

	    
	    
	    
	    long totalDataLen = data.length + 44;
	    long bitrate = srate * channel * format;
	    
	    header[0] = 'R'; 
	    header[1] = 'I';
	    header[2] = 'F';
	    header[3] = 'F';
	    header[4] = (byte) (totalDataLen & 0xff);
	    header[5] = (byte) ((totalDataLen >> 8) & 0xff);
	    header[6] = (byte) ((totalDataLen >> 16) & 0xff);
	    header[7] = (byte) ((totalDataLen >> 24) & 0xff);
	    header[8] = 'W';
	    header[9] = 'A';
	    header[10] = 'V';
	    header[11] = 'E';
	    header[12] = 'f'; 
	    header[13] = 'm';
	    header[14] = 't';
	    header[15] = ' ';
	    //header[16] = (byte) format; 
	    header[16] = 16; 
	    header[17] = 0;
	    header[18] = 0;
	    header[19] = 0;
	    header[20] = 1; 
	    header[21] = 0;
	    //header[22] = (byte) channel; 
	    header[22] = 1;  
	    header[23] = 0;
	    header[24] = (byte) (srate & 0xff);
	    header[25] = (byte) ((srate >> 8) & 0xff);
	    header[26] = (byte) ((srate >> 16) & 0xff);
	    header[27] = (byte) ((srate >> 24) & 0xff);
	    header[28] = (byte) ((bitrate / 8) & 0xff);
	    header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
	    header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
	    header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
	    //header[32] = (byte) ((channel * format) / 8); 
	    header[32] = (byte) ((2 * 16) / 8); //(2 * 16) / 8
	    header[33] = 0;
	    header[34] = 16; 
	    header[35] = 0;
	    header[36] = 'd';
	    header[37] = 'a';
	    header[38] = 't';
	    header[39] = 'a';
	    header[40] = (byte) (data.length  & 0xff);
	    header[41] = (byte) ((data.length >> 8) & 0xff);
	    header[42] = (byte) ((data.length >> 16) & 0xff);
	    header[43] = (byte) ((data.length >> 24) & 0xff);
	    
	    
	    
	    os.write(header, 0, 44);
	    
	    //byte[] little=new byte[data.length];
	    
	    
	   // ByteBuffer buffer=ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
	   
	    
	    //os.write(data);
	    //os.write(buffer.array());
	    
	    for(int i=0;i<shorts.length;i++)
	    {
	    	byte[] temp=new byte[2];
	    	
	    	temp[0]=(byte)(shorts[i]>>8);
	    	temp[1]=(byte)shorts[i];
	    	
	    	os.write(temp);
	    	
	    }
	   
	  
	    os.close(); 
	    
	    
	}

	String mp3_name;
	public void WavtoMp3(File source)
	{
		
		String root_dir=Environment.getExternalStorageDirectory().getAbsolutePath()+"/mic_record";
		//File source = new File("source.wav");

        String time=createName(System.currentTimeMillis());
        mp3_name=time+".mp3";
        String[] mp3Args = {"--preset","standard",
	            "-q","0",
	            "-m","s",
	            source.getAbsolutePath(),
	            root_dir+"/"+time+".mp3"};
	    Main m = new mp3.Main();
	    try
	    {
	        m.run(mp3Args);
	    }
	    catch(Exception e)
	    {
	    	//Toast.makeText(getContext(), "mp3 save error", Toast.LENGTH_SHORT).show();
	        //System.out.println("ERROR processing MP3 " + e);// Some bug in Android seems to cause error BufferedOutputSteam is Closed. But it still seems to work OK.
	    	//Log.e("mp3",e.getMessage());
	    }   

        source.delete();

        Mediascan();
	}
    private String createName(long dateTaken){
        Date date = new Date(dateTaken);
        SimpleDateFormat dateFormat =
                new SimpleDateFormat("yyyy-MM-dd HH.mm.ss");
        return dateFormat.format(date);
    }

    void Mediascan()
    {

        String root_dir=Environment.getExternalStorageDirectory().getAbsolutePath()+"/mic_record";
        String file=root_dir+"/"+mp3_name;

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.KITKAT)
        {
            MediaScannerConnection.scanFile(getContext(), new String[] {file }, null, new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    Log.i("ExternalStorage", "Scanned " + path + ":");
                    Log.i("ExternalStorage", "-> uri=" + uri);
                }
            });
        }
        else
        {
            getContext().sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" +file)));
        }
    }

    /*
	void VUlist_save()
	{
		
		String temp=new Gson().toJson(logs);
		String filename=Environment.getExternalStorageDirectory().getAbsolutePath()+"/mic_record/log.json";
		Log.i("json",temp);
		try 
		{
			FileOutputStream save=new FileOutputStream(filename);
			save.write(temp.getBytes());
			save.close();
			
			
		} 
		catch (Exception e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			Log.e("json",e.toString());
		}
		finally
		{
			logs.clear();
		}
		
		
	}
	*/
	private class PlayAudio extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			isPlaying = true;

			int bufferSize = AudioTrack.getMinBufferSize(OUT_FREQUENCY,
					CHANNEL_CONTIGURATION, AUDIO_ENCODING);
			short[] audiodata = new short[bufferSize / 4];

			try {
				DataInputStream dis = new DataInputStream(
						new BufferedInputStream(new FileInputStream(
								mRecordingFile)));

				AudioTrack audioTrack = new AudioTrack(
						AudioManager.STREAM_MUSIC, OUT_FREQUENCY,
						CHANNEL_CONTIGURATION, AUDIO_ENCODING, bufferSize,
						AudioTrack.MODE_STREAM);

				audioTrack.play();
				if (mPlaySeekBar.getProgress() == mPlaySeekBar.getMax()) {
					mPlaySeekBar.post(new Runnable() {
						@Override
						public void run() {
							mPlaySeekBar.setProgress(0);
						}
					});
				} else {
					int skipCount = ((mPlaySeekBar.getProgress() - (mPlaySeekBar
							.getProgress() % audiodata.length)) * FREQUENCY / 1000);
					double time = (double) mPlaySeekBar.getProgress() / 1000.0f;
					skipCount = (int) (time * (double) FREQUENCY);
					skipCount = (skipCount - (skipCount % audiodata.length)) * 2;
					dis.skip(skipCount);
				}
				while (isPlaying && dis.available() > 0) {
					int i = 0;

					while (dis.available() > 0 && i < audiodata.length) {
						audiodata[i] = dis.readShort();
						i++;
					}
					audioTrack.write(audiodata, 0, audiodata.length);
					final int length = audiodata.length;
					mPlaySeekBar.post(new Runnable() {
						@Override
						public void run() {
							mPlaySeekBar.setProgress(mPlaySeekBar.getProgress()
									+ (int) ((float) length * 1000.0f / (float) FREQUENCY));

						}
					});
				}

				dis.close();
			} catch (Throwable t) {
				Log.e(TAG, "Playback Failed");
			}

			if (isPlaying) {
				mPlaySeekBar.setProgress(mPlaySeekBar.getMax());
				isPlaying = false;

				mPlayStartButton.post(new Runnable() {
					@Override
					public void run() {
						mPlayStartButton.setEnabled(true);
						mPlayStopButton.setEnabled(false);
						mPlayStartButton.setVisibility(View.VISIBLE);
						mPlayStopButton.setVisibility(View.GONE);
						mDeleteVoiceButton.setEnabled(true);
						mRecordingStartButton.setEnabled(true);
					}
				});
			}

			return null;
		}
	}

    /*
	public class VU_Log
	{
		public int time;
		public int value;
	}
	int current_time=0;
	ArrayList<VU_Log>logs=new ArrayList<VU_Log>();
	*/
	// Record AsyncTask
	private class RecordAudio extends AsyncTask<Void, Integer, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			isRecording = true;
			try {
				DataOutputStream dos = new DataOutputStream(
						new BufferedOutputStream(new FileOutputStream(
								mRecordingFile, true)));

				int bufferSize = AudioRecord.getMinBufferSize(FREQUENCY,
						CHANNEL_CONTIGURATION, AUDIO_ENCODING);

				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, FREQUENCY,
						CHANNEL_CONTIGURATION, AUDIO_ENCODING, bufferSize);

				short[] buffer = new short[bufferSize];
				audioRecord.startRecording();

				
				
				while (isRecording) {
					int bufferReadResult = audioRecord.read(buffer, 0,
							bufferSize);
					int amplitude = 0;
					for (int i = 0; i < bufferReadResult; i++) {
						dos.writeShort(buffer[i]);
						amplitude += Math.abs((int) buffer[i]);
					}
					final int amp = amplitude;
					/*
                    VU_Log temp=new VU_Log();
					temp.time=current_time;
					temp.value=amp;
					
					logs.add(temp);
					current_time+=1;
					*/
					mPlaySeekBar.post(new Runnable() {
						@Override
						public void run() {
							mPlaySeekBar.setProgress(amp / 10000);
							
							
						}
					});
					
					
				}
					
				
				audioRecord.stop();
				dos.close();
				
				
				mPlaySeekBar.post(new Runnable() {
					@Override
					public void run() {
						int time = (int) ((float) mRecordingFile.length() * 500.0f / (float) FREQUENCY);
						
						mPlaySeekBar.setMax(time);
						mPlaySeekBar.setProgress(0);
						Log.d(TAG,"length:"+ String.valueOf( mRecordingFile.length()));
						Log.d(TAG, "Recorded time : " + time);
					}
				});
			} catch (Throwable t) {
			}

			return null;
		}

		protected void onPostExecute(Void result) {
			mRecordingStartButton.setEnabled(true);
			mRecordingStartButton.setVisibility(View.VISIBLE);
			mRecordingStopButton.setEnabled(false);
			mRecordingStopButton.setVisibility(View.GONE);
			mPlayStartButton.setEnabled(true);

            /*
			current_time=0;
			
			//Time_Correction
			int time = (int) ((float) mRecordingFile.length() * 500.0f / (float) FREQUENCY);
			int json_time=logs.get(logs.size()-1).time;
			for(int i=0;i<logs.size();i++)
			{
				logs.get(i).time=(logs.get(i).time*time)/json_time;
			}
			*/
		}
	}

    public class Save_task extends AsyncTask<Void,Void,Void>
    {
        ProgressDialog pd;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            pd=new ProgressDialog(getContext());
            pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);

            pd.setTitle("Saving");
            pd.setMessage("Please Wait");
            pd.setCancelable(false);
            Log.i("saving", "setting end");

            pd.show();

        }
        @Override
        protected Void doInBackground(Void... params) {

            Log.i("saving","save start");
            save();
            Log.i("saving","save ending");
            return null;
        }


        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);

            pd.dismiss();
            Log.i("saving", "save end");
            Toast.makeText(getContext(),"Save Complete!",Toast.LENGTH_SHORT).show();

        }

    }

}
