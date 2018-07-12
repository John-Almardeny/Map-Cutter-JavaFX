import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import javafx.scene.Cursor;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;


/**
 * Map Cutter Class for geo-referencing map image
 * @author Yahya Almardeny
 * All Rights Reserved
 * @version 27/03/2017
 */
public class MapCutter extends Application{
	
	Canvas c;
	GraphicsContext gc;
	ImageView map;
	Scene scene;
	Map<String, String> regions = new LinkedHashMap<>();
	Map<String, Double> moveX = new LinkedHashMap<>();
	Map<String, Double> moveY = new LinkedHashMap<>();
	Boolean dragging = false, validImg=false, validSize=false;
	String path="", dir, regionName;
	int totalRegions=0;
	double targetSceneHeight=0, targetSceneWidth=0,
			sceneHeight=0, sceneWidth=0, xOffset=0,yOffset=0;
	

	@Override
	public void start(Stage primaryStage) {
		mainMenu(primaryStage); // main menu and entry point to the entire program
	}
	
	
	public static void main (String[] args){
		launch(args);
    }

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	/**
	 * main menu and entry point
	 * for the entire program
	 * @param primaryStage
	 */
	private void mainMenu(Stage primaryStage){
		StackPane root = new StackPane();
		TextField targetWidth, targetHeight,
				  sessionSceneWidth, sessionSceneHeight;
		Button browse = new Button();
		Button go = new Button();
		try {
			root = (StackPane)FXMLLoader.load(MapCutter.class.getResource("MapCutterOpeningScreen.fxml"));
		} catch (Exception e) {
			e.printStackTrace();
		}	
		((StackPane)root.lookup("#root")).getChildren().add(0, 
						new ImageView(new Image(MapCutter.this.getClass().getResourceAsStream("bg.jpg"))));
		targetWidth = (TextField)root.lookup("#width");
		targetHeight = (TextField)root.lookup("#height");
		sessionSceneWidth = (TextField)root.lookup("#sessionWidth");
		sessionSceneHeight = (TextField)root.lookup("#sessionHeight");
		browse = (Button)root.lookup("#browse");
		go = (Button)root.lookup("#go");
		
		browse.setOnAction(e->{
			FileChooser chooser = new FileChooser();
			FileChooser.ExtensionFilter imgFilter = new FileChooser.ExtensionFilter("Image files", "*.png", "*.jpg", "*.gif");
			chooser.getExtensionFilters().add(imgFilter);
		    chooser.setTitle("Choose Map Image");
		    File file = chooser.showOpenDialog(new Stage());
		    if (file!=null){dir = "file:"+file.getPath(); validImg=true;}
		});
		go.setOnAction(e->{
			if(validSize(sessionSceneWidth.getText())&&validSize(sessionSceneHeight.getText())
						&&validSize(targetWidth.getText())&&validSize(targetHeight.getText())){
				sceneWidth = Double.parseDouble(sessionSceneWidth.getText());
				sceneHeight = Double.parseDouble(sessionSceneHeight.getText());
				targetSceneWidth = Double.parseDouble(targetWidth.getText());
				targetSceneHeight = Double.parseDouble(targetHeight.getText());
				validSize=true;
			}
			Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
			if (validImg&&validSize&&sceneWidth<=primaryScreenBounds.getWidth()&&sceneHeight<=primaryScreenBounds.getHeight()){
				primaryStage.close();
				//upload image
				Image img = new Image(dir);
				map= new ImageView(img);
				//create application window
				//primaryStage= new Stage();
				StackPane root1 = new StackPane();
				selectRegion(primaryStage);
				root1.getChildren().addAll(map,c);
				scene = new Scene(root1,sceneWidth,sceneHeight);
				c.widthProperty().bind(scene.widthProperty());
				c.heightProperty().bind(scene.heightProperty());
				map.fitWidthProperty().bind(scene.widthProperty()); 
			    map.fitHeightProperty().bind(scene.heightProperty());
			    primaryStage.setScene(scene);
			    primaryStage.setResizable(false);
			    primaryStage.sizeToScene(); // because of a bug in javaFx caused by setResizable(false)
				cutRegion();
				moveScreen(primaryStage);
				primaryStage.setTitle("Cutting Map");;
				primaryStage.show();	
			}
			
		});
		Scene s = new Scene(root,600,400);
		primaryStage.setScene(s);
		primaryStage.setTitle("Initialization");
		primaryStage.getIcons().add(new Image(MapCutter.class.getResourceAsStream("/icon.png")));
		primaryStage.setResizable(false);
		primaryStage.show();
		
	}
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * create canvas with listener 
	 * to capture the region selection
	 * @param primaryStage
	 */
	private void selectRegion(Stage primaryStage){
		c=new Canvas();
		gc=c.getGraphicsContext2D();
		c.setOnMouseDragged(e -> {
			if(!dragging){
				primaryStage.getScene().setCursor(Cursor.HAND);
				gc.setFill(Color.PURPLE);
				gc.fillRect(e.getX(), e.getY(), 3,3);
				path += e.getX() + " " + e.getY()+",";//capture the SVGPath content					
			}
		});		
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	/**
	 * listener to the scene
	 * press Enter after selecting region to accept
	 * or press escape to cancel
	 */
	private void cutRegion(){
		scene.addEventHandler(KeyEvent.KEY_PRESSED, ev -> {
	        if (ev.getCode() == KeyCode.ENTER) {
	        	if(!path.isEmpty()){
	        		if(fillRegionName(path)){
	        			String correctedPath = correctedPath("path");
		        		regions.put(regionName, "M"+correctedPath); // record region name and correctedPath
		        		moveX.put("moveX"+totalRegions, xAverageToScene(correctedPath, targetSceneWidth));
		        		moveY.put("moveY"+totalRegions, yAverageToScene(correctedPath, targetSceneHeight));
		        		totalRegions++;
	        		}
	        		path = new String();
	        	}
	        	ev.consume();
	        }
	        
	        else if (ev.getCode() == KeyCode.ESCAPE) {
	        	if(!path.isEmpty()){
	        		clearSelection(path);
	        	}
	        	path = new String();
	        	ev.consume();
	        }
	    });
	}
	
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * dialog to get the selected region name
	 * and validate it
	 * @param path
	 * @return boolean
	 */
	private Boolean fillRegionName(String path){
		TextInputDialog dialog = new TextInputDialog("region"+totalRegions);
		dialog.setTitle("Region Name");
		dialog.setHeaderText("Insert a name for this region");
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent() && !result.get().isEmpty() && !result.get().equalsIgnoreCase("Regions")/*because it's the class name*/
				&& validRegionName(result.get())){ 
			regionName = result.get().replace(" ", "");
			gc.setStroke(Color.RED);
			gc.strokeText(regionNameInput(regionName),
					xAverageToScene(path,sceneWidth)*sceneWidth, yAverageToScene(path,sceneHeight)*sceneHeight);
			return true;
		}
		else {clearSelection(path); return false;}
	}
	
	
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * validate the region name input
	 * and return it if it's valid
	 * or generate a new one if it's not valid
	 * @param text
	 * @return rName
	 */
	private String regionNameInput(String text) {
		if(text.length()==1){return text.toUpperCase();}
		
		return  (text.substring(0, 1)+regionName.substring(
	        			  text.length()-1, regionName.length())).toUpperCase(); 
	}
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * 
	 * @param text
	 * @return boolean
	 */
	private boolean validRegionName(String text){
		for (int i=0; i<text.length(); i++){
			char c = text.charAt(i);
			if(i==0 && !Character.isAlphabetic(c)){
				return false;
			}
			if ((!Character.isAlphabetic(c)&&!Character.isDigit(c))){
				return false;
			}	
		}
		if (regions.containsKey(text.replace(" ", ""))){
			return false;
		}
	    return true;
	}
	
	


	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	/**
	 * clear last selection
	 * @param path
	 */
	private void clearSelection(String path){
		double []xs = xCoordinates(path);
		double []ys = yCoordinates(path);
		int length = xs.length;
		for (int i=0;i<length;i++){
			gc.clearRect(xs[i], ys[i], 3, 3);
		}
		
	}
	
	

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	/**
	 * move the screen when control key
	 * is pressed + mouse drag
	 */
	private void moveScreen(Stage primaryStage){
		scene.setOnKeyPressed(ke -> {
	        if (ke.getCode() == KeyCode.CONTROL) {
	        	dragging=true;
	        	scene.setOnMousePressed(event -> {
		            xOffset = (primaryStage.getX() - event.getScreenX());
		            yOffset = (primaryStage.getY() - event.getScreenY());
		            primaryStage.getScene().setCursor(Cursor.OPEN_HAND);
		        });
	        	scene.setOnMouseDragged(event -> {
		            primaryStage.setX(event.getScreenX() + xOffset);
		            primaryStage.setY(event.getScreenY() + yOffset);
		            primaryStage.getScene().setCursor(Cursor.CLOSED_HAND);
		        });
	        	ke.consume();
	        }});
		
		scene.setOnKeyReleased(ke -> {
			dragging=false;
			scene.setOnMousePressed(event ->{});
        	scene.setOnMouseDragged(event -> {});
        	ke.consume();
		});	        
	}
	

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	/**
	 * to validate the text input 
	 * in the main menu for the scenes sizes
	 * @param size
	 * @return boolean
	 */
	private boolean validSize(String size){
		if (!size.isEmpty()){ // if there is an input
			for(int i = 0; i < size.length(); i++){ // check for any non-digit
				char c = size.charAt(i);
		        if((c<48 || c>57)){ // if any non-digit found (ASCII chars values for [0-9] are [48-57]
		            return false;
		        }
		    }
			return true;
		}
		return false;
	}
	
	

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	

