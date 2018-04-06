package nearsoft.academy.bigdata.recommendation;

import org.apache.commons.lang.time.StopWatch;
import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;


public class MovieRecommender {
    private ArrayList<Double> scoresArray = new ArrayList<Double>();
    private ArrayList<String> usersArray = new ArrayList<String>();
    private ArrayList<String> productsArray = new ArrayList<String>();
    private Map<String,Integer> mapUsers = new HashMap<String,Integer>();
    private Map<String,Integer> mapProducts = new HashMap<String,Integer>();

    public MovieRecommender(String path) {
        this.readFile(path);
    }

    public void readFile(String path){
        String userKey = "review/userId: ";
        String productKey = "product/productId: ";
        String scoreKey = "review/score: ";

        InputStream fileStream = null;
        try {
            fileStream = new FileInputStream(path);
            InputStream gzipStream = new GZIPInputStream(fileStream);
            Reader decoder = new InputStreamReader(gzipStream, "UTF-8");
            BufferedReader buffered = new BufferedReader(decoder);

            String content;

            while ((content = buffered.readLine()) != null){
                if (content.contains(userKey)) {
                    String userId = content.split(userKey)[1];
                    usersArray.add(userId);
                }else if (content.contains(productKey)){
                    String product = content.split(productKey)[1];
                    productsArray.add(product);
                }else if(content.contains(scoreKey)){
                    String score = content.split(scoreKey)[1];
                    scoresArray.add(Double.valueOf(score));
                }
            }

            mapUsers = this.getMap(usersArray);
            mapProducts = this.getMap(productsArray);

            System.out.println("Total Users: " + String.valueOf(mapUsers.size()));
            System.out.println("Total Products: " + String.valueOf(mapProducts.size()));
            System.out.println("Total Reviews: " + String.valueOf(scoresArray.size()));

            this.writeCsvFile();

        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (UnsupportedEncodingException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }



    public void writeCsvFile(){
        FileWriter writer = null;

        try {
            writer = new FileWriter("movies.csv");

            for (int i = 0; i < this.usersArray.size()-1; i++) {
                Integer idUser = this.mapUsers.get(this.usersArray.get(i));
                writer.append(String.valueOf(idUser));
                writer.append(",");

                Integer idProduct = this.mapProducts.get(this.productsArray.get(i));
                writer.append(String.valueOf(idProduct));
                writer.append(",");

                writer.append(String.valueOf(this.scoresArray.get(i)));

                writer.append('\n');
            }

            System.out.println("CSV file created...");

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Map<String,Integer> getMap(ArrayList<String> list){
        Map<String,Integer> map = new HashMap<String,Integer>();
        int id = 1;
        for(String s:list){
            if(!map.containsKey(s)){
                map.put(s,id);
                id++;
            }
        }
        return map;
    }

    public int getTotalReviews(){
        return scoresArray.size();
    }

    public int getTotalProducts(){
        return mapProducts.size();
    }

    public int getTotalUsers(){
        return mapUsers.size();
    }


    public List<String> getRecommendationsForUser(String userId){
        System.out.println("Getting recommendations");
        List<String> foundRecommendations = new ArrayList<String>();
        List<RecommendedItem> recommendations = null;

        try{
            DataModel model = new FileDataModel(new File("movies.csv"));
            UserSimilarity similarity = new PearsonCorrelationSimilarity(model);
            UserNeighborhood neighborhood = new ThresholdUserNeighborhood(0.1, similarity, model);
            UserBasedRecommender recommended = new GenericUserBasedRecommender(model, neighborhood, similarity);
            recommendations = recommended.recommend(mapUsers.get(userId), 3);
        }catch(IOException e){
            e.printStackTrace();
        } catch(TasteException e){
            e.printStackTrace();
        }catch(Exception e){
            e.printStackTrace();
        }

        for (RecommendedItem recommendation : recommendations) {
            String r = getKeyFromProductMap((int) recommendation.getItemID());
            foundRecommendations.add(r);
        }

        return foundRecommendations;
    }

    public String getKeyFromProductMap(int value){
        for(String key: mapProducts.keySet()){
            if(mapProducts.get(key)==value)
                return key;
        }
        return null;
    }


}
