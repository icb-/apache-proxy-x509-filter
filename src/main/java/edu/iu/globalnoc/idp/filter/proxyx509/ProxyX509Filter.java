package edu.iu.globalnoc.idp.filter.proxyx509;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import java.security.cert.X509Certificate;
import org.opensaml.security.x509.X509Support;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.annotation.constraint.NonnullElements;
import net.shibboleth.utilities.java.support.annotation.constraint.NotEmpty;
import net.shibboleth.utilities.java.support.primitive.StringSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProxyX509Filter implements Filter {
  @Nonnull @NotEmpty private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
  @Nonnull @NotEmpty private static final String END_CERT = "-----END CERTIFICATE-----";
  @Nonnull @NotEmpty private static final String APACHE_NULL = "(null)";

  @Nonnull private final Logger log = LoggerFactory.getLogger(ProxyX509Filter.class);

  /** Init parameter controlling what headers to check for the leaf certificate. */
  @Nonnull @NotEmpty private static final String LEAF_HEADERS_PARAM = "leafHeader";
  /** Init parameter controlling what headers to check for the chain certificates. */
  @Nonnull @NotEmpty private static final String CHAIN_HEADERS_PARAM = "chainHeaders";

  @Nullable @NotEmpty private String leafHeader;
  @Nonnull @NonnullElements private Collection<String> chainHeaders;

  public ProxyX509Filter() {
      chainHeaders = Collections.emptyList();
  }

  /* @{inheritDoc} */
  @Override
  public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
    throws IOException, ServletException {
      HttpServletRequest httpRequest = (HttpServletRequest) request;
      X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
      try {
        if (null == certs || 0 == certs.length) {

          List<X509Certificate> proxyCerts = new ArrayList<X509Certificate>();

      if (leafHeader != null) {
            String pem = httpRequest.getHeader(leafHeader);
            if (pem != null && !pem.isEmpty() && !APACHE_NULL.equals(pem)) {
              pem = pem.replace(BEGIN_CERT, "").replace(END_CERT, "");
              final X509Certificate cert = X509Support.decodeCertificate(pem);
              proxyCerts.add(cert);
            }
      }
      for (final String s : chainHeaders) {
            String pem = httpRequest.getHeader(s);
            if (pem != null && !pem.isEmpty() && !APACHE_NULL.equals(pem)) {
              pem = pem.replace(BEGIN_CERT, "").replace(END_CERT, "");
              final X509Certificate cert = X509Support.decodeCertificate(pem);
              proxyCerts.add(cert);
            }
          }

          request.setAttribute("javax.servlet.request.X509Certificate", proxyCerts.toArray(new X509Certificate[proxyCerts.size()]));
        }
      }
      catch (Exception e) { log.warn(e.getMessage()); }
      finally {
        chain.doFilter(request, response);
      }
    }

  /* @{inheritDoc} */
  @Override
  public void init(final FilterConfig config) throws ServletException {
    String param = config.getInitParameter(LEAF_HEADERS_PARAM);
    if (param != null) {
    leafHeader = param;
  }
  log.info("ProxyX509Filter will check for the leaf certificate in: {}", leafHeader);

  param = config.getInitParameter(CHAIN_HEADERS_PARAM);
    if (param != null) {
        final String[] headers = param.split(" ");
        if (headers != null) {
            chainHeaders = StringSupport.normalizeStringCollection(Arrays.asList(headers));
        }
    }
  log.info("ProxyX509Filter will check for chain certificates in: {}", chainHeaders);
  }

  /* {@inheritDoc} */
  @Override
  public void destroy () {
    // XXX
  }
}
