(ns cdq.levelgen
  (:require [cdq.db :as db]
            [cdq.graphics]
            [cdq.files :as files]
            [cdq.impl.db]
            [cdq.world-fns.modules]
            [cdq.world-fns.uf-caves]
            [cdq.world-fns.tmx]
            [cdq.world-fns.creature-tiles]
            [com.badlogic.gdx.graphics.orthographic-camera :as camera]
            [com.badlogic.gdx.graphics.texture :as texture]
            [com.badlogic.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [com.badlogic.gdx.graphics.g2d.sprite-batch :as sprite-batch]
            [com.badlogic.gdx.scenes.scene2d.stage]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [com.badlogic.gdx.utils.viewport.fit-viewport :as viewport]
            [gdl.graphics :as graphics]
            [gdl.graphics.color :as color]
            [gdl.graphics.viewport]
            [gdl.input :as input]
            [org.lwjgl.system.configuration]
            [gdl.scene2d :as scene2d]
            [gdl.scene2d.stage :as stage]
            [gdl.tiled :as tiled]
            [com.kotcrab.vis.ui.vis-ui :as vis-ui]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl3]))

(def initial-level-fn [cdq.world-fns.uf-caves/create
                       {:tile-size 48
                        :texture-path "maps/uf_terrain.png"
                        :spawn-rate 0.02
                        :scaling 3
                        :cave-size 200
                        :cave-style :wide}])

(def level-fns
  [[#'cdq.world-fns.tmx/create {:tmx-file "maps/vampire.tmx"
                                :start-position [32 71]}]
   [#'cdq.world-fns.uf-caves/create
    {:tile-size 48
     :texture-path "maps/uf_terrain.png"
     :spawn-rate 0.02
     :scaling 3
     :cave-size 200
     :cave-style :wide}]
   [#'cdq.world-fns.modules/create
    {:world/map-size 5,
     :world/max-area-level 3,
     :world/spawn-rate 0.05}]])

(defn- show-whole-map! [{:keys [ctx/camera
                                ctx/tiled-map]}]
  (camera/set-position! camera
                        [(/ (:tiled-map/width  tiled-map) 2)
                         (/ (:tiled-map/height tiled-map) 2)])
  (camera/set-zoom! camera
                    (camera/calculate-zoom camera
                                           :left [0 0]
                                           :top [0 (:tiled-map/height tiled-map)]
                                           :right [(:tiled-map/width tiled-map) 0]
                                           :bottom [0 0])))

(def tile-size 48)

(defn- generate-level [{:keys [ctx/db
                               ctx/textures
                               ctx/tiled-map] :as ctx} level-fn]
  (when tiled-map
    (disposable/dispose! tiled-map))
  (let [level (let [[f params] level-fn]
                (f (assoc params
                          :level/creature-properties (cdq.world-fns.creature-tiles/prepare
                                                      (db/all-raw db :properties/creatures)
                                                      (reify cdq.graphics/Graphics
                                                        (texture-region [_ {:keys [image/file image/bounds]}]
                                                          (assert file)
                                                          (assert (contains? textures file))
                                                          (let [texture (get textures file)]
                                                            (if bounds
                                                              (texture/region texture bounds)
                                                              (texture/region texture))))))
                          :textures textures)))
        tiled-map (:tiled-map level)
        ctx (assoc ctx :ctx/tiled-map tiled-map)]
    (tiled/set-visible! (tiled/get-layer tiled-map "creatures") true)
    (show-whole-map! ctx)
    ctx))

(def state (atom nil))

