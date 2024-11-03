(defproject moon "-SNAPSHOT"
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [gdl "0.1"]
                 [metosin/malli "0.13.0"]
                 [com.github.damn/reduce-fsm "eb1a2c1ff0"]]
  :plugins [[lein-hiera "2.0.0"]
            [lein-codox "0.10.8"]]
  :target-path "target/%s/" ; https://stackoverflow.com/questions/44246924/clojure-tools-namespace-refresh-fails-with-no-namespace-foo
  :uberjar-name "vampire.jar"
  :jvm-opts ["-Xms256m"
             "-Xmx256m"
             "-Dvisualvm.display.name=CDQ"
             "-XX:-OmitStackTraceInFastThrow" ; disappeared stacktraces
             ; for visualvm profiling
             ;"-Dcom.sun.management.jmxremote=true"
             ;"-Dcom.sun.management.jmxremote.port=20000"
             ;"-Dcom.sun.management.jmxremote.ssl=false"
             ;"-Dcom.sun.management.jmxremote.authenticate=false"
             ]
  :injections [(do
                (require 'malli.core) ; fix strange_malli_bug.clj
                (load "clojure/moon"))]
  :codox {:source-uri "https://github.com/damn/moon/blob/main/{filepath}#L{line}"
          :metadata {:doc/format :markdown}}
  ; this from engine, what purpose?
  ;:javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]
  :global-vars {*warn-on-reflection* false
                ;*unchecked-math* :warn-on-boxed
                ;*assert* false
                ;*print-level* 3
                }
  :profiles {:uberjar {:aot [moon.app]}}
  :main moon.app)

; * Notes

; * openjdk@8 stops working with long error
; * fireplace 'cp' evaluation does not work with openJDK17
; * using openjdk@11 right now and it works.
; -> report to vim fireplace?

; :FireplaceConnect 7888
