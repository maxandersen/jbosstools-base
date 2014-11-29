package org.jboss.tools.tests.tests;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.equinox.internal.p2.metadata.repository.CompositeMetadataRepository;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.collection.IsEmptyCollection;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class P2RepositoryTest {


	@Parameters(name = "Check {0}")
	static public Collection<URI[]> getLocations() throws URISyntaxException, IOException {
		
		
		Set<URI> urls = new LinkedHashSet<>();
		urls.add(new URI("https://devstudio.redhat.com/updates/8.0-development/integration-stack/"));
		urls.add(new URI("https://devstudio.redhat.com/updates/7.0/integration-stack/"));
		urls.add(new URI("https://devstudio.redhat.com/updates/8.0-development/"));
		urls.add(new URI("https://devstudio.redhat.com/updates/7.0/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/stable/luna/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/nightly/luna/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/stable/kepler/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/nightly/core/4.1.kepler/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/stable/juno/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/stable/indigo/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/stable/helios/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/stable/galileo/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/stable/ganymede/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/development/luna/integration-stack/"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/stable/kepler/integration-stack"));
		urls.add(new URI("http://download.jboss.org/jbosstools/updates/stable/indigo/soa-tooling/"));

		Properties p = IDEPropertiesSanityTest.loadIDEProperties();
		urls.addAll(getP2ReposFromProperties(p));
		
		Set<URI[]> result = new LinkedHashSet<>(urls.size());
		
		for (URI uri : urls) {
			result.add(new URI[] {uri} );
		}
		
		return result;
	}
	
	private static Collection<URI> getP2ReposFromProperties(Properties properties) throws IOException {
		if (properties == null) {
		  return Collections.emptySet();	
		}
		Set<URI> uris = new LinkedHashSet<>();
		for (Entry entry : properties.entrySet()) {
			String key = (String)entry.getKey();
			if (!key.contains(".discovery.site.")) {
				continue;
			}
			String value = (String)entry.getValue();
			URI uri = asUri(value);
			if (uri != null) {
				System.err.println("Retaining "+key);
				uris.add(uri);
			}
		}
		
		return uris;
	}

	private static URI asUri(String url) {
		if (url != null) {
			try {
				return new URI(url);
			} catch (URISyntaxException ignored){
			}
		}
		return null;
	}

	@Parameter
	public URI location;
	
	private List<IStatus> errors;

	private Map<URI, IMetadataRepository> allrepositories;

	private IMetadataRepository repository;
	
	@Before
	public void loadContent() throws ProvisionException, OperationCanceledException {
		IMetadataRepositoryManager repositoryManager = Activator
				.getRepositoryManager();

		IProgressMonitor ipm = new NullProgressMonitor() {

			@Override
			public void beginTask(String name, int totalWork) {
				System.out.println("Begin: " + name + "(" + totalWork + ")");
			}
		};
	
		errors = new ArrayList<>();
		allrepositories = new HashMap<>();
		
		repository = loadRepository(repositoryManager, allrepositories, 
				location, false, errors, ipm);
		
	}
	
	@After
	public void cleanUp() {
		errors = null;
		allrepositories = null;
	}
	
	@Test
	public void noErrors() throws ProvisionException, OperationCanceledException,
			URISyntaxException {
		assertThat("Errors while loading " + errors, errors, IsEmptyCollection.empty());
	}

	@Test
	public void noStreamCrossing() {
		String host = location.getHost();
		
		assertThat("All referenced repositories should come from same base host",allrepositories.keySet(), everyItem(hasHost(host)));
		
	}
	
	@Test
	public void isNotEmpty() {
		assertThat(repository.getReferences(), not(empty()));
	}
	
	Matcher<URI> hasHost(final String host) {
		return new BaseMatcher<URI>() {
		      @Override
		      public boolean matches(final Object item) {
		         final URI foo = (URI) item;
		         return host.equals(foo.getHost());
		      }
		      @Override
		      public void describeTo(final Description description) {
		         description.appendText("getHost should return ").appendValue(host);
		      }
		      @Override
		      public void describeMismatch(final Object item, final
		Description description) {
		         description.appendText("was").appendValue(((URI) item).getHost());
		     }
			
		   };
	}
	@SuppressWarnings("restriction")
	private IMetadataRepository loadRepository(IMetadataRepositoryManager repoMgr,
			Map<URI, IMetadataRepository> allrepositories, URI location,
			boolean refresh, List<IStatus> errors, IProgressMonitor monitor)
			 {
		if (!allrepositories.containsKey(location)) {
			try {
				IMetadataRepository repository;
				if (refresh) {
					repository = repoMgr.refreshRepository(location, monitor);
				} else {
					repository = repoMgr.loadRepository(location, monitor);
				}
				allrepositories.put(location, repository);

				if (repository instanceof CompositeMetadataRepository) {
					for (URI childUri : ((CompositeMetadataRepository) repository)
							.getChildren()) {
						// composite repository refresh refreshes all child
						// repositories. do not re-refresh children
						// here
						loadRepository(repoMgr, allrepositories, childUri,
								false, errors, monitor);
					}
				}
				
				return repository;
			} catch (ProvisionException e) {
				errors.add(e.getStatus());
			}
		}
		
		return null; //already loaded
	}
}
