package com.vip.saturn.job.console.mybatis.repository;

import com.vip.saturn.job.console.mybatis.entity.NamespaceVersionMapping;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author hebelala
 */
@Repository
public interface NamespaceVersionMappingRepository {

	int insert(NamespaceVersionMapping namespaceVersionMapping);

	int update(NamespaceVersionMapping namespaceVersionMapping);

	NamespaceVersionMapping selectByNamespace(String namespace);

	List<NamespaceVersionMapping> selectAllWithNotDeleted();

	/**
	 * 根据namespace和版本号删记录
	 * @param namespace
	 * @param versionNumber
	 * @return
	 */
	int deleteByNamespaceAndVersionNumber(@Param("namespace") String namespace,
			@Param("versionNumber") String versionNumber);
}
