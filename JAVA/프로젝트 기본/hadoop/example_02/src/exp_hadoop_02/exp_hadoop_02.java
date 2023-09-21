package exp_hadoop_02;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;


public class exp_hadoop_02
{
	public static void main( String[] args ) throws Exception
	{
		System.out.println( "exp_hadoop_02 start" );
		
        Configuration conf = new Configuration();
        
        if( args.length != 2 )
        {
            System.out.println( "Usage: exp_hadoop_02 <input> <output>" );
            
            System.exit( 2 );
        }
        
        System.out.println( "exp_hadoop_02 FileSystem.get" );

        FileSystem hdfs = FileSystem.get( conf );        

        Path path = new Path( args[ 0 ] );
        
        if( hdfs.exists( path ) )
        {
            hdfs.delete( path, true );
        }
        
        System.out.println( "exp_hadoop_02 hdfs.create" );
        
        FSDataOutputStream outStream = hdfs.create( path );

        for( int i = 0; i < 10000; i++ )
        {
        	outStream.writeUTF( new String( "apple orange strawberry banana\n" ) );
        	
        	outStream.writeUTF( new String( "apple strawberry graph\n" ) );
        }
        
        System.out.println( "exp_hadoop_02 hdfs.close" );
        
        outStream.close();        
        
        Job job = new Job( conf, "WordCount" );
        
        job.setJarByClass  ( exp_hadoop_02.class    );
        job.setMapperClass ( WordCountMapper.class  );
        job.setReducerClass( WordCountReducer.class );

        job.setInputFormatClass ( TextInputFormat.class  );
        job.setOutputFormatClass( TextOutputFormat.class );

        job.setOutputKeyClass  ( Text.class        );
        job.setOutputValueClass( IntWritable.class );

        FileInputFormat.addInputPath  ( job, new Path( args[ 0 ] ) );
        FileOutputFormat.setOutputPath( job, new Path( args[ 1 ] ) );
        
        job.waitForCompletion( true );
	}
}
