package com.whispir.sdk.interfaces;

import com.whispir.sdk.WhispirResponse;
import com.whispir.sdk.exceptions.WhispirSDKException;
/**
 * @author jordan
 * @version 1.0
 * @since 2.0
 * 
 * <p>Interface that must be implemented by both the WhispirSDK object and the implementing object.</p>
 * <p>This ensures a contract is upheld between the SDK's exposed methods, and the implementation classes in the background</p>
 */
public interface WorkspaceHelper {
	public WhispirResponse getWorkspaces() throws WhispirSDKException;
}
