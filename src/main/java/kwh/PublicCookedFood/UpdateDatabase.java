package kwh.PublicCookedFood;

import java.io.FileReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class UpdateDatabase { //엑셀에서 이미지
    public static void main(String[] args) {
        // JSON 파일 경로
        String filePath = "C:\\Users\\yh245\\Downloads\\datas.json";

        // MySQL 연결 정보
        String url = "jdbc:mysql://localhost:3306/food";
        String user = "root";
        String password = "wnrmdakrpd145!";

        Connection conn = null;
        PreparedStatement updateStmt = null;
        PreparedStatement insertStmt = null;

        try {
            // MySQL 데이터베이스 연결
            conn = DriverManager.getConnection(url, user, password);

            // JSON 파일 읽기
            JSONTokener tokener = new JSONTokener(new FileReader(filePath));
            JSONArray jsonArray = new JSONArray(tokener);

            // UPDATE 쿼리
            String updateQuery = "UPDATE recipe_crse SET IMG_URL = ? WHERE RECIPE_ID = ? AND COOKING_NO = ?";
            updateStmt = conn.prepareStatement(updateQuery);

            // INSERT 쿼리
            String insertQuery = "INSERT INTO recipe_crse (RECIPE_ID, COOKING_NO, IMG_URL) VALUES (?, ?, ?)";
            insertStmt = conn.prepareStatement(insertQuery);

            // JSON 데이터 처리
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject item = jsonArray.getJSONObject(i);
                int recipeID = item.getInt("recipe_ID");
                int cookingNO = item.getInt("cooking_NO");
                String imgURL = item.getString("img_URL");

                // 업데이트
                updateStmt.setString(1, imgURL);
                updateStmt.setInt(2, recipeID);
                updateStmt.setInt(3, cookingNO);
                int rowsAffected = updateStmt.executeUpdate();

                // 만약 업데이트된 행이 없으면 삽입
                if (rowsAffected == 0) {
                    insertStmt.setInt(1, recipeID);
                    insertStmt.setInt(2, cookingNO);
                    insertStmt.setString(3, imgURL);
                    insertStmt.executeUpdate();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (updateStmt != null) updateStmt.close();
                if (insertStmt != null) insertStmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}