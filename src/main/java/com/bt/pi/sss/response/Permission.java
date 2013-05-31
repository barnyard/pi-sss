/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.response;

public enum Permission {
    READ, /* when applied to a bucket, grants permission to list the bucket. When applied to an object, this grants permission to read the object data and/or metadata. */
    WRITE, /* when applied to a bucket, grants permission to create, overwrite, and delete any object in the bucket. This permission is not supported for objects.*/
    READ_ACP, /* grants permission to read the ACL for the applicable bucket or object. The owner of a bucket or object always has this permission implicitly.*/
    WRITE_ACP, /* gives permission to overwrite the ACP for the applicable bucket or object. The owner of a bucket or object always has this permission implicitly. Granting this permission is equivalent to granting FULL_CONTROL because the grant recipient can make any changes to the ACP. */
    FULL_CONTROL; /* provides READ, WRITE, READ_ACP, and WRITE_ACP permissions. It does not convey additional rights and is provided only for convenience. */
}
