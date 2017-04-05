
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.*;

//import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

/**
 * Classe de gestion d'un signal audio
 * @author Extrait de plusieurs classes
 */

public class SoundSignal {
    
    private AudioInputStream stream;
    private short[] signal;
    private int samplingFrequency;
    
    /**
     * Constructeur du lecteur audio
     */
    public SoundSignal() {
	signal = null;
    }
    
    /**
     * AudioFormat.Encoding.PCM_SIGNED
     */
    public static AudioFormat defFormat = new AudioFormat(
							  
							  (float) 22050, 16, 1, true, false);
    
    
    /**
     * Lit un fichier wav
     * @param file fichier contenant le signal wav
     * @throws UnsupportedAudioFileException
     * @throws IOException
     */
    public void setSignal(String file) throws UnsupportedAudioFileException,
					      IOException {
	if (file != null) {
	    stream = AudioSystem.getAudioInputStream(new File(file));
	    this.samplingFrequency = (int)stream.getFormat().getSampleRate(); 
	    int lenByte = (int) (stream.getFrameLength() * stream.getFormat().getFrameSize());
	    byte[] signalByBytes = new byte[lenByte];
	    int off = 0;
	    int len = stream.getFormat().getFrameSize();
	    boolean doneReading = false;
	    while (!doneReading) {
		int nb = stream.read(signalByBytes, off, len);
		if (nb == -1) {
		    doneReading = true;
		} else {
		    off += nb;
		}
	    }
	    ByteBuffer bb = ByteBuffer.wrap(signalByBytes);
	    if (!stream.getFormat().isBigEndian())
		bb.order(ByteOrder.LITTLE_ENDIAN);
	    else
		bb.order(ByteOrder.BIG_ENDIAN);
	    ShortBuffer sb = bb.asShortBuffer();
	    int limit = sb.remaining();
	    signal = new short[limit];
	    sb.get(signal);
	    
	    stream.close();
	}
    }
    
    /**
     * Lit un tableau de short
     * @param  tableau (short) contenant un signal
     * @param  frequence d'echantillonnage
     */
    public void setSignal(short [] newsignal, int samplingFrequency ){
	
	/*signal = new short[newsignal.length];
	for(int i=0;i<newsignal.length;i++)
	    signal[i]=newsignal[i];
	*/
	signal = newsignal;
	this.samplingFrequency = samplingFrequency;
    }
    
    
    /**
     * Retourne le signal lu sous forme d'un tableau de short
     * @return un short[] tableau representant le signal
     */
    public short[] getSignal() {
	return signal;
	
    }
    
    /**
     * Retourne l'echantillon i du signal lu
     * @return short
     */
    public short getSignalSample(int i)
    {
	return signal[i];
    }
    
    /**
     * Retourne la longueur du signal en echantillons
     * @return un int longueur du signal en echantillons
     */
    public int getSignalLength() {
	return signal.length;
    }
    
    /**
     * Retourne la frequence d'echantillonnage du signal
     * @return un int pour le frequence d'echantillonnage
     */
    public int getSamplingFrequency() {
	return samplingFrequency;
    }
    
    /**
     * Write in LittleEndian a short
     * 
     */
    private void writeShort(DataOutputStream out, int arg0) throws IOException 
    {
	out.writeByte(arg0 & 0xff);
	out.writeByte((arg0 >> 8) & 0xff);
    }
    
    /**
     * Write in LittleEndian an int
     * 
     */
    private void writeInt(DataOutputStream out,int arg0) throws IOException {
        out.writeByte(arg0 & 0xff);
        out.writeByte((arg0 >> 8) & 0xff);
        out.writeByte((arg0 >> 16) & 0xff);
        out.writeByte((arg0 >> 24) & 0xff);
    }
    
    /**
     * Export a signal (array) in a sound file.
     * @param output the sound file name for output
     * @param header true if wav file, else raw file (wav file : 16KHz, 16bits, Mono)
     * @throws IOException if problem with output file
     */
    public void exportSignal(String output,boolean header) throws IOException
    {
	
	DataOutputStream data = new DataOutputStream(new FileOutputStream(output)) ;
	
	if (header) 
	    {
		int sizeHeader = 44 ;
		//WRITE HEADER
		data.write("RIFF".getBytes()) ; //4
		this.writeInt(data, (int)sizeHeader - 8 + signal.length*2) ; //8
		data.write("WAVE".getBytes()); //12
		data.write("fmt ".getBytes()); //16
		this.writeInt(data, (int)16); //20
		this.writeShort(data, (short)1) ; //22 PCM
		this.writeShort(data, (short)1) ; //24 NUMCHANEL CANAUX 1=MONO
		this.writeInt(data, (int)this.getSamplingFrequency()) ; //28 SAMPLERATE (FREQUENCE)
		this.writeInt(data, (int)(this.getSamplingFrequency()*2)) ; //32 BYTERATE nb octets par seconde (=SAMPLERATE*NUMCHANEL*BITPERSAMPLE/8)
		this.writeShort(data, (short)2) ; //34 BLOCKALIGN NB octet by frame (=NUMCHANEL * BITPERSAMPLE/8)
		this.writeShort(data, (short)16) ; //36 BITEPERSAMPLE nb octet par sample
		data.write("data".getBytes()); //40
		this.writeInt(data, (int)this.getSignalLength()*2) ; //44
	    }
	
	for (int i = 0 ; i < this.getSignalLength() ; i++)
	    this.writeShort(data, signal[i]) ;
	
	data.close() ;
    }
    
    
    public double[] signalEnergy(int m, int N){
      	//System.out.println("m :"+m+" N: "+N);
      	double[] res = new double[((signal.length-N)/m)+1];
      	//System.out.println(res.length);
      	int j = 0;
      	for(int i = 0; i < this.getSignalLength()-N; i+=m){
			res[j] = energy(i,N);
			j++;
    	}
      return res;
    }
    
