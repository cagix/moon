(ns cdq.render.assoc-interaction-state
  (:require [cdq.creature :as creature]
            [cdq.entity :as entity]
            [com.badlogic.gdx.math.vector2 :as v]
            [cdq.stage :as stage]))

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
    (stage/actor-information stage mouseover-actor)]

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