(require 'com.badlogic.gdx.scenes.scene2d.ui.table)
(require 'com.badlogic.gdx.scenes.scene2d.ui.widget-group)
(defn- edit-window []
  {:actor/type :actor.type/window
   :title "Edit"
   :cell-defaults {:pad 10}
   :rows (for [level-fn level-fns]
           [{:actor {:actor/type :actor.type/text-button
                     :text (str "Generate " (first level-fn))
                     :on-clicked (fn [_actor _ctx]
                                   (swap! state (fn [ctx] (generate-level ctx level-fn))))}}])
   :pack? true})

(defrecord Context [])

(defn create!
  [{:keys [clojure.gdx/files
           clojure.gdx/input
           clojure.gdx/graphics]}]
  (let [ctx (map->Context {:ctx/input input})
        ui-viewport (viewport/create 1440 900 (camera/orthographic))
        sprite-batch (sprite-batch/create)
        stage (com.badlogic.gdx.scenes.scene2d.stage/create ui-viewport sprite-batch state)
        _  (input/set-processor! input stage)
        tile-size 48
        world-unit-scale (float (/ tile-size))
        ctx (assoc ctx :ctx/stage stage)
        ctx (assoc ctx :ctx/db (cdq.impl.db/create {:schemas "schema.edn"
                                                    :properties "properties.edn"}))
        world-viewport (let [world-width  (* 1440 world-unit-scale)
                             world-height (* 900  world-unit-scale)]
                         (viewport/create world-width
                                          world-height
                                          (camera/orthographic :y-down? false
                                                               :world-width world-width
                                                               :world-height world-height)))
        ctx (assoc ctx
                   :ctx/graphics graphics
                   :ctx/world-viewport world-viewport
                   :ctx/ui-viewport ui-viewport
                   :ctx/vis-ui (vis-ui/load! {:skin-scale :x1})
                   :ctx/textures (into {} (for [[path file-handle] (files/search files
                                                                                 {:folder "resources/"
                                                                                  :extensions #{"png" "bmp"}})]
                                            [path (texture/from-file file-handle)]))
                   :ctx/camera (:viewport/camera world-viewport)
                   :ctx/color-setter (constantly [1 1 1 1])
                   :ctx/zoom-speed 0.1
                   :ctx/camera-movement-speed 1
                   :ctx/sprite-batch sprite-batch
                   :ctx/tiled-map-renderer (tm-renderer/create world-unit-scale sprite-batch))
        ctx (generate-level ctx initial-level-fn)]
    (stage/add! (:ctx/stage ctx) (scene2d/build (edit-window)))
    (reset! state ctx)))

(defn dispose! []
  ; TODO ? disposing properly everything cdq.start stuff??
  ; batch, cursors, default-font, shape-drawer-texture, etc.
  (com.badlogic.gdx.utils.Disposable/.dispose (:ctx/vis-ui @state))
  (let [{:keys [ctx/sprite-batch
                ctx/tiled-map]} @state]
    (disposable/dispose! sprite-batch) ; TODO that wont work anymore -> and one more fn so have to move it together?
    (disposable/dispose! tiled-map)))

(defn- draw-tiled-map! [{:keys [ctx/color-setter
                                ctx/tiled-map
                                ctx/tiled-map-renderer
                                ctx/world-viewport]}]
  (tm-renderer/draw! tiled-map-renderer
                     world-viewport
                     tiled-map
                     color-setter))

(defn- camera-movement-controls! [{:keys [ctx/input
                                          ctx/camera
                                          ctx/camera-movement-speed]}]
  (let [apply-position (fn [idx f]
                         (camera/set-position! camera
                                               (update (:camera/position camera)
                                                       idx
                                                       #(f % camera-movement-speed))))]
    (if (input/key-pressed? input :left)  (apply-position 0 -))
    (if (input/key-pressed? input :right) (apply-position 0 +))
    (if (input/key-pressed? input :up)    (apply-position 1 +))
    (if (input/key-pressed? input :down)  (apply-position 1 -))))

(defn- camera-zoom-controls! [{:keys [ctx/input
                                      ctx/camera
                                      ctx/zoom-speed]}]
  (when (input/key-pressed? input :minus)  (camera/inc-zoom! camera zoom-speed))
  (when (input/key-pressed? input :equals) (camera/inc-zoom! camera (- zoom-speed))))

(defn render! []
  (graphics/clear! (:ctx/graphics @state) color/black)
  (draw-tiled-map! @state)
  (camera-zoom-controls! @state)
  (camera-movement-controls! @state)
  (stage/act! (:ctx/stage @state))
  (stage/draw! (:ctx/stage @state)))

(defn resize! [width height]
  (let [{:keys [ctx/ui-viewport
                ctx/world-viewport]} @state]
    (gdl.graphics.viewport/update! ui-viewport    width height {:center? true})
    (gdl.graphics.viewport/update! world-viewport width height {:center? false})))

(defn -main []
  (org.lwjgl.system.configuration/set-glfw-library-name! "glfw_async")
  (lwjgl3/start-application!
   {:create! create!
    :dispose! dispose!
    :render! render!
    :resize! resize!
    :resume! (fn [])
    :pause! (fn [])}
   {:title "Levelgen test"
    :windowed-mode {:width 1440 :height 900}
    :foreground-fps 60}
   []))
