(ns cdq.g
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity] ; -> protocolize
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera] ; -> graphics ?
            [cdq.stage :as stage] ; -> protocolize
            [cdq.math :refer [circle->outer-rectangle]]
            [cdq.property :as property]
            [cdq.utils :as utils :refer [sort-by-order
                                         pretty-pst
                                         bind-root]]
            [cdq.world :as world] ; -> protocolize
            [cdq.world.grid :as grid]
            [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx Input$Keys)
           (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application Lwjgl3ApplicationConfiguration)
           (com.badlogic.gdx.graphics Texture)
           (com.badlogic.gdx.utils Disposable SharedLibraryLoader Os)
           (java.awt Taskbar Toolkit)
           (org.lwjgl.system Configuration)))

(defn- reset-game! [world-fn]
  (bind-root #'ctx/elapsed-time 0)
  (bind-root #'ctx/stage ((requiring-resolve 'cdq.impl.stage/create!)))
  (bind-root #'ctx/world ((requiring-resolve 'cdq.impl.world/create) ((requiring-resolve world-fn))))
  ((requiring-resolve 'cdq.game.spawn-enemies/do!))
  ((requiring-resolve 'cdq.game.spawn-player/do!)))

(bind-root #'ctx/reset-game! reset-game!)

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(def ^:private factions-iterations {:good 15 :evil 5})

(defn- draw-before-entities! []
  (let [g ctx/graphics
        cam (:camera (:world-viewport g))
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when tile-grid?
      (graphics/draw-grid g
                          (int left-x) (int bottom-y)
                          (inc (int (:width  (:world-viewport g))))
                          (+ 2 (int (:height (:world-viewport g))))
                          1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell ((:grid ctx/world) [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (graphics/draw-filled-rectangle g x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (graphics/draw-filled-rectangle g x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (graphics/draw-filled-rectangle g x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- geom-test [g]
  (let [position (graphics/world-mouse-position g)
        radius 0.8
        circle {:position position :radius radius}]
    (graphics/draw-circle g position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells (:grid ctx/world) circle))]
      (graphics/draw-rectangle g x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (graphics/draw-rectangle g x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [g]
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (graphics/world-mouse-position g))
          cell ((:grid ctx/world) [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (graphics/draw-rectangle g x y 1 1
                                 (case (:movement @cell)
                                   :air  [1 1 0 0.5]
                                   :none [1 0 0 0.5]))))))

(defn- draw-after-entities! []
  #_(geom-test ctx/graphics)
  (highlight-mouseover-tile ctx/graphics))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [g entity color]
  (let [[x y] (:left-bottom entity)]
    (graphics/draw-rectangle g x y (:width entity) (:height entity) color)))

; I can create this later after loading all the component namespaces
; just go through the systems
; and see which components are signed up for it
; => I get an overview what is rendered how...
#_(def ^:private entity-render-fns
  {:below {:entity/mouseover? draw-faction-ellipse
           :player-item-on-cursor draw-world-item-if-exists
           :stunned draw-stunned-circle}
   :default {:entity/image draw-image-as-of-body
             :entity/clickable draw-text-when-mouseover-and-text
             :entity/line-render draw-line}
   :above {:npc-sleeping draw-zzzz
           :entity/string-effect draw-text
           :entity/temp-modifier draw-filled-circle-grey}
   :info {:entity/hp draw-hpbar-when-mouseover-and-not-full
          :active-skill draw-skill-image-and-active-effect}})

(defn- render-entities! []
  (let [entities (map deref (:active-entities ctx/world))
        player @ctx/player-eid
        g ctx/graphics]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              world/render-z-order)
            render! [entity/render-below!
                     entity/render-default!
                     entity/render-above!
                     entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (world/line-of-sight? player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect g entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (render! component entity g))
       (catch Throwable t
         (draw-body-rect g entity :red)
         (pretty-pst t))))))

(defn- update-mouseover-entity! []
  (let [new-eid (if (stage/mouse-on-actor? ctx/stage)
                  nil
                  (let [player @ctx/player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities (:grid ctx/world)
                                                           (graphics/world-mouse-position ctx/graphics)))]
                    (->> world/render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(world/line-of-sight? player @%))
                         first)))]
    (when-let [eid ctx/mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root #'ctx/mouseover-eid new-eid)))

(def pausing? true)

(defn- pause-game? []
  (or #_error
      (and pausing?
           (state/pause-game? (entity/state-obj @ctx/player-eid))
           (not (or (.isKeyJustPressed Gdx/input Input$Keys/P)
                    (.isKeyPressed     Gdx/input Input$Keys/SPACE))))))

(defn- tick-entities! [{:keys [active-entities]}]
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (entity/tick! [k v] eid))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (pretty-pst t)
     (stage/show-error-window! ctx/stage t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- camera-controls! [camera]
  (let [zoom-speed 0.025]
    (when (.isKeyPressed Gdx/input Input$Keys/MINUS)  (camera/inc-zoom camera    zoom-speed))
    (when (.isKeyPressed Gdx/input Input$Keys/EQUALS) (camera/inc-zoom camera (- zoom-speed)))))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)]
    (run! require (:requires config))
    ((requiring-resolve 'cdq.game.load-schemas/do!) "schema.edn")
    (bind-root #'ctx/db ((requiring-resolve (:db config)) "properties.edn"))
    (when (= SharedLibraryLoader/os Os/MacOsX)
      (.setIconImage (Taskbar/getTaskbar)
                     (.getImage (Toolkit/getDefaultToolkit)
                                (io/resource "moon.png")))
      (.set Configuration/GLFW_LIBRARY_NAME "glfw_async"))
    (Lwjgl3Application. (proxy [ApplicationAdapter] []
                          (create []
                            (bind-root #'ctx/assets ((requiring-resolve (:assets config))
                                                     {:folder "resources/"
                                                      :asset-type-extensions {Sound   #{"wav"}
                                                                              Texture #{"png" "bmp"}}}))
                            (bind-root #'ctx/graphics ((requiring-resolve (:graphics* config)) (:graphics config)))
                            (reset-game! (:world-fn config)))

                          (dispose []
                            (Disposable/.dispose ctx/assets)
                            (Disposable/.dispose ctx/graphics)
                            ; TODO vis-ui dispose
                            ; TODO dispose world tiled-map/level resources?
                            )

                          (render []
                            ((requiring-resolve 'cdq.game.cache-active-entities/do!))
                            ((requiring-resolve 'cdq.game.set-camera-on-player/do!))
                            ((requiring-resolve 'cdq.game.clear-screen/do!))
                            ((requiring-resolve 'cdq.game.draw-tiled-map/do!))
                            (graphics/draw-on-world-view! ctx/graphics
                                                          (fn []
                                                            (draw-before-entities!)
                                                            (render-entities!)
                                                            (draw-after-entities!)))
                            (stage/draw! ctx/stage)
                            (stage/act! ctx/stage)
                            (state/manual-tick (entity/state-obj @ctx/player-eid))
                            (update-mouseover-entity!)
                            (bind-root #'ctx/paused? (pause-game?))
                            (when-not ctx/paused?
                              (let [delta-ms (min (.getDeltaTime Gdx/graphics) world/max-delta)]
                                (alter-var-root #'ctx/elapsed-time + delta-ms)
                                (bind-root #'ctx/delta-time delta-ms))
                              (world/update-potential-fields! ctx/world)
                              (tick-entities! ctx/world))

                            ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                            (world/remove-destroyed-entities! ctx/world)

                            (camera-controls! (:camera (:world-viewport ctx/graphics)))
                            (stage/check-window-controls! ctx/stage))

                          (resize [width height]
                            (graphics/resize! ctx/graphics width height)))
                        (doto (Lwjgl3ApplicationConfiguration.)
                          (.setTitle "Cyber Dungeon Quest")
                          (.setWindowedMode 1440 900)
                          (.setForegroundFPS 60)))))
