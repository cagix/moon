(defproject moon "-SNAPSHOT"
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [[org.clojure/clojure "1.12.0"]

                 ; only cdq.application(.desktop) => android, iOs project also possible ...
                 ; the rest of the code does not depend
                 ; => project 'cdq.application' => depends only on 'cdq.context.impl' & 'cdq.render'
                 ; & 'cdq.graphics' ???
                 ; => code should have minimum dependencies, on project level too -> see checkouts if it works properly
                 ; or too complicated
                 ; there was this github repo for clojure for setting boundaries.... rules ...
                 ; e.g. that package only can depend on packages x,y,z...
                 ; can write myself also simply ?
                 ; => main project visual map hierarchy generate & deploy somewhere.
                 [com.github.damn/clojure.gdx.backends.lwjgl "d2d6f14b13"]

                 ; only context create ...
                 [space.earlygrey/shapedrawer "2.5.0"]
                 [com.badlogicgames.gdx/gdx-freetype          "1.13.0"]
                 [com.badlogicgames.gdx/gdx-freetype-platform "1.13.0" :classifier "natives-desktop"]
                 [com.kotcrab.vis/vis-ui "1.5.2"]

                 ; only cdq.schema ...
                 [metosin/malli "0.13.0"]

                 ; ? also only context create? & otherwise supply 'cdq.fsm' with protocols ?
                 ; pass the actual fsm maps as arg (dont need to depend on it)
                 ; & use namespaced keywords
                 [com.github.cdorrat/reduce-fsm "fe1c914d68"]

                 ; only 'dev' ... not deploy/test/etc.
                 [com.github.damn/clojure.dev-loop "ef54a03"]

                 ]
  :java-source-paths ["src-java"]
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
  :codox {:source-uri "https://github.com/damn/moon/blob/main/{filepath}#L{line}"
          :metadata {:doc/format :markdown}}
  ; this from engine, what purpose?
  ;:javac-options ["-target" "1.7" "-source" "1.7" "-Xlint:-options"]
  :global-vars {*warn-on-reflection* true
                ;*unchecked-math* :warn-on-boxed
                ;*assert* false
                ;*print-level* 3
                }
  :profiles {:uberjar {:aot [cdq.context]}}
  #_:main #_cdq.context)

; * Notes

; * openjdk@8 stops working with long error
; * fireplace 'cp' evaluation does not work with openJDK17
; * using openjdk@11 right now and it works.
; -> report to vim fireplace?

; :FireplaceConnect 7888
