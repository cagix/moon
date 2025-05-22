(ns cdq.application
  (:require [cdq.ctx :as ctx]
            [cdq.cell :as cell]
            cdq.create.assets
            [cdq.content-grid :as content-grid]
            cdq.db
            [cdq.entity :as entity]
            cdq.input
            [cdq.g]
            [cdq.state :as state]
            [cdq.tx.spawn-creature]
            [cdq.grid :as grid]
            [cdq.grid2d :as g2d]
            [cdq.raycaster :as raycaster]
            [cdq.potential-field.movement :as potential-field]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.entity-info]
            [cdq.ui.inventory :as inventory-window]
            [cdq.ui.hp-mana-bar]
            [cdq.ui.player-state-draw]
            [cdq.ui.windows]
            [cdq.ui.message]
            [cdq.utils :as utils :refer [mapvals
                                         io-slurp-edn
                                         safe-get
                                         safe-merge]]
            [cdq.malli :as m]
            [cdq.vector2 :as v]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [gdl.graphics :as graphics]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.viewport :as viewport]
            [gdl.input :as input]
            [gdl.tiled :as tiled]
            [gdl.ui :as ui]
            [gdl.utils])
  (:import (com.badlogic.gdx ApplicationAdapter)))

(defrecord Body [position
                 left-bottom

                 width
                 height
                 half-width
                 half-height
                 radius

                 collides?
                 z-order
                 rotation-angle]
  entity/Entity
  (in-range? [entity target* maxrange] ; == circle-collides?
    (< (- (float (v/distance (:position entity)
                             (:position target*)))
          (float (:radius entity))
          (float (:radius target*)))
       (float maxrange))))

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}
                    minimum-size
                    z-orders]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? minimum-size 0)))
  (assert (>= height (if collides? minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set z-orders) z-order))
  (assert (or (nil? rotation-angle)
              (<= 0 rotation-angle 360)))
  (map->Body
   {:position (mapv float position)
    :left-bottom [(float (- x (/ width  2)))
                  (float (- y (/ height 2)))]
    :width  (float width)
    :height (float height)
    :half-width  (float (/ width  2))
    :half-height (float (/ height 2))
    :radius (float (max (/ width  2)
                        (/ height 2)))
    :collides? collides?
    :z-order z-order
    :rotation-angle (or rotation-angle 0)}))

(defn- create-vs [components ctx]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v] ctx)))
          {}
          components))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [viewport entity]
  (let [[x y] (:position entity)
        x (float x)
        y (float y)
        [cx cy] (camera/position (:camera viewport))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (:width viewport))  2)))
     (<= ydist (inc (/ (float (:height viewport)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

(extend-type cdq.g.Game
  cdq.g/World
  (spawn-entity! [{:keys [ctx/id-counter
                          ctx/entity-ids
                          ctx/content-grid
                          ctx/grid]
                   :as ctx}
                  position body components]

    ; TODO SCHEMA COMPONENTS !

    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))))
    (let [eid (atom (-> body
                        (assoc :position position)
                        (create-body ctx/minimum-size ctx/z-orders)
                        (utils/safe-merge (-> components
                                              (assoc :entity/id (swap! id-counter inc))
                                              (create-vs ctx)))))]

      ;;

      (let [id (:entity/id @eid)]
        (assert (number? id))
        (swap! entity-ids assoc id eid))

      (content-grid/add-entity! content-grid eid)

      ; https://github.com/damn/core/issues/58
      ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
      (grid/add-entity! grid eid)

      ;;


      (doseq [component @eid]
        (ctx/handle-txs! ctx (entity/create! component eid ctx)))
      eid))

  ; does not take into account size of entity ...
  ; => assert bodies <1 width then
  (line-of-sight? [{:keys [ctx/world-viewport
                           ctx/raycaster]}
                   source
                   target]
    (and (or (not (:entity/player? source))
             (on-screen? world-viewport target))
         (not (and los-checks?
                   (raycaster/blocked? raycaster
                                       (:position source)
                                       (:position target))))))

  (path-blocked? [{:keys [ctx/raycaster]} start end width]
    (raycaster/path-blocked? raycaster
                             start
                             end
                             width))

  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-field/find-direction grid eid))

  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid (mapv int (:position entity)))
                                  (entity/enemy entity)))

  (player-movement-vector [_]
    (cdq.input/player-movement-vector))

  )

