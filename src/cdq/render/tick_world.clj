(ns cdq.render.tick-world
  (:require [cdq.app :as app]
            [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.graphics :as g]
            [cdq.potential-fields.update :as potential-fields.update]
            [cdq.ui.error-window :as error-window]
            [cdq.ui.stage :as stage]
            [cdq.w :as w]
            cdq.entity.alert-friendlies-after-duration
            cdq.entity.animation
            cdq.entity.delete-after-animation-stopped
            cdq.entity.delete-after-duration
            cdq.entity.movement
            cdq.entity.projectile-collision
            cdq.entity.skills
            cdq.entity.state.active-skill
            cdq.entity.state.npc-idle
            cdq.entity.state.npc-moving
            cdq.entity.state.npc-sleeping
            cdq.entity.state.stunned
            cdq.entity.string-effect
            cdq.entity.temp-modifier))

(defn- update-time [{:keys [ctx/graphics
                            ctx/world]
                     :as ctx}]
  (update ctx :ctx/world w/update-time (g/delta-time graphics)))

(defn- tick-potential-fields!
  [{:keys [world/factions-iterations
           world/potential-field-cache
           world/grid
           world/active-entities]}]
  (doseq [[faction max-iterations] factions-iterations]
    (potential-fields.update/tick! potential-field-cache
                                   grid
                                   faction
                                   active-entities
                                   max-iterations)))

(defn- update-potential-fields!
  [{:keys [ctx/world]
    :as ctx}]
  (tick-potential-fields! world)
  ctx)

(def ^:private entity->tick
  {:entity/alert-friendlies-after-duration cdq.entity.alert-friendlies-after-duration/tick!
   :entity/animation cdq.entity.animation/tick!
   :entity/delete-after-animation-stopped? cdq.entity.delete-after-animation-stopped/tick!
   :entity/delete-after-duration cdq.entity.delete-after-duration/tick!
   :entity/movement cdq.entity.movement/tick!
   :entity/projectile-collision cdq.entity.projectile-collision/tick!
   :entity/skills cdq.entity.skills/tick!
   :active-skill cdq.entity.state.active-skill/tick!
   :npc-idle cdq.entity.state.npc-idle/tick!
   :npc-moving cdq.entity.state.npc-moving/tick!
   :npc-sleeping cdq.entity.state.npc-sleeping/tick!
   :stunned cdq.entity.state.stunned/tick!
   :entity/string-effect cdq.entity.string-effect/tick!
   :entity/temp-modifier cdq.entity.temp-modifier/tick!})

(defn- tick-entity! [{:keys [ctx/world] :as ctx} eid]
  (doseq [k (keys @eid)]
    (try (when-let [v (k @eid)]
           (ctx/handle-txs! ctx (when-let [f (entity->tick k)]
                                  (f v eid world))))
         (catch Throwable t
           (throw (ex-info "entity-tick"
                           {:k k
                            :entity/id (entity/id @eid)}
                           t))))))
(defn- tick-entities!
  [{:keys [ctx/app
           ctx/stage
           ctx/world]
    :as ctx}]
  (try
   (doseq [eid (:world/active-entities world)]
     (tick-entity! ctx eid))
   (catch Throwable t
     (app/pretty-pst app t)
     (stage/add! stage (error-window/create t))
     #_(bind-root ::error t)))
  ctx)

(defn do! [ctx]
  (if (get-in ctx [:ctx/world :world/paused?])
    ctx
    (-> ctx
        update-time
        update-potential-fields!
        tick-entities!)))
