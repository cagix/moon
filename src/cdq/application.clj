; => TODO ui is not disposed - this is the VisUI skin and is global state there - so just define it in my context ?
; then it doesn't know its disposed ? omg ...
;:gdl.context/ui-skin (com.kotcrab.vis.ui.VisUI/getSkin)

; TODO tiled-map also dispose if new game state add
; this also if world restarts !!

; => the comments are the problem!

; separate concerns - create -> all the instances - rest of code w. protocols (also tiledmap drawer etc.)
; * draw
; * update

; TODO move to config
; pass only order of [create], [render]
; save in context too
; so can change rendering or updates during the game
(ns cdq.application
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :refer [mac-osx?]]
            [clojure.java.awt :as awt]
            [clojure.java.io :as io]
            [clojure.lwjgl.system :as lwjgl-system]
            [clojure.platform.gdx]
            [clojure.utils :refer [dispose disposable? resize resizable?]]
            [cdq.create :as create]
            [cdq.render :as render])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [config (-> "config.edn" io/resource slurp edn/read-string)]
    (when-let [icon (:icon config)]
      (awt/set-taskbar-icon icon))
    (when (and mac-osx? (:glfw-async-on-mac-osx? config))
      (lwjgl-system/set-glfw-library-name "glfw_async"))
    (lwjgl/application (proxy [com.badlogic.gdx.ApplicationAdapter] []
                         (create []
                           (reset! state (create/game (gdx/context) config)))

                         (dispose []
                           ; don't dispose internal classes (:clojure.gdx/graphics,etc. )
                           ; which Lwjgl3Application will handle
                           ; otherwise app crashed w. asset-manager
                           ; which was disposed after graphics
                           ; -> so there is a certain order to cleanup...
                           (doseq [[k value] @state
                                   :when (and (not (= (namespace k) "clojure.gdx"))
                                              (disposable? value))]
                             (when (:log-dispose-lifecycle? config)
                               (println "Disposing " k " - " value))
                             (dispose value)))

                         (render []
                           (swap! state render/game))

                         (resize [width height]
                           (doseq [[k value] @state
                                   :when (resizable? value)]
                             (when (:log-resize-lifecycle? config)
                               (println "Resizing " k " - " value))
                             (resize value width height))))
                       config)))