(def application-configuration {:title "Cyber Dungeon Quest"
                                :windowed-mode {:width 1440
                                                :height 900}
                                :foreground-fps 60
                                :mac-os {:glfw-async? true
                                         :dock-icon "moon.png"}})

; TODO do also for entities ... !!
; => & what components allowed/etc.

(def ctx-schema (m/schema [:map {:closed true}

                           ; missing gdx app, input, graphics, audio ?
                           ; => abstract from the whole plattform itself ??
                           ; => move into 1 context ?

                           ; config ?
                           [:ctx/pausing? :some]
                           [:ctx/zoom-speed :some]
                           [:ctx/controls :some]
                           [:ctx/sound-path-format :some]
                           [:ctx/effect-body-props :some]

                           [:ctx/config :some]

                           ; db
                           [:ctx/db :some]

                           ; sounds & textures
                           [:ctx/assets :some]
                           ; * dispose
                           ; * play sound
                           ; * sprites

                           ; graphics
                           [:ctx/batch :some]
                           ; usage:
                           ; * dispose
                           ; * create shape-drawer/stage/tiled-map-renderer
                           ; * cdq.draw
                           ; * draw on world-viewport
                           [:ctx/shape-drawer-texture :some]
                           ; * only dispose / shape-drawer
                           [:ctx/shape-drawer :some]
                           ; * draw
                           [:ctx/unit-scale :some]
                           [:ctx/world-unit-scale :some]
                           [:ctx/cursors :some]
                           [:ctx/default-font :some]
                           [:ctx/world-viewport :some]
                           [:ctx/get-tiled-map-renderer :some]
                           [:ctx/ui-viewport :some]

                           ; ui
                           [:ctx/stage :some]

                           ; game logic:

                           ; time
                           [:ctx/elapsed-time :some]
                           [:ctx/delta-time {:optional true} number?] ; optional - added in render each frame
                           [:ctx/paused? {:optional true} :boolean] ; optional - added in render each frame

                           [:ctx/tiled-map :some]

                           ; < - comes from level/tiled-map
                           [:ctx/grid :some]
                           [:ctx/raycaster :some]
                           [:ctx/content-grid :some]
                           [:ctx/explored-tile-corners :some]

                           ;
                           [:ctx/id-counter :some]
                           [:ctx/entity-ids :some]
                           [:ctx/potential-field-cache :some]
                           ;

                           ; control pointers:
                           [:ctx/mouseover-eid :any]
                           [:ctx/player-eid :some]
                           [:ctx/active-entities {:optional true} :some] ; optional - added in render each frame
                           ]))

(comment

 (m/validate-humanize ctx-schema @state)
 ; delta-time missing required key
 ; mouseover-eid unknown error (can be nil)

 ; TODO these fields are _not_ optional at render/dispose/resize!
 ; only after create ...
 ; +> fix

 )

(defn check-validity [ctx]
  (m/validate-humanize ctx-schema ctx))

(def initial-context {:ctx/pausing? true
                      :ctx/zoom-speed 0.025
                      :ctx/controls {:zoom-in :minus
                                     :zoom-out :equals
                                     :unpause-once :p
                                     :unpause-continously :space}
                      :ctx/sound-path-format "sounds/%s.wav"
                      :ctx/unit-scale 1
                      :ctx/mouseover-eid nil ; needed ?
                      :ctx/effect-body-props {:width 0.5
                                              :height 0.5
                                              :z-order :z-order/effect}})

