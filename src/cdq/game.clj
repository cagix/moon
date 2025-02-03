; what are the 'docking APIs' ?
; - schema
; - effects
; - entity/states
; - world generation
; - what is the API for something?
; -> schemas part of db
; -> db part of world?
; schemas comes from 'propertyies' ...


(ns cdq.game ; game = context = application ?
  (:require cdq.application
            [cdq.create.assets :as assets]
            [cdq.create.batch :as batch]
            [cdq.create.cursors :as cursors]
            [cdq.create.default-font :as default-font]
            [cdq.create.db :as db]
            cdq.create.effects
            cdq.create.entity-components
            [cdq.create.schemas :as schemas]
            [cdq.create.shape-drawer :as shape-drawer]
            [cdq.create.shape-drawer-texture :as shape-drawer-texture]
            [cdq.create.stage :as stage]
            [cdq.create.tiled-map-renderer :as tiled-map-renderer]
            [cdq.create.ui-viewport :as ui-viewport]
            [cdq.create.world-unit-scale :as world-unit-scale]
            [cdq.create.world-viewport :as world-viewport]
            [cdq.context :as context]
            [cdq.entity :as entity]
            [cdq.fsm :as fsm]
            [cdq.graphics.animation :as animation]
            [cdq.inventory :as inventory]
            [cdq.widgets.inventory :as widgets.inventory]
            [cdq.world :as world]
            cdq.world.context
            [clojure.gdx.utils :as utils]
            [clojure.gdx.utils.viewport :as viewport]
            [clojure.utils :refer [safe-merge]]))

(def context-keyset
  ; also :unit-scale added for rendering ..
  #{:cdq/assets                                             ; all sounds and textures in resources/
    :cdq/db                                                 ; different properties (item, creature, projectile, world, skill, ? )
    :cdq/effects                                            ; the effects implementations (skill contains effects )
    :cdq/schemas                                            ; the schemas of the stuff
    :cdq.context/content-grid

    ; delta-time gets added after first frame
    :cdq.context/delta-time

    :cdq.context/elapsed-time
    :cdq.context/entity-ids
    :cdq.context/error
    :cdq.context/explored-tile-corners
    :cdq.context/factions-iterations
    :cdq.context/grid
    :cdq.context/level
    :cdq.context/mouseover-eid
    :cdq.context/paused?
    :cdq.context/player-eid
    :cdq.context/player-message
    :cdq.context/raycaster
    :cdq.context/stage
    :cdq.context/tiled-map
    :cdq.game/active-entities
    :cdq.graphics/batch
    :cdq.graphics/cursors
    :cdq.graphics/default-font
    :cdq.graphics/shape-drawer
    :cdq.graphics/shape-drawer-texture
    :cdq.graphics/tiled-map-renderer
    :cdq.graphics/ui-viewport
    :cdq.graphics/world-unit-scale
    :cdq.graphics/world-viewport
    :context/entity-components
    :world/potential-field-cache}
  )

(comment
 (clojure.pprint/pprint (sort (keys @state)))

 (= context-keyset (set (keys @state)))
 ; => validate each frame !
 ; => after each render/effect ?
 )

