/* (c) British Telecommunications plc, 2009, All Rights Reserved */
package com.bt.pi.sss.robustness.commands.jets3t;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.io.IOUtils;
import org.jets3t.service.model.S3Bucket;
import org.jets3t.service.model.S3Object;

import com.ragstorooks.testrr.ScenarioCommandBase;
import com.ragstorooks.testrr.ScenarioListener;

public class GetObjectCommand extends Jets3tCommand{

	public GetObjectCommand(String scenarioId, ScenarioListener scenarioListener, AtomicBoolean scenarioCompleted, Executor executor, Map<String, Object> params) {
		super(scenarioId, scenarioListener, scenarioCompleted, executor, params);
	}

	@Override
	protected void cleanup(Map<String, Object> params) throws Throwable {
	}

	@Override
	protected void execute(Map<String, Object> params) throws Throwable {
		String bucketName = getBucketName(params);
		S3Bucket bucket = getWalrusService(params).getBucket(bucketName);
		String objectKey = getObjectKey(params);
		S3Object object = getWalrusService(params).getObject(bucket, objectKey);
		InputStream inputStream = object.getDataInputStream();
		File tmpFile = File.createTempFile("robustness", null);
		tmpFile.deleteOnExit();
		OutputStream outputStream = new FileOutputStream(tmpFile);
		IOUtils.copy(inputStream, outputStream);
		long length = tmpFile.length();
		if (length != getDataSize(params))
			getScenarioListener().scenarioFailure(getScenarioId(), String.format("file not the correct size, expected %d, but was %d", getDataSize(params), length));
		tmpFile.delete();
	}

	@Override
	protected ScenarioCommandBase getCompensationCommand() {
		return null;
	}

	@Override
	public long getDelayMillis() {
		return 0;
	}
}
