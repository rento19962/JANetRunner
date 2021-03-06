package org.keyser.anr.web;

import static java.text.MessageFormat.format;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * Permet de charger des images, par défaut les pngs de http://netrunnerdb.com.
 * 
 * @author PAF
 * 
 */
public class RemoteCardLoader implements InitializingBean {

	private final static Logger logger = LoggerFactory.getLogger(RemoteCardLoader.class);

	private File downloadDir = new File("cache/download");

	private File cacheDir = new File("cache");

	private final Executor executor;

	private String urlFormat = "http://netrunnerdb.com/bundles/netrunnerdbcards/images/cards/en/{0}.png";

	private String cacheFormat = "{0}.png";

	private MediaType mediaType = MediaType.IMAGE_PNG;

	public RemoteCardLoader(Executor executor) {
		this.executor = executor;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		downloadDir.mkdirs();
		cacheDir.mkdirs();
	}

	private Runnable fetch(final DeferredResult<ResponseEntity<Resource>> to, final String path, final File fileInCache) {
		return () -> {
			String url = format(urlFormat, path);
			logger.debug("fetching {}", url);

			File tmp = null;
			try {
				tmp = loadURL(url);
			} catch (IOException e) {
				logger.error("fetch error " + url, e);
			}
			if (tmp != null) {
				tmp.renameTo(fileInCache);

				result(to, fileInCache, null);
			}
		};
	}

	private File fileInCache(String path) {
		return new File(cacheDir, format(cacheFormat, path));
	}

	private File getTmpFile() throws IOException {
		return File.createTempFile("download", ".tmp", downloadDir);
	}

	/**
	 * Permet de charger
	 * 
	 * @param to
	 * @param path
	 */
	public void load(DeferredResult<ResponseEntity<Resource>> to, String path, HttpHeaders ifModifiedSince) {
		File inCache = fileInCache(path);
		if (inCache.exists())
			result(to, inCache, ifModifiedSince);
		else {
			executor.execute(fetch(to, path, inCache));
		}
	}

	private File loadURL(String url) throws IOException {
		URL u = new URL(url);
		File tmp = getTmpFile();
		FileUtils.copyURLToFile(u, tmp);

		return tmp;

	}

	/**
	 * Création du résultat
	 * 
	 * @param to
	 * @param inCache
	 */
	private void result(DeferredResult<ResponseEntity<Resource>> to, File inCache, HttpHeaders requestHeader) {

		long lastModified = inCache.lastModified();
		HttpHeaders headers = new HttpHeaders();
		long millis = TimeUnit.DAYS.toMillis(180);

		headers.setExpires(System.currentTimeMillis() + millis);
		headers.setCacheControl("public, max-age=" + millis);
		headers.setLastModified(lastModified);

		if (requestHeader != null) {
			long date = requestHeader.getIfModifiedSince();
			if (date <= lastModified)
				to.setResult(new ResponseEntity<Resource>(headers, HttpStatus.NOT_MODIFIED));
			return;
		}

		headers.setContentType(mediaType);

		to.setResult(new ResponseEntity<Resource>(new FileSystemResource(inCache), headers, HttpStatus.OK));
	}

	public void setCacheDir(File cacheDir) {
		this.cacheDir = cacheDir;
	}

	public void setDownloadDir(File downloadDir) {
		this.downloadDir = downloadDir;
	}

	public void setUrlFormat(String urlFormat) {
		this.urlFormat = urlFormat;
	}
}
