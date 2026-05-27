/*
 * Copyright (c) Rich Hickey. All rights reserved.
 * The use and distribution terms for this software are covered by the
 * Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
 * which can be found in the file epl-v10.html at the root of this distribution.
 * By using this software in any fashion, you are agreeing to be bound by
 * the terms of this license.
 * You must not remove this notice, or any other, from this software.
 */
package clojure.tools.deps.util;

import eu.maveniverse.maven.mima.context.Lookup;
import eu.maveniverse.maven.mima.runtime.shared.PreBoot;
import eu.maveniverse.maven.mima.runtime.standalonestatic.MemoizingRepositorySystemSupplierLookup;
import eu.maveniverse.maven.mima.runtime.standalonestatic.StandaloneStaticRuntime;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.http.ChecksumExtractor;

public final class MimaRuntime extends StandaloneStaticRuntime {

    public MimaRuntime() {
        // MIMA's Runtimes.getRuntime() orders ascending by priority and returns
        // first() — so a LOWER number wins. standalone-static is 40; pick 30 to
        // take precedence.
        super("tools.deps", 30);
    }

    private final Lookup lookup = new MemoizingRepositorySystemSupplierLookup() {
        @Override
        protected Map<String, TransporterFactory> getTransporterFactories(
                Map<String, ChecksumExtractor> extractors) {
            Map<String, TransporterFactory> m = new HashMap<>(super.getTransporterFactories(extractors));
            m.put("s3", new S3TransporterFactory());
            return m;
        }
    };

    @Override
    protected Lookup createRepositorySystemLookup(PreBoot preBoot) {
        return lookup;
    }
}