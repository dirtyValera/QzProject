package com.me.qzproject.fragments;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.me.qzproject.APIHandler;
import com.me.qzproject.R;
import com.me.qzproject.User;

public class GameFragment extends Fragment{
	

	public int CURRENT_FRAGMENT_STATE = 1;
	private boolean isFinalized = false;
	
	private User player = APIHandler.user;
	private User op = new User("2", "Valera Gryazniy", "dirtyvalera@gmail.com", true);//TODO set;
	
	private String host = APIHandler.ip;
	private int port = 1225;
	private String id = player.id;
	private String rid = op.id;
	private boolean isRequesting = true;
	private String themeId = "1";
	private GameState gameState;
	private EventLoopGroup group;
	private Channel channel;
	
	private boolean isReady = false;;
	private volatile Thread countDownThread;
	private volatile boolean isCountDownRunning = false;
	
	//UI
	
	private TextView tv;
	private Button btn;
	private ImageView img;
	private Button ans1;
	private Button ans2;
	private Button ans3;
	private Button ans4;
	private TextView opScore;
	private TextView yourScore;
	private View opInd;
	private View playerInd;
	private ProgressBar pb;
	private TextView countDown;
	
	private View gameView;
	private View loadingView;
	private View roundView;
	private TextView loadingStatus;
	private TextView roundStatus;
		
	private View curView;
	
