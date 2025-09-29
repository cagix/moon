(ns cdq.levelgen
  (:require [cdq.db :as db]
            [clojure.files.utils :as files-utils]
            [cdq.application.create.db]
            [cdq.application.create.vis-ui]
            [cdq.world-fns.creature-tiles]
            [com.badlogic.gdx.utils.disposable :as disposable]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.badlogic.gdx.graphics.orthographic-camera :as orthographic-camera]
            [com.badlogic.gdx.graphics :as graphics]
            [clojure.graphics.orthographic-camera :as camera]
            [clojure.graphics.color :as color]
            [com.badlogic.gdx.graphics.texture :as texture]
            [clojure.graphics.viewport]
            [com.badlogic.gdx.input :as input]
            [com.badlogic.gdx.scenes.scene2d :as scene2d]
            [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]
            [gdl.maps.tiled :as tiled]
            [gdl.maps.tiled.renderers.orthogonal :as tm-renderer]
            [com.badlogic.gdx :as gdx-ctx]
            [com.badlogic.gdx.backends.lwjgl3 :as lwjgl]
            [org.lwjgl.system.configuration :as lwjgl-system])
  (:import (com.badlogic.gdx ApplicationListener)))

(def initial-level-fn "world_fns/uf_caves.edn")

(def level-fns
  ["world_fns/vampire.edn"
   "world_fns/uf_caves.edn"
   "world_fns/modules.edn"])

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
  (let [level (let [[f params] (-> level-fn
                                   io/resource
                                   slurp
                                   edn/read-string)]
                ((requiring-resolve f)
                 (assoc params
                        :level/creature-properties (cdq.world-fns.creature-tiles/prepare
                                                    (db/all-raw db :properties/creatures)
                                                    (fn [{:keys [image/file image/bounds]}]
                                                      (assert file)
                                                      (assert (contains? textures file))
                                                      (let [texture (get textures file)]
                                                        (if bounds
                                                          (texture/region texture bounds)
                                                          (texture/region texture)))))
                        :textures textures)))
        tiled-map (:tiled-map level)
        ctx (assoc ctx :ctx/tiled-map tiled-map)]
    (tiled/set-visible! (tiled/get-layer tiled-map "creatures") true)
    (show-whole-map! ctx)
    ctx))


(defn- edit-window []
  {:actor/type :actor.type/window
   :title "Edit"
   :cell-defaults {:pad 10}
   :rows (for [level-fn level-fns]
           [{:actor {:actor/type :actor.type/text-button
                     :text (str "Generate " level-fn)
                     :on-clicked (fn [actor ctx]
                                   (let [stage (actor/get-stage actor)
                                         new-ctx (generate-level ctx level-fn)]
                                     (stage/set-ctx! stage new-ctx)))}}])
   :pack? true})

(defrecord Context [])

(defn create!
  [{:keys [ctx/files
           ctx/graphics
           ctx/input]}]
  (let [ctx (map->Context {:ctx/input input})
        ui-viewport (graphics/fit-viewport graphics 1440 900 (orthographic-camera/create))
        sprite-batch (graphics/sprite-batch graphics)
        stage (stage/create ui-viewport sprite-batch)
        _  (input/set-processor! input stage)
        tile-size 48
        world-unit-scale (float (/ tile-size))
        ctx (assoc ctx :ctx/stage stage)
        ctx (-> ctx
                cdq.application.create.db/do!
                cdq.application.create.vis-ui/do!)
        world-viewport (let [world-width  (* 1440 world-unit-scale)
                             world-height (* 900  world-unit-scale)]
                         (graphics/fit-viewport graphics
                                                world-width
                                                world-height
                                                (orthographic-camera/create :y-down? false
                                                                            :world-width world-width
                                                                            :world-height world-height)))
        ctx (assoc ctx
                   :ctx/graphics graphics
                   :ctx/world-viewport world-viewport
                   :ctx/ui-viewport ui-viewport
                   :ctx/textures (into {} (for [[path file-handle] (files-utils/search files
                                                                                       {:folder "resources/"
                                                                                        :extensions #{"png" "bmp"}})]
                                            [path (graphics/texture graphics file-handle)]))
                   :ctx/camera (:viewport/camera world-viewport)
                   :ctx/color-setter (constantly [1 1 1 1])
                   :ctx/zoom-speed 0.1
                   :ctx/camera-movement-speed 1
                   :ctx/sprite-batch sprite-batch
                   :ctx/tiled-map-renderer (tm-renderer/create world-unit-scale sprite-batch))
        ctx (generate-level ctx initial-level-fn)]
    (stage/add! (:ctx/stage ctx) (scene2d/build (edit-window)))
    ctx))

(defn dispose!
  [{:keys [ctx/sprite-batch
           ctx/tiled-map
           ctx/vis-ui]}]
  (disposable/dispose! vis-ui)
  (disposable/dispose! sprite-batch)
  (disposable/dispose! tiled-map))

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

(defn render!
  [{:keys [ctx/graphics
           ctx/stage]
    :as ctx}]
  (let [ctx (if-let [new-ctx (stage/get-ctx stage)]
              new-ctx
              ctx)]
    (graphics/clear! graphics color/black)
    (draw-tiled-map! ctx)
    (camera-zoom-controls! ctx)
    (camera-movement-controls! ctx)
    (stage/set-ctx! stage ctx)
    (stage/act!     stage)
    (stage/draw!    stage)
    (stage/get-ctx  stage)))

(defn resize!
  [{:keys [ctx/ui-viewport
           ctx/world-viewport]}
   width height]
  (clojure.graphics.viewport/update! ui-viewport    width height {:center? true})
  (clojure.graphics.viewport/update! world-viewport width height {:center? false}))

(def state (atom nil))

(defn -main []
  (lwjgl-system/set-glfw-library-name! "glfw_async")
  (lwjgl/application (reify ApplicationListener
                       (create [_]
                         (reset! state (create! {:ctx/audio    (gdx-ctx/audio)
                                                 :ctx/files    (gdx-ctx/files)
                                                 :ctx/graphics (gdx-ctx/graphics)
                                                 :ctx/input    (gdx-ctx/input)})))
                       (dispose [_]
                         (dispose! @state))
                       (render [_]
                         (swap! state render!))
                       (resize [_ width height]
                         (resize! @state width height))
                       (pause [_])
                       (resume [_]))
                     {:title "Levelgen test"
                      :windowed-mode {:width 1440 :height 900}
                      :foreground-fps 60}))
