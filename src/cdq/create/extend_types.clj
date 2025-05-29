(ns cdq.create.extend-types
  (:require [cdq.cell :as cell]
            [cdq.content-grid :as content-grid]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [cdq.g.info]
            [cdq.g.player-movement-vector]
            [cdq.g.interaction-state]
            [cdq.g.spawn-entity]
            [cdq.g.spawn-creature]
            [cdq.g.handle-txs]
            [cdq.graphics :as graphics]
            [cdq.grid :as grid]
            [cdq.potential-fields.movement :as potential-fields.movement]
            [cdq.raycaster :as raycaster]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.error-window :as error-window]
            [cdq.vector2 :as v]
            [gdl.assets :as assets]
            [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.viewport :as viewport])
  (:import (cdq.game Context)))

(defn do! [ctx]
  ctx)

(extend-type Context
  g/MouseViewports
  (world-mouse-position [{:keys [ctx/graphics
                                 ctx/input]}]
    (viewport/unproject (:world-viewport graphics)
                        (input/mouse-position input)))

  (ui-mouse-position [{:keys [ctx/ui-viewport
                              ctx/input]}]
    (viewport/unproject ui-viewport
                        (input/mouse-position input))))

(extend-type Context
  g/Stage
  (mouseover-actor [{:keys [ctx/stage] :as ctx}]
    (ui/hit stage (g/ui-mouse-position ctx))))

(extend-type Context
  g/Context
  (context-entity-add! [{:keys [ctx/entity-ids
                                ctx/content-grid
                                ctx/grid]}
                        eid]
    (let [id (entity/id @eid)]
      (assert (number? id))
      (swap! entity-ids assoc id eid))
    (content-grid/add-entity! content-grid eid)
    ; https://github.com/damn/core/issues/58
    ;(assert (valid-position? grid @eid)) ; TODO deactivate because projectile no left-bottom remove that field or update properly for all
    (grid/add-entity! grid eid))

  (context-entity-remove! [{:keys [ctx/entity-ids
                                   ctx/grid]}
                           eid]
    (let [id (entity/id @eid)]
      (assert (contains? @entity-ids id))
      (swap! entity-ids dissoc id))
    (content-grid/remove-entity! eid)
    (grid/remove-entity! grid eid))

  (context-entity-moved! [{:keys [ctx/content-grid
                                  ctx/grid]}
                          eid]
    (content-grid/position-changed! content-grid eid)
    (grid/position-changed! grid eid)))

(extend-type Context
  g/StageActors
  (open-error-window! [{:keys [ctx/stage]} throwable]
    (ui/add! stage (error-window/create throwable)))

  (selected-skill [{:keys [ctx/stage]}]
    (action-bar/selected-skill (:action-bar stage))))

(extend-type Context
  cdq.g/Grid
  (nearest-enemy-distance [{:keys [ctx/grid]} entity]
    (cell/nearest-entity-distance @(grid/cell grid (mapv int (entity/position entity)))
                                  (entity/enemy entity)))

  (nearest-enemy [{:keys [ctx/grid]} entity]
    (cell/nearest-entity @(grid/cell grid (mapv int (entity/position entity)))
                         (entity/enemy entity)))

  (potential-field-find-direction [{:keys [ctx/grid]} eid]
    (potential-fields.movement/find-direction grid eid)))

(extend-type Context
  g/Graphics
  (sprite [{:keys [ctx/assets] :as ctx} texture-path] ; <- textures should be inside graphics, makes this easier.
    (graphics/sprite (:ctx/graphics ctx)
                     (assets/texture assets texture-path)))

  (sub-sprite [ctx sprite [x y w h]]
    (graphics/sub-sprite (:ctx/graphics ctx)
                         sprite
                         [x y w h]))

  (sprite-sheet [{:keys [ctx/assets] :as ctx} texture-path tilew tileh]
    (graphics/sprite-sheet (:ctx/graphics ctx)
                           (assets/texture assets texture-path)
                           tilew
                           tileh))

  (sprite-sheet->sprite [ctx sprite-sheet [x y]]
    (graphics/sprite-sheet->sprite (:ctx/graphics ctx)
                                   sprite-sheet
                                   [x y])))

(extend-type Context
  g/EffectContext
  (player-effect-ctx [{:keys [ctx/mouseover-eid]
                       :as ctx}
                      eid]
    (let [target-position (or (and mouseover-eid
                                   (entity/position @mouseover-eid))
                              (g/world-mouse-position ctx))]
      {:effect/source eid
       :effect/target mouseover-eid
       :effect/target-position target-position
       :effect/target-direction (v/direction (entity/position @eid) target-position)}))

  (npc-effect-ctx [ctx eid]
    (let [entity @eid
          target (g/nearest-enemy ctx entity)
          target (when (and target
                            (g/line-of-sight? ctx entity @target))
                   target)]
      {:effect/source eid
       :effect/target target
       :effect/target-direction (when target
                                  (v/direction (entity/position entity)
                                               (entity/position @target)))})))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [graphics position]
  (let [[x y] position
        x (float x)
        y (float y)
        [cx cy] (graphics/camera-position graphics)
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (graphics/world-viewport-width graphics))  2)))
     (<= ydist (inc (/ (float (graphics/world-viewport-height graphics)) 2))))))

; TODO at wrong point , this affects targeting logic of npcs
; move the debug flag to either render or mouseover or lets see
(def ^:private ^:dbg-flag los-checks? true)

(extend-type Context
  g/LineOfSight
  ; does not take into account size of entity ...
  ; => assert bodies <1 width then
  (line-of-sight? [{:keys [ctx/graphics
                           ctx/raycaster]}
                   source
                   target]
    (and (or (not (:entity/player? source))
             (on-screen? graphics (entity/position target)))
         (not (and los-checks?
                   (raycaster/blocked? raycaster
                                       (entity/position source)
                                       (entity/position target)))))))
(extend-type Context
  g/InfoText
  (info-text [ctx object]
    (cdq.g.info/text ctx object)))

(extend-type Context
  g/PlayerMovementInput
  (player-movement-vector [{:keys [ctx/input]}]
    (cdq.g.player-movement-vector/WASD-movement-vector input)))

(extend-type Context
  g/InteractionState
  (interaction-state [ctx eid]
    (cdq.g.interaction-state/create ctx eid)))

(extend-type Context
  g/SpawnEntity
  (spawn-entity! [ctx
                  position
                  body
                  components]
    (cdq.g.spawn-entity/spawn-entity! ctx position body components)))

(extend-type Context
  g/EffectHandler
  (handle-txs! [ctx transactions]
    (doseq [transaction transactions
            :when transaction
            :let [_ (assert (vector? transaction)
                            (pr-str transaction))
                  ; TODO also should be with namespace 'tx' the first is a keyword
                  ]]
      (try (cdq.g.handle-txs/handle-tx! transaction ctx)
           (catch Throwable t
             (throw (ex-info "" {:transaction transaction} t)))))))

(extend-type Context
  g/Creatures
  (spawn-creature! [ctx opts]
    (cdq.g.spawn-creature/spawn-creature! ctx opts)))
