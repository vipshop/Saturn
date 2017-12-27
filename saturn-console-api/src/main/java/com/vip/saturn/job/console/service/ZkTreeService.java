package com.vip.saturn.job.console.service;

import com.vip.saturn.job.console.domain.ZkTree;
import com.vip.saturn.job.console.exception.SaturnJobConsoleException;
import java.io.File;
import java.io.InputStream;

/**
 * @author hebelala
 */
public interface ZkTreeService {

	ZkTree getZkTreeByNamespaceOfSession() throws SaturnJobConsoleException;

	ZkTree getZkTreeByNamespace(String namespace) throws SaturnJobConsoleException;

	ZkTree convertFileToZkTree(File file) throws SaturnJobConsoleException;

	ZkTree convertInputStreamToZkTree(InputStream inputStream) throws SaturnJobConsoleException;

	File convertZkTreeToFile(ZkTree zkTree) throws SaturnJobConsoleException;

}
