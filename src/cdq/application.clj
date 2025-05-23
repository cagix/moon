(ns cdq.application
  (:require [cdq.application.assets :as assets]
            [cdq.application.config :as config]
            [cdq.application.db :as db]
            [cdq.application.ctx-schema :as ctx-schema]
            [cdq.application.potential-fields.update :as potential-fields.update]
            [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.state :as state]
            [cdq.g :as g]
            [cdq.math :as math]

            [cdq.utils :as utils :refer [mapvals
                                         sort-by-order
                                         pretty-pst]]

            [clojure.gdx.backends.lwjgl :as lwjgl]

            [gdl.graphics :as graphics]
            [gdl.graphics.batch :as batch]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.graphics.viewport :as viewport]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.utils :refer [dispose!]]
            [gdl.ui :as ui])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defn- create-app-state [config]
  (run! require (:requires config))
  (ui/load! (:ui config))
  (let [batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))
        ui-viewport (graphics/ui-viewport (:ui-viewport config))
        stage (ui/stage (:java-object ui-viewport)
                        (:java-object batch))]
    (input/set-processor! stage)
    (cdq.g/map->Game
     {:ctx/config config
      :ctx/db (db/create (:db config))
      :ctx/assets (assets/create (:assets config))
      :ctx/batch batch
      :ctx/unit-scale 1
      :ctx/world-unit-scale world-unit-scale
      :ctx/shape-drawer-texture shape-drawer-texture
      :ctx/shape-drawer (graphics/shape-drawer batch (graphics/texture-region shape-drawer-texture 1 0 1 1))
      :ctx/cursors (mapvals
                    (fn [[file [hotspot-x hotspot-y]]]
                      (graphics/cursor (format (:cursor-path-format config) file)
                                       hotspot-x
                                       hotspot-y))
                    (:cursors config))
      :ctx/default-font (graphics/truetype-font (:default-font config))
      :ctx/world-viewport (graphics/world-viewport world-unit-scale (:world-viewport config))
      :ctx/ui-viewport ui-viewport
      :ctx/tiled-map-renderer (memoize (fn [tiled-map]
                                         (tiled/renderer tiled-map
                                                         world-unit-scale
                                                         (:java-object batch))))
      :ctx/stage stage})))

(defn- create-game-state! [config]
  (g/reset-game-state! (create-app-state config)))

(extend-type cdq.g.Game
  g/Config
  (config [{:keys [ctx/config]} key]
    (get config key)))

