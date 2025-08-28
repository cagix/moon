(ns cdq.render.tick-entities
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.ui.error-window :as error-window]
            [cdq.app :as app]
            [cdq.ui.stage :as stage]
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

(defn do!
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
