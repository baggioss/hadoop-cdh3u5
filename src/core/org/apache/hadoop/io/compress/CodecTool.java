package org.apache.hadoop.io.compress;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.util.ReflectionUtils;

public class CodecTool {

  /**
   * @param args
   * @throws IOException 
   */
  public static void main(String[] args) throws IOException {
    if (args.length < 6) {
      System.err.println("usage: CodecTool <codec> <-c|-x> <bufsize bytes> <bytes write once> <input file> <output file>");
      System.err.println("       -c for compress, -x for decompress, size 0 for default");
      System.exit(1);
    }

    String codecClass = args[0];
    String mode = args[1];
    String bufsize = args[2];
    String writesize = args[3];
    String inputPath = args[4];
    String outputPath = args[5];

    int size = 0;
    try {
      size = Integer.parseInt(bufsize);
    } catch (Exception e) {
      // nothing
    }
    if (size <= 0) {
      System.err.println("WARN: use default buffer size 4096 bytes");
      size = 4096;
    }

    byte[] buf = new byte[size];

    int writeonce = 0;
    try {
      writeonce = Integer.parseInt(writesize);
    } catch(Exception e) {
      // nothing
    }

    if (writeonce < 0) {
      System.err.println("WARN: use default to write all read bytes");
      writeonce = 0;
    }

    int n = 0;
    
    long start = System.currentTimeMillis();
    Configuration conf = new Configuration();
    CompressionCodec codec = null;
    
    try {
      codec = (CompressionCodec)
      ReflectionUtils.newInstance(conf.getClassByName(codecClass), conf);
    } catch (ClassNotFoundException cnfe) {
      throw new IOException("Illegal codec : " + codecClass);
    }
     
    FileInputStream fin = null;
    DataInputStream in = null;
    DataOutputStream out = null; 

    if ("-x".equals(mode)) { 
      in = new DataInputStream(codec.createInputStream(new FileInputStream(inputPath)));
      out = new DataOutputStream(new FileOutputStream(outputPath));
    } else if("-c".equals(mode)) {
      in = new DataInputStream(new FileInputStream(inputPath));
      out = new DataOutputStream(codec.createOutputStream(new FileOutputStream(outputPath)));
    } else {
      System.err.println("Illegal mode : " + mode);
    }

    if (in != null && out !=null) {
      while((n = in.read(buf, 0, size)) > 0){
        //out.write(buf, 0, n);
        int off = 0;
        while(n > 0){
          if(writeonce == 0){
            out.write(buf, 0, n);
            n = 0;
          }else if(writeonce <= n){
            out.write(buf, off, writeonce);
            n -= writeonce;
            off += writeonce;
          }else if(writeonce > n){
            out.write(buf, off, n);
            n = 0;
          }
        }
      }
      out.close();
      in.close();
      System.err.println("OK! used " + (System.currentTimeMillis() - start) + " ms");
    }
  }

}

