(ns gdl.app.desktop
  (:require [clojure.edn :as edn]
            [clojure.gdx :as gdx]
            [clojure.java.io :as io]
            [gdl.app :as app]
            [gdl.platform.libgdx]
            [gdl.utils :refer [dispose disposable? resize resizable? require-ns-resolve]])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [config (-> "config.edn" io/resource slurp edn/read-string)
        render-fns (map require-ns-resolve '[cdq.render/set-camera-on-player!
                                             gdl.graphics/clear-screen
                                             cdq.graphics.tiled-map/render
                                             cdq.graphics/draw-world-view
                                             gdl.graphics/draw-stage
                                             gdl.context/update-stage
                                             cdq.context/handle-player-input
                                             cdq.context/update-mouseover-entity
                                             cdq.context/update-paused-state
                                             cdq.context/progress-time-if-not-paused
                                             cdq.context/remove-destroyed-entities  ; do not pause this as for example pickup item, should be destroyed.
                                             gdl.context/check-camera-controls
                                             cdq.context/check-ui-key-listeners])
        create-fns '[[:gdl.db/schemas [gdl.context/load-schemas]]
                     [:gdl/db [cdq.db/create {:schema "schema.edn"
                                              :properties "properties.edn"}]]
                     ; ah ok ! gdl.context is not loaded yet at this point
                     ; so :s/map is not available
                     [:gdl/assets [cdq.assets/create {:folder "resources/"
                                                      :type-exts {:sound   #{"wav"}
                                                                  :texture #{"png" "bmp"}}}]]
                     [:gdl.graphics/batch [cdq.graphics.batch/create]]
                     [:gdl.graphics/shape-drawer-texture [cdq.graphics.shape-drawer-texture/create]]
                     [:gdl.graphics/shape-drawer [cdq.graphics.shape-drawer/create]]
                     [:gdl.graphics/cursors [cdq.graphics.cursors/create {:cursors/bag                   ["bag001"       [0   0]]
                                                                          :cursors/black-x               ["black_x"      [0   0]]
                                                                          :cursors/default               ["default"      [0   0]]
                                                                          :cursors/denied                ["denied"       [16 16]]
                                                                          :cursors/hand-before-grab      ["hand004"      [4  16]]
                                                                          :cursors/hand-before-grab-gray ["hand004_gray" [4  16]]
                                                                          :cursors/hand-grab             ["hand003"      [4  16]]
                                                                          :cursors/move-window           ["move002"      [16 16]]
                                                                          :cursors/no-skill-selected     ["denied003"    [0   0]]
                                                                          :cursors/over-button           ["hand002"      [0   0]]
                                                                          :cursors/sandclock             ["sandclock"    [16 16]]
                                                                          :cursors/skill-not-usable      ["x007"         [0   0]]
                                                                          :cursors/use-skill             ["pointer004"   [0   0]]
                                                                          :cursors/walking               ["walking"      [16 16]]}]]
                     [:gdl.graphics/default-font [cdq.graphics.default-font/create {:file "fonts/exocet/films.EXL_____.ttf"
                                                                                    :size 16
                                                                                    :quality-scaling 2}]]
                     [:gdl.graphics/world-unit-scale [cdq.graphics.world-unit-scale/create 48]]
                     [:gdl.graphics/tiled-map-renderer [cdq.graphics.tiled-map-renderer/create]]
                     [:gdl.graphics/ui-viewport [cdq.graphics.ui-viewport/create {:width 1440
                                                                                  :height 900}]]
                     [:gdl.graphics/world-viewport [cdq.graphics.world-viewport/create {:width 1440
                                                                                        :height 900}]]
                     [:gdl.context/stage [gdl.ui/setup-stage! {:skin-scale :x1
                                                               :actors [cdq.ui.dev-menu/create
                                                                        cdq.ui.actionbar/create
                                                                        cdq.ui.hp-mana-bar/create
                                                                        cdq.ui.windows/create
                                                                        cdq.ui.player-state/create
                                                                        cdq.ui.player-message/create]}]]
                     [:gdl.context/elapsed-time [cdq.time/create]]
                     [:cdq.context/player-message [cdq.ui.player-message/create* {:duration-seconds 1.5}]]
                     [:cdq.context/level [cdq.level/create :worlds/uf-caves]]
                     [:cdq.context/error [cdq.create/error*]]
                     [:cdq.context/tiled-map [cdq.create/tiled-map*]]
                     [:cdq.context/explored-tile-corners [cdq.create/explored-tile-corners*]]
                     [:cdq.context/grid [cdq.create/grid*]]
                     [:cdq.context/raycaster [cdq.create/create-raycaster]]
                     [:cdq.context/content-grid [cdq.create/content-grid* {:cell-size 16}]]
                     [:cdq.context/entity-ids [cdq.create/entity-ids*]]
                     [:cdq.context/factions-iterations [cdq.create/factions-iterations* {:good 15 :evil 5}]]
                     [:cdq.context/player-eid [cdq.create/player-eid*]]
                     [:cdq.context/enemies [cdq.create/spawn-enemies!]]]]
    (.setIconImage (java.awt.Taskbar/getTaskbar)
                   (.getImage (java.awt.Toolkit/getDefaultToolkit)
                              (io/resource "moon.png")))
    (when com.badlogic.gdx.utils.SharedLibraryLoader/isMac
      (.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.
     (proxy [com.badlogic.gdx.ApplicationAdapter] []
       (create []
         (reset! state
                 (reduce (fn [context [k [var params]]]
                           (println (keys context))
                           (let [f (require-ns-resolve var)]
                             (assert f (str var))
                             (assoc context k (f context params))))
                         (assoc (into {}
                                      (for [[k v] (gdx/context)]
                                        [(keyword (str "gdl/" (name k))) v]))
                                :gdl/config config)
                         create-fns)))

       (dispose []
         ; don't dispose internal classes (:gdl/graphics,etc. )
         ; which Lwjgl3Application will handle
         ; otherwise app crashed w. asset-manager
         ; which was disposed after graphics
         ; -> so there is a certain order to cleanup...
         (doseq [[k value] @state
                 :when (and (not (= (namespace k) "gdl"))
                            (disposable? value))]
           (when (:log-dispose-lifecycle? config)
             (println "Disposing " k " - " value))
           (dispose value)))

       (render []
         (swap! state (fn [context]
                        (reduce (fn [context f]
                                  (f context))
                                context
                                render-fns))))

       (resize [width height]
         (doseq [[k value] @state
                 :when (resizable? value)]
           (when (:log-resize-lifecycle? config)
             (println "Resizing " k " - " value))
           (resize value width height))))
     (doto (com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.)
       (.setTitle "Cyber Dungeon Quest")
       (.setWindowedMode 1440 900)
       (.setForegroundFPS 60)))))

(defn post-runnable [f]
  (app/post-runnable (:gdl/app @state)
                     #(f @state)))
