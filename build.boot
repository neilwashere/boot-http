(set-env!
 :source-paths #{"src" "test"}
 :dependencies '[[org.clojure/clojure "1.7.0"  :scope "provided"]
                 [boot/core           "2.3.0"  :scope "provided"]
                 [adzerk/bootlaces    "0.1.12" :scope "test"]
                 [adzerk/boot-test    "1.0.4"  :scope "test"]])

(require
 '[adzerk.bootlaces :refer :all] ;; tasks: build-jar push-snapshot push-release
 '[adzerk.boot-test :refer :all]
 '[pandeiro.boot-http :refer :all])

(def +version+ "0.8.0-SNAPSHOT")

(bootlaces! +version+)

(task-options!
 pom {:project     'pandeiro/boot-http
      :version     +version+
      :description "Boot task to serve HTTP."
      :url         "https://github.com/pandeiro/boot-http"
      :scm         {:url "https://github.com/pandeiro/boot-http"}
      :license     {"Eclipse Public License" "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask test-boot-http []
  (merge-env! :dependencies serve-deps)
  (test :namespaces #{'pandeiro.boot-http-tests}))
