(ns cdq.levelgen
  (:require [cdq.impl.db]
            [cdq.db :as db]
            [cdq.world-fns.creature-tiles]
            [clojure.color :as color]
            [clojure.edn :as edn]
            [clojure.lwjgl.system.configuration :as lwjgl-config]
            [clojure.gdx.backends.lwjgl.application :as application]
            [clojure.gdx.backends.lwjgl.application.config :as app-config]
            [clojure.gdx.files.utils :as files-utils]
            [clojure.gdx.graphics :as graphics]
            [clojure.gdx.input :as input]
            [clojure.gdx.maps.tiled :as tiled]
            [clojure.gdx.maps.tiled.renderers.orthogonal :as tm-renderer]
            [clojure.gdx.orthographic-camera :as camera]
            [clojure.gdx.viewport :as viewport]
            [clojure.java.io :as io]
            [clojure.scene2d.vis-ui :as vis-ui]
            [clojure.scene2d.vis-ui.text-button :as text-button]
            [clojure.scene2d.vis-ui.window :as window])
  (:import (cdq.ui Stage)
           (com.badlogic.gdx ApplicationListener
                             Gdx
                             Input$Keys)
           (com.badlogic.gdx.graphics OrthographicCamera
                                      Texture)
           (com.badlogic.gdx.graphics.g2d SpriteBatch
                                          TextureRegion)
           (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.utils Disposable)
           (com.badlogic.gdx.utils.viewport FitViewport)))

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
    (Disposable/.dispose tiled-map))
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
                                                          (let [[x y w h] bounds]
                                                            (TextureRegion. texture x y w h))
                                                          (TextureRegion. texture)))))
                        :textures textures)))
        tiled-map (:tiled-map level)
        ctx (assoc ctx :ctx/tiled-map tiled-map)]
    (tiled/set-visible! (tiled/get-layer tiled-map "creatures") true)
    (show-whole-map! ctx)
    ctx))


(defn- edit-window []
  (window/create
   {:title "Edit"
    :cell-defaults {:pad 10}
    :rows (for [level-fn level-fns]
            [{:actor (text-button/create
                      {:text (str "Generate " level-fn)
                       :on-clicked (fn [actor ctx]
                                     (let [stage (Actor/.getStage actor)
                                           new-ctx (generate-level ctx level-fn)]
                                       (set! (.ctx stage) new-ctx)))})}])
    :pack? true}))

(defn create!
  [{:keys [ctx/files
           ctx/input]
    :as ctx}]
  (vis-ui/load! {:skin-scale :x1})
  (let [ui-viewport (FitViewport. 1440 900 (OrthographicCamera.))
        sprite-batch (SpriteBatch.)
        stage (Stage. ui-viewport sprite-batch)
        _  (input/set-processor! input stage)
        tile-size 48
        world-unit-scale (float (/ tile-size))
        ctx (assoc ctx :ctx/stage stage)
        ctx (-> ctx
                (assoc :ctx/db (cdq.impl.db/create)))
        world-viewport (let [world-width  (* 1440 world-unit-scale)
                             world-height (* 900  world-unit-scale)]
                         (FitViewport. world-width
                                       world-height
                                       (doto (OrthographicCamera.)
                                         (.setToOrtho false world-width world-height))))
        ctx (assoc ctx
                   :ctx/world-viewport world-viewport
                   :ctx/ui-viewport ui-viewport
                   :ctx/textures (into {} (for [path (files-utils/search files
                                                                         {:folder "resources/"
                                                                          :extensions #{"png" "bmp"}})]
                                            [path (Texture. path)]))
                   :ctx/camera (viewport/camera world-viewport)
                   :ctx/color-setter (constantly [1 1 1 1])
                   :ctx/zoom-speed 0.1
                   :ctx/camera-movement-speed 1
                   :ctx/sprite-batch sprite-batch
                   :ctx/tiled-map-renderer (tm-renderer/create world-unit-scale sprite-batch))
        ctx (generate-level ctx initial-level-fn)]
    (.addActor (:ctx/stage ctx) (edit-window))
    ctx))

(defn dispose!
  [{:keys [ctx/sprite-batch
           ctx/tiled-map ]}]
  (vis-ui/dispose!)
  (Disposable/.dispose sprite-batch)
  (Disposable/.dispose tiled-map))

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
                                               (update (camera/position camera)
                                                       idx
                                                       #(f % camera-movement-speed))))]
    (if (input/key-pressed? input Input$Keys/LEFT)  (apply-position 0 -))
    (if (input/key-pressed? input Input$Keys/RIGHT) (apply-position 0 +))
    (if (input/key-pressed? input Input$Keys/UP)    (apply-position 1 +))
    (if (input/key-pressed? input Input$Keys/DOWN)  (apply-position 1 -))))

(defn- camera-zoom-controls! [{:keys [ctx/input
                                      ctx/camera
                                      ctx/zoom-speed]}]
  (when (input/key-pressed? input Input$Keys/MINUS)  (camera/inc-zoom! camera zoom-speed))
  (when (input/key-pressed? input Input$Keys/EQUALS) (camera/inc-zoom! camera (- zoom-speed))))

(defn render!
  [{:keys [ctx/graphics
           ctx/stage]
    :as ctx}]
  (let [ctx (if-let [new-ctx (.ctx stage)]
              new-ctx
              ctx)]
    (graphics/clear! graphics color/black)
    (draw-tiled-map! ctx)
    (camera-zoom-controls! ctx)
    (camera-movement-controls! ctx)
    (set! (.ctx stage) ctx)
    (.act     stage)
    (.draw    stage)
    (.ctx  stage)))

(defn resize!
  [{:keys [ctx/ui-viewport
           ctx/world-viewport]}
   width height]
  (viewport/update! ui-viewport    width height {:center? true})
  (viewport/update! world-viewport width height {:center? false}))

(def state (atom nil))

(defn -main []
  (lwjgl-config/set-glfw-library-name! "glfw_async")
  (application/create (reify ApplicationListener
                        (create [_]
                          (reset! state (create!
                                         {:ctx/graphics Gdx/graphics
                                          :ctx/input    Gdx/input
                                          :ctx/files    Gdx/files})))
                        (dispose [_]
                          (dispose! @state))
                        (render [_]
                          (swap! state render!))
                        (resize [_ width height]
                          (resize! @state width height))
                        (pause [_])
                        (resume [_]))
                      (app-config/create
                       {:title "Levelgen Test"
                        :window {:width 1440
                                 :height 900}
                        :fps 60})))
