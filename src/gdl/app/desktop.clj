(ns gdl.app.desktop
  (:require [clojure.edn]
            [clojure.java.io]
            [gdl.app]
            [gdl.context] ; schema needs to be loaded before db cdq.schema/validate!
            [gdl.platform.libgdx] ; interop extend types -> we write a game not want to do APi's
            [gdl.utils]
            [cdq.schema])) ; utils/... required -> same ->
; => all non game relevant code move into a clojure fork which is the _perfect_ language for 'Cyber Dungeon Quest'

(defn -main []
  (let [render-fns (map gdl.utils/require-ns-resolve
                        '[cdq.render/set-camera-on-player!
                          gdl.graphics/clear-screen
                          cdq.graphics.tiled-map/render
                          cdq.graphics/draw-world-view
                          gdl.graphics/draw-stage
                          gdl.context/update-stage
                          cdq.context/handle-player-input
                          cdq.context/update-mouseover-entity
                          cdq.context/update-paused-state
                          cdq.context/progress-time-if-not-paused
                          cdq.context/remove-destroyed-entities  ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                          gdl.context/check-camera-controls
                          cdq.context/check-ui-key-listeners])
        create-fns [[:gdl/db (fn [_context _config]
                               (let [properties-file (clojure.java.io/resource "properties.edn")
                                     schemas (-> "schema.edn" clojure.java.io/resource slurp clojure.edn/read-string)
                                     properties (-> properties-file slurp clojure.edn/read-string)]
                                 (assert (or (empty? properties)
                                             (apply distinct? (map :property/id properties))))
                                 (run! (partial cdq.schema/validate! schemas) properties)
                                 {:db/data (zipmap (map :property/id properties) properties)
                                  :db/properties-file properties-file
                                  :db/schemas schemas}))]
                    [:gdl/assets '[cdq.assets/create {:folder "resources/"
                                                      :type-exts {:sound   #{"wav"}
                                                                  :texture #{"png" "bmp"}}}]]
                    [:gdl.graphics/batch '[cdq.graphics.batch/create]]
                    [:gdl.graphics/shape-drawer-texture '[cdq.graphics.shape-drawer-texture/create]]
                    [:gdl.graphics/shape-drawer '[cdq.graphics.shape-drawer/create]]
                    [:gdl.graphics/cursors '[cdq.graphics.cursors/create {:cursors/bag                   ["bag001"       [0   0]]
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
                    [:gdl.graphics/default-font '[cdq.graphics.default-font/create {:file "fonts/exocet/films.EXL_____.ttf"
                                                                                    :size 16
                                                                                    :quality-scaling 2}]]
                    [:gdl.graphics/world-unit-scale '[cdq.graphics.world-unit-scale/create 48]]
                    [:gdl.graphics/tiled-map-renderer '[cdq.graphics.tiled-map-renderer/create]]
                    [:gdl.graphics/ui-viewport '[cdq.graphics.ui-viewport/create {:width 1440 :height 900}]]
                    [:gdl.graphics/world-viewport '[cdq.graphics.world-viewport/create {:width 1440 :height 900}]]
                    [:gdl.context/stage '[gdl.ui/setup-stage! {:skin-scale :x1
                                                               :actors [[cdq.ui.dev-menu/create]
                                                                        [cdq.ui.actionbar/create]
                                                                        [cdq.ui.hp-mana-bar/create]
                                                                        [cdq.ui.windows/create [cdq.ui.entity-info-window/create
                                                                                                cdq.widgets.inventory/create]]
                                                                        [cdq.ui.player-state/create]
                                                                        [cdq.ui.player-message/actor]]}]]
                    [:gdl.context/elapsed-time '[cdq.time/create]]
                    [:cdq.context/player-message '[cdq.ui.player-message/create* {:duration-seconds 1.5}]]
                    [:cdq.context/level '[cdq.level/create :worlds/uf-caves]]
                    [:cdq.context/error '[cdq.create/error*]]
                    [:cdq.context/tiled-map '[cdq.create/tiled-map*]]
                    [:cdq.context/explored-tile-corners '[cdq.create/explored-tile-corners*]]
                    [:cdq.context/grid '[cdq.create/grid*]]
                    [:cdq.context/raycaster '[cdq.create/create-raycaster]]
                    [:cdq.context/content-grid '[cdq.create/content-grid* {:cell-size 16}]]
                    [:cdq.context/entity-ids '[cdq.create/entity-ids*]]
                    [:cdq.context/factions-iterations '[cdq.create/factions-iterations* {:good 15 :evil 5}]]
                    [:cdq.context/player-eid '[cdq.create/player-eid*]]
                    [:cdq.context/enemies '[cdq.create/spawn-enemies!]]]]
    (.setIconImage (java.awt.Taskbar/getTaskbar)
                   (.getImage (java.awt.Toolkit/getDefaultToolkit)
                              (clojure.java.io/resource "moon.png")))
    (when com.badlogic.gdx.utils.SharedLibraryLoader/isMac
      (.set org.lwjgl.system.Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.
     (proxy [com.badlogic.gdx.ApplicationAdapter] []
       (create []
         (reset! gdl.app/state
                 (reduce (fn [context [k component]]
                           (let [f (if (vector? component)
                                     (gdl.utils/require-ns-resolve (component 0))
                                     component)
                                 params (if (and (vector? component) (= (count component) 2))
                                          (component 1)
                                          nil)]
                             (assoc context k (f context params))))
                         {}
                         create-fns)))

       (dispose []
         (doseq [[k value] @gdl.app/state
                 :when (gdl.utils/disposable? value)]
           ;(println "Disposing " k " - " value)
           (gdl.utils/dispose value)))

       (render []
         (swap! gdl.app/state (fn [context]
                                (reduce (fn [context f]
                                          (f context))
                                        context
                                        render-fns))))

       (resize [width height]
         (doseq [[k value] @gdl.app/state
                 :when (gdl.utils/resizable? value)]
           ;(println "Resizing " k " - " value)
           (gdl.utils/resize value width height))))
     (doto (com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.)
       (.setTitle "Cyber Dungeon Quest")
       (.setWindowedMode 1440 900)
       (.setForegroundFPS 60)))))