(extend-type cdq.g.Game
  g/Database
  (get-raw [{:keys [ctx/db]} property-id]
    (db/get-raw db property-id))

  (build [{:keys [ctx/db] :as ctx} property-id]
    (db/build db property-id ctx))

  (build-all [{:keys [ctx/db] :as ctx} property-type]
    (db/build-all db property-type ctx))

  (property-types [{:keys [ctx/db]}]
    (filter #(= "properties" (namespace %)) (keys (:schemas db))))

  (schemas [{:keys [ctx/db]}]
    (:schemas db))

  (update-property! [{:keys [ctx/db] :as ctx}
                     property]
    (let [new-db (db/update db property)]
      (db/save! new-db)
      (assoc ctx :ctx/db new-db)))

  (delete-property! [{:keys [ctx/db] :as ctx}
                     property-id]
    (let [new-db (db/delete db property-id)]
      (db/save! new-db)
      (assoc ctx :ctx/db new-db))))

(extend-type cdq.g.Game
  g/Textures
  (texture [{:keys [ctx/assets]} path]
    (assets path))

  (all-textures [{:keys [ctx/assets]}]
    (assets/all-textures assets))

  g/Sounds
  (sound [{:keys [ctx/assets]} path]
    (assets path))

  (all-sounds [{:keys [ctx/assets]}]
    (assets/all-sounds assets)))

(extend-type cdq.g.Game
  g/Input
  (button-just-pressed? [_ button]
    (input/button-just-pressed? button))

  (key-pressed? [_ key]
    (input/key-pressed? key))

  (key-just-pressed? [_ key]
    (input/key-just-pressed? key)))

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions
  "scale can be a number for multiplying the texture-region-dimensions or [w h]."
  [{:keys [texture-region] :as image} scale world-unit-scale]
  {:pre [(or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (graphics/dimensions texture-region)
                                             scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

(defrecord Sprite [texture-region
                   pixel-dimensions
                   world-unit-dimensions
                   color]) ; optional

(defn- sprite* [texture-region world-unit-scale]
  (-> {:texture-region texture-region}
      (assoc-dimensions 1 world-unit-scale) ; = scale 1
      map->Sprite))

(defn- unit-dimensions [image unit-scale]
  (if (= unit-scale 1)
    (:pixel-dimensions image)
    (:world-unit-dimensions image)))

(defn- with-line-width [shape-drawer width draw-fn]
  (sd/with-line-width shape-drawer width draw-fn))

(defmulti draw! (fn [[k] _ctx]
                  k))

(defmethod draw! :draw/image [[_ {:keys [texture-region color] :as image} position]
                              {:keys [ctx/batch
                                      ctx/unit-scale]}]
  (batch/draw-texture-region! batch
                              texture-region
                              position
                              (unit-dimensions image unit-scale)
                              0 ; rotation
                              color))

(defmethod draw! :draw/rotated-centered [[_ {:keys [texture-region color] :as image} rotation [x y]]
                                         {:keys [ctx/batch
                                                 ctx/unit-scale]}]
  (let [[w h] (unit-dimensions image unit-scale)]
    (batch/draw-texture-region! batch
                                texture-region
                                [(- (float x) (/ (float w) 2))
                                 (- (float y) (/ (float h) 2))]
                                [w h]
                                rotation
                                color)))

(defmethod draw! :draw/centered [[_ image position] ctx]
  (draw! [:draw/rotated-centered image 0 position] ctx))

  "font, h-align, up? and scale are optional.
  h-align one of: :center, :left, :right. Default :center.
  up? renders the font over y, otherwise under.
  scale will multiply the drawn text size with the scale."
(defmethod draw! :draw/text [[_ {:keys [font scale x y text h-align up?]}]
                             {:keys [ctx/default-font
                                     ctx/batch
                                     ctx/unit-scale]}]
  (graphics/draw-text! (or font default-font)
                       batch
                       {:scale (* (float unit-scale)
                                  (float (or scale 1)))
                        :x x
                        :y y
                        :text text
                        :h-align h-align
                        :up? up?}))

(defmethod draw! :draw/ellipse [[_ [x y] radius-x radius-y color]
                                {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/filled-ellipse [[_ [x y] radius-x radius-y color]
                                       {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-ellipse! shape-drawer x y radius-x radius-y))

(defmethod draw! :draw/circle [[_ [x y] radius color]
                               {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/circle! shape-drawer x y radius))

(defmethod draw! :draw/filled-circle [[_ [x y] radius color]
                                      {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-circle! shape-drawer x y radius))

(defmethod draw! :draw/rectangle [[_ x y w h color]
                                  {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/rectangle! shape-drawer x y w h))

(defmethod draw! :draw/filled-rectangle [[_ x y w h color]
                                         {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/filled-rectangle! shape-drawer x y w h))

(defmethod draw! :draw/arc [[_ [center-x center-y] radius start-angle degree color]
                            {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/arc! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/sector [[_ [center-x center-y] radius start-angle degree color]
                               {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/sector! shape-drawer center-x center-y radius start-angle degree))

(defmethod draw! :draw/line [[_ [sx sy] [ex ey] color]
                             {:keys [ctx/shape-drawer]}]
  (sd/set-color! shape-drawer color)
  (sd/line! shape-drawer sx sy ex ey))

(defmethod draw! :draw/grid [[_ leftx bottomy gridw gridh cellw cellh color] ctx]
  (let [w (* (float gridw) (float cellw))
        h (* (float gridh) (float cellh))
        topy (+ (float bottomy) (float h))
        rightx (+ (float leftx) (float w))]
    (doseq [idx (range (inc (float gridw)))
            :let [linex (+ (float leftx) (* (float idx) (float cellw)))]]
      (draw! [:draw/line [linex topy] [linex bottomy] color] ctx))
    (doseq [idx (range (inc (float gridh)))
            :let [liney (+ (float bottomy) (* (float idx) (float cellh)))]]
      (draw! [:draw/line [leftx liney] [rightx liney] color] ctx))))

(defmethod draw! :draw/with-line-width [[_ width draws]
                                        {:keys [ctx/shape-drawer]
                                         :as ctx}]
  (sd/with-line-width shape-drawer width
    (fn []
      (g/handle-draws! ctx draws))))

(extend-type cdq.g.Game
  g/Graphics
  (handle-draws! [ctx draws]
    (doseq [component draws
            :when component]
      (draw! component ctx)))

  (draw-on-world-viewport! [{:keys [ctx/batch
                                    ctx/world-viewport
                                    ctx/world-unit-scale
                                    ctx/shape-drawer]
                             :as ctx} fns]
    (batch/draw-on-viewport! batch
                             world-viewport
                             (fn []
                               (sd/with-line-width shape-drawer world-unit-scale
                                 (fn []
                                   (doseq [f fns]
                                     (f (assoc ctx :ctx/unit-scale world-unit-scale))))))))

  (world-mouse-position [{:keys [ctx/world-viewport]}]
    (viewport/mouse-position world-viewport))

  (ui-mouse-position [{:keys [ctx/ui-viewport]}]
    (viewport/mouse-position ui-viewport))

  (draw-tiled-map! [{:keys [ctx/tiled-map-renderer
                            ctx/world-viewport]}
                    tiled-map
                    color-setter]
    (tiled/draw! (tiled-map-renderer tiled-map)
                 tiled-map
                 color-setter
                 (:camera world-viewport)))

  (ui-viewport-width [{:keys [ctx/ui-viewport]}]
    (:width ui-viewport))

  (ui-viewport-height [{:keys [ctx/ui-viewport]}]
    (:height ui-viewport))

  (world-viewport-width [{:keys [ctx/world-viewport]}]
    (:width world-viewport))

  (world-viewport-height [{:keys [ctx/world-viewport]}]
    (:height world-viewport))

  (camera-position [{:keys [ctx/world-viewport]}]
    (camera/position (:camera world-viewport)))

  (inc-zoom! [{:keys [ctx/world-viewport]} amount]
    (camera/inc-zoom! (:camera world-viewport) amount))

  (camera-frustum [{:keys [ctx/world-viewport]}]
    (camera/frustum (:camera world-viewport)))

  (visible-tiles [{:keys [ctx/world-viewport]}]
    (camera/visible-tiles (:camera world-viewport)))

  (set-camera-position! [{:keys [ctx/world-viewport]} position]
    (camera/set-position! (:camera world-viewport)
                          position))

  (camera-zoom [{:keys [ctx/world-viewport]}]
    (camera/zoom (:camera world-viewport)))

  (pixels->world-units [{:keys [ctx/world-unit-scale]} pixels]
    (* pixels world-unit-scale))

  (sprite [{:keys [ctx/world-unit-scale] :as ctx}
           texture-path]
    (sprite* (graphics/texture-region (g/texture ctx texture-path))
             world-unit-scale))

  (sub-sprite [{:keys [ctx/world-unit-scale]}
               sprite
               [x y w h]]
    (sprite* (graphics/sub-region (:texture-region sprite) x y w h)
             world-unit-scale))

  (sprite-sheet [{:keys [ctx/world-unit-scale] :as ctx}
                 texture-path
                 tilew
                 tileh]
    {:image (sprite* (graphics/texture-region (g/texture ctx texture-path))
                     world-unit-scale)
     :tilew tilew
     :tileh tileh})

  (sprite-sheet->sprite [ctx
                         {:keys [image tilew tileh]}
                         [x y]]
    (g/sub-sprite ctx
                  image
                  [(* x tilew) (* y tileh) tilew tileh]))

  (set-cursor! [{:keys [ctx/cursors]} cursor-key]
    (graphics/set-cursor! (utils/safe-get cursors cursor-key))))

(extend-type cdq.g.Game
  g/Stage
  (draw-stage! [{:keys [ctx/stage] :as ctx}]
    (reset! (.ctx stage) ctx)
    (ui/draw! stage)
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; so it has outdated context
    ; => maybe context should be an immutable data structure with mutable fields?
    #_(reset! (.ctx stage) nil)
    nil)

  (update-stage! [{:keys [ctx/stage] :as ctx}]
    (reset! (.ctx stage) ctx)
    (ui/act! stage)
    ; We cannot pass this
    ; because input events are handled outside ui/act! and in the Lwjgl3Input system:
    ;                         com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.<init>   Lwjgl3Application.java:  153
    ;                           com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application.loop   Lwjgl3Application.java:  181
    ;                              com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window.update        Lwjgl3Window.java:  414
    ;                        com.badlogic.gdx.backends.lwjgl3.DefaultLwjgl3Input.update  DefaultLwjgl3Input.java:  190
    ;                                            com.badlogic.gdx.InputEventQueue.drain     InputEventQueue.java:   70
    ;                             gdl.ui.proxy$gdl.ui.CtxStage$ILookup$a65747ce.touchUp                         :
    ;                                     com.badlogic.gdx.scenes.scene2d.Stage.touchUp               Stage.java:  354
    ;                              com.badlogic.gdx.scenes.scene2d.InputListener.handle       InputListener.java:   71
    #_@(.ctx stage)
    ; we need to set nil as input listeners
    ; are updated outside of render
    ; inside lwjgl3application code
    ; so it has outdated context
    #_(reset! (.ctx stage) nil)
    nil)

  (get-actor [{:keys [ctx/stage]} id]
    (id stage))

  (find-actor-by-name [{:keys [ctx/stage]} name]
    (-> stage
        ui/root
        (ui/find-actor name)))

  (add-actor! [{:keys [ctx/stage]} actor]
    (ui/add! stage actor))

  (mouseover-actor [{:keys [ctx/stage] :as ctx}]
    (ui/hit stage (g/ui-mouse-position ctx)))

  (reset-actors! [{:keys [ctx/stage]} actors]
    (ui/clear! stage)
    (run! #(ui/add! stage %) actors)))

(defn- dispose-game-state! [{:keys [ctx/assets
                                    ctx/batch
                                    ctx/shape-drawer-texture
                                    ctx/cursors
                                    ctx/default-font]}]
  (dispose! assets)
  (dispose! batch)
  (dispose! shape-drawer-texture)
  (run! dispose! (vals cursors))
  (dispose! default-font)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )

(defn- geom-test* [ctx]
  (let [position (g/world-mouse-position ctx)
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (g/circle->cells ctx circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{[x y] :left-bottom
                 :keys [width height]} (math/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn- geom-test [ctx]
  (g/handle-draws! ctx (geom-test* ctx)))

(defn- highlight-mouseover-tile* [ctx]
  (let [[x y] (mapv int (g/world-mouse-position ctx))
        cell (g/grid-cell ctx [x y])]
    (when (and cell (#{:air :none} (:movement @cell)))
      [[:draw/rectangle x y 1 1
        (case (:movement @cell)
          :air  [1 1 0 0.5]
          :none [1 0 0 0.5])]])))

(defn- highlight-mouseover-tile [ctx]
  (g/handle-draws! ctx (highlight-mouseover-tile* ctx)))

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    [[:draw/rectangle x y (:width entity) (:height entity) color]]))

(defn- draw-tile-grid* [ctx]
  (when ctx/show-tile-grid?
    (let [[left-x _right-x bottom-y _top-y] (g/camera-frustum ctx)]
      [[:draw/grid
        (int left-x)
        (int bottom-y)
        (inc (int (g/world-viewport-width ctx)))
        (+ 2 (int (g/world-viewport-height ctx)))
        1
        1
        [1 1 1 0.8]]])))

(defn- draw-tile-grid [ctx]
  (g/handle-draws! ctx (draw-tile-grid* ctx)))

(defn- draw-cell-debug* [ctx]
  (apply concat
         (for [[x y] (g/visible-tiles ctx)
               :let [cell (g/grid-cell ctx [x y])]
               :when cell
               :let [cell* @cell]]
           [(when (and ctx/show-cell-entities? (seq (:entities cell*)))
              [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
            (when (and ctx/show-cell-occupied? (seq (:occupied cell*)))
              [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
            (when-let [faction ctx/show-potential-field-colors?]
              (let [{:keys [distance]} (faction cell*)]
                (when distance
                  (let [ratio (/ distance (ctx/factions-iterations faction))]
                    [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))])))

(defn- draw-cell-debug [ctx]
  (g/handle-draws! ctx (draw-cell-debug* ctx)))

(defn- render-entities! [{:keys [ctx/active-entities
                                 ctx/player-eid]
                          :as ctx}]
  (let [entities (map deref active-entities)
        player @player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              ctx/render-z-order)
            render! [#'entity/render-below!
                     #'entity/render-default!
                     #'entity/render-above!
                     #'entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (g/line-of-sight? ctx player entity))]
      (try
       (when ctx/show-body-bounds?
         (g/handle-draws! ctx (draw-body-rect entity (if (:collides? entity) :white :gray))))
       (doseq [component entity]
         (g/handle-draws! ctx (render! component entity ctx)))
       (catch Throwable t
         (g/handle-draws! ctx (draw-body-rect entity :red))
         (pretty-pst t))))))

(defn- camera-controls! [ctx]
  (let [controls (g/config ctx :controls)
        zoom-speed (g/config ctx :zoom-speed)]
    (when (g/key-pressed? ctx (:zoom-in controls))  (g/inc-zoom! ctx    zoom-speed))
    (when (g/key-pressed? ctx (:zoom-out controls)) (g/inc-zoom! ctx (- zoom-speed)))))

(defn- assoc-active-entities [ctx]
  (assoc ctx :ctx/active-entities (g/get-active-entities ctx)))

(defn- draw-on-world-viewport! [ctx]
  (g/draw-on-world-viewport! ctx [draw-tile-grid
                                  draw-cell-debug
                                  render-entities!
                                  ;geom-test
                                  highlight-mouseover-tile])
  nil)

(defn- player-state-handle-click! [{:keys [ctx/player-eid] :as ctx}]
  (g/handle-txs! ctx
                 (state/manual-tick (entity/state-obj @player-eid)
                                    player-eid
                                    ctx))
  nil)

(defn- update-mouseover-entity! [{:keys [ctx/player-eid
                                         ctx/mouseover-eid]
                                  :as ctx}]
  (let [new-eid (if (g/mouseover-actor ctx)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (g/point->entities ctx (g/world-mouse-position ctx)))]
                    (->> ctx/render-z-order
                         (sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(g/line-of-sight? ctx player @%))
                         first)))]
    (when-let [eid mouseover-eid]
      (swap! eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))

(defn- pause-game? [{:keys [ctx/player-eid] :as ctx}]
  (let [controls (g/config ctx :controls)]
    (or #_error
        (and (g/config ctx :pausing?)
             (state/pause-game? (entity/state-obj @player-eid))
             (not (or (g/key-just-pressed? ctx (:unpause-once controls))
                      (g/key-pressed? ctx (:unpause-continously controls))))))))

(defn- assoc-paused [ctx]
  (assoc ctx :ctx/paused? (pause-game? ctx)))

(defn- tick-entities!
  [{:keys [ctx/active-entities] :as ctx}]
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
               (g/handle-txs! ctx (entity/tick! [k v] eid ctx)))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (pretty-pst t)
     (g/open-error-window! ctx t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- assoc-delta-time
  [ctx]
  (assoc ctx :ctx/delta-time (min (graphics/delta-time) ctx/max-delta)))

(defn- update-elapsed-time
  [{:keys [ctx/delta-time]
    :as ctx}]
  (update ctx :ctx/elapsed-time + delta-time))

(defn- render-game-state! [{:keys [ctx/player-eid] :as ctx}]
  (let [ctx (assoc-active-entities ctx)]
    (g/set-camera-position! ctx (:position @player-eid))
    (graphics/clear-screen!)
    (g/draw-world-map! ctx)
    (draw-on-world-viewport! ctx)
    (g/draw-stage! ctx)
    (g/update-stage! ctx)
    (player-state-handle-click! ctx)
    (let [ctx (update-mouseover-entity! ctx)
          ctx (assoc-paused ctx)
          ctx (if (:ctx/paused? ctx)
                ctx
                (let [ctx (-> ctx
                              assoc-delta-time
                              update-elapsed-time)]
                  (potential-fields.update/do! ctx)
                  (tick-entities! ctx)
                  ctx))]
      (g/remove-destroyed-entities! ctx) ; do not pause as pickup item should be destroyed
      (camera-controls! ctx)
      ctx)))

(defn- update-viewports! [{:keys [ctx/ui-viewport
                                  ctx/world-viewport]}]
  (viewport/update! ui-viewport)
  (viewport/update! world-viewport))

(def state (atom nil))

(defn -main []
  (let [config (config/create "config.edn")]
    (lwjgl/application (:clojure.gdx.backends.lwjgl config)
                       (proxy [ApplicationAdapter] []
                         (create []
                           (reset! state (create-game-state! config))
                           (ctx-schema/validate @state))

                         (dispose []
                           (ctx-schema/validate @state)
                           (dispose-game-state! @state))

                         (render []
                           (ctx-schema/validate @state)
                           (swap! state render-game-state!)
                           (ctx-schema/validate @state))

                         (resize [_width _height]
                           (ctx-schema/validate @state)
                           (update-viewports! @state))))))
