(ns cdq.render.assoc-interaction-state
  (:require [cdq.creature :as creature]
            [cdq.entity :as entity]
            [cdq.gdx.math.vector2 :as v]
            [cdq.stage :as stage]
            [cdq.ui.windows.inventory :as inventory-window]
            [clojure.gdx.scene2d.ui.button :as button]
            [clojure.vis-ui.window :as window]))

(defn- distance [a b]
  (v/distance (entity/position a)
              (entity/position b)))

(defn- in-click-range? [player-entity clicked-entity]
  (< (distance player-entity clicked-entity)
     (:entity/click-distance-tiles player-entity)))

(defn- player-effect-ctx [mouseover-eid world-mouse-position player-eid]
  (let [target-position (or (and mouseover-eid
                                 (entity/position @mouseover-eid))
                            world-mouse-position)]
    {:effect/source player-eid
     :effect/target mouseover-eid
     :effect/target-position target-position
     :effect/target-direction (v/direction (entity/position @player-eid) target-position)}))

(defn- interaction-state
  [{:keys [ctx/mouseover-actor
           ctx/stage
           ctx/world-mouse-position]}
   mouseover-eid
   player-eid]
  (cond
   mouseover-actor
   [:interaction-state/mouseover-actor
    (let [actor mouseover-actor]
      (let [inventory-slot (inventory-window/cell-with-item? actor)]
        (cond
         inventory-slot            [:mouseover-actor/inventory-cell inventory-slot]
         (window/title-bar? actor) [:mouseover-actor/window-title-bar]
         (button/is?        actor) [:mouseover-actor/button]
         :else                     [:mouseover-actor/unspecified])))]

   (and mouseover-eid
        (:entity/clickable @mouseover-eid))
   [:interaction-state/clickable-mouseover-eid
    {:clicked-eid mouseover-eid
     :in-click-range? (in-click-range? @player-eid @mouseover-eid)}]

   :else
   (if-let [skill-id (stage/action-bar-selected-skill stage)]
     (let [entity @player-eid
           skill (skill-id (:entity/skills entity))
           effect-ctx (player-effect-ctx mouseover-eid world-mouse-position player-eid)
           state (creature/skill-usable-state entity skill effect-ctx)]
       (if (= state :usable)
         [:interaction-state.skill/usable [skill effect-ctx]]
         [:interaction-state.skill/not-usable state]))
     [:interaction-state/no-skill-selected])))

(defn do! [ctx]
  (assoc ctx :ctx/interaction-state (interaction-state ctx
                                                       (:world/mouseover-eid (:ctx/world ctx))
                                                       (:world/player-eid (:ctx/world ctx)))))
