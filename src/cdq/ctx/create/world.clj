(ns cdq.ctx.create.world
  (:require [cdq.db :as db]
            [cdq.graphics.textures :as textures]
            [cdq.world :as world]
            [cdq.world.assoc-entity-spawn-schema :as assoc-entity-spawn-schema]
            [cdq.world.content-grid :as content-grid]
            [cdq.world.create-fsms :as create-fsms]
            [cdq.world.grid :as grid]
            [cdq.world.grid.cell :as cell]
            [cdq.world.tick-entities :as tick-entities]
            [cdq.world.update-potential-fields :as update-potential-fields]
            [cdq.world-fns.creature-tiles]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [com.badlogic.gdx.maps.tiled :as tiled]
            [gdl.disposable :as disposable]
            [gdl.grid2d :as g2d]
            [gdl.txs :as txs]
            [gdl.utils :as utils]))

(defn- create-world-grid [width height cell-movement]
  (g2d/create-grid width
                   height
                   (fn [position]
                     (atom (cell/create position (cell-movement position))))))

(defn- update-entity! [{:keys [grid cell-w cell-h]} eid]
  (let [{:keys [cdq.content-grid/content-cell
                entity/body]} @eid
        [x y] (:body/position body)
        new-cell (get grid [(int (/ x cell-w))
                            (int (/ y cell-h))])]
    (when-not (= content-cell new-cell)
      (swap! new-cell update :entities conj eid)
      (swap! eid assoc :cdq.content-grid/content-cell new-cell)
      (when content-cell
        (swap! content-cell update :entities disj eid)))))

(defrecord ContentGrid []
  content-grid/ContentGrid
  (add-entity! [this eid]
    (update-entity! this eid))

  (remove-entity! [_ eid]
    (-> @eid
        :cdq.content-grid/content-cell
        (swap! update :entities disj eid)))

  (position-changed! [this eid]
    (update-entity! this eid))

  (active-entities [{:keys [grid]} center-entity]
    (->> (let [idx (-> center-entity
                       :cdq.content-grid/content-cell
                       deref
                       :idx)]
           (cons idx (g2d/get-8-neighbour-positions idx)))
         (keep grid)
         (mapcat (comp :entities deref)))))

