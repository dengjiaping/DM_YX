package com.dm.yx.view.other;


import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import com.lurencun.android.resource.SDCard;

@SuppressLint("NewApi")
public class DownloadService extends Service{
	private DownloadManager downloadManager=null;
	private long lastDownload = -1L;
	private String remoteFileName = "";
	private String localFileName = "";
	private String localDir = "";
	private final String DOWNLOAD_STATE_SUCCESS = "SUCCESS";
	private final String DOWNLOAD_STATE_DELETE = "DELETE";
	private final String DOWNLOAD_STATE_FAIL = "FAILURE";
//	private short isUpdate;
	private OnCompleteReceiver onComplete;
	
//	private final Uri CONTENT_URI = Uri.parse("content://downloads/my_downloads");
	
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		 unregisterReceiver(onComplete);
		 unregisterReceiver(onNotificationClick);
		 
	}
	
	

	@Override
	public void onCreate() {
		super.onCreate();
		downloadManager=(DownloadManager)getSystemService(DOWNLOAD_SERVICE);
		onComplete = new OnCompleteReceiver();
	    registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
	    registerReceiver(onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));
	    

	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (intent != null) {
			final String uri = intent.getStringExtra("download_uri");
			localFileName = intent.getStringExtra("file_name");
			localDir = intent.getStringExtra("file_dir");
//			isUpdate = intent.getShortExtra("is_update", (short)2);
			  if (!SDCard.IS_MOUNTED) 
			  {
				  Toast.makeText(this, "请检查SD卡是否安装", Toast.LENGTH_LONG).show();
			  } else {
//				  File file = new File(localDir);
//				  if (!file.exists()) {
//					  file.mkdirs();
//				  }
				Environment.getExternalStoragePublicDirectory(localDir).mkdirs();
				startDownload(uri);
//				showDownloadManagerView();
			  }
			
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	public void startDownload(String downloadPath) {
		int startIndex = downloadPath.lastIndexOf("/");
		remoteFileName = downloadPath.substring(startIndex + 1);
		if (localFileName == null || "".equals(localFileName)) {
			localFileName = remoteFileName;
		}
	    Uri uri = Uri.parse(downloadPath);
	    boolean isDownloading = queryStatus(downloadPath);
	    if (isDownloading) {
	    	return;
	    }
	    try {
		    DownloadManager.Request  request = new DownloadManager.Request(uri);
		    request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI |
	                                DownloadManager.Request.NETWORK_MOBILE);
	        request.setAllowedOverRoaming(true);
	        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();  
	        String mimeString = mimeTypeMap.getMimeTypeFromExtension(MimeTypeMap.getFileExtensionFromUrl(downloadPath));  
	        request.setMimeType(mimeString);  
	        request.setTitle(localFileName);
	        request.addRequestHeader("Content-Type","application/x-www-form-urlencoded; charset=utf-8");
	        request.setDescription("");
	        request.setDestinationInExternalPublicDir(localDir,localFileName);
	        lastDownload = downloadManager.enqueue(request);
	        onComplete.setLastDownloadId(lastDownload);
	    } catch (IllegalArgumentException e) {
	    	Toast.makeText(this, "下载路径无效", Toast.LENGTH_LONG).show();
	    	
	    }
	    
	    
	  }
	  
	  public boolean queryStatus(String downloadPath) {
	    Cursor c=downloadManager.query(new DownloadManager.Query().setFilterByStatus(DownloadManager.STATUS_RUNNING 
	    		| DownloadManager.STATUS_PAUSED | DownloadManager.STATUS_PENDING));
	    if (c==null) {
	      Toast.makeText(this, "Download not found!", Toast.LENGTH_LONG).show();
	    }
	    else {
	    	while (c.moveToNext()) {
	    		String uri = c.getString(c.getColumnIndex(DownloadManager.COLUMN_URI));
	    		if (uri != null && uri.equals(downloadPath)) {
	    			 c.close();
	    			 Toast.makeText(this, "任务已经在下载列表", Toast.LENGTH_LONG).show();
	    			 return true;
	    		}
	    	}
	    }
	    c.close();
//	    Toast.makeText(this, "������������", Toast.LENGTH_LONG).show();
	    return false;
	  }
	  
	  private void showDownloadManagerView() {
		 Intent intent = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
		 intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
		 startActivity(intent);
	  }
	  
	  private String statusMessage(Cursor c) {
	    String msg = DOWNLOAD_STATE_FAIL;
	    try {
			switch(c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
			  case DownloadManager.STATUS_FAILED:
			    msg = DOWNLOAD_STATE_FAIL;
			    break;
			  
			  case DownloadManager.STATUS_PAUSED:
			    msg = "Download paused!";
			    break;
			  
			  case DownloadManager.STATUS_PENDING:
			    msg = "Download pending!";
			    break;
			  
			  case DownloadManager.STATUS_RUNNING:
			    msg = "Download in progress!";
			    break;
			  
			  case DownloadManager.STATUS_SUCCESSFUL:
			    msg = DOWNLOAD_STATE_SUCCESS;
			    break;
			  default:
			    msg="Download is nowhere in sight";
			    break;
			}
			
		} catch (CursorIndexOutOfBoundsException e) {
//			e.printStackTrace();
			msg = DOWNLOAD_STATE_DELETE; 
		}
	    
	    return(msg);
	  }
	  
	  private void installApkByGuide(String localFilePath) {
		    Intent intent = new Intent(Intent.ACTION_VIEW);
	        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
	        intent.setDataAndType(Uri.parse("file://" + localFilePath),"application/vnd.android.package-archive");
	        startActivity(intent);
	  }
	

	  
	  //������������������������������
	BroadcastReceiver onNotificationClick=new BroadcastReceiver() {
	    @Override
		public void onReceive(Context ctxt, Intent intent) {
	      showDownloadManagerView();
	    }
	};

	//��������������������������
	private class OnCompleteReceiver extends BroadcastReceiver {
		
		private long lastDownloadId;
		
		 public void setLastDownloadId(long lastDownloadId) {
			this.lastDownloadId = lastDownloadId;
		}
		 
		@Override
		public void onReceive(Context ctxt, Intent intent) {
	    	Cursor c=downloadManager.query(new DownloadManager.Query().setFilterById(lastDownloadId));
	    	c.moveToFirst();
	    	String downloadStatus = statusMessage(c);
	    	if (DOWNLOAD_STATE_SUCCESS.equals(downloadStatus)) {
    			String localFilePath = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
    			if (localFilePath.endsWith(".apk")) {
    				installApkByGuide(localFilePath);
    			} 
	    	}
	    	c.close();
	        stopSelf();
	    }
	}
	
	
        
        
        
	
}
