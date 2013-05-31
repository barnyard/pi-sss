/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss;

import com.bt.pi.app.common.entities.User;

public interface UserManager {
    boolean userExists(String accessKey);

    User getUserByAccessKey(String accessKey);
}
