(ns cdq.application.render.tick-entities
  (:require [cdq.ctx :as ctx]))

(def ^:private k->tick-fn
  (update-vals
   '{:entity/alert-friendlies-after-duration cdq.entity.alert-friendlies-after-duration/tick
     :entity/animation                       cdq.entity.animation/tick
     :entity/delete-after-duration           cdq.entity.delete-after-duration/tick
     :entity/movement                        cdq.entity.movement/tick
     :entity/projectile-collision            cdq.entity.projectile-collision/tick
     :entity/skills                          cdq.entity.skills/tick
     :active-skill                           cdq.entity.state.active-skill/tick
     :npc-idle                               cdq.entity.state.npc-idle/tick
     :npc-moving                             cdq.entity.state.npc-moving/tick
     :npc-sleeping                           cdq.entity.state.npc-sleeping/tick
     :stunned                                cdq.entity.state.stunned/tick
     :entity/string-effect                   cdq.entity.string-effect/tick
     :entity/temp-modifier                   cdq.entity.temp-modifier/tick}
   (fn [sym]
     (let [avar (requiring-resolve sym)]
       (assert avar sym)
       avar))))

(defn do!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (if (:world/paused? world)
    ctx
    (do (try
         (doseq [eid (:world/active-entities world)
                 [k v] @eid
                 :let [f (k->tick-fn k)]
                 :when f]
           (ctx/handle-txs! ctx (f v eid world)))
         (catch Throwable t
           (ctx/handle-txs! ctx [[:tx/print-stacktrace  t]
                                 [:tx/show-error-window t]])))
        ctx)))
