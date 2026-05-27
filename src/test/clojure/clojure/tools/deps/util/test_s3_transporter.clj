(ns clojure.tools.deps.util.test-s3-transporter
  (:require
    [clojure.test :refer [are deftest]]
    [clojure.tools.deps.util.s3-transporter :as s3t])
  (:import
    [org.eclipse.aether.repository RemoteRepository$Builder]))

(set! *warn-on-reflection* true)

(defn- repo [url]
  (.build (RemoteRepository$Builder. "test" "default" url)))

(deftest test-parse
  (are [u r b p] (= (merge {:region nil, :bucket nil, :repo-path nil}
                      (s3t/parse-url (repo u)))
                   {:region r :bucket b :repo-path p})
    "s3://BUCKET/PATH1/PATH2" nil "BUCKET" "PATH1/PATH2"
    "s3://BUCKET/PATH1/PATH2?region=REGION" "REGION" "BUCKET" "PATH1/PATH2"))

(comment
  (import
    '[java.io File]
    '[java.net URI]
    '[eu.maveniverse.maven.mima.context Runtimes ContextOverrides]
    '[org.eclipse.aether.repository RemoteRepository$Builder]
    '[org.eclipse.aether.spi.connector.transport Transporter GetTask TransportListener])

  ;; End-to-end probe: builds a MIMA context via the runtime registered by this
  ;; artifact's META-INF/services entry, asks it for a session, then uses the s3
  ;; transporter directly to GET a known object. Requires ambient AWS creds.
  (defn downloader
    [repo-id url path]
    (let [ctx (.create (.getRuntime Runtimes/INSTANCE) (.build (ContextOverrides/create)))
          session (.repositorySystemSession ctx)
          remote-repo (.build (RemoteRepository$Builder. repo-id "default" url))
          transporter (s3t/new-transporter session remote-repo)
          task (GetTask. (URI/create path))
          temp (File/createTempFile "dload-" nil)]
      (.setDataFile task temp)
      (.setListener task (proxy [TransportListener] []
                           (transportStarted [_ _])
                           (transportProgressed [_])))
      (.get ^Transporter transporter task)
      (slurp temp)))

  (downloader "datomic" "s3://datomic-releases-1fc2183a/maven/releases" "com/datomic/ion/0.9.35/ion-0.9.35.pom")
  (downloader "datomic" "s3://datomic-releases-1fc2183a/maven/releases?region=us-east-1" "com/datomic/ion/0.9.35/ion-0.9.35.pom")
  )