    public double energy(int instant_echt, int N){
	    double res;
	    double som = 0;
	    for(int i = 0; i < N; i++){
			som += (double)this.signal[instant_echt+i]*this.signal[instant_echt+i];
	    }

      	res = som/(N+1);
      	if(res < 10e-6){
      		return 0;
      	}else{
      		return 10*Math.log10(res);
  		}
    }
    
    
    public double[] signalZeroRate(int m, int N){
      	double[] res = new double[((signal.length-N)/m)+1];
      	int j = 0;
      	for(int i = 0; i < this.getSignalLength()-N; i+=m){
			res[j] = zeroRate(i,N);
			j++;
      	}
      	return res;
    }
    
    public double zeroRate(int instant_echt, int N){
      	double res;
      	double som = 0;
      	for(int i = 1; i <= N; i++){
			som += Math.abs(Math.signum(this.signal[instant_echt+i]) - Math.signum(this.signal[instant_echt+i-1]));
      	}

      	res = som/(2*N);
      	return res;
    }

    public double[][] signalCoefficientAutocorrelation(int m, int M, int N){
    	double[][] res = new double[((signal.length-N)/m)+1][M];
      	int j = 0;
      	for(int i = 0; i < this.getSignalLength()-N; i+=m){
			res[j] = coeffAutocorrelation(i, M, N);
			j++;
      	}
      	return res;
    }

    public double[] coeffAutocorrelation(int instant_echt, int M, int N){
    	double[] fonctionAutoco = new double[M];
    	double[] coeffAutoco = new double[M];
    	double som = 0;
    	for(int l = 0; l < M; l++){
    		for(int k = 0; k < N-l-1; k++){
    			som += this.signal[instant_echt+k]*this.signal[instant_echt+k+l];
    		}
    		fonctionAutoco[l] = som/(N-l);
    		coeffAutoco[l] = fonctionAutoco[l]/fonctionAutoco[0];
    	}

    	return coeffAutoco;
    }
    
    
    public static void main(String[] args) {
	
	if (args.length != 1) {
	    System.out.println("Usage : SoundSignal <file.wav>");
	    System.exit(-1);
	}
	try {
	    SoundSignal signal = new SoundSignal();
	    
	    signal.setSignal(args[0]);
	    
	    System.out.println("Length of the signal (in sample) : "+ signal.getSignalLength());
	    System.out.println("Sampling frequency : " + signal.getSamplingFrequency());
	    System.out.println("Length of the signal (in ms) : "+ signal.getSignalLength() * 1000 / signal.getSamplingFrequency());
	    //Test d'ecriture d'un signal dans un fichier wav			
	    signal.exportSignal("essai_output.wav", true);
	    System.out.println("The file essai_output.wav has been created");
	    
	    //Test d'un deuxieme signal par recopie du premier
	    SoundSignal signal2 = new SoundSignal();
	    signal2.setSignal(signal.getSignal(), signal.getSamplingFrequency());
	    signal2.exportSignal("essai_output2.wav", true);
	    System.out.println("The file essai_output2.wav has been created");
	    
	    //System.out.println(signal.getSignalLength());
	    
	    // Test signalEnergy
	    /*double[] res = signal.signalEnergy(8*signal.getSamplingFrequency()/1000, 32*signal.getSamplingFrequency()/1000);
	    for(int i = 0; i < res.length; i++){
	      	System.out.println((int)res[i]);
	    }*/

	    // Test signalZeroRate
	    /*double[] res = signal.signalZeroRate(8*signal.getSamplingFrequency()/1000, 32*signal.getSamplingFrequency()/1000);
	    for(int i = 0; i < res.length; i++){
	    	// Afficher en % car on a des valeurs entre 0 et 1
	      	System.out.println((int)(res[i]*100));
	    }*/

	    // Test signalAutoCorrelation
	    double[][] res = signal.signalCoefficientAutocorrelation(8*signal.getSamplingFrequency()/1000, 32*signal.getSamplingFrequency()/1000, 32*signal.getSamplingFrequency()/1000);
	    for(int i = 0; i < res.length; i++){
	    	for(int j = 0; j < res[i].length; j++){
	    		System.out.println(res[i][j]);
	    	}
	    }
	    
	} catch (IOException e) {
	    System.err.println("error cannot read file <" + args[0] + ">");
	    
	} catch (UnsupportedAudioFileException e) {
	    System.err.println("error bad sound file <" + args[0] + ">");
	}
    }
    
}