	/**
	 * find the average of the X's points
	 * and return its ratio to the scene width
	 * @param path
	 * @param sceneW
	 * @return avgX/sceneW
	 */
	private double xAverageToScene(String path, double sceneW){
		double[] xCoords = xCoordinates(path);
		double avgX = 0;
		for(double x : xCoords){
			avgX+=x;
		}
		avgX/=xCoords.length;
		return Double.parseDouble(new DecimalFormat("##.###").format(avgX/sceneW));
	}
	

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	/**
	 * find the average of the Y's points
	 * and return its ratio to the scene height
	 * @param path
	 * @param sceneH
	 * @return avgY/sceneH
	 */
	private double yAverageToScene(String path, double sceneH){
		double[] yCoords = yCoordinates(path);
		double avgY = 0;
		for(double y : yCoords){
			avgY+=y;
		}
		avgY/=yCoords.length;
		return Double.parseDouble(new DecimalFormat("##.###").format(avgY/sceneH));
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	 
	
	/**
	 * create any array of doubles from a given 
	 * SVGPath content string, containing the X's points
	 * @param path
	 * @return xCoord
	 */
	private double[] xCoordinates(String path){
		String[] coord = path.split(",");
		double [] xCoord = new double[coord.length];
		for(int i=0;i<coord.length;i++){
			String[] temp = coord[i].split(" ");
			xCoord[i] = Double.parseDouble(temp[0]);	
		}
	  return xCoord;
	}
	

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	/**
	 * create any array of doubles from a given 
	 * SVGPath content string, containing the Y's points
	 * @param path
	 * @return yxCoord
	 */
	private double[] yCoordinates(String path){
		String[] coord = path.split(",");
		double[] yCoord = new double[coord.length];;
		for(int i=0;i<coord.length;i++){
			String[] temp = coord[i].split(" ");
			yCoord[i] = Double.parseDouble(temp[1]);		
		}
	  return yCoord;
	}
	

	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	/**
	 * to calculate the displacement factor
	 * for X's and Y'x points in the SVGPath content
	 * @param Path
	 * @return correctedPath
	 */
	private String correctedPath(String Path){
		String correctedPath = "";
		double[] xCoords = xCoordinates(path);
		double[] yCoords = yCoordinates(path);
		double difX = targetSceneWidth/sceneWidth;
		double difY = targetSceneHeight/sceneHeight;
		for (int i=0; i<xCoords.length;i++){
			xCoords[i]*=difX;
			yCoords[i]*=difY;
			correctedPath+= xCoords[i]+" "+yCoords[i]+",";
		}
		correctedPath = correctedPath.substring(0, correctedPath.length()-1);
		return correctedPath;
	}
	


	
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	
	
	
	
	/**
	 * when user close the program 
	 * create the Regions Class in a java file
	 * in the directory selected by the user
	 */
	public void stop(){
		
	  if (sceneWidth!=0){// user did not close the program from the main menu
		DirectoryChooser dc = new DirectoryChooser();
		File file = dc.showDialog(null);
        if(file != null){
			PrintWriter out = null;
			File f = null;
			try {
				f = new File(file.getAbsolutePath()+ "/Regions.java");
				if(f.exists()){f.delete();}	
				out = new PrintWriter(f);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			
			
			
			
			
			out.append("import java.util.ArrayList;" +System.getProperty("line.separator")  
									+"import java.util.Collections;" +System.getProperty("line.separator") 
									+ "import javafx.animation.PauseTransition;" +System.getProperty("line.separator") 
									+ "import javafx.scene.Scene;" +System.getProperty("line.separator")
									+ "import javafx.scene.image.Image;" +System.getProperty("line.separator")
									+ "import javafx.scene.image.ImageView;" +System.getProperty("line.separator")
									+ "import javafx.scene.layout.StackPane;" +System.getProperty("line.separator")
									+ "import javafx.scene.paint.ImagePattern;" +System.getProperty("line.separator")
									+ "import javafx.scene.shape.Circle;" +System.getProperty("line.separator")
									+ "import javafx.scene.text.Font;" +System.getProperty("line.separator")
									+ "import javafx.scene.text.Text;" +System.getProperty("line.separator")
									+ "import javafx.scene.text.TextBoundsType;" +System.getProperty("line.separator")
									+ "import javafx.stage.Stage;" +System.getProperty("line.separator")
									+ "import javafx.scene.paint.Color;" +System.getProperty("line.separator") 
									+ "import javafx.scene.shape.SVGPath;" +System.getProperty("line.separator") 
									+ "import javafx.util.Duration;\n" +System.getProperty("line.separator") + System.getProperty("line.separator")

									+ "/**" + System.getProperty("line.separator") + "* Regions Class for geo-referencing map image" + System.getProperty("line.separator")
									+ "* @author  " + System.getProperty("line.separator")
									+ "* @version " + new SimpleDateFormat("dd/MM/yyyy").format(new Date()) + System.getProperty("line.separator")
									+ "* @credit Yahya Almardeny " + System.getProperty("line.separator")
									+  "*/" + System.getProperty("line.separator")
									+ "public class Regions {" + 	System.getProperty("line.separator") +System.getProperty("line.separator")
									+ "public static Scene scene;"+ System.getProperty("line.separator")
									+ "public static double sceneWidth=" + targetSceneWidth +", sceneHeight=" + targetSceneHeight+";" +System.getProperty("line.separator")
									+ "public static StackPane root;"+ System.getProperty("line.separator")
									+ "public static SVGPath ");
			//SVGPath
			int counter =0;
			for (String key: regions.keySet()){
				out.append(key);
				if(counter!=totalRegions-1){	
					out.append(", ");
				}
				else {out.append(";" +System.getProperty("line.separator"));}
				counter++;
			}
			
			//DeltasX
			out.append("private double ");
			for (int i=0; i<totalRegions; i++){
				out.append("deltaX"+i +"=0.0");
				if(i!=totalRegions-1){	
					out.append(", ");
				}
				else {out.append(";" +System.getProperty("line.separator"));}
			}
			//DeltasY
			out.append("private double ");
			for (int i=0; i<totalRegions; i++){
				out.append("deltaY"+i+"=0.0");
				if(i!=totalRegions-1){	
					out.append(", ");
				}
				else {out.append(";"+System.getProperty("line.separator"));}
			}
			//MoveXs
			out.append("private double ");
			counter=0;
			for (String key: moveX.keySet()){
				out.append(key +"=" + moveX.get(key));
				if(counter!=totalRegions-1){	
					out.append(", ");
				}
				else {out.append(";" +System.getProperty("line.separator"));}
				counter++;
			}
			//MoveYs
			out.append("private double ");
			counter=0;
			for (String key: moveY.keySet()){
				out.append(key +"=" + moveY.get(key));
				if(counter!=totalRegions-1){	
					out.append(", ");
				}
				else {out.append(";" +System.getProperty("line.separator")+System.getProperty("line.separator"));}
				counter++;
			}
			
			//Constructor
			out.append("public Regions(Stage primaryStage, Image map){" + System.getProperty("line.separator") +System.getProperty("line.separator")
						+ "\tscene = primaryStage.getScene();" + System.getProperty("line.separator"));
			
			for(String key: regions.keySet()){
				out.append("\t"+key + "= new SVGPath();" +System.getProperty("line.separator"));
				out.append("\t"+key + ".setContent(\"" + regions.get(key) + "\");" +System.getProperty("line.separator"));
				out.append("\t"+key+".setOpacity(0.0);" +System.getProperty("line.separator"));
				out.append("\t"+key+".setManaged(false);" +System.getProperty("line.separator"));
				out.append("\t"+key + ".scaleXProperty().bind(scene.widthProperty().divide(sceneWidth));" +System.getProperty("line.separator"));
				out.append("\t"+key + ".scaleYProperty().bind(scene.heightProperty().divide(sceneHeight));" +System.getProperty("line.separator"));
				
			}
			
			out.append("\tImageView imageview = new ImageView(map);" + System.getProperty("line.separator")
						+ "\timageview.fitWidthProperty().bind(scene.widthProperty());" + System.getProperty("line.separator")
						+ "\timageview.fitHeightProperty().bind(scene.heightProperty());" + System.getProperty("line.separator")
					    + "\troot = new StackPane();" + System.getProperty("line.separator")
					    + "\troot.getChildren().addAll(imageview, ");
			counter =0;
			for(String key: regions.keySet()){
				out.append(key);
				if(counter!=totalRegions-1){
					out.append(", ");
				}
				counter++;
			}
			out.append(");" + System.getProperty("line.separator"));
			
			out.append("\tscene.setRoot(root);" + System.getProperty("line.separator") 
						+ "\tprimaryStage.setMinWidth(sceneWidth/2);" + System.getProperty("line.separator")
						+ "\tprimaryStage.setMinHeight(sceneHeight/2);"+ System.getProperty("line.separator"));
						
			out.append("\tscene.widthProperty().addListener( (e) -> {" +System.getProperty("line.separator")
					+ "\t\t double dif = scene.getWidth()-sceneWidth;" +System.getProperty("line.separator")
					+ "\t\t for(int x=0;x<Math.abs(dif); x++){" +System.getProperty("line.separator")
					+ "\t\t if (dif>=0){" +System.getProperty("line.separator"));
			for (int i=0;i<totalRegions;i++){
				out.append("\t\t deltaX" +i+"+=moveX"+i+";" +System.getProperty("line.separator"));
			}
			out.append("\t\t }" +System.getProperty("line.separator") + "\t\t else {" +System.getProperty("line.separator"));			
			for (int i=0;i<totalRegions;i++){
				out.append("\t\t deltaX" +i+"-=moveX"+i+";" +System.getProperty("line.separator"));
			}
			out.append("\t\t }}" +System.getProperty("line.separator"));
			counter =0;
			for(String key: regions.keySet()){
				out.append("\t\t " +key + ".setTranslateX(deltaX"+counter+");" +System.getProperty("line.separator"));
				counter++;
			}
			out.append("\t\t sceneWidth = scene.getWidth();" +System.getProperty("line.separator")+ "\t});" +System.getProperty("line.separator"));
			
			
			 
			out.append("\tscene.heightProperty().addListener( (e) -> {" +System.getProperty("line.separator")
					+ "\t\t double dif = scene.getHeight()-sceneHeight;" +System.getProperty("line.separator")
					+ "\t\t for(int x=0;x<Math.abs(dif); x++){" +System.getProperty("line.separator")
					+ "\t\t if (dif>=0){" +System.getProperty("line.separator"));
			for (int i=0;i<totalRegions;i++){
				out.append("\t\t deltaY" +i+"+=moveY"+i+";" +System.getProperty("line.separator"));
			}
			out.append("\t\t }" +System.getProperty("line.separator") + "\t\t else {" +System.getProperty("line.separator"));			
			for (int i=0;i<totalRegions;i++){
				out.append("\t\t deltaY" +i+"-=moveY"+i+";" +System.getProperty("line.separator"));
			}
			out.append("\t\t }}" +System.getProperty("line.separator"));
			counter =0;
			for(String key: regions.keySet()){
				out.append("\t\t "+key + ".setTranslateY(deltaY"+counter+");" +System.getProperty("line.separator"));
				counter++;
			}
			out.append("\t\t sceneHeight = scene.getHeight();" +System.getProperty("line.separator") + "\t});" +System.getProperty("line.separator") + 
					"}" +System.getProperty("line.separator"));
			
			// Static Methods
			out.append("static void flashRegion (SVGPath region, Color color, double time){" +System.getProperty("line.separator")+
							"\t region.setFill(color);" +System.getProperty("line.separator") + "\t region.setOpacity(1.0);" +System.getProperty("line.separator") + 
					"\t PauseTransition delay = new PauseTransition(Duration.seconds(time));" +System.getProperty("line.separator") + 
							"\t delay.setOnFinished(event -> {region.setOpacity(0.0);});" +System.getProperty("line.separator") + "\t delay.play();" 
							+System.getProperty("line.separator") 
							+ "}" +System.getProperty("line.separator")+System.getProperty("line.separator"));
			
			out.append("static void colorRegion (SVGPath region, Color color){" +System.getProperty("line.separator") + 
					"\t region.setFill(color);" +System.getProperty("line.separator") + 
							"\t region.setOpacity(1.0);" +System.getProperty("line.separator") + 
							"}"+System.getProperty("line.separator")+System.getProperty("line.separator"));
		    
			out.append("static void fillImage(SVGPath region, Image img){" +System.getProperty("line.separator")
						+ "\tregion.setFill(new ImagePattern(img));" +System.getProperty("line.separator")
						+ "\tregion.setOpacity(1.0);" + System.getProperty("line.separator")
						+ "}" + System.getProperty("line.separator") +System.getProperty("line.separator"));
			
			out.append("static ImageView marker(SVGPath region, Image img, double imgWidth, double imgHeight){" +System.getProperty("line.separator")
						+"\tdouble[] coords = regionCenterCoords(region);" +System.getProperty("line.separator")
						+"\tImageView iv = new ImageView(img);" +System.getProperty("line.separator")
						+"\tiv.setManaged(false);" +System.getProperty("line.separator")
						+"\tiv.setFitHeight(imgHeight);" +System.getProperty("line.separator")
						+"\tiv.setFitWidth( imgWidth);" +System.getProperty("line.separator")
						+"\tiv.translateXProperty().bind(scene.widthProperty().divide(scene.getWidth()/coords[0]));" +System.getProperty("line.separator")
						+"\tiv.translateYProperty().bind(scene.heightProperty().divide(scene.getHeight()/coords[1]));" +System.getProperty("line.separator")
						+"\tiv.scaleXProperty().bind(scene.widthProperty().divide(scene.getWidth()));" +System.getProperty("line.separator")
						+"\tiv.scaleYProperty().bind(scene.heightProperty().divide(scene.getHeight()));" +System.getProperty("line.separator") 
						+"\treturn iv;" +System.getProperty("line.separator")
						+"}" +System.getProperty("line.separator")+System.getProperty("line.separator"));
			
			out.append("static ImageView marker(SVGPath region, Image img, double imgWidth, double imgHeight, double x, double y){" +System.getProperty("line.separator")
						+"\tImageView iv = new ImageView(img);" +System.getProperty("line.separator")
						+"\tiv.setManaged(false);" +System.getProperty("line.separator")
						+"\tiv.setFitHeight(imgHeight);" +System.getProperty("line.separator")
						+"\tiv.setFitWidth( imgWidth);" +System.getProperty("line.separator")
						+"\tiv.translateXProperty().bind(scene.widthProperty().divide(scene.getWidth()/x));" +System.getProperty("line.separator")
						+"\tiv.translateYProperty().bind(scene.heightProperty().divide(scene.getHeight()/y));" +System.getProperty("line.separator")
						+"\tiv.scaleXProperty().bind(scene.widthProperty().divide(scene.getWidth()));" +System.getProperty("line.separator") 
						+"\tiv.scaleYProperty().bind(scene.heightProperty().divide(scene.getHeight()));" +System.getProperty("line.separator") 
						+"\treturn iv;" +System.getProperty("line.separator")
						+ "}" +System.getProperty("line.separator")+System.getProperty("line.separator"));



			out.append("static Circle marker(SVGPath region, Color color, int size){" +System.getProperty("line.separator")
						+"\tdouble[] coords = regionCenterCoords(region);" +System.getProperty("line.separator")
						+"\tCircle marker = new Circle(size);" +System.getProperty("line.separator")
						+"\tmarker.setManaged(false);" +System.getProperty("line.separator")
						+"\tmarker.setFill(color);" +System.getProperty("line.separator")
						+"\tmarker.layoutXProperty().bind(scene.widthProperty().divide(scene.getWidth()/coords[0]));" +System.getProperty("line.separator")
						+"\tmarker.layoutYProperty().bind(scene.heightProperty().divide(scene.getHeight()/coords[1]));" +System.getProperty("line.separator")
						+"\tmarker.scaleXProperty().bind(scene.widthProperty().divide(scene.getWidth()));" +System.getProperty("line.separator") 
						+"\tmarker.scaleYProperty().bind(scene.heightProperty().divide(scene.getHeight()));" +System.getProperty("line.separator") 
						+"\treturn marker;" +System.getProperty("line.separator")
						+"}" +System.getProperty("line.separator")+System.getProperty("line.separator"));


			out.append("static Circle marker(Color color, int size, double x, double y){" +System.getProperty("line.separator")
						+"\tCircle marker = new Circle(size);" +System.getProperty("line.separator")
						+"\tmarker.setManaged(false);" +System.getProperty("line.separator")
						+"\tmarker.setFill(color);" +System.getProperty("line.separator")
						+"\tmarker.layoutXProperty().bind(scene.widthProperty().divide(scene.getWidth()/x));" +System.getProperty("line.separator")
						+"\tmarker.layoutYProperty().bind(scene.heightProperty().divide(scene.getHeight()/y));" +System.getProperty("line.separator")
						+"\tmarker.scaleXProperty().bind(scene.widthProperty().divide(scene.getWidth()));" +System.getProperty("line.separator") 
						+"\tmarker.scaleYProperty().bind(scene.heightProperty().divide(scene.getHeight()));" +System.getProperty("line.separator") 
						+"\treturn marker;" +System.getProperty("line.separator")
						+"}" +System.getProperty("line.separator")+System.getProperty("line.separator"));



			out.append("static Text regionName(SVGPath region, String text, Color color, Font f){" +System.getProperty("line.separator")
						+"\tdouble[] coords = regionCenterCoords(region);" +System.getProperty("line.separator")
						+"\tText name = new Text(text);" +System.getProperty("line.separator")
						+"\tname.setFill(color);" +System.getProperty("line.separator")
						+"\tname.setFont(f);" +System.getProperty("line.separator")
						+"\tname.setManaged(false);" +System.getProperty("line.separator")
						+"\tname.setBoundsType(TextBoundsType.VISUAL);" +System.getProperty("line.separator")
						+"\tname.layoutXProperty().bind(scene.widthProperty().divide(scene.getWidth()/coords[0]));" +System.getProperty("line.separator")
						+"\tname.layoutYProperty().bind(scene.heightProperty().divide(scene.getHeight()/coords[1]));" +System.getProperty("line.separator")
						+"\tname.scaleXProperty().bind(scene.widthProperty().divide(scene.getWidth()));" +System.getProperty("line.separator") 
						+"\tname.scaleYProperty().bind(scene.heightProperty().divide(scene.getHeight()));" +System.getProperty("line.separator") 
						+"\treturn name;" +System.getProperty("line.separator")
						+"}" +System.getProperty("line.separator")+System.getProperty("line.separator"));



			out.append("static Text regionName(String text, Color color, Font f, double x , double y){" +System.getProperty("line.separator")
						+"\tText name = new Text(text);" +System.getProperty("line.separator")
						+"\tname.setFill(color);" +System.getProperty("line.separator")
						+"\tname.setFont(f);" +System.getProperty("line.separator")
						+"\tname.setManaged(false);" +System.getProperty("line.separator")
						+"\tname.setBoundsType(TextBoundsType.VISUAL);" +System.getProperty("line.separator")
						+"\tname.layoutXProperty().bind(scene.widthProperty().divide(scene.getWidth()/x));" +System.getProperty("line.separator")
						+"\tname.layoutYProperty().bind(scene.heightProperty().divide(scene.getHeight()/y));" +System.getProperty("line.separator")
						+"\tname.scaleXProperty().bind(scene.widthProperty().divide(scene.getWidth()));" +System.getProperty("line.separator") 
						+"\tname.scaleYProperty().bind(scene.heightProperty().divide(scene.getHeight()));" +System.getProperty("line.separator") 
						+"\treturn name;" +System.getProperty("line.separator")
						+"}" +System.getProperty("line.separator")+System.getProperty("line.separator"));


			out.append("static double[] regionCenterCoords(SVGPath region){" +System.getProperty("line.separator")
						+"\tString path = updatedPath(region);" +System.getProperty("line.separator")
						+"\tpath = path.substring(1, path.length()-1);" +System.getProperty("line.separator")
						+"\tString[] coord = path.split(\",\");" +System.getProperty("line.separator")
						+"\tdouble [] xCoords = new double[coord.length];" +System.getProperty("line.separator")
						+"\tdouble [] yCoords = new double[coord.length];" +System.getProperty("line.separator")
						+"\tfor(int i=0;i<coord.length;i++){" +System.getProperty("line.separator")
						+"\t\tString[] temp = coord[i].split(\" \");" +System.getProperty("line.separator")
						+"\t\txCoords[i] = Double.parseDouble(temp[0]);" +System.getProperty("line.separator")
						+"\t\tyCoords[i] = Double.parseDouble(temp[1]);" +System.getProperty("line.separator")			
						+"\t}" +System.getProperty("line.separator")
						+"\tdouble avgX=0, avgY = 0;" +System.getProperty("line.separator")
						+"\tfor (int i=0;i<xCoords.length;i++){" +System.getProperty("line.separator")
						+"\t\tavgX+=xCoords[i];" +System.getProperty("line.separator")
						+"\t\tavgY+=yCoords[i];" +System.getProperty("line.separator")
						+"\t}" +System.getProperty("line.separator") 
						+"\tavgY/=yCoords.length;" +System.getProperty("line.separator")
						+"\tavgX/=xCoords.length;" +System.getProperty("line.separator")
						+"\tstandardDeviation(xCoords, avgX);" +System.getProperty("line.separator")
						+"\tstandardDeviation(yCoords, avgY);" +System.getProperty("line.separator")
						+"\treturn new double[]{avgX-(standardDeviation(xCoords, avgX)/2),"+System.getProperty("line.separator")
						+"\tavgY - (standardDeviation(yCoords, avgY)/2)};" +System.getProperty("line.separator")
						+"}" +System.getProperty("line.separator")+System.getProperty("line.separator"));

			out.append("private static double standardDeviation(double[] coords, double average){"+System.getProperty("line.separator")
						+"\tdouble sumX=0;"+System.getProperty("line.separator")
						+"\tArrayList<Double> x1_average = new ArrayList<Double>();"+System.getProperty("line.separator")
						+"\tfor (int i = 0; i<coords.length; i++){"+System.getProperty("line.separator")
						+"\t\tx1_average.add(i,(Math.pow((coords[i] - average), 2)));"+System.getProperty("line.separator")
						+"\t}"+System.getProperty("line.separator")
						+"\t for(double i : x1_average) {"+System.getProperty("line.separator")
						+"\t\tsumX += i;"+System.getProperty("line.separator")
						+"\t}"+System.getProperty("line.separator")
						+"\treturn Math.sqrt(sumX/(coords.length));"+System.getProperty("line.separator")
						+"}"+System.getProperty("line.separator")+System.getProperty("line.separator"));
			
			out.append("private static String updatedPath(SVGPath region){"+System.getProperty("line.separator")
						+"\tString origPath = region.getContent();"+System.getProperty("line.separator")
						+"\torigPath = origPath.substring(1, origPath.length()-1);"+System.getProperty("line.separator")
						+"\tString[] coord = origPath.split(\",\");"+System.getProperty("line.separator")
						+"\tdouble [] xCoords = new double[coord.length];"+System.getProperty("line.separator")
						+"\tdouble [] yCoords = new double[coord.length];"+System.getProperty("line.separator")
						+"\tfor(int i=0;i<coord.length;i++){"+System.getProperty("line.separator")
						+"\t\tString[] temp = coord[i].split(\" \");"+System.getProperty("line.separator")
						+"\t\txCoords[i] = Double.parseDouble(temp[0]);"+System.getProperty("line.separator")
						+"\t\tyCoords[i] = Double.parseDouble(temp[1]);"+System.getProperty("line.separator")
						+"\t}" +System.getProperty("line.separator")
						+"\tArrayList<Double> xArray = new ArrayList<Double>();"+System.getProperty("line.separator")
						+"\tfor(double x : xCoords) xArray.add(x);"+System.getProperty("line.separator")
						+"\tdouble origMinX = Collections.min(xArray);"+System.getProperty("line.separator")
						+"\tArrayList<Double> yArray = new ArrayList<Double>();"+System.getProperty("line.separator")
						+"\tfor(double y : yCoords) yArray.add(y);"+System.getProperty("line.separator")
						+"\tdouble origMinY = Collections.min(yArray);"+System.getProperty("line.separator")
						+"\tdouble minX = region.getBoundsInParent().getMinX();"+System.getProperty("line.separator")
						+"\tdouble minY = region.getBoundsInParent().getMinY();"+System.getProperty("line.separator")
						+"\tString updatedPath=\"M\";"+System.getProperty("line.separator")
						+"\tfor(int i=0 ; i<xCoords.length; i++){"+System.getProperty("line.separator")
						+"\t\txCoords[i] *= minX/origMinX;"+System.getProperty("line.separator")
						+"\t\tyCoords[i] *= minY/origMinY;"+System.getProperty("line.separator")
						+"\t\tupdatedPath+= (xCoords[i]+\" \"+yCoords[i] +\",\");"+System.getProperty("line.separator")
						+"\t}"+System.getProperty("line.separator")
						+"\treturn updatedPath;"+System.getProperty("line.separator")
						+"\t}"+System.getProperty("line.separator")+System.getProperty("line.separator"));


			out.append(System.getProperty("line.separator") + "}");
			
			out.close();
		}
		
	}
	}
	
}


	
