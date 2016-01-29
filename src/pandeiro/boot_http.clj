(ns pandeiro.boot-http
  {:boot/export-tasks true}
  (:require
   [boot.pod           :as pod]
   [boot.util          :as util]
   [boot.core          :as core :refer [deftask]]
   [boot.task.built-in :as task]))

(def default-port 3000)

(def serve-deps
  '[[ring/ring-core "1.4.0"]
    [ring/ring-devel "1.4.0"]])

(def jetty-dep
  '[ring/ring-jetty-adapter "1.4.0"])

(def httpkit-dep
  '[http-kit "2.1.19"])

(def aleph-dep
  '[aleph "0.4.1-beta3"])

(def nrepl-dep
  '[org.clojure/tools.nrepl "0.2.11"])

(defn- silence-jetty! []
  (.put (System/getProperties) "org.eclipse.jetty.LEVEL" "WARN"))

(deftask serve
  "Start a web server on localhost, serving resources and optionally a directory.
  Listens on port 3000 by default."

  [d dir           PATH str  "The directory to serve; created if doesn't exist."
   H handler       SYM  sym  "The ring handler to serve."
   i init          SYM  sym  "A function to run prior to starting the server."
   c cleanup       SYM  sym  "A function to run after the server stops."
   r resource-root ROOT str  "The root prefix when serving resources from classpath"
   p port          PORT int  "The port to listen on. (Default: 3000)"
   S server        KW   kw   "use :httpkit or :aleph instead of Jetty (default)"
   s silent             bool "Silent-mode (don't output anything)"
   R reload             bool "Reload modified namespaces on each request."
   n nrepl         REPL edn  "nREPL server parameters e.g. \"{:port 3001, :bind \"0.0.0.0\"}\""]

  (let [port        (or port default-port)
        server-dep  (case server
                      :httpkit httpkit-dep
                      :aleph   aleph-dep
                      jetty-dep)
        deps        (cond-> serve-deps
                      true        (conj server-dep)
                      (seq nrepl) (conj nrepl-dep))
        worker      (pod/make-pod (update-in (core/get-env) [:dependencies]
                                             into deps))
        server-name (case server
                      :httpkit "HTTP Kit"
                      :aleph "Aleph"
                      "Jetty")
        start       (delay
                     (pod/with-eval-in worker
                       (require '[pandeiro.boot-http.impl :as http]
                                '[pandeiro.boot-http.util :as u])
                       (when '~init
                         (u/resolve-and-invoke '~init))
                       (def http-server
                         (http/server
                          {:dir ~dir, :port ~port, :handler '~handler,
                           :reload '~reload, :env-dirs ~(vec (:directories pod/env)), :engine ~server,
                           :resource-root ~resource-root}))
                       (def nrepl-server
                         (when ~nrepl
                           (http/nrepl-server {:nrepl ~nrepl}))))
                     (when-not silent
                       (util/info
                        "Started %s on http://localhost:%d\n"
                        server-name port)))]
    (when (and silent (not server))
      (silence-jetty!))
    (core/cleanup
     (pod/with-eval-in worker
       (when http-server
         (when-not silent
           (util/info "Stopping %s\n" server-name)))
       (when nrepl-server
         (util/info "Stopping boot-http nREPL server")
         (.stop nrepl-server))
       (if ~server
         (http-server)
         (.stop http-server))
       (when '~cleanup
         (u/resolve-and-invoke '~cleanup))))
    (core/with-pre-wrap fileset
      @start
      fileset)))
