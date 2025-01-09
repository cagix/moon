
; Now I get it -
; the 'language' of game development is
; https://javadoc.io/static/com.badlogicgames.gdx/gdx/1.13.0/index.html
; those amazing package names
; starting more general getting more specific
; thats the 'gdl'

; now the 'scenes.scene2d' and 'maps.tiled' and 'assets.asset-manager' makes sense
; if we want the perfect language for writing games we orient us at that
; ...

; a 'abstract' game engine.




;;;;;

; Okay: 'clojure.gdx' is just a helper wherever it can make things simpler, _not_ an API

; for simple things like disposable or interfaces we define _new_ protocols in 'gdl' with same naming conventions

; they will get extend-type'd

; but also we want to create a language just for 'cdq' right /... only what we need ....

; but all 'gdx'etc stuff can be extended in my own 'gdl' with same naming conventions as gdx
; so it can be extended & good

; so the 'clojure.gdx' is just a helper which is used _inside_ the extend statements?



; 1. fix reflections / move functions where used (gdl/actor/gdx?)
; 2. gdl separate project unifying the 'clojure.gdx' stuff
; => cdq does not use clojure.gdx directly then but new layer 'gdl' ?



; => the _perfect_ cyber dungeon quest language
; => move stuff closer to 'clojure.*'
; => I don't think we need to fork clojure itself ?
; or do we have so many imports???

(defproject moon "-SNAPSHOT"
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [

                 ; the question is what should 'gdl' wrap ?
                 ; obviously all libgdx and vis-ui related stuff !?
                 ; => no, everything !
                 ; the language for 'cdq' to be dependency free .....

                 ; _ the outside world _ the 'earth' _ is given through a 'trunk' whichs is gdl
                 ; and the dependencies are the roots
                 ; this project _cdq_ sees only the 'trunk' and not any other dependency than 'cdq' or 'gdl' !!!!!
                 ; even grid2d let's see we redirect 'gdl.data.g2d'  ?

                 ; clojure.utils -> gdl.utils or cdq.utils

                 [org.clojure/clojure "1.12.0"]
                 [com.badlogicgames.gdx/gdx "1.13.0"]
                 [com.badlogicgames.gdx/gdx-platform       "1.13.0" :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-backend-lwjgl3 "1.13.0"]
                 [space.earlygrey/shapedrawer "2.5.0"]
                 ; TODO release lib with docs
                 [com.badlogicgames.gdx/gdx-freetype          "1.13.0"]
                 ; TODO this manual ?
                 [com.badlogicgames.gdx/gdx-freetype-platform "1.13.0" :classifier "natives-desktop"]
                 ; - no dep ? - gdl -
                 [com.kotcrab.vis/vis-ui "1.5.2"]

                 ; this only gdl.db ?
                 [metosin/malli "0.13.0"]
                 [org.clj-commons/pretty "3.2.0"]
                 [clojure.gdx.dev-loop "-SNAPSHOT"]
                 [com.github.damn/grid2d "1.0"]
                 [com.github.damn/reduce-fsm "eb1a2c1ff0"] ; TODO use updated main version
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
  #_:main #_cdq.context
  )

; * Notes

; * openjdk@8 stops working with long error
; * fireplace 'cp' evaluation does not work with openJDK17
; * using openjdk@11 right now and it works.
; -> report to vim fireplace?

; :FireplaceConnect 7888
