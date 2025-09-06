(ns cdq.reset-game-state
  (:require [cdq.grid.cell :as cell]
            [cdq.grid-impl :as grid-impl]
            [cdq.raycaster]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.gdx.math.vector2 :as v]
            [cdq.grid2d :as g2d]
            [cdq.math.raycaster :as raycaster]
            [clojure.gdx.scenes.scene2d :as scene2d]
            [clojure.gdx.scenes.scene2d.actor :as actor]
            [clojure.gdx.scenes.scene2d.stage :as stage]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.content-grid :as content-grid]))

; not tested
(defn- create-double-ray-endpositions
  "path-w in tiles."
  [[start-x start-y] [target-x target-y] path-w]
  {:pre [(< path-w 0.98)]} ; wieso 0.98??
  (let [path-w (+ path-w 0.02) ;etwas gr�sser damit z.b. projektil nicht an ecken anst�sst
        v (v/direction [start-x start-y]
                       [target-y target-y])
        [normal1 normal2] (v/normal-vectors v)
        normal1 (v/scale normal1 (/ path-w 2))
        normal2 (v/scale normal2 (/ path-w 2))
        start1  (v/add [start-x  start-y]  normal1)
        start2  (v/add [start-x  start-y]  normal2)
        target1 (v/add [target-x target-y] normal1)
        target2 (v/add [target-x target-y] normal2)]
    [start1,target1,start2,target2]))

(defn- set-arr [arr cell cell->blocked?]
  (let [[x y] (:position cell)]
    (aset arr x y (boolean (cell->blocked? cell)))))

(defn- create-raycaster-arr [grid]
  (let [width  (g2d/width  (.g2d grid))
        height (g2d/height (.g2d grid))
        arr (make-array Boolean/TYPE width height)]
    (doseq [cell (g2d/cells (.g2d grid))]
      (set-arr arr @cell cell/blocks-vision?))
    [arr width height]))

(defn- create-raycaster [grid]
  (let [arr (create-raycaster-arr grid)]
    (reify cdq.raycaster/Raycaster
      (blocked? [_ start end]
        (raycaster/blocked? arr start end))

      (path-blocked? [_ start target path-w]
        (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
          (or
           (raycaster/blocked? arr start1 target1)
           (raycaster/blocked? arr start2 target2))))

      (line-of-sight? [_ source target]
        (not (raycaster/blocked? arr
                                 (:body/position (:entity/body source))
                                 (:body/position (:entity/body target))))))))

(defn- world-ctx
  [{:keys [tiled-map] :as config}]
  (let [grid (grid-impl/create tiled-map)
        z-orders [:z-order/on-ground
                  :z-order/ground
                  :z-order/flying
                  :z-order/effect]
        ; so that at low fps the game doesn't jump faster between frames used @ movement to set a max speed so entities don't jump over other entities when checking collisions
        max-delta 0.04
        ; setting a min-size for colliding bodies so movement can set a max-speed for not
        ; skipping bodies at too fast movement
        ; TODO assert at properties load
        minimum-size 0.39 ; == spider smallest creature size.
        ; set max speed so small entities are not skipped by projectiles
        ; could set faster than max-speed if I just do multiple smaller movement steps in one frame
        max-speed (/ minimum-size max-delta)]
    {:ctx/grid grid
     :ctx/raycaster (create-raycaster grid)
     :ctx/elapsed-time 0
     :ctx/max-delta max-delta
     :ctx/max-speed max-speed
     :ctx/minimum-size minimum-size
     :ctx/z-orders z-orders}))

(defn- create-explored-tile-corners [tiled-map]
  (atom (g2d/create-grid (:tiled-map/width  tiled-map)
                         (:tiled-map/height tiled-map)
                         (constantly false))))

(defn- assoc-player-eid
  [{:keys [ctx/entity-ids]
    :as ctx}]
  (let [eid (get @entity-ids 1)]
    (assert (:entity/player? @eid))
    (assoc ctx :ctx/player-eid eid)))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db]
    :as ctx}
   start-position]
  (ctx/handle-txs! ctx
                   [[:tx/spawn-creature (let [{:keys [creature-id
                                                      components]} (:cdq.game/player-props config)]
                                          {:position (utils/tile->middle start-position)
                                           :creature-property (db/build db creature-id)
                                           :components components})]])
  ctx)

(defn- spawn-enemies!
  [{:keys [ctx/config
           ctx/db
           ctx/tiled-map]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property tiled-map "creatures" "id")]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature {:position (utils/tile->middle position)
                                           :creature-property (db/build db (keyword creature-id))
                                           :components (:cdq.game/enemy-components config)}]]))
  ctx)

; TODO dispose old ctx/tiled-map if already present
; TODO is this not a 'tx/' ???
; can I just [:tx/reset-game-state] somewhere ?
; tx.game/?
; then even at cdq.start ? just [:tx.app/] ?
; this is just a bunch of functions in the context of 'cdq.reset-game-state' ...
(defn do!
  [{:keys [ctx/config
           ctx/stage]
    :as ctx}
   world-fn]
  (stage/clear! stage)
  (doseq [actor-decl (map #(let [[f params] %]
                             ((requiring-resolve f) ctx params))
                          (:create-ui-actors config))]
    (stage/add! stage (scene2d/create actor-decl)))
  (let [world-config (merge (::world config)
                            (let [[f params] world-fn]
                              ((requiring-resolve f) ctx params)))
        world-ctx* (world-ctx world-config)]
    ;World data structure:
    ; * from tiled-map
    ; => grid, raycaster, explored-tile-corners, content-grid, potential-field-cache
    ; etc. id-counter, etc.
    ; => world protocol ???
    (-> ctx
        (merge world-ctx*)
        (assoc :ctx/tiled-map (:tiled-map world-config))
        (assoc :ctx/explored-tile-corners (create-explored-tile-corners (:tiled-map world-config)))
        (assoc :ctx/content-grid (content-grid/create (:tiled-map/width  (:tiled-map world-config))
                                                      (:tiled-map/height (:tiled-map world-config))
                                                      (:content-grid-cell-size world-config)))
        (assoc :ctx/potential-field-cache (atom nil))
        (assoc :ctx/factions-iterations (:potential-field-factions-iterations world-config))
        (assoc :ctx/id-counter (atom 0))
        (assoc :ctx/entity-ids (atom {}))
        (assoc :ctx/render-z-order (utils/define-order (:ctx/z-orders world-ctx*)))
        (spawn-player! (:start-position world-config))
        assoc-player-eid
        spawn-enemies!)))
