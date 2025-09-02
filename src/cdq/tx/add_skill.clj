(ns cdq.tx.add-skill
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.graphics :as graphics]
            [cdq.ui.action-bar :as action-bar]
            [cdq.world.entity :as entity]))

(defn- add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (-> stage
      :action-bar
      (action-bar/add-skill! {:skill-id (:property/id skill)
                              :texture-region (graphics/image->texture-region graphics (:entity/image skill))
                              ; (assoc ctx :effect/source (world/player)) FIXME
                              :tooltip-text #(ctx/info-text % skill)}))
  nil)

#_(defn- remove-skill! [{:keys [ctx/stage]} skill]
    (-> stage
        :action-bar
        (action-bar/remove-skill! (:property/id skill)))
    nil)

(defn do! [[_ eid skill] ctx]
  (swap! eid entity/add-skill skill)
  (when (:entity/player? @eid)
    (add-skill! ctx skill))
  nil)

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (swap! eid update :entity/skills dissoc id)
    (when (:entity/player? @eid)
      (remove-skill! ctx skill)))