(defn create-app-state []
  (let [config (let [m (io-slurp-edn "config.edn")]
                 (reify clojure.lang.ILookup
                   (valAt [_ k]
                     (safe-get m k))))
        batch (graphics/sprite-batch)
        shape-drawer-texture (graphics/white-pixel-texture)
        world-unit-scale (float (/ (:tile-size config)))
        ui-viewport (graphics/ui-viewport (:ui-viewport config))
        stage (ui/stage (:java-object ui-viewport)
                        (:java-object batch))]
    (run! require (:requires config))
    (ui/load! (:ui config))
    (input/set-processor! stage)
    (cdq.g/map->Game
     {:ctx/config config
      :ctx/db (cdq.db/create "properties.edn" "schema.edn")

      :ctx/assets (cdq.create.assets/create {:folder "resources/"
                                             :asset-type-extensions {:sound   #{"wav"}
                                                                     :texture #{"png" "bmp"}}})
      :ctx/batch batch
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
      :ctx/get-tiled-map-renderer (memoize (fn [tiled-map]
                                             (tiled/renderer tiled-map
                                                             world-unit-scale
                                                             (:java-object batch))))
      :ctx/stage stage})))

(defn- create-actors [{:keys [ctx/ui-viewport] :as ctx}]
  [((requiring-resolve 'cdq.ui.dev-menu/create) ctx)
   (cdq.ui.action-bar/create :id :action-bar)
   (cdq.ui.hp-mana-bar/create [(/ (:width ui-viewport) 2)
                               80 ; action-bar-icon-size
                               ]
                              ctx)
   (cdq.ui.windows/create :id :windows
                          :actors [(cdq.ui.entity-info/create [(:width ui-viewport) 0])
                                   (cdq.ui.inventory/create ctx
                                                            :id :inventory-window
                                                            :position [(:width  ui-viewport)
                                                                       (:height ui-viewport)])])
   (cdq.ui.player-state-draw/create)
   (cdq.ui.message/create :name "player-message")])

(defn reset-stage! [stage actors]
  (ui/clear! stage)
  (run! #(ui/add! stage %) actors))

(defn- player-entity-props [start-position {:keys [creature-id
                                                   free-skill-points
                                                   click-distance-tiles]}]
  {:position start-position
   :creature-id creature-id
   :components {:entity/fsm {:fsm :fsms/player
                             :initial-state :player-idle}
                :entity/faction :good
                :entity/player? {:state-changed! (fn [new-state-obj]
                                                   (when-let [cursor (state/cursor new-state-obj)]
                                                     [[:tx/set-cursor cursor]]))
                                 :skill-added! (fn [{:keys [ctx/stage]} skill]
                                                 (-> stage
                                                     :action-bar
                                                     (action-bar/add-skill! skill)))
                                 :skill-removed! (fn [{:keys [ctx/stage]} skill]
                                                   (-> stage
                                                       :action-bar
                                                       (action-bar/remove-skill! skill)))
                                 :item-set! (fn [{:keys [ctx/stage]} inventory-cell item]
                                              (-> stage
                                                  :windows
                                                  :inventory-window
                                                  (inventory-window/set-item! inventory-cell item)))
                                 :item-removed! (fn [{:keys [ctx/stage]} inventory-cell]
                                                  (-> stage
                                                      :windows
                                                      :inventory-window
                                                      (inventory-window/remove-item! inventory-cell)))}
                :entity/free-skill-points free-skill-points
                :entity/clickable {:type :clickable/player}
                :entity/click-distance-tiles click-distance-tiles}})

(defn- spawn-player-entity [ctx start-position]
  (cdq.tx.spawn-creature/do! ctx
                             (player-entity-props (utils/tile->middle start-position)
                                                  ctx/player-entity-config)))

(defn- spawn-enemies* [tiled-map]
  (for [props (for [[position creature-id] (tiled/positions-with-property tiled-map :creatures :id)]
                {:position position
                 :creature-id (keyword creature-id)
                 :components {:entity/fsm {:fsm :fsms/npc
                                           :initial-state :npc-sleeping}
                              :entity/faction :evil}})]
    [:tx/spawn-creature (update props :position utils/tile->middle)]))

(defn- spawn-enemies! [{:keys [ctx/tiled-map] :as ctx}]
  (ctx/handle-txs! ctx
                   (spawn-enemies* tiled-map)))

(defn create-game-state [ctx]
  (reset-stage! (:ctx/stage ctx)
                (create-actors ctx))
  (let [{:keys [tiled-map
                start-position]} ((requiring-resolve 'cdq.level.vampire/create) ctx)
        grid (cdq.grid/create tiled-map)
        ctx (merge ctx
                   {:ctx/tiled-map tiled-map
                    :ctx/elapsed-time 0
                    :ctx/grid grid
                    :ctx/raycaster (cdq.raycaster/create grid)
                    :ctx/content-grid (cdq.content-grid/create tiled-map 16)
                    :ctx/explored-tile-corners (atom (g2d/create-grid (tiled/tm-width  tiled-map)
                                                                      (tiled/tm-height tiled-map)
                                                                      (constantly false)))
                    :ctx/id-counter (atom 0)
                    :ctx/entity-ids (atom {})
                    :ctx/potential-field-cache (atom nil)})
        ctx (assoc ctx :ctx/player-eid (spawn-player-entity ctx start-position))]
    (spawn-enemies! ctx)
    ctx))

(defn create! []
  (create-game-state
   (safe-merge (create-app-state)
               initial-context)))

(def render-fns '[cdq.render.bind-active-entities/do!
                  cdq.render.set-camera-on-player/do!
                  cdq.render.clear-screen/do!
                  cdq.render.draw-tiled-map/do!
                  cdq.render.draw-on-world-viewport/do!
                  cdq.render.draw-ui/do!
                  cdq.render.update-ui/do!
                  cdq.render.player-state-handle-click/do!
                  cdq.render.update-mouseover-entity/do!
                  cdq.render.bind-paused/do!
                  cdq.render.when-not-paused/do!
                  cdq.render.remove-destroyed-entities/do! ; do not pause as pickup item should be destroyed
                  cdq.render.camera-controls/do!])

(defn render! [ctx]
  (reduce (fn [ctx render-fn]
            (if-let [result ((requiring-resolve render-fn) ctx)]
              result
              ctx))
          ctx
          render-fns))

(defn dispose! [{:keys [ctx/assets
                        ctx/batch
                        ctx/shape-drawer-texture
                        ctx/cursors
                        ctx/default-font]}]
  (gdl.utils/dispose! assets)
  (gdl.utils/dispose! batch)
  (gdl.utils/dispose! shape-drawer-texture)
  (run! gdl.utils/dispose! (vals cursors))
  (gdl.utils/dispose! default-font)
  ; TODO vis-ui dispose
  ; TODO dispose world tiled-map/level resources?
  )

(defn resize! [{:keys [ctx/ui-viewport
                       ctx/world-viewport]}]
  (viewport/update! ui-viewport)
  (viewport/update! world-viewport))

(def state (atom nil))

(comment
 (spit "state.clj"
       (with-out-str
        (clojure.pprint/pprint
         (sort (keys @state)))))
 )

(defn reset-game-state! []
  (swap! state create-game-state))

(defn -main []
  (lwjgl/application application-configuration
                     (proxy [ApplicationAdapter] []
                       (create []
                         (reset! state (create!))
                         (check-validity @state))

                       (dispose []
                         (check-validity @state)
                         (dispose! @state))

                       (render []
                         (check-validity @state)
                         (swap! state render!)
                         (check-validity @state))

                       (resize [_width _height]
                         (check-validity @state)
                         (resize! @state)))))
