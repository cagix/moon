(defproject cdq "-SNAPSHOT"
  :repositories [["jitpack" "https://jitpack.io"]]
  :dependencies [
                 [com.badlogicgames.gdx/gdx                   "1.13.5"]
                 [com.badlogicgames.gdx/gdx-platform          "1.13.5" :classifier "natives-desktop"]
                 [com.badlogicgames.gdx/gdx-freetype          "1.13.5"]
                 [com.badlogicgames.gdx/gdx-freetype-platform "1.13.5" :classifier "natives-desktop"]
                 [com.kotcrab.vis/vis-ui "1.5.2"]
                 [space.earlygrey/shapedrawer "2.5.0"]

                 [com.badlogicgames.gdx/gdx-lwjgl3-angle "1.13.5"]

                 ;[com.badlogicgames.gdx/gdx-backend-lwjgl3 "1.13.5"]
                 ; commit 078ba3ca5e13b2ede4c4051f300f6f262a42dd00
                 ; Author: CuriousTorvald <curioustorvald@gmail.com>
                 ; Date:   Sun Apr 27 08:58:13 2025 +0900
                 ;
                 ;     WORLD1/2 keys for Lwjgl3 backends (#7456)
                 ;
                 ;     * WORLD1/2 keys for Lwjgl3 backends
                 ;
                 ;     * Apply formatter
                 ;
                 ;     ---------
                 ;
                 ;     Co-authored-by: GitHub Action <action@github.com>
                 ;     Co-authored-by: Tomski <tomwojciechowski@asidik.com>
                 [com.badlogicgames.jlayer/jlayer "1.0.1-gdx"]
                 [org.jcraft/jorbis "0.0.17"]
                 [org.lwjgl/lwjgl-glfw "3.3.3"]
                 [org.lwjgl/lwjgl-glfw "3.3.3" :classifier "natives-linux-arm32"]
                 [org.lwjgl/lwjgl-glfw "3.3.3" :classifier "natives-linux-arm64"]
                 [org.lwjgl/lwjgl-glfw "3.3.3" :classifier "natives-linux"]
                 [org.lwjgl/lwjgl-glfw "3.3.3" :classifier "natives-macos-arm64"]
                 [org.lwjgl/lwjgl-glfw "3.3.3" :classifier "natives-macos"]
                 [org.lwjgl/lwjgl-glfw "3.3.3" :classifier "natives-windows-x86"]
                 [org.lwjgl/lwjgl-glfw "3.3.3" :classifier "natives-windows"]
                 [org.lwjgl/lwjgl-jemalloc "3.3.3"]
                 [org.lwjgl/lwjgl-jemalloc "3.3.3" :classifier "natives-linux-arm32"]
                 [org.lwjgl/lwjgl-jemalloc "3.3.3" :classifier "natives-linux-arm64"]
                 [org.lwjgl/lwjgl-jemalloc "3.3.3" :classifier "natives-linux"]
                 [org.lwjgl/lwjgl-jemalloc "3.3.3" :classifier "natives-macos-arm64"]
                 [org.lwjgl/lwjgl-jemalloc "3.3.3" :classifier "natives-macos"]
                 [org.lwjgl/lwjgl-jemalloc "3.3.3" :classifier "natives-windows-x86"]
                 [org.lwjgl/lwjgl-jemalloc "3.3.3" :classifier "natives-windows"]
                 [org.lwjgl/lwjgl-openal "3.3.3"]
                 [org.lwjgl/lwjgl-openal "3.3.3" :classifier "natives-linux-arm32"]
                 [org.lwjgl/lwjgl-openal "3.3.3" :classifier "natives-linux-arm64"]
                 [org.lwjgl/lwjgl-openal "3.3.3" :classifier "natives-linux"]
                 [org.lwjgl/lwjgl-openal "3.3.3" :classifier "natives-macos-arm64"]
                 [org.lwjgl/lwjgl-openal "3.3.3" :classifier "natives-macos"]
                 [org.lwjgl/lwjgl-openal "3.3.3" :classifier "natives-windows-x86"]
                 [org.lwjgl/lwjgl-openal "3.3.3" :classifier "natives-windows"]
                 [org.lwjgl/lwjgl-opengl "3.3.3"]
                 [org.lwjgl/lwjgl-opengl "3.3.3" :classifier "natives-linux-arm32"]
                 [org.lwjgl/lwjgl-opengl "3.3.3" :classifier "natives-linux-arm64"]
                 [org.lwjgl/lwjgl-opengl "3.3.3" :classifier "natives-linux"]
                 [org.lwjgl/lwjgl-opengl "3.3.3" :classifier "natives-macos-arm64"]
                 [org.lwjgl/lwjgl-opengl "3.3.3" :classifier "natives-macos"]
                 [org.lwjgl/lwjgl-opengl "3.3.3" :classifier "natives-windows-x86"]
                 [org.lwjgl/lwjgl-opengl "3.3.3" :classifier "natives-windows"]
                 [org.lwjgl/lwjgl-stb "3.3.3"]
                 [org.lwjgl/lwjgl-stb "3.3.3" :classifier "natives-linux-arm32"]
                 [org.lwjgl/lwjgl-stb "3.3.3" :classifier "natives-linux-arm64"]
                 [org.lwjgl/lwjgl-stb "3.3.3" :classifier "natives-linux"]
                 [org.lwjgl/lwjgl-stb "3.3.3" :classifier "natives-macos-arm64"]
                 [org.lwjgl/lwjgl-stb "3.3.3" :classifier "natives-macos"]
                 [org.lwjgl/lwjgl-stb "3.3.3" :classifier "natives-windows-x86"]
                 [org.lwjgl/lwjgl-stb "3.3.3" :classifier "natives-windows"]
                 [org.lwjgl/lwjgl "3.3.3"]
                 [org.lwjgl/lwjgl "3.3.3" :classifier "natives-linux-arm32"]
                 [org.lwjgl/lwjgl "3.3.3" :classifier "natives-linux-arm64"]
                 [org.lwjgl/lwjgl "3.3.3" :classifier "natives-linux"]
                 [org.lwjgl/lwjgl "3.3.3" :classifier "natives-macos-arm64"]
                 [org.lwjgl/lwjgl "3.3.3" :classifier "natives-macos"]
                 [org.lwjgl/lwjgl "3.3.3" :classifier "natives-windows-x86"]
                 [org.lwjgl/lwjgl "3.3.3" :classifier "natives-windows"]



                 [cdq.malli "0.1"]
                 [clojure.rand "0.1"]
                 [com.github.cdorrat/reduce-fsm "fe1c914d68"]
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
  :aliases {"dev"      ["run" "-m" "clojure.dev-loop" "((requiring-resolve 'cdq.application/-main))"]
            "levelgen" ["run" "-m" "clojure.dev-loop" "((requiring-resolve 'cdq.levelgen/-main))"]
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
