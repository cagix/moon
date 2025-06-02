(ns clojure.entity.state.npc-idle
  (:require [clojure.effect :as effect]
            [clojure.entity :as entity]
            [clojure.ctx :as ctx]
            [clojure.state :as state]
            [clojure.utils :refer [defcomponent]]))

(defn- npc-choose-skill [ctx entity effect-ctx]
  (->> entity
       :entity/skills
       vals
       (sort-by #(or (:skill/cost %) 0))
       reverse
       (filter #(and (= :usable (entity/skill-usable-state entity % effect-ctx))
                     (effect/applicable-and-useful? ctx effect-ctx (:skill/effects %))))
       first))

(defcomponent :npc-idle
  (entity/tick! [_ eid ctx]
    (let [effect-ctx (ctx/npc-effect-ctx ctx eid)]
      (if-let [skill (npc-choose-skill ctx @eid effect-ctx)]
        [[:tx/event eid :start-action [skill effect-ctx]]]
        [[:tx/event eid :movement-direction (or (ctx/potential-field-find-direction ctx eid)
                                                [0 0])]]))))
