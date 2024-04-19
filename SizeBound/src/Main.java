import java.util.*;
import java.io.*;
public class Main {
    public static void main(String[] args) {
        HashMap<Integer, HashSet<Integer>> graph = new HashMap<Integer, HashSet<Integer>>();
        try {
            BufferedReader fr = new BufferedReader(new FileReader("C:\\Users\\刘凯乐\\Desktop\\facebook.txt"));
            String str;

            // 读取数据
            while((str = fr.readLine()) != null) {
                String []strSplit = str.split("\t");
                int value1 = Integer.parseInt(strSplit[0]);
                int value2 = Integer.parseInt(strSplit[1]);


                if(!graph.containsKey(value1)) {
                    graph.put(value1, new HashSet<Integer>());
                }
                if(!graph.containsKey(value2)) {
                    graph.put(value2, new HashSet<Integer>());
                }
                graph.get(value1).add(value2);
                graph.get(value2).add(value1);
            }
            fr.close();
        }catch(Exception e) {
            e.printStackTrace();
        }

        System.out.printf("please input q,l,h:\n");
        Scanner in=new Scanner(System.in);
        int q=in.nextInt();
        int l=in.nextInt();
        int h= in.nextInt();
        long startTime = System.currentTimeMillis();
        BRB Brb = new BRB(graph, l,h);
        Brb.query(q);
        long endTime = System.currentTimeMillis();
        System.out.println(Brb.getH());
        System.out.println(Brb.getK());
        System.out.println("Total Time :"+(endTime-startTime)+"ms");
    }
}