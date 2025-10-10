; This is the trick - 1 class one API
; no "pass gdx" or "vector2/convert" or "lwjgl-config" !!!!
; because _some_ 1 class-APIs actually do something (table cell, lwjgl config, etc. ?)
; only smoothen out API / just names / consistency
; one _tiny_ step
; what is the smallest step I can take
; make it into a function/library
; e.g. colors not together with float-bits

; => minimal dependencies
; => maximum design


(defproject cdq "-SNAPSHOT"
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [
                 [com.github.cdorrat/reduce-fsm "fe1c914d68"]

                 ; _SMALL_ libraries !!!!
                 ; hundreds !
                 ; powerful !
                 ; realesas3 your whole game as l;ibraryes (world, audio, graphics,ui, editor , tests)

                 ; but keep it in 1 project !!!

                 ; => pull out _1_ class _1_ namespace 'views/facades/simler models'
                 ; then can do package-based e.g. vis-ui one 'API' namespace?
                 ; or combined creator (full API for cdq wrapper ? what is visible what noget-t ?)


                 ; TODO FIXME !! THOSE SMALL HELPERS LIKE vector2 or the smoohtening of names
                 ; or the potential to see a simpler API !

                 ; consistency starts at the _bottom_ e.g. Lwjgl3Application/create does _nothing_
                 ; but I can independently reqrite it !
                 ; consistency ! rules !
                 #_(comment

                  (ns clojure.vis-ui.window
                    (:import (com.kotcrab.vis.ui.widget VisWindow)))

                  (defn create
                    [{:keys [title
                             close-button?
                             center?
                             close-on-escape?]}]
                    (let [show-window-border? true
                          window (VisWindow. ^String title (boolean show-window-border?))]
                      (when close-button?    (.addCloseButton window))
                      (when center?          (.centerWindow   window))
                      (when close-on-escape? (.closeOnEscape  window))
                      window))
                  )

                 ; => 'cdq.ui' or 'gdl.ui' is then a combination between
                 ; scene2d and visui
                 ; and you should pass the skin
                 [com.badlogicgames.gdx/gdx-backend-lwjgl3    "1.13.5"]
                 [com.badlogicgames.gdx/gdx-platform          "1.13.5" :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-freetype          "1.13.5"]
                 [com.badlogicgames.gdx/gdx-freetype-platform "1.13.5" :classifier "natives-desktop"]
                 [space.earlygrey/shapedrawer "2.5.0"]
                 [com.kotcrab.vis/vis-ui "1.5.2"]
                 [com.github.damn/clojure.grid2d "538fc4c44b"]
                 [com.github.damn/clojure.math.raycaster "0956fc0e9b"]
                 [com.github.damn/clojure.math.vector2 "9b3fd73f9b"]
                 [com.github.damn/clojure.rand "6a273c942b"]
                 [com.github.damn/malli.utils "5da493efcb"]
                 [fr.reuz/qrecord "0.1.0"]
                 [org.clj-commons/pretty "3.2.0"]
                 [org.clojure/clojure "1.12.0"]
                 ; dev-only
                 [nrepl "0.9.0"]
                 [org.clojure/tools.namespace "1.3.0"]
                 [lein-hiera "2.0.0"]
                 [com.github.damn/clojure.dev-loop "ef54a03"]
                 ;
                 ]
  :java-source-paths ["src"]
  :aliases {"dev"      ["run" "-m" "clojure.dev-loop" "((requiring-resolve 'cdq.start/-main))"]
            "levelgen" ["run" "-m" "clojure.dev-loop" "((requiring-resolve 'cdq.levelgen/-main))"]
            "nsgraph"  ["run" "-m" "clojure.dev-loop" "((requiring-resolve 'ns-graph.core/-main))"]
            "ns"       ["hiera" ":layout" ":horizontal"]}
  :plugins [[lein-hiera "2.0.0"]
            [lein-codox "0.10.8"]]
  :target-path "target/%s/" ; https://stackoverflow.com/questions/44246924/clojure-tools-namespace-refresh-fails-with-no-namespace-foo
  :jvm-opts ["-Xms512m" ; 256 for game ok, lein hiera in repl needs more
             "-Xmx512m"
             "-Dvisualvm.display.name=CDQ"
             "-XX:-OmitStackTraceInFastThrow" ; disappeared stacktraces
             ; for visualvm profiling
             ;"-Dcom.sun.management.jmxremote=true"
             ;"-Dcom.sun.management.jmxremote.port=20000"
             ;"-Dcom.sun.management.jmxremote.ssl=false"
             ;"-Dcom.sun.management.jmxremote.authenticate=false"
             ]
  :codox {:source-uri "https://github.com/damn/cdq/blob/main/{filepath}#L{line}"
          :metadata {:doc/format :markdown}}
  ; lein hiera :layout :horizontal :ignore "#{cdq.render}"
  ; unfortunately cannot exclude only 'cdq.render.*' , would like to do for entity/effect...
  ; this from engine, what purpose?
  ;:javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]
  :global-vars {*warn-on-reflection* true
                ;*unchecked-math* :warn-on-boxed
                ;*assert* false
                *print-level* 3}
  :profiles {:uberjar {:aot [cdq.application]}}
  :uberjar-name "cdq.jar"
  :main cdq.application)

; * Notes

; * openjdk@8 stops working with long error
; * fireplace 'cp' evaluation does not work with openJDK17
; * using openjdk@11 right now and it works.
; -> report to vim fireplace?

; :FireplaceConnect 7888
