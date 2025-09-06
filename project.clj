(def libgdx-version "1.13.5")

(def main-namespace 'cdq.start)

(defproject moon "-SNAPSHOT"
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [[org.clojure/clojure "1.12.0"]
                 [clojure.gdx.backends.lwjgl "1.13.5"]
                 [com.badlogicgames.gdx/gdx                   ~libgdx-version]
                 [com.badlogicgames.gdx/gdx-freetype          ~libgdx-version]
                 [com.badlogicgames.gdx/gdx-freetype-platform ~libgdx-version :classifier "natives-desktop"]
                 [space.earlygrey/shapedrawer "2.5.0"]
                 [com.kotcrab.vis/vis-ui "1.5.2"]
                 [metosin/malli "0.13.0"]
                 [com.github.cdorrat/reduce-fsm "fe1c914d68"]
                 [com.github.damn/clojure.dev-loop "ef54a03"]
                 [fr.reuz/qrecord "0.1.0"]]

  :java-source-paths ["src"]

  :aliases {
            "dev"      ["run" "-m" "clojure.dev-loop" "((requiring-resolve 'cdq.start/-main))"]
            "levelgen" ["run" "-m" "clojure.dev-loop" "((requiring-resolve 'cdq.levelgen/-main))"]
            "ns"       ["hiera" ":layout" ":horizontal"]
            }

  :plugins [[lein-hiera "2.0.0"]
            [lein-codox "0.10.8"]]

  :target-path "target/%s/" ; https://stackoverflow.com/questions/44246924/clojure-tools-namespace-refresh-fails-with-no-namespace-foo

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

  :codox {:source-uri "https://github.com/damn/moon/blob/main/{filepath}#L{line}"
          :metadata {:doc/format :markdown}
          ;:namespaces [#"^gdl\."]
          }

  ; lein hiera :layout :horizontal :ignore "#{cdq.render}"
  ; unfortunately cannot exclude only 'cdq.render.*' , would like to do for entity/effect...

  ; this from engine, what purpose?
  ;:javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]

  :global-vars {*warn-on-reflection* true
                ;*unchecked-math* :warn-on-boxed
                ;*assert* false
                *print-level* 3
                }

  :profiles {:uberjar {:aot [~main-namespace]}}

  :uberjar-name "cdq.jar"

  :main ~main-namespace)

; * Notes

; * openjdk@8 stops working with long error
; * fireplace 'cp' evaluation does not work with openJDK17
; * using openjdk@11 right now and it works.
; -> report to vim fireplace?

; :FireplaceConnect 7888
