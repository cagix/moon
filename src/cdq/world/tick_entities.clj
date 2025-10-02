(ns cdq.world.tick-entities)

(defn- tick-entities!*
  [{:keys [world/active-entities]
    :as world}
   k->tick-fn]
  (mapcat (fn [eid]
            (mapcat (fn [[k v]]
                      (try (when-let [f (k->tick-fn k)]
                             (f v eid world))
                           (catch Throwable t
                             (throw (ex-info "" {:eid eid} t)))))
                    @eid))
          active-entities))

(comment
 (= (tick-entities!* {:world/active-entities [(atom {:firstk :foo
                                                     :secondk :bar})
                                              (atom {:firstk2 :foo2
                                                     :secondk2 :bar2})]}
                     {:firstk (fn [v eid world]
                                [[:foo/bar]])
                      :secondk (fn [v eid world]
                                 [[:foo/barz]
                                  [:asdf]])
                      :firstk2 (fn [v eid world]
                                 nil)
                      :secondk2 (fn [v eid world]
                                  [[:asdf2] [:asdf3]])})
    [[:foo/bar]
     [:foo/barz]
     [:asdf]
     [:asdf2]
     [:asdf3]])
 )

(def ^:private k->tick-fn
  (update-vals '{:entity/alert-friendlies-after-duration cdq.entity.alert-friendlies-after-duration.tick/txs
                 :entity/animation                       cdq.entity.animation.tick/txs
                 :entity/delete-after-duration           cdq.entity.delete-after-duration.tick/txs
                 :entity/movement                        cdq.entity.movement.tick/txs
                 :entity/projectile-collision            cdq.entity.projectile-collision.tick/txs
                 :entity/skills                          cdq.entity.skills.tick/txs
                 :active-skill                           cdq.entity.state.active-skill.tick/txs
                 :npc-idle                               cdq.entity.state.npc-idle.tick/txs
                 :npc-moving                             cdq.entity.state.npc-moving.tick/txs
                 :npc-sleeping                           cdq.entity.state.npc-sleeping.tick/txs
                 :stunned                                cdq.entity.state.stunned.tick/txs
                 :entity/string-effect                   cdq.entity.string-effect.tick/txs
                 :entity/temp-modifier                   cdq.entity.temp-modifier.tick/txs}
               requiring-resolve))

(defn do! [world]
  (tick-entities!* world k->tick-fn))
