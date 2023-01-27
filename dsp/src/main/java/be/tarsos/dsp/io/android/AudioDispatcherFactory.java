/*
*      _______                       _____   _____ _____
*     |__   __|                     |  __ \ / ____|  __ \
*        | | __ _ _ __ ___  ___  ___| |  | | (___ | |__) |
*        | |/ _` | '__/ __|/ _ \/ __| |  | |\___ \|  ___/
*        | | (_| | |  \__ \ (_) \__ \ |__| |____) | |
*        |_|\__,_|_|  |___/\___/|___/_____/|_____/|_|
*
* -------------------------------------------------------------
*
* TarsosDSP is developed by Joren Six at IPEM, University Ghent
*
* -------------------------------------------------------------
*
*  Info: http://0110.be/tag/TarsosDSP
*  Github: https://github.com/JorenSix/TarsosDSP
*  Releases: http://0110.be/releases/TarsosDSP/
*
*  TarsosDSP includes modified source code by various authors,
*  for credits and info, see README.
*
*/

package be.tarsos.dsp.io.android;

import android.content.Context;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.util.Log;

import com.arthenica.ffmpegkit.FFmpegKit;
import com.arthenica.ffmpegkit.FFmpegKitConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import be.tarsos.dsp.io.TarsosDSPAudioInputStream;
import be.tarsos.dsp.io.UniversalAudioInputStream;

/**
 * The Factory creates {@link AudioDispatcher} objects from the
 * configured default microphone of an Android device. 
 * It depends on the android runtime and does not work on the standard Java runtime.
 *
 * @author Joren Six
 * @see AudioDispatcher
 */
public class AudioDispatcherFactory {

	static private final String TAG = "AudioDispatcherFactory";
	static private int pipe_counter = 1;

	/**
	 * Create a new AudioDispatcher connected to the default microphone.
	 *
	 * @param sampleRate
	 *            The requested sample rate.
	 * @param audioBufferSize
	 *            The size of the audio buffer (in samples).
	 *
	 * @param bufferOverlap
	 *            The size of the overlap (in samples).
	 * @return A new AudioDispatcher
	 */
	public static AudioDispatcher fromDefaultMicrophone(final int sampleRate,
			final int audioBufferSize, final int bufferOverlap) {
		int minAudioBufferSize = AudioRecord.getMinBufferSize(sampleRate,
				android.media.AudioFormat.CHANNEL_IN_MONO,
				android.media.AudioFormat.ENCODING_PCM_16BIT);
		int minAudioBufferSizeInSamples =  minAudioBufferSize/2;
		if(minAudioBufferSizeInSamples <= audioBufferSize ){
		AudioRecord audioInputStream = new AudioRecord(
				MediaRecorder.AudioSource.MIC, sampleRate,
				android.media.AudioFormat.CHANNEL_IN_MONO,
				android.media.AudioFormat.ENCODING_PCM_16BIT,
				audioBufferSize * 2);

		TarsosDSPAudioFormat format = new TarsosDSPAudioFormat(sampleRate, 16,1, true, false);
		
		TarsosDSPAudioInputStream audioStream = new AndroidAudioInputStream(audioInputStream, format);
		//start recording ! Opens the stream.
		audioInputStream.startRecording();
		return new AudioDispatcher(audioStream,audioBufferSize,bufferOverlap);
		}else{
			throw new IllegalArgumentException("Buffer size too small should be at least " + (minAudioBufferSize *2));
		}
	}


	/**
	 * Create a stream from a piped sub process and use that to create a new
	 * {@link AudioDispatcher} The sub-process writes a WAV-header and
	 * PCM-samples to standard out. The header is ignored and the PCM samples
	 * are are captured and interpreted. Examples of executables that can
	 * convert audio in any format and write to stdout are ffmpeg and avconv.
	 *
	 * @param selectedFileUri
	 *            The file or stream to capture.
	 * @param targetSampleRate
	 *            The target sample rate.
	 * @param audioBufferSize
	 *            The number of samples used in the buffer.
	 * @param bufferOverlap
	 * 			  The number of samples to overlap the current and previous buffer.
	 * @return A new audioprocessor.
	 */
	public static AudioDispatcher fromPipe(Context context, Uri selectedFileUri, final int targetSampleRate, final int audioBufferSize, final int bufferOverlap){
		String inputFile = FFmpegKitConfig.getSafParameterForRead(context, selectedFileUri);
		String outputFile = registerNewPipe(context);

		Log.d(TAG, "Pipe output :" + outputFile);

		FFmpegKit.execute("-ss 20 -t 3 -y -i " + inputFile + " -f s16le -acodec pcm_s16le -ar 44100 -ac 1 " + outputFile);

		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(new File(outputFile));
			Log.d("FILE", "opened");
		} catch (FileNotFoundException e) {
			Log.d("FILE", "error");
		}

		TarsosDSPAudioInputStream audioStream = new UniversalAudioInputStream(outputFile, new TarsosDSPAudioFormat(targetSampleRate, 16, 1, true, false));
		return new AudioDispatcher(audioStream, audioBufferSize, bufferOverlap);
	}

	public static String registerNewPipe(final Context context) {
		// PIPES ARE CREATED UNDER THE PIPES DIRECTORY
		final File cacheDir = context.getCacheDir();
		final File pipesDir = new File(cacheDir, "pipes");

		if (!pipesDir.exists()) {
			final boolean pipesDirCreated = pipesDir.mkdirs();
			if (!pipesDirCreated) {
				android.util.Log.e(TAG, String.format("Failed to create pipes directory: %s.", pipesDir.getAbsolutePath()));
				return null;
			}
		}

		final String newFFmpegPipePath = pipesDir +"/pipe"  + pipe_counter;
		pipe_counter += 1;

		// FIRST CLOSE OLD PIPES WITH THE SAME NAME
		closePipe(newFFmpegPipePath);

		return newFFmpegPipePath;
	}

	public static void closePipe(final String ffmpegPipePath) {
		final File file = new File(ffmpegPipePath);
		if (file.exists()) {
			file.delete();
			Log.d(TAG, "Pipe deleted : " + ffmpegPipePath);
		}
	}
}



