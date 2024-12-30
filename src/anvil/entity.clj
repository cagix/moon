(ns anvil.entity
  (:require [anvil.component :as component]
            [gdl.context :refer [set-cursor]]
            [gdl.math.vector :as v]
            [gdl.math.shapes :as shape]
            [reduce-fsm :as fsm]))

(defn direction [entity other-entity]
  (v/direction (:position entity) (:position other-entity)))

(defn collides? [entity other-entity]
  (shape/overlaps? entity other-entity))

(defn tile [entity]
  (mapv int (:position entity)))

(defn enemy [{:keys [entity/faction]}]
  (case faction
    :evil :good
    :good :evil))

(defn state-k [entity]
  (-> entity :entity/fsm :state))

(defn state-obj [entity]
  (let [k (state-k entity)]
    [k (k entity)]))

(defmethod component/cursor :stunned               [_] :cursors/denied)
(defmethod component/cursor :player-moving         [_] :cursors/walking)
(defmethod component/cursor :player-item-on-cursor [_] :cursors/hand-grab)
(defmethod component/cursor :player-dead           [_] :cursors/black-x)
(defmethod component/cursor :active-skill          [_] :cursors/sandclock)

(defn- send-event! [c eid event params]
  (when-let [fsm (:entity/fsm @eid)]
    (let [old-state-k (:state fsm)
          new-fsm (fsm/fsm-event fsm event)
          new-state-k (:state new-fsm)]
      (when-not (= old-state-k new-state-k)
        (let [old-state-obj (state-obj @eid)
              new-state-obj [new-state-k (component/->v (if params
                                                          [new-state-k eid params]
                                                          [new-state-k eid])
                                                        c)]]
          (when (:entity/player? @eid)
            (when-let [cursor (component/cursor new-state-obj)]
              (set-cursor c cursor)))
          (swap! eid #(-> %
                          (assoc :entity/fsm new-fsm
                                 new-state-k (new-state-obj 1))
                          (dissoc old-state-k)))
          (component/exit  old-state-obj c)
          (component/enter new-state-obj c))))))

(defn event
  ([c eid event]
   (send-event! c eid event nil))
  ([c eid event params]
   (send-event! c eid event params)))

(defn set-item [eid cell item])

(defn remove-item [c eid cell])

(defn stack-item [c eid cell item])

(defn can-pickup-item? [{:keys [entity/inventory]} item])

(defn pickup-item [c eid item])

(defn stat [entity k])

(defn mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity])

(defn mana-val [entity])

(defn pay-mana-cost [entity cost])

(defn mod-add    [entity mod])
(defn mod-remove [entity mod])
(defn mod-value  [base-value entity modifier-k])
(defn apply-max-modifier [val-max entity modifier-k])
(defn apply-min-modifier [val-max entity modifier-k])

(defn hitpoints
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [entity]
  (-> entity
      :entity/hp
      (apply-max-modifier entity :modifier/hp-max)))

(defn damage
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (apply-min-modifier source :modifier/damage-deal-min)
                (apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (damage source damage)
           :damage/min-max
           apply-max-modifier
           target
           :modifier/damage-receive-max)))