(defn- create-content-grid [width height cell-size]
  (map->ContentGrid
   {:grid (g2d/create-grid
           (inc (int (/ width  cell-size)))
           (inc (int (/ height cell-size)))
           (fn [idx]
             (atom {:idx idx,
                    :entities #{}})))
    :cell-w cell-size
    :cell-h cell-size}))

(defn- create-explored-tile-corners [width height]
  (atom (g2d/create-grid width height (constantly false))))

(defn- create-raycaster [g2d]
  (let [width  (g2d/width  g2d)
        height (g2d/height g2d)
        cells  (for [cell (map deref (g2d/cells g2d))]
                 [(:position cell)
                  (boolean (cell/blocks-vision? cell))])]
    (let [arr (make-array Boolean/TYPE width height)]
      (doseq [[[x y] blocked?] cells]
        (aset arr x y (boolean blocked?)))
      [arr width height])))

(defrecord RWorld []
  disposable/Disposable
  (dispose! [{:keys [world/tiled-map]}]
    (assert tiled-map) ; only dispose after world was created
    (disposable/dispose! tiled-map))

  world/World
  (cache-active-entities [{:keys [world/content-grid
                                  world/player-eid]
                           :as world}]
    (assoc world
           :world/active-entities
           (content-grid/active-entities content-grid
                                         @player-eid)))

  (update-potential-fields! [this]
    (update-potential-fields/do! this))

  (tick-entities! [this]
    (tick-entities/do! this))
  )

(defn- assoc-state [world {:keys [tiled-map
                                  start-position]}]
  (let [width  (:tiled-map/width  tiled-map)
        height (:tiled-map/height tiled-map)
        grid (create-world-grid width height
                                #(case (tiled/movement-property tiled-map %)
                                   "none" :none
                                   "air"  :air
                                   "all"  :all))]
    (assoc world
           :world/tiled-map tiled-map
           :world/start-position start-position
           :world/grid grid
           :world/content-grid (create-content-grid width height (:content-grid-cell-size world))
           :world/explored-tile-corners (create-explored-tile-corners width height)
           :world/raycaster (create-raycaster grid)
           :world/elapsed-time 0
           :world/potential-field-cache (atom nil)
           :world/id-counter (atom 0)
           :world/entity-ids (atom {})
           :world/paused? false
           :world/mouseover-eid nil)))

(defn- calculate-max-speed
  [{:keys [world/minimum-size
           world/max-delta]
    :as world}]
  (assoc world :world/max-speed (/ minimum-size max-delta)))

(defn- define-render-z-order
  [{:keys [world/z-orders]
    :as world}]
  (assoc world :world/render-z-order (utils/define-order z-orders)))

(defn- create-world [initial-config world-fn-result]
  (-> (merge (map->RWorld {}) initial-config)
      assoc-entity-spawn-schema/step
      create-fsms/step
      calculate-max-speed
      define-render-z-order
      (assoc-state world-fn-result)))

(defn- call-world-fn
  [world-fn creature-properties graphics]
  (let [[f params] (-> world-fn io/resource slurp edn/read-string)]
    ((requiring-resolve f)
     (assoc params
            :level/creature-properties (cdq.world-fns.creature-tiles/prepare creature-properties
                                                                             #(textures/texture-region graphics %))
            :textures (:graphics/textures graphics)))))

(def ^:private params
  {:content-grid-cell-size 16
   :world/factions-iterations {:good 15 :evil 5}
   :world/max-delta 0.04
   :world/minimum-size 0.39
   :world/z-orders [:z-order/on-ground
                    :z-order/ground
                    :z-order/flying
                    :z-order/effect]
   :world/enemy-components {:entity/fsm {:fsm :fsms/npc
                                         :initial-state :npc-sleeping}
                            :entity/faction :evil}
   :world/player-components {:creature-id :creatures/vampire
                             :components {:entity/fsm {:fsm :fsms/player
                                                       :initial-state :player-idle}
                                          :entity/faction :good
                                          :entity/player? true
                                          :entity/free-skill-points 3
                                          :entity/clickable {:type :clickable/player}
                                          :entity/click-distance-tiles 1.5}}
   :world/effect-body-props {:width 0.5
                             :height 0.5
                             :z-order :z-order/effect}})

(defn- spawn-player!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (txs/handle! ctx
               [[:tx/spawn-creature (let [{:keys [creature-id
                                                  components]} (:world/player-components world)]
                                      {:position (mapv (partial + 0.5) (:world/start-position world))
                                       :creature-property (db/build db creature-id)
                                       :components components})]])
  (let [eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @eid))
    (assoc-in ctx [:ctx/world :world/player-eid] eid)))

(defn- spawn-enemies!
  [{:keys [ctx/db
           ctx/world]
    :as ctx}]
  (txs/handle!
   ctx
   (for [[position creature-id] (tiled/positions-with-property
                                 (:world/tiled-map world)
                                 "creatures"
                                 "id")]
     [:tx/spawn-creature {:position (mapv (partial + 0.5) position)
                          :creature-property (db/build db (keyword creature-id))
                          :components (:world/enemy-components world)}]))
  ctx)

(defn do! [{:keys [ctx/db
                   ctx/graphics
                   ctx/world]
            :as ctx}
           world-fn]
  (let [world-fn-result (call-world-fn world-fn
                                       (db/all-raw db :properties/creatures)
                                       graphics)]
    (-> ctx
        (assoc :ctx/world (create-world params world-fn-result))
        spawn-player!
        spawn-enemies!)))
