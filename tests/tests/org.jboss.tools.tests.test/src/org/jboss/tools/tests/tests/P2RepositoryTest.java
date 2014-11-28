package org.jboss.tools.tests.tests;

import static org.junit.Assert.*;

import java.net.URI;
import java.net.URISyntaxException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.junit.Test;

public class P2RepositoryTest {


	@Test
	public void test() throws ProvisionException, OperationCanceledException, URISyntaxException {
		IMetadataRepositoryManager repositoryManager = Activator.getRepositoryManager();
		
		IProgressMonitor ipm = new NullProgressMonitor() {
			
			@Override
			public void beginTask(String name, int totalWork) {
				System.out.println("Begin: " + name  + "(" + totalWork + ")");
			}
		};
		
		IMetadataRepository repository = repositoryManager.loadRepository(new URI("http://download.jboss.org/jbosstools/updates/nightly/luna/"), ipm);
		
		assertNull(repository);
	}

}
