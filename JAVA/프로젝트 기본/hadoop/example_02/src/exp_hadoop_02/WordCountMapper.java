package exp_hadoop_02;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Mapper;


public class WordCountMapper extends Mapper< LongWritable, Text, Text, IntWritable >
{
    private final static IntWritable one = new IntWritable( 1 );
    private Text word = new Text();

    
    @Override
    protected void map( LongWritable key
    		          , Text value
    		          , Mapper< LongWritable, Text, Text, IntWritable >.Context context
    		          )
                      throws IOException, InterruptedException 
    {
    	//StringTokenizer itr = new StringTokenizer( value.toString() );
        
        StringTokenizer itr = new StringTokenizer( new String( value.toString().getBytes(), 2, value.toString().getBytes().length -2 ) );

        while( itr.hasMoreTokens() )
        {
            word.set( itr.nextToken() );
            context.write( word, one );
        }
    }
}