(defrecord Game []
  cdq.application/Game
  (create [_]
    (let [schemas (schemas/create)
          batch (batch/create)
          shape-drawer-texture (shape-drawer-texture/create)
          world-unit-scale (world-unit-scale/create)
          ui-viewport (ui-viewport/create)
          context (map->Game
                   {:cdq/assets (assets/create)
                    ;; graphics
                    :cdq.graphics/batch batch
                    :cdq.graphics/cursors (cursors/create)
                    :cdq.graphics/default-font (default-font/create)
                    :cdq.graphics/shape-drawer (shape-drawer/create batch shape-drawer-texture)
                    :cdq.graphics/shape-drawer-texture shape-drawer-texture
                    :cdq.graphics/tiled-map-renderer (tiled-map-renderer/create batch world-unit-scale)
                    :cdq.graphics/ui-viewport ui-viewport
                    :cdq.graphics/world-unit-scale world-unit-scale
                    :cdq.graphics/world-viewport (world-viewport/create world-unit-scale)
                    ;;
                    :cdq/db (db/create schemas)
                    :context/entity-components (cdq.create.entity-components/create)
                    :cdq/schemas schemas                    ; part of db?
                    :cdq.context/stage (stage/create batch ui-viewport)}) ]
      ; TODO here need to create itself !
      (cdq.world.context/reset context :worlds/vampire)))

  (dispose [context]
    (doseq [[_k value] context
            :when (utils/disposable? value)]
      (utils/dispose value)))

  (render [context]
    (reduce (fn [context f]
              (f context))
            context
            (for [ns-sym '[cdq.render.assoc-active-entities
                           cdq.render.set-camera-on-player
                           cdq.render.clear-screen
                           cdq.render.tiled-map
                           cdq.render.draw-on-world-view
                           cdq.render.stage
                           cdq.render.player-state-input
                           cdq.render.update-mouseover-entity
                           cdq.render.update-paused
                           cdq.render.when-not-paused

                           ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                           cdq.render.remove-destroyed-entities

                           cdq.render.camera-controls
                           cdq.render.window-controls]]
              (do
               (require ns-sym)
               (resolve (symbol (str ns-sym "/render")))))))

  (resize [context width height]
    (viewport/update (:cdq.graphics/ui-viewport    context) width height :center-camera? true)
    (viewport/update (:cdq.graphics/world-viewport context) width height))
  )

(defn -main []
  (cdq.application/start (->Game)))

; so the body is part of the game/context/application/world

(defrecord Body [position
                 left-bottom
                 width
                 height
                 half-width
                 half-height
                 radius
                 collides?
                 z-order
                 rotation-angle])

(defn- create-body [{[x y] :position
                     :keys [position
                            width
                            height
                            collides?
                            z-order
                            rotation-angle]}]
  (assert position)
  (assert width)
  (assert height)
  (assert (>= width  (if collides? world/minimum-size 0)))
  (assert (>= height (if collides? world/minimum-size 0)))
  (assert (or (boolean? collides?) (nil? collides?)))
  (assert ((set world/z-orders) z-order))
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

(defn- create-vs [components context]
  (reduce (fn [m [k v]]
            (assoc m k (entity/create [k v] context)))
          {}
          components))

(defmulti create! (fn [[k] eid c]
                    k))
(defmethod create! :default [_ eid c])

(def id-counter (atom 0))

(extend-type Game
  world/World
  (spawn-entity [context position body components]
    (assert (and (not (contains? components :position))
                 (not (contains? components :entity/id))))
    (let [eid (atom (-> body
                        (assoc :position position)
                        create-body
                        (safe-merge (-> components
                                        (assoc :entity/id (swap! id-counter inc))
                                        (create-vs context)))))]
      (doseq [component context]
        (context/add-entity component eid))
      (doseq [component @eid]
        (create! component eid context))
      eid)))

(defmethod create! :entity/inventory
  [[k items] eid c]
  (swap! eid assoc k inventory/empty-inventory)
  (doseq [item items]
    (widgets.inventory/pickup-item c eid item)))

(defmethod create! :entity/skills
  [[k skills] eid c]
  (swap! eid assoc k nil)
  (doseq [skill skills]
    (world/add-skill c eid skill)))

(defmethod create! :entity/animation
  [[_ animation] eid c]
  (swap! eid assoc :entity/image (animation/current-frame animation)))

(defmethod create! :entity/delete-after-animation-stopped?
  [_ eid c]
  (-> @eid :entity/animation :looping? not assert))

(defmethod create! :entity/fsm
  [[k {:keys [fsm initial-state]}] eid c]
  (swap! eid assoc
         k (fsm/create fsm initial-state)
         initial-state (entity/create [initial-state eid] c)))