	private Bitmap playerImg;
	private Bitmap opImg;
	
	

	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.game_fragment, container, false);
        
		gameState = new GameState(id, rid);
		
		tv = (TextView) view.findViewById(R.id.socket_tv);
		btn = (Button) view.findViewById(R.id.socket_go);
		img = (ImageView) view.findViewById(R.id.socket_image);
		ans1 = (Button) view.findViewById(R.id.socket_b1_m);
		ans2 = (Button) view.findViewById(R.id.socket_b2_m);
		ans3 = (Button) view.findViewById(R.id.socket_b3_m);
		ans4 = (Button) view.findViewById(R.id.socket_b4_m);
		opScore = (TextView) view.findViewById(R.id.socket_op_score);
		yourScore = (TextView) view.findViewById(R.id.socket_your_score);
		countDown = (TextView) view.findViewById(R.id.socket_countdown_m);
		opInd = view.findViewById(R.id.socket_opp_ind);
		playerInd = view.findViewById(R.id.socket_player_ind);
		pb = (ProgressBar) view.findViewById(R.id.socket_pb);
		
		gameView = view.findViewById(R.id.game_view);
		loadingView = view.findViewById(R.id.game_loading_view);
		loadingStatus = (TextView) view.findViewById(R.id.game_loading_status);
		roundView = view.findViewById(R.id.game_round_view);
		roundStatus = (TextView) view.findViewById(R.id.round_status);
		curView = loadingView;
		//gameView.setVisibility(View.INVISIBLE);
		
		btn.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {
				new Thread(new Runnable(){

					@Override
					public void run() {
						getActivity().runOnUiThread(new Runnable(){

							@Override
							public void run() {
								btn.setVisibility(View.INVISIBLE);
								
							}
							
						});
						runn();
					}
					
				}).start();
			}			
		});
		AnswerHandler answerHandler = new AnswerHandler();
		ans1.setOnClickListener(answerHandler);
		ans2.setOnClickListener(answerHandler);
		ans3.setOnClickListener(answerHandler);
		ans4.setOnClickListener(answerHandler);
		//ans1.setVisibility(View.GONE);
		//ans2.setVisibility(View.GONE);
		//ans3.setVisibility(View.GONE);
		//ans4.setVisibility(View.GONE);
        return view;
	}

	class AnswerHandler implements OnClickListener{

		@Override
		public void onClick(View v) {
			gameState.hasPlayerAnswered = true;
			String ans = null;
			
			if(v.getId() == R.id.socket_b1_m){
				ans = "1";
			}else if(v.getId() == R.id.socket_b2_m){
				ans = "2";
			}else if(v.getId() == R.id.socket_b3_m){
				ans = "3";
			}else{
				ans = "4";
			}

			channel.writeAndFlush("SEND#" + id + "_" + rid + "_" + ans + "\n");
			
			setUnclickable();
			Map<String, String> question = gameState.questions.get(gameState.qIds.get(gameState.round));
			if(gameState.hasPlayerAnswered && gameState.hasOpAnswered){
				if(question.get("right_ans").equals(ans)){
					gameState.playerScore += Integer.parseInt(countDown.getText().toString());
					//TODO change score					
					animateIndicator(playerInd, true);
	    				
				}else{					
					animateIndicator(playerInd, false);	
				}	
				setRoundView();
    			stopCountDown();        		
			}else{
				if(question.get("right_ans").equals(ans)){
					gameState.playerScore += Integer.parseInt(countDown.getText().toString());
					/*runOnUiThread(new Runnable(){

						@Override
						public void run() {
							yourScore.setText(gameState.playerScore);
						}
						
					});*/
					animateIndicator(playerInd, true);
	    				
				}else{					
					animateIndicator(playerInd, false);	
				}	
			}
		}		
	}
	
	private class GameState{
		public int round;
		public String aId;
		public String bId;
		public int opScore;
		public int playerScore;
		public List<String> qIds;
		public boolean hasOpAnswered = false;
		public boolean hasPlayerAnswered = false;

		public Map<String, Map<String, String>> questions;
		public Map<String, Bitmap> images;	
		
		public GameState(String aId, String bId){
			qIds = new ArrayList<String>();
			this.aId = aId;
			this.bId = bId;
			round = -1;
			opScore = 0;
			playerScore = 0;
		}				
	}
	
	public void setNextQuestion(){
		gameState.round++;
		gameState.hasOpAnswered = false;
		gameState.hasPlayerAnswered = false;
		setClickable();
		getActivity().runOnUiThread(new Runnable(){

			@Override
			public void run() {
				Map<String, String> question = gameState.questions.get(gameState.qIds.get(gameState.round));
				img.setImageBitmap(gameState.images.get(gameState.qIds.get(gameState.round)));
				tv.setText(question.get("question"));
				ans1.setText(question.get("ans1"));
				ans2.setText(question.get("ans2"));
				ans3.setText(question.get("ans3"));
				ans4.setText(question.get("ans4"));

				/*img.setVisibility(View.VISIBLE);
				ans1.setVisibility(View.VISIBLE);
				ans2.setVisibility(View.VISIBLE);
				ans3.setVisibility(View.VISIBLE);
				ans4.setVisibility(View.VISIBLE);*/
				
				FriendsFragment.crossfade(gameView, curView, getResources().getInteger(android.R.integer.config_shortAnimTime));
				curView = gameView;
			}
			
		});
		startCountDown();
	}
	
	private void setRoundView(){
		new Thread(new Runnable(){		

			@Override
			public void run() {

				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				
				getActivity().runOnUiThread(new Runnable(){
					@Override
					public void run() {
						/*ans1.setVisibility(View.GONE);
						ans2.setVisibility(View.GONE);
						ans3.setVisibility(View.GONE);
						ans4.setVisibility(View.GONE);
						img.setVisibility(View.GONE);
						tv.setText("Round " + gameState.round);*/
						if(gameState.round < gameState.qIds.size() - 2){
							roundStatus.setText("Round " + (gameState.round + 2));
							FriendsFragment.crossfade(roundView, curView, getResources().getInteger(android.R.integer.config_shortAnimTime));
							curView = roundView;
						}else if(gameState.round == gameState.qIds.size() - 2){
							roundStatus.setText("Final Round");
							FriendsFragment.crossfade(roundView, curView, getResources().getInteger(android.R.integer.config_shortAnimTime));
							curView = roundView;
						}else{
							//roundStatus.setText("Done");
							handleFinal(false);
						}
						//TODO if last round 
					}			
				});
				
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}				
				//TODO if last round send finalize and show final fragment
				
				if(!isReady){
					setNextQuestion();
				}else{
					if(gameState.round >= gameState.qIds.size() - 1){
						//TODO finalize
						//roundStatus.setText("Done");
						
					}else{
						channel.writeAndFlush("NEXT#" + id + "_" + rid + "\n");
					}
				}
			}	
		}).start();
	}
	
	private void setUnclickable(){
		getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run() {
				ans1.setClickable(false);
				ans2.setClickable(false);
				ans3.setClickable(false);
				ans4.setClickable(false);
			}			
		});
	}
	
	private void setClickable(){
		getActivity().runOnUiThread(new Runnable(){
			@Override
			public void run() {
				ans1.setClickable(true);
				ans2.setClickable(true);
				ans3.setClickable(true);
				ans4.setClickable(true);
			}			
		});	
	}
	
	public final void runn(){
		group = new NioEventLoopGroup();
		
		try{
			Bootstrap bootstrap = new Bootstrap()
				.group(group)
				.channel(NioSocketChannel.class)
				.handler(new ClientInitializer());
			
			channel = bootstrap.connect(host, port).sync().channel();
			
			//channel.writeAndFlush(id + "#" + rid + "#SERVER#REQUEST#" + ((isRequesting) ? "1" : "0") + "\n");
			channel.writeAndFlush("REQUEST#" + id + "_" + rid + "_" + ((isRequesting) ? "1" : "0") + "_" + themeId + "\n");
			//BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			/*while(true){
				channel.writeAndFlush(in.readLine() + "\r\n");
				//channel.writeAndFlush("Hi" + "\r\n");
				//Thread.sleep(2000);
			}*/			
		}catch(Exception t){
			//TODO handle
			final Exception e = t;
			getActivity().runOnUiThread(new Runnable(){

				@Override
				public void run() {
					tv.setText(e.toString());
				}
				
			});
		}
		
	}

	private class ClientInitializer extends ChannelInitializer<SocketChannel>{

		@Override
		protected void initChannel(SocketChannel arg0) throws Exception {
			ChannelPipeline pipeline = arg0.pipeline();
			
			pipeline.addLast("framer", new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
			pipeline.addLast("decoder", new StringDecoder());
			pipeline.addLast("encoder", new StringEncoder());
			pipeline.addLast("handler", new ClientHandler());
		}			
	}
	
	private class ClientHandler extends SimpleChannelInboundHandler<String>{


		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
			stopCountDown();
			showError(e.toString());
		}
		
		@Override
		protected void channelRead0(ChannelHandlerContext arg0, String arg1) throws Exception {
						
			if(arg1.equals("NEXT")){
				if(!isReady){
					//showGameView();
					new Thread(new Runnable(){

						@Override
						public void run() {
							getActivity().runOnUiThread(new Runnable(){

								@Override
								public void run() {
									FriendsFragment.crossfade(null, getActivity().findViewById(R.id.game_loading_spinner), getResources().getInteger(android.R.integer.config_shortAnimTime));
									
								}
								
							});
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							getActivity().runOnUiThread(new Runnable(){

								@Override
								public void run() {

									FriendsFragment.crossfade(null, loadingStatus, getResources().getInteger(android.R.integer.config_shortAnimTime));
								}
							});
							try {
								Thread.sleep(500);
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}

							final TextView startTimer = (TextView) getActivity().findViewById(R.id.game_loading_start_timer);
							for(int j = 3; j > 0 ; j--){
								final int i = j;
								getActivity().runOnUiThread(new Runnable(){

									@Override
									public void run() {

										startTimer.setText(i + "");
										
										FriendsFragment.crossfade(startTimer, null, getResources().getInteger(android.R.integer.config_shortAnimTime));
									}
								});
								try {
									Thread.sleep(800);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								getActivity().runOnUiThread(new Runnable(){

									@Override
									public void run() {

										FriendsFragment.crossfade(null, startTimer, getResources().getInteger(android.R.integer.config_shortAnimTime));
									}
								});
								try {
									Thread.sleep(400);
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}

							CURRENT_FRAGMENT_STATE = 2;
							setRoundView();
							isReady = true;
						}
						
					}).start();
				}else{
					stopCountDown();
					setNextQuestion();
				}
				return;
			}			
			
			if(arg1.equals("FIN")){
				stopCountDown();
				if(CURRENT_FRAGMENT_STATE == 1){
					showError("Opponent Canceled the Game");
				}else if(CURRENT_FRAGMENT_STATE == 2){
					handleFinal(true);
				}else if(CURRENT_FRAGMENT_STATE == 3){
					//Do Nothing
				}
				return;
			}
			
			String[] strs = arg1.split("#");
			String command = strs[0];
			String[] args = strs[1].split("_");
			
			
			if(command.equals("QIDS")){
				
				List<String> qIds = new ArrayList<String>();
        		for(String qid : args){
        			qIds.add(qid);
        		}
        		gameState.qIds = qIds;
        		new GetQuestions(qIds, getActivity()).execute();
        		
			}else if(command.equals("ANS")){
				
				String ans = args[0];
				gameState.hasOpAnswered = true;
				Map<String, String> question = gameState.questions.get(gameState.qIds.get(gameState.round));
				
				if(gameState.hasOpAnswered && gameState.hasPlayerAnswered){
					if(question.get("right_ans").equals(ans)){
						animateIndicator(opInd, true);
						gameState.opScore = gameState.opScore +  Integer.parseInt(countDown.getText().toString());
						//opScore.setText(gameState.opScore);        				
					}else{
						animateIndicator(opInd, false);						
					}						
        			stopCountDown();
        			setRoundView();					
				}else{
					if(question.get("right_ans").equals(ans)){
						animateIndicator(opInd, true);
						gameState.opScore = gameState.opScore +  Integer.parseInt(countDown.getText().toString());
						//opScore.setText(gameState.opScore);        				
					}else{
						animateIndicator(opInd, false);						
					}	
				} 
			}else if(command.equals("ERR")){
				String errCode = args[0];
			}
		}
	}
	
	class GetQuestions extends AsyncTask<String, String, String>{
		List<String> ids;
		Activity activity;
		GetQuestions(List<String> ids, Activity activity){
			this.ids = ids;
			this.activity = activity;
		}
		
		protected void onPreExecute() {
			//loadingStatus.setText("Loading questions");
			activity.runOnUiThread(new Runnable(){
				@Override
				public void run() {
					loadingStatus.setText("Loading Questions");
				}
        		
        	});
		}
		
		@Override
		protected String doInBackground(String... arg0) {
        	gameState.questions = APIHandler.getQuestionsByIds(themeId, ids);			
			return null;
		}
		
		protected void onPostExecute(String unused) {
			if(gameState.questions != null){
				List<String> img_ids = new ArrayList<String>();
				for(Entry<String, Map<String, String>> entry: gameState.questions.entrySet()){
					if(entry.getValue().get("has_img").equals("1")){
						img_ids.add(entry.getKey());
					}
				}				
				new GetImages(img_ids, activity).execute();				
	        }else{
	        	//tv.setText(APIHandler.error);
	        	//TODO notify other
	        	activity.runOnUiThread(new Runnable(){
					@Override
					public void run() {
						loadingStatus.setText("Loading Questions Error");
					}
	        		
	        	});
	        }		        	
		}		
	}
	
	class GetImages extends AsyncTask<String, String, String>{

		List<String> ids;
		Activity activity;
		GetImages(List<String> ids, Activity activity){
			this.ids = ids;
			this.activity = activity;
		}
		
		protected void onPreExecute() {
			//loadingStatus.setText("Loading images");
			activity.runOnUiThread(new Runnable(){
				@Override
				public void run() {
					loadingStatus.setText("Loading images");
				}
			});
		}
		
		@Override
		protected String doInBackground(String... params) {

        	gameState.images = new HashMap<String, Bitmap>();
			try {
				for(String id : ids){
					String url = "http://" + APIHandler.ip + ":80/laravel/public/images/questions/question_" + themeId + "_" + id + ".jpg";				
		            InputStream in = new java.net.URL(url).openStream();
		            Bitmap bitmap = BitmapFactory.decodeStream(in);
		            gameState.images.put(id, bitmap);			        
				}
				String playerUrl = "http://" + APIHandler.ip + ":80/laravel/public/images/" + player.thumbnail_img_url;
				String opUrl = "http://" + APIHandler.ip + ":80/laravel/public/images/" + op.thumbnail_img_url;
				InputStream playerIn = new java.net.URL(playerUrl).openStream();
	            playerImg = BitmapFactory.decodeStream(playerIn);

				InputStream opIn = new java.net.URL(opUrl).openStream();
	            opImg = BitmapFactory.decodeStream(opIn);
				
			} catch (final Exception e) {
				gameState.images = null;
	        	activity.runOnUiThread(new Runnable(){
					@Override
					public void run() {
						loadingStatus.setText("Images loading error: " + e.toString());
					}
	        		
	        	});
	        }
				
			return null;
		}
		

		protected void onPostExecute(String unused) {
			if(gameState.images != null && playerImg != null && opImg != null){

				loadingStatus.setText("Waiting for opponent");
				channel.writeAndFlush("NEXT#" + id + "_" + rid + "\n");
				new Thread(new Runnable(){

					@Override
					public void run() {
						getActivity().runOnUiThread(new Runnable(){

							@Override
							public void run() {
								ImageView img1 = (ImageView) getActivity().findViewById(R.id.game_loading_user_1_img);
								ImageView img2 = (ImageView) getActivity().findViewById(R.id.game_loading_user_2_img);
								img1.setImageBitmap(playerImg);
								img2.setImageBitmap(opImg);
								TextView tv1 = (TextView) getActivity().findViewById(R.id.game_loading_user_1_name);
								TextView tv2 = (TextView) getActivity().findViewById(R.id.game_loading_user_2_name);
								tv1.setText(player.name);
								tv2.setText(op.name);
								
								View v1 = getActivity().findViewById(R.id.game_loading_user_1_view);
								
								FriendsFragment.crossfade(v1, null, getResources().getInteger(android.R.integer.config_shortAnimTime));
							}
							
						});
						
						try {
							Thread.sleep(800);
						} catch (InterruptedException e) {
						}
						
						getActivity().runOnUiThread(new Runnable(){

							@Override
							public void run() {
								View v2 = getActivity().findViewById(R.id.game_loading_user_2_view);
								
								FriendsFragment.crossfade(v2, null, getResources().getInteger(android.R.integer.config_shortAnimTime));
							}
						});
						
					}
					
				}).start();
				//setRoundView();
	        }else{
	        	//TODO handle error;
	        	tv.setText("Images Loading error");	        	
	        }		        	
		}		
	}
	
	public void animateIndicator(final View ind, final boolean isRight){
		new Thread(new Runnable(){
			@Override
			public void run() {
				getActivity().runOnUiThread(new Runnable(){

					@Override
					public void run() {
						if(isRight){
							ind.setBackgroundColor(getResources().getColor(R.color.Green));
						}else{
							ind.setBackgroundColor(getResources().getColor(R.color.Red));							
						}
					}
					
				});
				
				try {
					TimeUnit.MILLISECONDS.sleep(200);
				} catch (InterruptedException e) {
					
				}
				
				getActivity().runOnUiThread(new Runnable(){

					@Override
					public void run() {
						ind.setBackgroundColor(getResources().getColor(R.color.White));
					}
					
				});
			}
			
		}).start();
	}

	
	public void startCountDown(){
		
		countDownThread = new Thread(new Runnable(){

			@Override
			public void run() {
				boolean isInterrupted = false;
				isCountDownRunning = true;
				long time = 10*1000;//TODO set constant
				while(time>= 0 && !isInterrupted){
					final long temp = time;
					getActivity().runOnUiThread(new Runnable(){

						@Override
						public void run() {
							countDown.setText(temp/1000 + 1 + "");
							pb.setProgress(1000 - (int)temp/10);
						}
						
					});
					time -= 1;
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {
						isInterrupted = true;
					}
				}
				if(!isInterrupted){
					setRoundView();
				}
				isCountDownRunning = false;
			}
			
		});
		
		countDownThread.start();
	}
	
	public void stopCountDown(){
		if(isCountDownRunning){
			countDownThread.interrupt();
		}
	}
	
	/*@Override
	public void onBackPressed() {
		showSurrenderConfirmationDialog(this, id, channel);
	}*/
	
	public void showSurrenderConfirmationDialog(){
		
		AlertDialog alertDialog;		
		alertDialog = new AlertDialog.Builder(getActivity()).create();
	    alertDialog.setTitle("Surrender?");
	    alertDialog.setMessage("Dou you want to surrender?");
	    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "No", new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int id) {
	    		
	    	} 
	    }); 

	    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Yes", new DialogInterface.OnClickListener() {
	    	public void onClick(DialogInterface dialog, int id1) {
	    		//confirm
	    		finalizze();
	    		getActivity().finish();
         	}
	    }); 
	    
	    alertDialog.show();
	}
	
	public void handleFinal(final boolean hasOpSurrendered){
		if(!hasOpSurrendered){
			finalizze();
		}
		CURRENT_FRAGMENT_STATE = 3;
		//TODO send stats to server
		new Thread(new Runnable(){

			@Override
			public void run() {
				try {
					Thread.sleep(700);
				} catch (InterruptedException e) {
				}
				
				getActivity().runOnUiThread(new Runnable(){

					@Override
					public void run() {
						ImageView img1 = (ImageView) getActivity().findViewById(R.id.game_final_user_1_img);
						ImageView img2 = (ImageView) getActivity().findViewById(R.id.game_final_user_2_img);
						img1.setImageBitmap(playerImg);
						img2.setImageBitmap(opImg);
						TextView tv1 = (TextView) getActivity().findViewById(R.id.game_final_user_1_name);
						TextView tv2 = (TextView) getActivity().findViewById(R.id.game_final_user_2_name);
						tv1.setText(player.name);
						tv2.setText(op.name);
						
						View v1 = getActivity().findViewById(R.id.game_final_user_1_view);
						
						FriendsFragment.crossfade(v1, curView, getResources().getInteger(android.R.integer.config_shortAnimTime));
					}
					
				});
				
				try {
					Thread.sleep(800);
				} catch (InterruptedException e) {
				}
				
				getActivity().runOnUiThread(new Runnable(){

					@Override
					public void run() {
						View v2 = getActivity().findViewById(R.id.game_final_user_2_view);
						
						FriendsFragment.crossfade(v2, null, getResources().getInteger(android.R.integer.config_shortAnimTime));
					}
				});
				
				try {
					Thread.sleep(800);
				} catch (InterruptedException e) {
				}
				
				getActivity().runOnUiThread(new Runnable(){

					@Override
					public void run() {
						TextView res = (TextView) getActivity().findViewById(R.id.game_final_result);
						if(hasOpSurrendered){
							res.setText("Opponent Surrendered");
						}else{
							res.setText((gameState.opScore > gameState.playerScore) ? "You Lost" : "You Won");
						}
						FriendsFragment.crossfade(res, curView, getResources().getInteger(android.R.integer.config_shortAnimTime));
					}
				});
			}
			
		}).start();
	}
	
	public void finalizze(){
		//TODO
		if(!isFinalized){
			isFinalized = true;
			if(channel != null){
				channel.writeAndFlush("FINALIZE#" + id + "_" + rid + "\n");
			}
		}
	}
	
	public void showError(final String msg){
		getActivity().runOnUiThread(new Runnable(){

			@Override
			public void run() {
				View v = getActivity().findViewById(R.id.game_error_view);
				TextView tv = (TextView) getActivity().findViewById(R.id.game_error_status);
				tv.setText(msg);
				
				FriendsFragment.crossfade(v, curView, getResources().getInteger(android.R.integer.config_shortAnimTime));
			}
			
		});
	}
}
